package com.github.ahnfelt.topshell.worker

import com.github.ahnfelt.topshell.Main
import com.github.ahnfelt.topshell.language.Tokenizer.ParseException
import com.github.ahnfelt.topshell.language._
import org.scalajs.dom.raw.DedicatedWorkerGlobalScope

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.typedarray.Uint8ClampedArray

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
                val html = js.Dynamic.global.asInstanceOf[js.Dynamic].tsh.toHtml(value)
                js.Dictionary("event" -> "result", "name" -> name, "html" -> html, "codeVersion" -> currentVersion)
            }
            DedicatedWorkerGlobalScope.self.postMessage(message)
        }
    }

    def process(code : String) : Unit = {
        val currentVersion = Main.codeVersion

        val tokens = Tokenizer.tokenize("Unnamed.tsh", code)
        val (newImports, newSymbols) = new Parser("Unnamed.tsh", tokens).parseTopLevel()
        val topImports = UsedImports.completeImports(newSymbols, newImports)
        val topSymbols = Checker.check(topImports, newSymbols)
        val emitted = Emitter.emit(currentVersion, topImports, topSymbols)

        val names = topImports.map(_.name) ++ topSymbols.map(_.binding.name)
        val message = js.Dictionary(
            "event" -> "symbols",
            "symbols" -> js.Array(names : _*),
            "implied" -> js.Array((topImports.map(_.name).toSet -- newImports.map(_.name)).toSeq : _*),
            "codeVersion" -> currentVersion
        )
        DedicatedWorkerGlobalScope.self.postMessage(message)

        val _g = DedicatedWorkerGlobalScope.self
        val _d = emit : js.Function3[String, js.Any, js.Any, Unit]

        val symbols = js.Dynamic.newInstance(js.Dynamic.global.Function)("_g", "_d", emitted)(_g, _d)

        val oldBlocks = Block.globalBlocks.map(b => b.name -> b).toMap

        Block.globalStart = Set.empty
        Block.globalBlocks = symbols.selectDynamic("_blocks").asInstanceOf[js.Array[Block]]

        val topBlockMap = (topSymbols ++ topImports).map(s => s.name -> s).toMap
        for(block <- Block.globalBlocks) {
            block.cacheKey = CacheKey.cacheKey(topBlockMap(block.name.dropRight(1)), topBlockMap, Set())
        }

        val usedOldBlocks = for {
            (block, index) <- Block.globalBlocks.zipWithIndex
            oldBlock <- oldBlocks.get(block.name)
            if oldBlock.cacheKey == block.cacheKey
        } yield {
            Block.globalBlocks(index) = oldBlock
            // Set done results in the new scope and redirect future results to the new scope
            if(oldBlock.state.exists(_.isInstanceOf[Block.Done])) {
                block.setResult(oldBlock.result)
            }
            oldBlock.asInstanceOf[js.Dynamic].setResult = block.asInstanceOf[js.Dynamic].setResult
            Block.sendBlockStatus(oldBlock)
            oldBlock.name
        }

        for(block <- oldBlocks.values if !usedOldBlocks.contains(block.name)) {
            block.cancel.foreach(f => f())
        }

        Block.globalStepAll()

    }

    case class AbortedException() extends RuntimeException("Aborted due to new version of the code")

}
