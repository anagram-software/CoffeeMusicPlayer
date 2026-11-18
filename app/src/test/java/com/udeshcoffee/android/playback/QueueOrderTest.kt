package com.udeshcoffee.android.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import kotlin.random.Random

class QueueOrderTest {

    private val queue = (1..20).map { "s$it" }

    @Test
    fun `shuffle puts the current item first and keeps every item`() {
        val shuffled = QueueOrder.shuffledAround(queue, current = 7, random = Random(1))
        assertEquals("s8", shuffled.first())
        assertEquals(queue.sorted(), shuffled.sorted())
        assertNotEquals(queue, shuffled)
    }

    @Test
    fun `shuffle leaves tiny or invalid input alone`() {
        assertEquals(listOf("a"), QueueOrder.shuffledAround(listOf("a"), 0))
        assertEquals(queue, QueueOrder.shuffledAround(queue, current = 99))
    }

    @Test
    fun `restore undoes a shuffle`() {
        val shuffled = QueueOrder.shuffledAround(queue, current = 3, random = Random(7))
        assertEquals(queue, QueueOrder.restored(shuffled, original = queue))
    }

    @Test
    fun `restore drops removed songs and appends songs added while shuffled`() {
        val original = listOf("a", "b", "c", "d")
        val now = listOf("c", "new1", "a", "new2") // b and d removed, two added
        assertEquals(listOf("a", "c", "new1", "new2"), QueueOrder.restored(now, original))
    }

    @Test
    fun `restore matches duplicates one by one`() {
        val original = listOf("a", "b", "a")
        assertEquals(listOf("a", "b", "a"), QueueOrder.restored(listOf("b", "a", "a"), original))
        // One "a" was removed: the remaining one takes the first "a" slot.
        assertEquals(listOf("a", "b"), QueueOrder.restored(listOf("b", "a"), original))
    }
}
