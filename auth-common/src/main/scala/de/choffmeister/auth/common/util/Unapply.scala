package de.choffmeister.auth.common.util

import de.choffmeister.auth.common.util.Base64StringConverter._

private[auth] object UnapplyInt {
  def unapply(str: String): Option[Int] = {
    try { Some(str.toInt) }
    catch { case _: NumberFormatException ⇒ None }
  }
}

private[auth] object UnapplyByteArray {
  def unapply(str: String): Option[Array[Byte]] = Some(base64ToBytes(str))
}
