package de.choffmeister.auth.common

import org.specs2.mutable._

class PasswordHashAlgorithmSpec extends Specification {
  "Plain" should {
    "pass through passwords" in {
      Plain.hash(Nil, "").right.get === Array[Byte]()
      Plain.hash(Nil, "A").right.get === Array[Byte](65)
    }
  }

  "PBKDF2" should {
    "hash passwords" in {
      PBKDF2.hash("hmac-sha1" :: "100" :: "32" :: Nil, "").isLeft === true
      PBKDF2.hash("hmac-sha1" :: "100" :: "32" :: Nil, "foobar").isLeft === true
      PBKDF2.hash("hmac-sha1" :: "100" :: "32" :: "ABCD" :: Nil, "").isRight === true
      PBKDF2.hash("hmac-sha1" :: "100" :: "32" :: "ABCD" :: Nil, "foobar").isRight === true
    }
  }

  "PasswordHasher" should {
    "run with plain as default" in {
      val hasher = new PasswordHasher("plain", Nil, List(Plain))
      val a = hasher.hash("")
      val b = hasher.hash("foobar")
      val c = hasher.hash("FOOBAR")

      a === "plain:"
      b === "plain:Zm9vYmFy"
      c === "plain:Rk9PQkFS"
      hasher.validate(a, "") === true
      hasher.validate(a, "!") === false
      hasher.validate(b, "foobar") === true
      hasher.validate(b, "FOOBAR") === false
      hasher.validate(c, "FOOBAR") === true
      hasher.validate(c, "foobar") === false
    }

    "run with pbkdf2:hmac-sha1 as default" in {
      val hasher = new PasswordHasher("pbkdf2", "hmac-sha1" :: "10" :: "32" :: Nil, List(PBKDF2, Plain))
      val a = hasher.hash("")
      val b = hasher.hash("foobar")
      val c = hasher.hash("FOOBAR")

      hasher.validate(a, "") === true
      hasher.validate(a, "!") === false
      hasher.validate(b, "foobar") === true
      hasher.validate(b, "FOOBAR") === false
      hasher.validate(c, "FOOBAR") === true
      hasher.validate(c, "foobar") === false
    }
  }
}
