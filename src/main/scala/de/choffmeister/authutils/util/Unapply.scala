package de.choffmeister.authutils.util

import de.choffmeister.authutils.util.Base64StringConverter._

private[authutils] object UnapplyInt {
  def unapply(str: String): Option[Int] = {
    try { Some(str.toInt) }
    catch { case _: NumberFormatException ⇒ None }
  }
}

private[authutils] object UnapplyByteArray {
  def unapply(str: String): Option[Array[Byte]] = Some(base64ToBytes(str))
}
