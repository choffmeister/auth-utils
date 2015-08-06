package de.choffmeister.auth.common.util

import org.specs2.mutable.Specification

class UnapplySpec extends Specification {
  "UnapplyInt" in {
    UnapplyInt.unapply("0") must beSome(0)
    UnapplyInt.unapply("-128") must beSome(-128)
    UnapplyInt.unapply("128") must beSome(128)
    UnapplyInt.unapply("not-a-number") must beNone
  }

  "UnapplyByteArray" in {
    UnapplyByteArray.unapply("AAECA/8=").map(_.toList) must beSome(List[Byte](0, 1, 2, 3, 255.toByte))
  }
}
