package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.JSON

case class MainComponent(symbols : P[List[(String, Loader.Loaded[js.Any])]], implied : P[Set[String]], error : P[Option[String]]) extends Component[NoEmit] {

    val showOpen = State[Option[Int]](None)
    var altKey = false
    var ctrlKey = false
    var shiftKey = false

    dom.document.addEventListener("keydown", { event : dom.KeyboardEvent =>
        if(!event.altKey) altKey = false
        if(!event.ctrlKey) ctrlKey = false
        if(!event.shiftKey) shiftKey = false
        if(event.key == "Alt") altKey = true
        if(event.key == "Control") ctrlKey = true
        if(event.key == "Shift") shiftKey = true
        if((event.key == "e" || event.key == "Tab") && ctrlKey) {
            event.preventDefault()
            val delta = if(shiftKey) -1 else +1
            val index = Get.Unsafe(showOpen).getOrElse(0) + delta
            showOpen.set(Some(index % codeFiles.size))
        }
    })

    dom.document.addEventListener("keyup", { event : dom.KeyboardEvent =>
        if(event.key == "Alt") altKey = false
        if(event.key == "Control") ctrlKey = false
        if(event.key == "Shift") shiftKey = false
        if(event.key == "Escape" && Get.Unsafe(showOpen).nonEmpty) showOpen.set(None)
        if(event.key == "Control" && Get.Unsafe(showOpen).nonEmpty) {
            event.preventDefault()
            lastCode = codeFiles(Get.Unsafe(showOpen).get).copy(lastOpened = System.currentTimeMillis())
            saveCode(lastCode)
            codeFiles = loadCodeFiles()
            code.set(lastCode)
            showOpen.set(None)
        }
    })

    private def nextIndex() : Int = {
        val indexes = for {
            i <- 0 until dom.window.localStorage.length
            key = dom.window.localStorage.key(i)
            if key.startsWith("file-")
        } yield {
            key.drop("file-".length).toInt
        }
        (0 +: indexes).max + 1
    }

    private def loadOrCreate() : CodeFile = {
        val index = nextIndex()
        val blank = CodeFile.blank(index)
        val file = (if(index == 1) blank else {
            val codeFile = loadCode("file-" + (index - 1))
            if(codeFile.exists(_.code.exists(_.trim.isEmpty))) codeFile.getOrElse(blank) else blank
        }).copy(lastOpened = System.currentTimeMillis())
        saveCode(file)
        file
    }

    private def loadCode(name : String) : Option[CodeFile] = {
        Option(dom.window.localStorage.getItem(name)).map(JSON.parse(_)).
            map(_.asInstanceOf[CodeFileJs]).map(CodeFile.fromJs)
    }

    private def saveCode(codeFile : CodeFile) : Unit = {
        dom.window.localStorage.setItem("file-" + codeFile.index, JSON.stringify(CodeFile.toJs(codeFile)))
    }

    private def loadCodeFiles() : List[CodeFile] = {
        for {
            i <- 0 until Math.min(dom.window.localStorage.length, 1000)
            key = dom.window.localStorage.key(i)
            if key.startsWith("file-")
        } yield {
            Option(dom.window.localStorage.getItem(key)).map(JSON.parse(_)).
                map(_.asInstanceOf[CodeFileJs]).map(CodeFile.fromJs).get
        }
    }.sortBy(-_.lastOpened).take(20).toList

    var lastCode = loadOrCreate()
    val code = State(lastCode)
    val debouncedCode = Debounce(this, code, 500)
    var codeFiles = loadCodeFiles()

    override def componentWillRender(get : Get) : Unit = {
        if(get(code) != lastCode) Main.codeVersion += 1
        if(get(debouncedCode) != lastCode) {
            lastCode = get(debouncedCode)
            Main.worker.postMessage(js.Dictionary(
                "event" -> "code",
                "code" -> lastCode.code.getOrElse(""),
                "codeVersion" -> Main.codeVersion
            ))
            saveCode(lastCode)
        }
    }

    override def render(get : Get) : Element = {
        E.div(
            E.div(TopLeftAreaCss,
                E.div(ShortcutAreaCss, Text("Ctrl + O / E")),
                E.div(HintAreaCss, Text("Open / enter ...")),
                E.input(InputBarCss, A.placeholder("Open file ...")),
            ),
            E.div(LeftAreaCss,
                E.div(
                    S.position.absolute(),
                    S.top.px(0),
                    S.left.px(0),
                    S.bottom.px(0),
                    S.right.px(0),
                    Component(EditorComponent, get(code)).withHandler {
                        case NewCodeFile =>
                            saveCode(get(code))
                            code.set(loadOrCreate())
                        case OpenCodeFile =>
                            saveCode(get(code))
                        case EnterCodeFile =>
                            saveCode(get(code))
                        case SetCodeFile(c) =>
                            code.set(c)
                        case Execute(fromLine, toLine) =>
                            Main.worker.postMessage(js.Dictionary(
                                "event" -> "start",
                                "fromLine" -> fromLine,
                                "toLine" -> toLine
                            ))
                    }
                ),
                Tags(get(showOpen).map(Component(OpenFileComponent, codeFiles, _)))
            ),
            E.div(BottomLeftAreaCss,
                E.div(ShortcutAreaCss, Text("Ctrl + F / R")),
                E.div(HintAreaCss, Text("Find / replace ...")),
                E.input(InputBarCss, A.placeholder("Find in file ...")),
            ),
            E.div(TopRightAreaCss,
                ButtonAreaCss,
                E.div(
                    CenterTextCss,
                    S.fontStyle.italic(),
                    Text("Draft " + get(code).index),
                    A.title("This is a draft")
                ),
                E.div(
                    RightButtonAreaCss,
                    E.i(A.className("fa fa-question"), ButtonCss, A.title("Help")),
                    E.i(A.className("fa fa-cog"), ButtonCss, A.title("Settings")),
                ),
                //E.i(A.className("fa fa-pause"), ButtonCss, A.title("Pause execution until next edit or re-run")),
                E.i(
                    A.className("fa fa-forward"),
                    ButtonCss,
                    A.title("Execute all top level binds (Ctrl + Shift + Enter)"),
                    A.onLeftClick { _ =>
                        Main.worker.postMessage(js.Dictionary(
                            "event" -> "start",
                            "fromLine" -> 1,
                            "toLine" -> 1000000000
                        ))
                    }
                ),
            ),
            E.div(RightAreaCss, {
                Tags(for((symbol, status) <- get(symbols) if !get(implied)(symbol) || status.isInstanceOf[Loader.Error[_]]) yield {
                    Component(BlockComponent, symbol, status).withKey(symbol)
                }),
            }),
            E.div(BottomRightAreaCss,
                E.div(ShortcutAreaCss, Text("TopShell 2018.1")),
                E.div(CenterTextCss,
                    S.color(Palette.textHint),
                    Text(if(get(code).code == get(debouncedCode).code) "Saved." else "Saving...")
                ),
                E.div(HintAreaCss,
                    Text {
                        val errorCount = get(symbols).count(_._2.isInstanceOf[Loader.Error[_]])
                        get(error).getOrElse(
                            if(errorCount == 0) "No errors."
                            else if(errorCount == 1) "1 error."
                            else errorCount + " errors."
                        )
                    },
                    S.color(if(get(error).isEmpty) Palette.text else Palette.textError)
                ),
            ),
        )
    }

}

