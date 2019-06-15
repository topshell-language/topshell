package com.github.ahnfelt.topshell.data

import scala.collection.immutable.{SortedSet, TreeMap, TreeSet}
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("XSet")
object XSet {

    private implicit val ordering : Ordering[Any] = XOrder.ordering

    type XSet = SortedSet[Any]
    type Fun[A, B] = js.Function1[A, B]

    @JSExport
    def isInstance(any : Any) : Boolean = any.isInstanceOf[scala.collection.immutable.SortedSet[_]]

    @JSExport
    def of(array : js.Array[Any]) : XSet = {
        TreeSet(array : _*)
    }

    @JSExport
    def toList(set : XSet) : js.Array[Any] = {
        js.Array(set.toList : _*)
    }

    @JSExport
    def toMap(f : Fun[Any, Any], set : XSet) : XMap.XMap = {
        set.foldLeft(XMap.empty) ( (m, v) => m + (v -> f(v)) )
    }

    @JSExport
    def add(value : Any, set : XSet) : XSet = {
        set + value
    }

    @JSExport
    def remove(value : Any, set : XSet) : XSet = {
        set - value
    }

    @JSExport
    def union(a : XSet, b : XSet) : XSet = {
        a ++ b
    }

    @JSExport
    def intersect(a : XSet, b : XSet) : XSet = {
        a.intersect(b)
    }

    @JSExport
    def exclude(a : XSet, b : XSet) : XSet = {
        b -- a
    }

    @JSExport
    def has(value : Any, set : XSet) : Boolean = {
        set.contains(value)
    }

    @JSExport
    def from(value : Any, set : XSet) : XSet = {
        set.from(value)
    }

    @JSExport
    def until(value : Any, set : XSet) : XSet = {
        set.until(value)
    }

    @JSExport
    def foldLeft(f : Fun[Any, Fun[Any, Any]], z : Any, set : XSet) : Any = {
        set.foldLeft(z) { (v, p) => f(p)(v) }
    }

    @JSExport
    def foldRight(f : Fun[Any, Fun[Any, Any]], z : Any, set : XSet) : Any = {
        set.foldRight(z) { (p, v) => f(p)(v) }
    }

    @JSExport
    def size(set : XSet) : Int = {
        set.size
    }

    @JSExport
    def isEmpty(set : XSet) : Boolean = {
        set.isEmpty
    }

    @JSExport
    val empty : XSet = TreeSet.empty

}
