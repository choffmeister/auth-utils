package de.choffmeister.auth.common.util

import org.specs2.mutable.Specification

class NonceGeneratorSpec extends Specification {
  "NonceGenerator" in {
    val b1 = NonceGenerator.generateBytes(32)
    val b2 = NonceGenerator.generateBytes(32)
    b1 must haveLength(32)
    b2 must haveLength(32)
    b1 !== b2

    val s1 = NonceGenerator.generateString(32)
    val s2 = NonceGenerator.generateString(32)
    s1 must haveLength(64)
    s2 must haveLength(64)
    s1 !== s2
  }
}
