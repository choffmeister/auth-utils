package de.choffmeister.authutils.util

import java.security.SecureRandom

private[authutils] object NonceGenerator {
  lazy val random = new SecureRandom()

  def generateBytes(length: Int) = {
    val bytes = new Array[Byte](length)
    random.nextBytes(bytes)
    bytes
  }

  def generateString(length: Int): String = generateBytes(length).map("%02x" format _).mkString
}