object ValueCss extends CssClass(
    CodeCss,
)

object StringValueCss extends CssClass(
    CodeCss,
)

object TextCss extends CssClass(
    S.color(Palette.text),
    S.fontSize.px(14),
    S.fontFamily("'Montserrat', sans-serif"),
)

object CodeCss extends CssClass(
    S.fontFamily("'Fira Mono', monospace"),
    S.fontSize.px(14),
    S.whiteSpace("pre"),
    S("overflow-wrap", "normal"),
    S.color(Palette.text),
)

object HintAreaCss extends CssClass(
    TextCss,
    S.position.absolute(),
    S.top.px(10),
    S.left.px(20),
    S.color(Palette.textHint),
)

object CenterTextCss extends CssClass(
    TextCss,
    S.position.absolute(),
    S.left.px(100),
    S.right.px(100),
    S.top.px(10),
    S.textAlign.center(),
)

object ShortcutAreaCss extends CssClass(
    TextCss,
    S.position.absolute(),
    S.right.px(0),
    S.paddingTop.px(10),
    S.paddingRight.px(20),
    S.color(Palette.textHint),
    S.fontStyle.italic(),
)

object ButtonAreaCss extends CssClass(
    S.paddingTop.px(5),
    S.paddingLeft.px(15),
    S.paddingRight.px(15),
    S.fontWeight.px(20),
    S.color(Palette.text),
)

object RightButtonAreaCss extends CssClass(
    S.position.absolute(),
    S.right.px(15),
    S.top.px(5),
)

