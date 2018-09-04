package com.github.ahnfelt.topshell.worker

import com.github.ahnfelt.topshell.Main
import com.github.ahnfelt.topshell.language.Syntax.TopSymbol
import com.github.ahnfelt.topshell.worker.Block.BlockState
import org.scalajs.dom.raw.DedicatedWorkerGlobalScope

import scala.annotation.tailrec
import scala.scalajs.js

@js.native
trait Block extends js.Any {
    val name : String
    val module : Boolean
    val effect : Boolean
    val fromLine : Int
    val toLine : Int
    val dependencies : js.Array[String]
    val error : js.UndefOr[String]
    val compute : js.UndefOr[js.Function1[js.Any, js.Dynamic]]
    var state : js.UndefOr[BlockState]
    var cancel : js.UndefOr[js.Function0[Unit]]
    var result : js.UndefOr[js.Dynamic]
}

object Block {

    sealed abstract class BlockState
    case class Pending(awaiting : js.Array[String]) extends BlockState
    case class Computing() extends BlockState
    case class Runnable(task : js.Dynamic) extends BlockState
    case class Running(cancel : js.UndefOr[js.Function0[Unit]]) extends BlockState
    case class Error(message : String) extends BlockState
    case class Done(result : js.Dynamic) extends BlockState

    var globalBlocks = js.Array[Block]()
    var globalStart = Set[String]()

    def globalRunLines(fromLine : Int, toLine : Int) : Unit = {
        var start = Set[String]()
        for(block <- globalBlocks) {
            if(block.fromLine <= toLine && block.toLine >= fromLine) {
                start += block.name
                resetBlock(block, globalBlocks)
            }
        }
        if(start.nonEmpty) {
            globalStart ++= start
            globalStepAll()
        }
    }

    def resetBlock(block : Block, blocks : js.Array[Block]) : Unit = {
        globalStart -= block.name
        block.cancel.foreach(f => f())
        block.result = js.undefined
        block.state = Pending(block.dependencies)
        sendBlockStatus(block)
        for(b <- blocks if b.fromLine > block.toLine && b.dependencies.contains(block.name)) {
            resetBlock(b, blocks)
        }
    }

    def globalStepAll() : Unit = {
        stepAll(globalBlocks, globalStart)
    }

    @tailrec
    def stepAll(blocks : js.Array[Block], start : Set[String]) : Unit = {
        val done = blocks.filter(_.state.isInstanceOf[Done]).map(_.name).toSet
        val symbols = js.Dynamic.global.tsh.symbols
        var stepped = false
        for(block <- blocks) {
            val steppedBlock = step(block, done, start, symbols)
            if(steppedBlock) sendBlockStatus(block)
            stepped = stepped || steppedBlock
        }
        if(stepped) stepAll(blocks, start)
    }


    def step(block : Block, done : Set[String], start : Set[String], symbols : js.Any) : Boolean = {

        val initializeState = js.isUndefined(block.state)
        if(initializeState) {
            block.state =
                if(!js.isUndefined(block.error)) Error("" + block.error.get)
                else Pending(block.dependencies)
        }

        val stepped = block.state.get match {

            case Pending(awaiting) =>
                val remaining : js.Array[String] = awaiting -- done
                block.state = Pending(remaining)
                if(remaining.isEmpty && block.error.isEmpty) {
                    block.state = Computing()
                    val result = block.compute.get.apply(symbols)
                    if(block.effect && !js.isUndefined(result._run)) {
                        block.state = Runnable(result)
                    } else {
                        block.result = result
                        block.state = Done(result)
                    }
                }
                remaining.toSet != awaiting.toSet

            case Computing() =>
                false

            case Runnable(task) if start(block.name) || block.module =>
                block.cancel = task._run(
                    js.Dictionary(),
                    { v : js.Dynamic =>
                        block.result = v
                        block.state = Done(v)
                        sendBlockStatus(block)
                        js.Dynamic.global.setTimeout((() => globalStepAll()) : js.Function0[Unit], 0)
                    },
                    { e : js.Dynamic =>
                        val message = "" + e
                        block.state = Error(message)
                        sendBlockStatus(block)
                    }
                ).asInstanceOf[js.UndefOr[js.Function0[Unit]]]
                block.state = Running(block.cancel)
                true

            case Runnable(_) =>
                false

            case Running(_) =>
                false

            case Error(_) =>
                false

            case Done(_) =>
                false

        }

        initializeState || stepped
    }

    def sendBlockStatus(block : Block) : Unit = {
        val key = block.state.get.toString.takeWhile(_ != '(')
        val result : js.Any = block.state.get match {
            case Pending(awaiting) => status(key, awaiting.map(_.dropRight(1)).join(", "))
            case Computing() => status(key, "")
            case Runnable(_) => status(key, "")
            case Running(_) => status(key, "")
            case Error(message) => status(key, message)
            case Done(r) => r
        }
        send(block.name, result, block.error)
    }

    def send(escapedName : String, value : js.Any, error : js.Any) : Unit = {
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

    def status(key : String, value : String) : js.Any = {
        js.Dynamic.newInstance(js.Dynamic.global.tsh.Tag)(js.Dictionary(
            "_tag" -> ">status",
            "key" -> key,
            "value" -> value
        ))
    }

}