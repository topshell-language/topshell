package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s._

case class OpenFileComponent(codeFiles : P[List[CodeFile]], selectedIndex : P[Int]) extends Component[NoEmit] {

    override def render(get : Get) : Element = {
        E.div(
            S.position.absolute(),
            S.top.px(0),
            S.left.px(0),
            S.bottom.px(0),
            S.right.px(0),
            S.zIndex.number(10000),
            CodeCss,
            S.whiteSpace("nowrap"),
            S.width.percent(100),
            S.height.percent(100),
            S.backgroundColor("#fffff0"),
            Tags(
                for((codeFile, index) <- get(codeFiles).zipWithIndex)
                yield renderCodeFile(codeFile, index == get(selectedIndex))
            )
        )
    }

    def renderCodeFile(codeFile : CodeFile, selected : Boolean) : Tag = {
        E.div(
            S.padding.px(5),
            S.paddingLeft.px(20),
            S.backgroundColor("#e0f0f0").when(selected),
            E.span(
                S.color("#000000").when(selected),
                Text("Draft " + codeFile.index + " ")
            ),
            E.span(
                S.fontSize.px(10),
                S.color(Palette.textStringValue),
                S.paddingLeft.px(10),
                Text(codeFile.code.getOrElse(codeFile.path.get).take(200))
            )
        )
    }

}
