package com.udeshcoffee.android.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueStoreTest {

    @Test
    fun `encodes ids as reverse hex terminated by semicolons`() {
        // 26 = 0x1a -> least significant digit first.
        assertEquals("a1;0;f;", QueueStore.encode(listOf(26, 0, 15)))
    }

    @Test
    fun `skips negative ids`() {
        assertEquals("1;", QueueStore.encode(listOf(-5, 1)))
    }

    @Test
    fun `decodes what it encodes`() {
        val ids = listOf(0L, 1, 26, 4096, 123_456_789, Long.MAX_VALUE)
        assertEquals(ids, QueueStore.decode(QueueStore.encode(ids)))
    }

    @Test
    fun `decodes queues saved by older releases`() {
        assertEquals(listOf(26L, 255L), QueueStore.decode("a1;ff;"))
    }

    @Test
    fun `ignores an unterminated trailing id`() {
        assertEquals(listOf(26L), QueueStore.decode("a1;3"))
        assertEquals(emptyList<Long>(), QueueStore.decode(""))
    }
}
