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

    def main(arguments : Array[String]) : Unit = {
        if(!js.isUndefined(js.Dynamic.global.window)) {
            var symbols : List[(String, Loader.Loaded[js.Any])] = List()
            var error : Option[String] = None
            def update() : Unit = {
                val component = Component(MainComponent, symbols, error)
                ReactBridge.renderToDomById(component, "main")
            }
            worker = new Worker("worker.js")
            worker.onmessage = m => {
                val data = m.data.asInstanceOf[js.Dynamic]
                data.event.asInstanceOf[String] match {
                    case "symbols" =>
                        symbols = data.symbols.asInstanceOf[js.Array[String]].map(_ -> Loader.Loading()).toList
                    case "error" =>
                        val name = data.name.asInstanceOf[String]
                        val index = symbols.indexWhere(_._1 == name)
                        symbols = symbols.updated(
                            index,
                            name -> Loader.Error(new RuntimeException(data.error.asInstanceOf[String]))
                        )
                    case "result" =>
                        val name = data.name.asInstanceOf[String]
                        val index = symbols.indexWhere(_._1 == name)
                        symbols = symbols.updated(
                            index,
                            name -> Loader.Result(data.html)
                        )
                    case e =>
                        println("Not handled: " + e)
                }
                update()
            }
            dom.window.onload = _ => update()
        } else {
            DedicatedWorkerGlobalScope.self.onmessage = m => Processor.process(m.data.asInstanceOf[String])
        }
    }
}
