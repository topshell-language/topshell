package com.github.ahnfelt.topshell.worker

import com.github.ahnfelt.topshell.Main
import com.github.ahnfelt.topshell.language.Tokenizer.ParseException
import com.github.ahnfelt.topshell.language._
import org.scalajs.dom.raw.DedicatedWorkerGlobalScope

import scala.scalajs.js
import scala.scalajs.js.JSON
import scala.scalajs.js.typedarray.Uint8ClampedArray

object Processor {

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
        val _d = { (escapedName : String, value : js.Any, error : js.Any) =>
            val name = if(escapedName.endsWith("_")) escapedName.dropRight(1) else escapedName
            if(Main.codeVersion == currentVersion) {
                val message = if(!js.isUndefined(error) && error != null) {
                    js.Dictionary("event" -> "error", "name" -> name, "error" -> ("" + error), "codeVersion" -> currentVersion)
                } else {
                    val html = if(
                        js.isUndefined(value) ||
                            value == null ||
                            !value.asInstanceOf[js.Dictionary[_]].contains("_tag")
                    ) toHtml(value) else value
                    js.Dictionary("event" -> "result", "name" -> name, "html" -> html, "codeVersion" -> currentVersion)
                }
                DedicatedWorkerGlobalScope.self.postMessage(message)
            }
        } : js.Function3[String, js.Any, js.Any, Unit]

        js.Dynamic.newInstance(js.Dynamic.global.Function)("_g", "_d", emitted)(_g, _d)

    }

    def tag(tagName : String, children : Any*) = {
        js.Dictionary("_tag" -> tagName, "children" -> js.Array(children : _*))
    }

    def toHtml(value : Any) : Any = value match {
        case _ if js.isUndefined(value) => tag("span", "Undefined")
        case v if v == null => tag("span", "Null")
        case v : String => tag("span", JSON.stringify(v))
        case v : Double => tag("span", JSON.stringify(v))
        case v : Boolean => tag("span", if(v) "True" else "False")
        case _ : js.Function => tag("span", "Function")
        case _ : Uint8ClampedArray => tag("span", "Bytes")
        case v : js.Array[_] =>
            val items : Seq[Any] = v.toSeq.flatMap(i => Seq(", ", toHtml(i))).drop(1)
            tag("span", Seq("[") ++ items ++ Seq("]") : _*)
        case _ =>
            val v = value.asInstanceOf[js.Dictionary[_]]
            if(v.contains("_tag")) {
                v
            } else if(v.contains("_run")) {
                tag("span", "Task")
            } else {
                val items : Seq[Any] = v.toSeq.flatMap { case (k, i) => Seq(", ", k.replace("_", ""), ": ", toHtml(i)) }.drop(1)
                tag("span", Seq("{") ++ items ++ Seq("}") : _*)
            }
    }

    case class AbortedException() extends RuntimeException("Aborted due to new version of the code")

}
