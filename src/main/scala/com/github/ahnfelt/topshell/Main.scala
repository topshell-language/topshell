package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s._
import com.github.ahnfelt.topshell.language._
import com.github.ahnfelt.topshell.worker.Processor
import org.scalajs.dom
import org.scalajs.dom.raw.{DedicatedWorkerGlobalScope, WorkerGlobalScope}
import org.scalajs.dom.webworkers.Worker

import scala.scalajs.js

object Main {
    var worker : Worker = _
    var codeVersion : Double = 0

    def main(arguments : Array[String]) : Unit = {
        if(!js.isUndefined(js.Dynamic.global.window)) {
            var symbols : List[(String, Loader.Loaded[js.Any])] = List()
            var implied : Set[String] = Set()
            var error : Option[String] = None
            var resultTimeouts = Map[String, js.timers.SetTimeoutHandle]()
            def update() : Unit = {
                val component = Component(MainComponent, symbols, implied, error)
                ReactBridge.renderToDomById(component, "main")
            }
            worker = new Worker("worker.js")
            worker.onmessage = m => {
                val data = m.data.asInstanceOf[js.Dynamic]
                if(data.codeVersion.asInstanceOf[Double] == codeVersion) {
                    data.event.asInstanceOf[String] match {
                        case "symbols" =>
                            val cached = data.cached.asInstanceOf[js.Array[String]]
                            symbols = data.symbols.asInstanceOf[js.Array[String]].map(s =>
                                if(cached.contains(s)) symbols.find(_._1 == s).getOrElse(s -> Loader.Loading())
                                else s -> Loader.Loading()
                            ).toList
                            implied = data.implied.asInstanceOf[js.Array[String]].toSet
                            update()
                        case "error" =>
                            val name = data.name.asInstanceOf[String]
                            for(handle <- resultTimeouts.get(name)) js.timers.clearTimeout(handle)
                            resultTimeouts += (name -> js.timers.setTimeout(50.0) {
                                val index = symbols.indexWhere(_._1 == name)
                                symbols = symbols.updated(
                                    index,
                                    name -> Loader.Error(new RuntimeException(data.error.asInstanceOf[String]))
                                )
                                update()
                            })
                        case "result" =>
                            val name = data.name.asInstanceOf[String]
                            for(handle <- resultTimeouts.get(name)) js.timers.clearTimeout(handle)
                            resultTimeouts += (name -> js.timers.setTimeout(50.0) {
                                val index = symbols.indexWhere(_._1 == name)
                                symbols = symbols.updated(
                                    index,
                                    name -> Loader.Result(data.html)
                                )
                                update()
                            })
                        case e =>
                            println("Not handled: " + e)
                    }
                }
            }
            dom.window.onload = _ => update()
        } else {
            DedicatedWorkerGlobalScope.self.onmessage = m => {
                val data = m.data.asInstanceOf[js.Dynamic]
                if(data.event.asInstanceOf[String] == "code") {
                    codeVersion = data.codeVersion.asInstanceOf[Double]
                    Processor.process(data.asInstanceOf[js.Dynamic].code.asInstanceOf[String])
                } else if(data.event.asInstanceOf[String] == "start") {
                    Processor.start(data.fromLine.asInstanceOf[Int], data.toLine.asInstanceOf[Int])
                } else {
                    println("Could not understand message to worker: " + m)
                }
            }
        }
    }
}
