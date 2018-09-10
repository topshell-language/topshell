package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s._
import org.scalajs.dom

import scala.scalajs.js

case class EditorComponent(codeFile : P[CodeFile]) extends Component[EditorMessage] {

    var codeMirror : Option[CodeMirror] = None

    override def componentWillRender(get : Get) : Unit = {
        for(editor <- codeMirror) {
            if(changed(get, editor)) {
                editor.getDoc().setValue(get(codeFile).code.get)
                editor.getDoc().setCursor(get(codeFile).cursorLine - 1, get(codeFile).cursorColumn - 1)
            }
        }
    }

    private def changed(get : Get, editor : CodeMirror) : Boolean = {
        val value = editor.getDoc().getValue()
        val c = get(codeFile)
        val d = editor.getDoc()
        val cursor = d.getCursor()
        !c.code.contains(value) ||
        c.cursorLine - 1 != cursor.line ||
        c.cursorColumn - 1 != cursor.ch
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
            "value" -> get[CodeFile](codeFile).code.get,
            "extraKeys" -> js.Dictionary[js.Function1[CodeMirror, Unit]](
                "Tab" -> {editor => editor.execCommand("indentMore")},
                "Shift-Tab" -> {editor => editor.execCommand("indentLess")},
                "Ctrl-Space" -> {editor => editor.execCommand("autocomplete")},
                "Ctrl-Enter" -> {editor =>
                    val from = editor.getDoc().getCursor("from").line + 1
                    val cursor = editor.getDoc().getCursor("to")
                    val to = if(cursor.ch == 0) Math.max(cursor.line, from) else cursor.line + 1
                    emit(Execute(from, to))
                },
                "Shift-Ctrl-Enter" -> {editor =>
                    val from = 1
                    val to = editor.getDoc().lineCount()
                    emit(Execute(from, to))
                },
                //"Ctrl-R" -> {editor => editor.execCommand("replace")},
                //"Escape" -> {editor => editor.execCommand("clearSearch")},
            ),
        )
        val editor = js.Dynamic.global.CodeMirror(newElement.asInstanceOf[js.Any], config).asInstanceOf[CodeMirror]
        editor.on("changes", {editor =>
            val value = get(codeFile).copy(
                cursorLine = editor.getDoc().getCursor().line + 1,
                cursorColumn = editor.getDoc().getCursor().ch + 1,
                lastOpened = System.currentTimeMillis(),
                code = Some(editor.getDoc().getValue())
            )
            if(changed(get, editor)) emit(SetCodeFile(value))
        })
        codeMirror = Some(editor)
    }

}

sealed abstract class EditorMessage
case class SetCodeFile(codeFile : CodeFile) extends EditorMessage
case class Execute(fromLine : Int, toLine : Int) extends EditorMessage

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
    def setCursor(line : Int, column : Int) : Unit
    def lineCount() : Int
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

