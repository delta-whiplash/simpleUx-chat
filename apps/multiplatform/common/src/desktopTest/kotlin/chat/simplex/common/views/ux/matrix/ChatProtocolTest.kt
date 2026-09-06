package chat.simplex.common.views.ux.matrix

import kotlin.test.Test
import kotlin.test.assertEquals

class ChatProtocolTest {
  @Test
  fun matrixIdsAreRecognized() {
    assertEquals(ChatProtocol.Matrix, chatProtocolFromId("mx|!room:server"))
    assertEquals(ChatProtocol.Matrix, chatProtocolFromId("mx|@user:matrix.org"))
    assertEquals(ChatProtocol.Matrix, chatProtocolFromId("mx|"))
  }

  @Test
  fun simplexIdsAreNotMatrix() {
    // SimpleX direct-contact ids look like "<@1"; group ids are numeric.
    assertEquals(ChatProtocol.SimpleX, chatProtocolFromId("<@1"))
    assertEquals(ChatProtocol.SimpleX, chatProtocolFromId("17"))
    assertEquals(ChatProtocol.SimpleX, chatProtocolFromId(""))
  }

  @Test
  fun prefixMustBeAtStart() {
    assertEquals(ChatProtocol.SimpleX, chatProtocolFromId("xmx|!room:server"))
    assertEquals(ChatProtocol.SimpleX, chatProtocolFromId("17 mx|"))
  }
}
