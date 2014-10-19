package de.choffmeister.authutils.util

import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.binary.Base64

private[authutils] object HexStringConverter {
  def hex2bytes(hex: String): Array[Byte] = {
    Hex.decodeHex(hex.toCharArray)
  }

  def bytes2hex(bytes: Array[Byte]): String = {
    Hex.encodeHex(bytes).mkString("")
  }
}

private[authutils] object Base64StringConverter {
  private lazy val base64 = new Base64(80, Array.empty[Byte], false)

  def base64ToBytes(str: String): Array[Byte] = {
    base64.decode(str.getBytes("ASCII"))
  }

  def bytesToBase64(bytes: Array[Byte]): String = {
    new String(base64.encode(bytes), "ASCII")
  }

  def base64ToString(str: String): String = {
    new String(base64ToBytes(str), "UTF-8")
  }

  def stringToBase64(str: String): String = {
    bytesToBase64(str.getBytes("UTF-8"))
  }
}

private[authutils] object Base64UrlStringConverter {
  private lazy val base64 = new Base64(80, Array.empty[Byte], true)

  def base64ToBytes(str: String): Array[Byte] = {
    base64.decode(str.getBytes("ASCII"))
  }

  def bytesToBase64(bytes: Array[Byte]): String = {
    new String(base64.encode(bytes), "ASCII")
  }

  def base64ToString(str: String): String = {
    new String(base64ToBytes(str), "UTF-8")
  }

  def stringToBase64(str: String): String = {
    bytesToBase64(str.getBytes("UTF-8"))
  }
}
