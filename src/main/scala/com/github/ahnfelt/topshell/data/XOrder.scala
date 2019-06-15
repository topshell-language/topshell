package com.github.ahnfelt.topshell.data

import scala.collection.immutable.TreeMap
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("XOrder")
object XOrder {

    val ordering : Ordering[Any] = (l : Any, r : Any) => (l, r) match {
        case (a : js.Array[_], b : js.Array[_]) =>
            a.zip(b).find { case (x, y) =>
                ordering.compare(x, y) != 0
            }.map { case (x, y) => ordering.compare(x, y) }.getOrElse {
                ordering.compare(a.length, b.length)
            }
        case (a : String, b : String) => a.compareTo(b)
        case (a : Double, b : Double) => a.compareTo(b)
        case (a : Int, b : Int) => a.compareTo(b)
        case _ =>
            val a = l.asInstanceOf[js.Dynamic]
            val b = r.asInstanceOf[js.Dynamic]
            val v = ordering.compare(a.key, b.key)
            if(v != 0) v else ordering.compare(a.value, b.value)
    }

    @JSExport
    def min(a : Any, b : Any) : Any = ordering.min(a, b)

    @JSExport
    def max(a : Any, b : Any) : Any = ordering.max(a, b)

    @JSExport
    def equal(a : Any, b : Any) : Boolean = ordering.equiv(a, b)

    @JSExport
    def notEqual(a : Any, b : Any) : Boolean = !ordering.equiv(a, b)

    @JSExport
    def less(a : Any, b : Any) : Boolean = ordering.lt(a, b)

    @JSExport
    def lessEqual(a : Any, b : Any) : Boolean = ordering.lteq(a, b)

    @JSExport
    def greater(a : Any, b : Any) : Boolean = ordering.gt(a, b)

    @JSExport
    def greaterEqual(a : Any, b : Any) : Boolean = ordering.gteq(a, b)

}
