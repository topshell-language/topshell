package com.github.ahnfelt.topshell.data

import scala.collection.immutable.{TreeMap, TreeSet}
import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("XMap")
object XMap {

    private implicit val ordering : Ordering[Any] = XOrder.ordering

    type XMap = TreeMap[Any, Any]
    type Fun[A, B] = js.Function1[A, B]

    @JSExport
    def isInstance(any : Any) : Boolean = any.isInstanceOf[scala.collection.immutable.TreeMap[_, _]]

    @JSExport
    def of(array : js.Array[js.Dynamic]) : XMap = {
        TreeMap(array.map(d => d.key -> d.value) : _*)
    }

    @JSExport
    def fromList(f : Fun[Any, Fun[Any, Any]], list : js.Array[js.Dynamic]) : XMap = {
        list.foldLeft(TreeMap.empty[Any, Any]) { (m, p) =>
            m + (p.key -> m.get(p.key).map(v => f(v)(p.value)).getOrElse(p.value))
        }
    }

    @JSExport
    def toList(map : XMap) : js.Array[Any] = {
        js.Array(map.toList.map { case (k, v) => js.Dictionary("key" -> k, "value" -> v) } : _*)
    }

    @JSExport
    def keys(map : XMap) : XSet.XSet = {
        map.keySet
    }

    @JSExport
    def add(key : Any, value : Any, map : XMap) : XMap = {
        map + (key -> value)
    }

    @JSExport
    def remove(key : Any, map : XMap) : XMap = {
        map - key
    }

    @JSExport
    def union(f : Fun[Any, Fun[Any, Any]], a : XMap, b : XMap) : XMap = {
        b.foldLeft(a) { (m, p) =>
            val (k, v) = p
            m + a.get(k).map(w => k -> f(w)(v)).getOrElse(k -> v)
        }
    }

    @JSExport
    def intersect(f : Fun[Any, Fun[Any, Any]], a : XMap, b : XMap) : XMap = {
        b.foldLeft(TreeMap.empty[Any, Any]) { (m, p) =>
            val (k, v) = p
            a.get(k).map(w => m + (k -> f(w)(v))).getOrElse(m)
        }
    }

    @JSExport
    def exclude(a : XSet.XSet, b : XMap) : XMap = {
        b -- a
    }

    @JSExport
    def get(key : Any, map : XMap) : Any = {
        map.get(key).map(v => js.Dictionary("_" -> "Some", "_1" -> v)).getOrElse(js.Dictionary("_" -> "None"))
    }

    @JSExport
    def getOrElse(value : Any, key : Any, map : XMap) : Any = {
        map.getOrElse(key, value)
    }

    @JSExport
    def has(key : Any, map : XMap) : Boolean = {
        map.contains(key)
    }

    @JSExport
    def from(key : Any, map : XMap) : XMap = {
        map.from(key)
    }

    @JSExport
    def until(key : Any, map : XMap) : XMap = {
        map.until(key)
    }

    @JSExport
    def foldLeft(f : Fun[Any, Fun[Any, Fun[Any, Any]]], z : Any, map : XMap) : Any = {
        map.foldLeft(z) { (v, p) => f(p._1)(p._2)(v) }
    }

    @JSExport
    def foldRight(f : Fun[Any, Fun[Any, Fun[Any, Any]]], z : Any, map : XMap) : Any = {
        map.foldRight(z) { (p, v) => f(p._1)(p._2)(v) }
    }

    @JSExport
    def size(map : XMap) : Int = {
        map.size
    }

    @JSExport
    def isEmpty(map : XMap) : Boolean = {
        map.isEmpty
    }

    @JSExport
    val empty : XMap = TreeMap.empty

}
