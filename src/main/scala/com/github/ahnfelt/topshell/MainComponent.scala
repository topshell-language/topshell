package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s._
import org.scalajs.dom

import scala.scalajs.js

case class MainComponent(symbols : P[List[(String, Loader.Loaded[js.Any])]], implied : P[Set[String]], error : P[Option[String]]) extends Component[NoEmit] {

    val code = State(Option(dom.window.localStorage.getItem("code")).getOrElse(""))
    val debouncedCode = Debounce(this, code.map(_.trim))
    var lastCode = ""

    override def componentWillRender(get : Get) : Unit = {
        if(get(code).trim != lastCode) Main.codeVersion += 1
        if(get(debouncedCode) != lastCode) {
            lastCode = get(debouncedCode)
            Main.worker.postMessage(js.Dictionary(
                "event" -> "code",
                "code" -> lastCode,
                "codeVersion" -> Main.codeVersion
            ))
            dom.window.localStorage.setItem("code", lastCode)
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
                Component(EditorComponent, get(code)).withHandler {
                    case SetCode(c) => code.set(c)
                    case Execute(fromLine, toLine) =>
                        Main.worker.postMessage(js.Dictionary(
                            "event" -> "start",
                            "fromLine" -> fromLine,
                            "toLine" -> toLine
                        ))
                }
            ),
            E.div(BottomLeftAreaCss,
                E.div(ShortcutAreaCss, Text("Ctrl + F / R")),
                E.div(HintAreaCss, Text("Find / replace ...")),
                E.input(InputBarCss, A.placeholder("Find in file ...")),
            ),
            E.div(TopRightAreaCss,
                ButtonAreaCss,
                E.div(CenterTextCss, Text("Unnamed.tsh"), A.title("/home/me/tsh/Unnamed.tsh")),
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
                            "toLine" -> 1000000
                        ))
                    }
                ),
            ),
            E.div(RightAreaCss,
                Tags(for((symbol, status) <- get(symbols) if !get(implied)(symbol) || status.isInstanceOf[Loader.Error[_]]) yield {
                    E.div(
                        ResultCss,
                        E.div(ResultHeaderCss, Text(symbol)).when(!symbol.contains('_')),
                        E.div(ResultBodyCss,
                            status match {
                                case Loader.Loading() => E.div(E.span(SpinnerCss1), E.span(SpinnerCss2), E.span(SpinnerCss3))
                                case Loader.Error(e) => E.span(CodeCss, S.color(Palette.textError), Text(e.getMessage))
                                case Loader.Result(html) => try {
                                    E.span(CodeCss, renderValue(html))
                                } catch {
                                    case e : Throwable =>
                                        E.span(CodeCss, S.color(Palette.textError), Text("Internal error: " + e.getMessage))
                                }
                            }
                        )
                    ).withKey(symbol)
                }),
            ),
            E.div(BottomRightAreaCss,
                E.div(ShortcutAreaCss, Text("TopShell 2018.1")),
                E.div(CenterTextCss,
                    S.color(Palette.textHint),
                    Text("Saved.") // Text(if(get(code) == get(debouncedCode)) "Saved." else "Saving...")
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

    private def renderValue(value : Any) : Tag = value match {
        case v : String => Text(v)
        case v : js.Array[_] =>
            val nodes = for(i <- v) yield renderValue(i)
            Tags(nodes.toSeq)
        case _ =>
            val v = value.asInstanceOf[js.Dictionary[_]]
            val tagName = v("_tag").asInstanceOf[String]
            if(tagName == ">text") {
                Text(v("text").asInstanceOf[String])
            } else if(tagName == ">view") {
                renderValue(v("html"))
            } else if(tagName == ">attribute") {
                A("" + v("key"), "" + v("value"))
            } else if(tagName == ">style") {
                S("" + v("key"), "" + v("value"))
            } else if(tagName == ">status") {
                E.span(TextCss, S.color(Palette.textHint), v("key") match {
                    case "Pending" => Text("Pending: " + v("value"))
                    case "Runnable" => E.span(Text("Ready to run "), E.span(S.fontStyle.italic(), Text("(Ctrl + Enter)")))
                    case "Computing" => Text("Computing...")
                    case "Running" => Text("Running...")
                    case _ => Text(v("key") + ": " + v("value"))
                })
            } else {
                E(tagName, renderValue(v("children")))
            }
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
