package com.github.ahnfelt.topshell.worker

import com.github.ahnfelt.topshell.language.Tokenizer.ParseException
import com.github.ahnfelt.topshell.language._
import org.scalajs.dom.raw.DedicatedWorkerGlobalScope

import scala.scalajs.js
import scala.scalajs.js.JSON

// Render the results into a hypertext-like format here. Then render that to html in the UI thread.

object Processor {

    private var version : Double = 0

    def process(code : String) : Unit = {
        version += 1
        val currentVersion = version
        js.Dynamic.global.updateDynamic("_tsh_code_version")(currentVersion)

        val tokens = Tokenizer.tokenize("Unnamed.tsh", code)
        val (newImports, newSymbols) = new Parser("Unnamed.tsh", tokens).parseTopLevel()
        val topImports = UsedImports.completeImports(newSymbols, newImports)
        val topSymbols = Checker.check(topImports, newSymbols)
        val emitted = Emitter.emit(currentVersion, topImports.filter(_.error.isEmpty), topSymbols.filter(_.error.isEmpty))

        val names = topImports.map(_.name) ++ topSymbols.map(_.binding.name)
        val message = js.Dictionary("event" -> "symbols", "symbols" -> js.Array(names : _*))
        DedicatedWorkerGlobalScope.self.postMessage(message)

        val _g = DedicatedWorkerGlobalScope.self
        val _d = { (name : js.Any, value : js.Any, error : js.Any) =>
            if(version == currentVersion) {
                val message = if(!js.isUndefined(error) && error != null) {
                    js.Dictionary("event" -> "error", "name" -> name, "error" -> ("" + error))
                } else {
                    val html = if(
                        js.isUndefined(value) ||
                            value == null ||
                            !value.asInstanceOf[js.Dictionary[_]].contains("_tag")
                    ) toHtml(value) else value
                    js.Dictionary("event" -> "result", "name" -> name, "html" -> html)
                }
                DedicatedWorkerGlobalScope.self.postMessage(message)
            }
        } : js.Function3[js.Any, js.Any, js.Any, Unit]

        for(i <- topImports.filter(_.error.nonEmpty)) {
            _d(i.name, js.undefined, "" + i.error.get.getMessage)
        }
        for(i <- topSymbols.filter(_.error.nonEmpty)) i.error.get match {
            case e : ParseException => _d(i.binding.name, js.undefined, "" + e.message)
            case e => _d(i.binding.name, js.undefined, "" + e.getMessage)
        }

        js.Dynamic.newInstance(js.Dynamic.global.Function)("_g", "_d", emitted)(_g, _d)

    }

    def tag(tagName : String, attributes : Seq[(String, String)], children : Any*) = {
        js.Dictionary("_tag" -> tagName, "attributes" -> js.Dictionary(attributes : _*), "children" -> js.Array(children : _*))
    }

    def toHtml(value : Any) : Any = value match {
        case _ if js.isUndefined(value) => tag("span", Seq(), "Undefined")
        case v if v == null => tag("span", Seq(), "Null")
        case v : String => tag("span", Seq(), JSON.stringify(v))
        case v : Double => tag("span", Seq(), JSON.stringify(v))
        case v : Boolean => tag("span", Seq(), if(v) "True" else "False")
        case _ : js.Function => tag("span", Seq(), "Function")
        case v : js.Array[_] =>
            val items : Seq[Any] = v.toSeq.flatMap(i => Seq(", ", toHtml(i))).drop(1)
            tag("span", Seq(), Seq("[") ++ items ++ Seq("]") : _*)
        case _ =>
            val v = value.asInstanceOf[js.Dictionary[_]]
            if(v.contains("_tag")) v else {
                val items : Seq[Any] = v.toSeq.flatMap { case (k, i) => Seq(", ", k.replace("_", ""), ": ", toHtml(i)) }.drop(1)
                tag("span", Seq(), Seq("{") ++ items ++ Seq("}") : _*)
            }
    }

    case class AbortedException() extends RuntimeException("Aborted due to new version of the code")

}
