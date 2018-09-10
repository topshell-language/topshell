package com.github.ahnfelt.topshell

import scala.scalajs.js

@js.native
trait CodeFileJs extends js.Any {
    val index : Int
    val lastOpened : Double
    val cursorLine : Int
    val cursorColumn : Int
    // Either has a path on the file system or the draft code (via null)
    val path : String
    val code : String
}

case class CodeFile(
    index : Int,
    lastOpened : Long,
    cursorLine : Int,
    cursorColumn : Int,
    path : Option[String],
    code : Option[String]
)

object CodeFile {

    def toJs(codeFile : CodeFile) : CodeFileJs = js.Dictionary[js.Any](
        "index" -> codeFile.index,
        "lastOpened" -> codeFile.lastOpened.toDouble,
        "cursorLine" -> codeFile.cursorLine,
        "cursorColumn" -> codeFile.cursorColumn,
        "path" -> (codeFile.path.orNull[String] : String),
        "code" -> (codeFile.code.orNull[String] : String),
    ).asInstanceOf[CodeFileJs]

    def fromJs(codeFile : CodeFileJs) : CodeFile = CodeFile(
        index = codeFile.index,
        lastOpened = codeFile.lastOpened.toLong,
        cursorLine = codeFile.cursorLine,
        cursorColumn = codeFile.cursorColumn,
        path = Option(codeFile.path),
        code = Option(codeFile.code),
    )

    def blank(index : Int) = CodeFile(
        index = index,
        lastOpened = System.currentTimeMillis(),
        cursorLine = 1,
        cursorColumn = 1,
        path = None,
        code = Some("")
    )

}
