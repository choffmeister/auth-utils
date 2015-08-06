package de.choffmeister.auth.common.util

import org.specs2.mutable.Specification

class BinaryStringConverterSpec extends Specification {
  "HexStringConverter" in {
    HexStringConverter.bytes2hex(List[Byte](0, 1, 255.toByte).toArray) === "0001ff"
    HexStringConverter.hex2bytes("0001ff").toList === List[Byte](0, 1, 255.toByte)
  }

  "Base64StringConverter" in {
    Base64StringConverter.base64ToBytes("dGVzdA==").toList === "test".getBytes.toList
    Base64StringConverter.base64ToString("dGVzdA==") === "test"
    Base64StringConverter.bytesToBase64("test".getBytes) === "dGVzdA=="
    Base64StringConverter.stringToBase64("test") === "dGVzdA=="

    Base64StringConverter.base64ToBytes("AAECA/8=").toList === List[Byte](0, 1, 2, 3, 255.toByte)
    Base64StringConverter.bytesToBase64(List[Byte](0, 1, 2, 3, 255.toByte).toArray) === "AAECA/8="
  }

  "Base64UrlStringConverter" in {
    Base64UrlStringConverter.base64ToBytes("dGVzdA").toList === "test".getBytes.toList
    Base64UrlStringConverter.base64ToString("dGVzdA") === "test"
    Base64UrlStringConverter.bytesToBase64("test".getBytes) === "dGVzdA"
    Base64UrlStringConverter.stringToBase64("test") === "dGVzdA"

    Base64UrlStringConverter.base64ToBytes("AAECA_8").toList === List[Byte](0, 1, 2, 3, 255.toByte)
    Base64UrlStringConverter.bytesToBase64(List[Byte](0, 1, 2, 3, 255.toByte).toArray) === "AAECA_8"
  }
}
