package com.github.ahnfelt.topshell.worker

import com.github.ahnfelt.topshell.Main
import com.github.ahnfelt.topshell.language._
import org.scalajs.dom.raw.DedicatedWorkerGlobalScope

import scala.scalajs.js

object Processor {

    def start(fromLine : Int, toLine : Int) : Unit = {
        Block.globalRunLines(fromLine, toLine)
    }

    def emit(escapedName : String, value : js.Any, error : js.Any) : Unit = {
        val currentVersion = Main.codeVersion // TODO
        val name = if(escapedName.endsWith("_")) escapedName.dropRight(1) else escapedName
        if(Main.codeVersion == currentVersion) {
            val message = if(!js.isUndefined(error) && error != null) {
                js.Dictionary("event" -> "error", "name" -> name, "error" -> ("" + error), "codeVersion" -> currentVersion)
            } else {
                val html = js.Dynamic.global.tsh.toHtml(value)
                js.Dictionary("event" -> "result", "name" -> name, "html" -> html, "codeVersion" -> currentVersion)
            }
            DedicatedWorkerGlobalScope.self.postMessage(message)
        }
    }

    def process(code : String) : Unit = Timer.accumulate("process") {
        val currentVersion = Main.codeVersion

        val tokens = Timer.accumulate("tokenize") {
            js.Dynamic.global.tsh.tokenize("Unnamed.tsh", code).asInstanceOf[js.Array[Token]]
        }
        val (newImports, newSymbols) = Timer.accumulate("parse") {
            new Parser("Unnamed.tsh", tokens.toArray[Token]).parseTopLevel()
        }
        val topImports = Timer.accumulate("topImports") {
            UsedImports.completeImports(newSymbols, newImports)
        }
        val untypedTopSymbols = Timer.accumulate("check") {
            Checker.check(topImports, newSymbols)
        }
        val topSymbols = Timer.accumulate("type") {
            val coreModules =
                js.Dynamic.global.tsh.coreModules.asInstanceOf[js.Dictionary[js.Array[ModuleSymbol]]].toMap
            new Typer().check(coreModules.mapValues(_.toList), topImports, untypedTopSymbols)
        }
        val emitted = Timer.accumulate("emit") {
            Emitter.emit(currentVersion, topImports, topSymbols)
        }
        //println(emitted)

        Timer.accumulate("cache") {
            val names = topImports.map(_.name) ++ topSymbols.map(_.binding.name)

            val _g = DedicatedWorkerGlobalScope.self
            val _d = emit : js.Function3[String, js.Any, js.Any, Unit]

            val newBlocks = js.Dynamic.newInstance(js.Dynamic.global.Function)("_g", "_d", emitted)(_g, _d)

            val oldBlocks = Block.globalBlocks.map(b => b.name -> b).toMap

            Block.globalStart = Set.empty
            Block.globalBlocks = newBlocks.asInstanceOf[js.Array[Block]]

            val topBlockMap = (topSymbols ++ topImports).map(s => s.name -> s).toMap
            Timer.accumulate("cacheKey") { for(block <- Block.globalBlocks) {
                block.cacheKey = CacheKey.cacheKey(topBlockMap(block.name.dropRight(1)), topBlockMap, Set())
            } }

            val usedOldBlocks = Timer.accumulate("usedOldBlocks") { for {
                (block, index) <- Block.globalBlocks.zipWithIndex
                oldBlock <- oldBlocks.get(block.name)
                if oldBlock.cacheKey == block.cacheKey
            } yield Timer.accumulate("usedOldBlocks.iteration") {
                Block.globalBlocks(index) = oldBlock
                // Set done results in the new scope and redirect future results and reads to the new scope
                oldBlock.state.toOption match {
                    case Some(Block.Done(result)) => block.setResult(result)
                    case _ =>
                }
                oldBlock.asInstanceOf[js.Dynamic].setResult = block.asInstanceOf[js.Dynamic].setResult
                oldBlock.asInstanceOf[js.Dynamic].compute = block.asInstanceOf[js.Dynamic].compute
                Timer.accumulate("usedOldBlocks.sendBlockStatus") { Block.sendBlockStatus(oldBlock) }
                oldBlock.name
            } }

            for (block <- oldBlocks.values if !usedOldBlocks.contains(block.name)) {
                block.cancel.foreach(f => f())
            }

            val typePairs = topSymbols.map(_.binding).map(b =>
                b.name -> b.scheme.map(_.toString).getOrElse("")
            )
            val message = js.Dictionary(
                "event" -> "symbols",
                "symbols" -> js.Array(names : _*),
                "types" -> js.Dictionary(typePairs.filter(_._2.nonEmpty) : _*),
                "implied" -> js.Array((topImports.map(_.name).toSet -- newImports.map(_.name)).toSeq : _*),
                "cached" -> js.Array(usedOldBlocks.map(_.dropRight(1)) : _*),
                "codeVersion" -> currentVersion
            )
            DedicatedWorkerGlobalScope.self.postMessage(message)
        }

        Timer.accumulate("globalStepAll") {
            Block.globalStepAll()
        }

        Timer.printAndClearAccumulated()

    }

}
