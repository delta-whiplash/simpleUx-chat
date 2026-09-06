package chat.simplex.matrix.adapter

import org.junit.Assert.*
import org.junit.Test

/** Id scheme contract (#135): "mx|" prefix, stability, no SimpleX collisions. */
class MatrixAdapterIdsTest {

    @Test
    fun `chat id starts with mx prefix`() {
        assertEquals("mx|!abc:example.org", MatrixAdapter.matrixChatId("!abc:example.org"))
    }

    @Test
    fun `chat id is stable across calls`() {
        val a = MatrixAdapter.matrixChatId("!room:server.org")
        val b = MatrixAdapter.matrixChatId("!room:server.org")
        assertEquals(a, b)
    }

    @Test
    fun `distinct rooms yield distinct chat ids`() {
        assertNotEquals(
            MatrixAdapter.matrixChatId("!a:server.org"),
            MatrixAdapter.matrixChatId("!b:server.org")
        )
    }

    @Test
    fun `roomId is recoverable from chat id`() {
        val roomId = "!xYz9:matrix.org"
        assertEquals(roomId, MatrixAdapter.roomIdFromChatId(MatrixAdapter.matrixChatId(roomId)))
    }

    @Test
    fun `matrix chat ids never collide with SimpleX id patterns`() {
        // SimpleX chat ids: @N direct, #N group, *N note folder, :N connection, ?N invalid.
        val simplexIds = listOf("@1", "@12345", "#7", "#999", "*2", ":12", "?3", "@0")
        val matrixIds = listOf(
            MatrixAdapter.matrixChatId("!abc:example.org"),
            MatrixAdapter.matrixChatId("#fake:example.org"),
            MatrixAdapter.matrixChatId("@looksSimplex:example.org"),
            MatrixAdapter.matrixChatId("*star:example.org"),
        )
        for (m in matrixIds) {
            for (s in simplexIds) {
                assertNotEquals("collision between '$m' and '$s'", s, m)
            }
        }
    }

    @Test
    fun `matrix chat ids are recognizable by prefix`() {
        // The single documented distinction mechanism for gating/badge UI.
        assertTrue(MatrixAdapter.matrixChatId("!a:b").startsWith("mx|"))
    }

    @Test
    fun `stableItemId is deterministic`() {
        val a = MatrixAdapter.stableItemId("\$event1:server.org")
        val b = MatrixAdapter.stableItemId("\$event1:server.org")
        assertEquals(a, b)
    }

    @Test
    fun `stableItemId is positive and non-zero`() {
        assertTrue(MatrixAdapter.stableItemId("\$event1:server.org") > 0)
        assertNotEquals(0L, MatrixAdapter.stableItemId("\$anything"))
    }

    @Test
    fun `stableItemId distinguishes distinct events`() {
        assertNotEquals(
            MatrixAdapter.stableItemId("\$event1:server.org"),
            MatrixAdapter.stableItemId("\$event2:server.org")
        )
    }

    @Test
    fun `stableItemId is stable under equal-hash-code-resistant inputs`() {
        // "Aa" vs "BB" share a JVM String.hashCode; FNV-1a-64 must not share.
        assertNotEquals(MatrixAdapter.stableItemId("Aa"), MatrixAdapter.stableItemId("BB"))
    }
}
