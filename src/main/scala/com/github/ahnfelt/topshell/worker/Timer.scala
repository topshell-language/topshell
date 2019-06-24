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

    private val accumulateMap = mutable.Map[String, (Long, Long)]()
    def accumulate[T](label : String)(body : => T) : T = {
        if(!enable) body else {
            val started = System.nanoTime()
            val result = body
            val elapsed = System.nanoTime() - started
            val (oldElapsed, oldCount) = accumulateMap.getOrElseUpdate(label, 0L -> 0L)
            accumulateMap(label) = (oldElapsed + elapsed, oldCount + 1)
            result
        }
    }

    def printAndClearAccumulated() : Unit = if(enable) {
        println()
        println("ACCUMULATED TIMERS:")
        for((label, (elapsed, count)) <- accumulateMap.toList.sorted) {
            println(label + ": " + elapsed / (1000 * 1000) + " ms, " + count + " calls")
        }
        accumulateMap.clear()
        println()
    }

}
