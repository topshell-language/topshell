package com.github.ahnfelt.topshell.worker

import scala.collection.mutable

object Timer {

    val enable = false

    def time[T](label : String)(body : => T) : T = {
        if(enable) println("Begun " + label + "...")
        val started = System.currentTimeMillis()
        val result = body
        if(enable) println("Finished " + label + ": " + (System.currentTimeMillis() - started) + " ms")
        result
    }

    val accumulateMap = mutable.Map[String, Long]()
    def accumulate[T](label : String)(body : => T) : T = {
        if(!enable) body else {
            val started = System.nanoTime()
            val result = body
            val elapsed = System.nanoTime() - started
            val total = accumulateMap.getOrElseUpdate(label, 0L) + elapsed
            accumulateMap(label) = total
            result
        }
    }

    def printAndClearAccumulated() : Unit = if(enable) {
        println()
        println("ACCUMULATED TIMERS:")
        for((label, total) <- accumulateMap.toList.sorted) println(label + ": " + total / (1000 * 1000) + " ms")
        accumulateMap.clear()
        println()
    }

}
