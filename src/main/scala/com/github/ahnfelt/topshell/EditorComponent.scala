package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s._
import org.scalajs.dom

import scala.scalajs.js

case class EditorComponent(code : P[String]) extends Component[String] {

    var codeMirror : Option[CodeMirror] = None

    override def componentWillRender(get : Get) : Unit = {
        for(editor <- codeMirror if get(code) != editor.getDoc().getValue()) {
            editor.getDoc().setValue(get(code))
        }
    }

    override def render(get : Get) : Element = {
        E.div(S.width.percent(100), S.height.percent(100)).withKey("editor").withRef(addToDom(get, _))
    }

    private def addToDom(get : Get, newElement : Any) : Unit = if(codeMirror.isEmpty) {
        val config = js.Dictionary[js.Any](
            "indentUnit" -> 4,
            "gutters" -> js.Array("editor-gutter"),
            "autofocus" -> true,
            "dragDrop" -> false,
            "value" -> get[String](code),
            "extraKeys" -> js.Dictionary[js.Function1[CodeMirror, Unit]](
                "Tab" -> {editor => editor.execCommand("indentMore")},
                "Shift-Tab" -> {editor => editor.execCommand("indentLess")},
                "Ctrl-Space" -> {editor => editor.execCommand("autocomplete")},
                "Ctrl-Enter" -> {editor =>
                    val from = editor.getDoc().getCursor("from").line + 1
                    val to = editor.getDoc().getCursor("to").line + 1
                    println(from + " " + to)
                },
                //"Ctrl-R" -> {editor => editor.execCommand("replace")},
                //"Escape" -> {editor => editor.execCommand("clearSearch")},
            ),
        )
        val editor = js.Dynamic.global.CodeMirror(newElement.asInstanceOf[js.Any], config).asInstanceOf[CodeMirror]
        editor.on("changes", editor => emit(editor.getDoc().getValue()))
        codeMirror = Some(editor)
    }

}

@js.native
trait CodeMirror extends js.Any {
    def setSize(width : String, height : String) : Unit
    def on(event : String, callback : js.Function1[CodeMirror, Unit]) : Unit
    def getDoc() : CodeMirrorDocument
    def execCommand(command : String) : Unit
}

@js.native
trait CodeMirrorDocument extends js.Any {
    def getValue() : String
    def setValue(value : String) : Unit
    def getAllMarks() : js.Array[CodeMirrorTextMarker]
    def getCursor(start : String = "head") : CodeMirrorCursor
}

@js.native
trait CodeMirrorTextMarker extends js.Any {
    def clear() : Unit
}

@js.native
trait CodeMirrorCursor extends js.Any {
    val line : Int
    val ch : Int
}

