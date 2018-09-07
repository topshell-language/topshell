package com.github.ahnfelt.topshell

import com.github.ahnfelt.react4s.Loader.Loaded
import com.github.ahnfelt.react4s._

import scala.scalajs.js

case class BlockComponent(symbol : P[String], status : P[Loaded[js.Any]]) extends Component[NoEmit] {

    override def render(get : Get) = {
        E.div(
            ResultCss,
            E.div(ResultHeaderCss, Text(get(symbol))).when(!get(symbol).contains('_')),
            E.div(ResultBodyCss,
                get(status) match {
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
                    case "Pending" => E.span(E.i(A.className("fa fa-hourglass-half"), S.paddingRight.px(8)), Text("" + v("value")))
                    case "Runnable" => E.span(E.i(A.className("fa fa-play"), S.paddingRight.px(8)), Text("Ctrl + Enter"))
                    case "Computing" => E.div(E.span(SpinnerCss1), E.span(SpinnerCss2), E.span(SpinnerCss3), E.span(S.paddingLeft.px(8), Text("computing")))
                    case "Running" => E.div(E.span(SpinnerCss1), E.span(SpinnerCss2), E.span(SpinnerCss3))
                    case "Error" => E.span(CodeCss, S.color(Palette.textError), Text("" + v("value")))
                    case _ => Text(v("key") + ": " + v("value"))
                })
            } else {
                E(tagName, renderValue(v("children")))
            }
    }

}