object ButtonCss extends CssClass(
    S.boxSizing.borderBox(),
    S.display.inlineBlock(),
    S.cursor.pointer(),
    S.borderRadius.px(3),
    S.textAlign.center(),
    S.paddingTop.px(5),
    S.width.px(30),
    S.height.px(30),
    S.border("1px solid #ffffff"),
    Css.hover(
        S.border("1px solid " + Palette.border),
    ),
    Css.active(
        S.backgroundColor(Palette.border),
    ),
)

object ResultCss extends CssClass(
    TextCss,
    S.marginBottom.px(20),
    S.borderLeft("5px solid " + Palette.border),
    S.paddingLeft.px(5),
    S.marginLeft.px(-10),
)

object ResultHeaderCss extends CssClass(
    CodeCss,
    S.fontWeight.bold(),
    S.marginBottom.px(5),
)

object ResultBodyCss extends CssClass(
)

object EditorCss extends CssClass(
    CodeCss,
    S.position.absolute(),
    S.boxSizing.borderBox(),
    S.top.px(0),
    S.bottom.px(0),
    S.width.percent(100),
    S.height.percent(100),
    S.paddingLeft.px(20),
    S.paddingTop.px(10),
    S.border.none(),
    S.outline.none(),
    S.resize.none(),
    S.color(Palette.text),
)

object InputBarCss extends CssClass(
    S.position.absolute(),
    S.boxSizing.borderBox(),
    S.left.px(0),
    S.top.px(0),
    S.width.percent(100),
    S.height.percent(100),
    S.opacity.number(0.01),
    S.paddingLeft.px(20),
    Css.focus(
        S.opacity.number(1.00),
    )
)

object TopLeftAreaCss extends CssClass(
    S.position.absolute(),
    S.boxSizing.borderBox(),
    S.top.px(0),
    S.height.px(40),
    S.left.px(0),
    S.right.percent(50),
    S.borderBottom("1px solid " + Palette.border),
)

object LeftAreaCss extends CssClass(
    S.position.absolute(),
    S.boxSizing.borderBox(),
    S.overflowY("auto"),
    S.top.px(40),
    S.bottom.px(40),
    S.left.percent(0),
    S.right.percent(50),
    S.overflow("hidden"),
)

object BottomLeftAreaCss extends CssClass(
    S.position.absolute(),
    S.boxSizing.borderBox(),
    S.height.px(40),
    S.bottom.px(0),
    S.left.percent(0),
    S.right.percent(50),
    S.borderTop("1px solid " + Palette.border),
)

object TopRightAreaCss extends CssClass(
    S.position.absolute(),
    S.boxSizing.borderBox(),
    S.top.px(0),
    S.height.px(40),
    S.left.percent(50),
    S.right.percent(0),
    S.borderLeft("1px solid " + Palette.border),
    S.borderBottom("1px solid " + Palette.border),
)

object RightAreaCss extends CssClass(
    S.position.absolute(),
    S.boxSizing.borderBox(),
    S.top.px(40),
    S.bottom.px(40),
    S.left.percent(50),
    S.overflowY("auto"),
    S.right.px(0),
    S.borderLeft("1px solid " + Palette.border),
    S.padding.px(10).px(20),
)

object BottomRightAreaCss extends CssClass(
    S.position.absolute(),
    S.boxSizing.borderBox(),
    S.height.px(40),
    S.bottom.px(0),
    S.left.percent(50),
    S.right.percent(0),
    S.borderLeft("1px solid " + Palette.border),
    S.borderTop("1px solid " + Palette.border),
)

object SpinnerCss extends CssClass(
    S.width.px(10),
    S.height.px(10),
    S.borderRadius.px(10),
    S.backgroundColor(Palette.textHint),
    S.display.inlineBlock(),
    SpinnerKeyframes,
    S.animationDuration.s(1.2),
    S.animationIterationCount("infinite"),
    S.animationTimingFunction("ease-in-out")
)

object SpinnerCss1 extends CssClass(
    SpinnerCss,
    S.animationDelay.s(0)
)

object SpinnerCss2 extends CssClass(
    SpinnerCss,
    S.animationDelay.s(0.3)
)

object SpinnerCss3 extends CssClass(
    SpinnerCss,
    S.animationDelay.s(0.6)
)

object SpinnerKeyframes extends CssKeyframes(
    "0%, 80%, 100%" -> Seq(S.transform("scale(0)"), S.opacity.number(0)),
    "50%" -> Seq(S.transform("scale(1)"), S.opacity.number(1)),
)
