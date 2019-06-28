package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s.Loader.Loaded
import com.github.ahnfelt.react4s._

import scala.scalajs.js

case class BlockComponent(symbol : P[String], symbolType : P[Option[String]], status : P[Loaded[js.Any]]) extends Component[NoEmit] {

    val maxLength = State(10000)

    override def render(get : Get) = {
        val t = get(symbolType).map(_ + " ").getOrElse("")
        E.div(
            ResultCss,
            E.div(ResultHeaderCss,
                Text(get(symbol) + " "),
                E.span(ResultTypeColonCss, Text(": ")).when(t.nonEmpty),
                E.span(ResultTypeCss, Text(t))
            ).when(!get(symbol).contains('_')),
            E.div(ResultBodyCss,
                get(status) match {
                    case Loader.Loading() => E.div(E.span(SpinnerCss1), E.span(SpinnerCss2), E.span(SpinnerCss3))
                    case Loader.Error(e) => E.span(CodeCss, S.color(Palette.textError), Text(e.getMessage))
                    case Loader.Result(html) => try {
                        length = 0
                        E.span(
                            CodeCss,
                            renderValue(html, get(maxLength)),
                            E.span(
                                ButtonCss,
                                E.i(A.className("fa fa-ellipsis-h")),
                                A.onLeftClick { _ => maxLength.set(Int.MaxValue) }
                            ).when(length > get(maxLength))
                        )
                    } catch {
                        case e : Throwable =>
                            E.span(CodeCss, S.color(Palette.textError), Text("Internal error: " + e.getMessage))
                    }
                }
            )
        )
    }

    var length = 0

    private def renderValue(value : Any, maxLength : Int) : Tag = {
        value match {
            case v : String =>
                length += v.length
                Text(if(length > maxLength) v.dropRight(length - maxLength) else v)
            case v : js.Array[_] =>
                val nodes = for(i <- v) yield {
                    if(length < maxLength) Some(renderValue(i, maxLength))
                    else None
                }
                Tags(nodes.flatten)
            case _ =>
                val v = value.asInstanceOf[js.Dictionary[_]]
                val tagName = v("_tag").asInstanceOf[String]
                if(tagName == ">text") {
                    val text = v("text").asInstanceOf[String]
                    length += text.length
                    Text(if(length > maxLength) text.dropRight(length - maxLength) else text)
                } else if(tagName == ">view") {
                    length += 1
                    renderValue(v("html"), maxLength)
                } else if(tagName == ">attributes") {
                    val c = v("children").asInstanceOf[js.Array[js.Dynamic]]
                    length += 5 * c.length
                    Tags(for(p <- c) yield A("" + p.key, "" + p.value))
                } else if(tagName == ">styles") {
                    val c = v("children").asInstanceOf[js.Array[js.Dynamic]]
                    length += 5 * c.length
                    Tags(for(p <- c) yield S("" + p.key, "" + p.value))
                } else if(tagName == ">status") {
                    length += 30
                    E.span(TextCss, S.color(Palette.textHint), v("key") match {
                        case "Pending" => E.span(E.i(A.className("fa fa-hourglass-half"), S.paddingRight.px(8)), Text("" + v("value")))
                        case "Runnable" => E.span(E.i(A.className("fa fa-play"), S.paddingRight.px(8)), Text("Ctrl + Enter"))
                        case "Computing" => E.div(E.span(SpinnerCss1), E.span(SpinnerCss2), E.span(SpinnerCss3), E.span(S.paddingLeft.px(8), Text("computing")))
                        case "Running" => E.div(E.span(SpinnerCss1), E.span(SpinnerCss2), E.span(SpinnerCss3))
                        case "Error" => E.span(CodeCss, S.color(Palette.textError), Text("" + v("value")))
                        case _ => Text(v("key") + ": " + v("value"))
                    })
                } else {
                    length += 5
                    E(tagName, renderValue(v("children"), maxLength))
                }
        }
    }

}
