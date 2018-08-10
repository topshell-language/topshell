package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s._
import com.github.ahnfelt.topshell.language.Syntax.{Term, TopImport, TopSymbol}
import com.github.ahnfelt.topshell.language._
import com.github.ahnfelt.topshell.language.Tokenizer.{ParseException, Token}

import scala.scalajs.js

case class MainComponent() extends Component[NoEmit] {

    val code = State("")
    val debouncedCode = Debounce(this, code, 500)
    var lastCode = ""
    var topImports : List[TopImport] = List.empty
    var topSymbols : List[TopSymbol] = List.empty
    var error : Option[String] = None
    val timeout = Timeout(this, debouncedCode, true) { _ => 1000 } // Temporary workaround until we wait for async exec

    override def componentWillRender(get : Get) : Unit = {
        if(get(debouncedCode) != lastCode) {
            lastCode = get(debouncedCode)
            try {
                val tokens = Tokenizer.tokenize("Unnamed.tsh", lastCode)
                val (newImports, newSymbols) = new Parser("Unnamed.tsh", tokens).parseTopLevel()
                topImports = UsedImports.completeImports(newSymbols, newImports)
                topSymbols = newSymbols
                error = None
                println("===")
                println(topImports)
                println(topSymbols)
                println("---")
                topSymbols = Checker.check(topImports, topSymbols)
                val emitted = Emitter.emit(topImports, topSymbols)
                println(emitted)
                scalajs.js.eval(emitted)
            } catch { case e : ParseException =>
                topImports = List.empty
                topSymbols = List.empty
                error = Some(e.message + " " + e.at.toShortString)
            }
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
                E.textarea(
                    EditorCss,
                    A.onKeyDown(onKeyDown),
                    A.onChangeText(code.set),
                    A.autoFocus(),
                    A.spellCheck("false")
                ),
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
                E.i(A.className("fa fa-play"), ButtonCss, A.title("Re-run (Ctrl + Enter)")),
                E.i(A.className("fa fa-rocket"), ButtonCss, A.title("Re-run with all side effects enabled (Ctrl + Shift + Enter)")),
            ),
            E.div(RightAreaCss,
                Tags(for(topImport <- topImports) yield {
                    val global = scalajs.js.Dynamic.global
                    E.div(
                        ResultCss,
                        E.div(ResultHeaderCss, Text(topImport.name)),
                        E.div(ResultBodyCss,
                            topImport.error.map(_.message).map(m =>
                                E.span(CodeCss, S.color(Palette.textError), Text(m))
                            ).getOrElse {
                                if(!js.isUndefined(global.tsh.selectDynamic(topImport.name + "_e"))) {
                                    val value = global.tsh.selectDynamic(topImport.name + "_e")
                                    E.span(CodeCss, S.color(Palette.textError), Text(value + ""))
                                } else {
                                    E.div(ValueCss, Text("Module " + topImport.url))
                                }
                            }
                        )
                    )
                }),
                Tags(for(symbol <- topSymbols) yield {
                    val global = scalajs.js.Dynamic.global
                    E.div(
                        ResultCss,
                        E.div(ResultHeaderCss, Text(symbol.binding.name)).when(!symbol.binding.name.contains('_')),
                        E.div(ResultBodyCss,
                            symbol.error.map(_.message).map(m =>
                                E.span(CodeCss, S.color(Palette.textError), Text(m))
                            ).getOrElse {
                                E.div(
                                    if(js.isUndefined(global.tsh)) {
                                        Text("")
                                    } else if(!js.isUndefined(global.tsh.selectDynamic(symbol.binding.name + "_e"))) {
                                        val value = global.tsh.selectDynamic(symbol.binding.name + "_e")
                                        E.span(CodeCss, S.color(Palette.textError), Text(value + ""))
                                    } else {
                                        val value = global.tsh.selectDynamic(symbol.binding.name + "_")
                                        renderValue(value)
                                    }
                                )
                            }
                        )
                    )
                })
            ),
            E.div(BottomRightAreaCss,
                E.div(ShortcutAreaCss, Text("TopShell 2018.1")),
                E.div(CenterTextCss,
                    S.color(Palette.textHint),
                    Text(if(get(code) == get(debouncedCode)) "Saved." else "Saving...")
                ),
                E.div(HintAreaCss,
                    Text {
                        val errorCount = topSymbols.count(_.error.nonEmpty)
                        error.getOrElse(
                            if(errorCount == 0) "No errors."
                            else if(errorCount == 1) "1 error."
                            else errorCount + " errors."
                        )
                    },
                    S.color(if(error.isEmpty) Palette.text else Palette.textError)
                ),
            ),
        )
    }

    private def renderValue(value : Any) : Tag = value match {
        case v : String => E.div(StringValueCss, Text(js.JSON.stringify(v)))
        case v : Double => E.div(ValueCss, S.color(Palette.textError).when(v.isNaN), Text(v + ""))
        case v : Boolean => E.div(ValueCss, Text(if(v) "True" else "False"))
        case v if v == null => E.div(ValueCss, Text("Null"))
        case v if js.isUndefined(v) => E.div(ValueCss, S.color(Palette.textError), Text("Undefined"))
        case _ : js.Function => E.div(ValueCss, Text("Function"))
        case v if js.Dynamic.global.tsh_prelude.isVisual_(v.asInstanceOf[js.Any]).asInstanceOf[Boolean] =>
            E.div().withRef(container => v.asInstanceOf[js.Dynamic].setHtml(container.asInstanceOf[js.Any]))
        case v : js.Array[_] => E.div(Tags({
            if(v.isEmpty) List(List(E.span(ValueCss, Text("[]"))))
            else for ((item, k) <- v.zipWithIndex.toList) yield List(
                E.div(
                    S.position.relative(),
                    S.paddingLeft.px(18),
                    E.div(ValueCss, Text("•"), S.position.absolute(), S.left.px(4)),
                    renderValue(item)
                )
            )
        }.flatten))
        case v => E.div(Tags({
            val d = v.asInstanceOf[js.Dictionary[js.Any]]
            if(d.isEmpty) List(List(E.span(ValueCss, Text("{}"))))
            else for((k, item) <- d.toList) yield List(
                E.div(StringValueCss, Text(k.replace("_", "") + ": ")),
                E.div(S.paddingLeft.px(18), renderValue(item))
            )
        }.flatten))
    }

    private def onKeyDown(e : KeyboardEvent) : Unit = {
        if(e.keyCode == 9) {
            e.preventDefault()
            val start = e.target.selectionStart.asInstanceOf[Double]
            val end = e.target.selectionEnd.asInstanceOf[Double]
            e.target.value = e.target.value.substring(0, start) + "    " + e.target.value.substring(end)
            e.target.selectionStart = start + 4
            e.target.selectionEnd = start + 4
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

