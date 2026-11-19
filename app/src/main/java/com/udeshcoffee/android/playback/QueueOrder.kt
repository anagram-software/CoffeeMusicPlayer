package com.udeshcoffee.android.playback

import kotlin.random.Random

/** Pure ordering rules behind the app's visible shuffle (see [SessionPlayer]). */
object QueueOrder {

    /** The item at [current] first, then every other item in random order. */
    fun <T> shuffledAround(items: List<T>, current: Int, random: Random = Random.Default): List<T> {
        if (items.size < 2 || current !in items.indices) return items
        return listOf(items[current]) + items.filterIndexed { i, _ -> i != current }.shuffled(random)
    }

    /**
     * The pre-shuffle order of [items]: songs still queued keep their place in [original]
     * (duplicates are matched one by one), songs added while shuffled follow in their current order.
     */
    fun <T> restored(items: List<T>, original: List<T>): List<T> {
        val remaining = items.toMutableList()
        val target = ArrayList<T>(items.size)
        for (item in original) if (remaining.remove(item)) target.add(item)
        target.addAll(remaining)
        return target
    }
}
