package de.choffmeister.auth.common

import java.time.Instant

import org.specs2.mutable._
import spray.json._

class JsonWebTokenSpec extends Specification {
  def time(delta: Long) = Instant.ofEpochSecond(System.currentTimeMillis / 1000L + delta)

  "JsonWebToken" should {
    "read and write tokens" in {
      val sec = "secret".getBytes("ASCII")

      val jwt1 = JsonWebToken(subject = "jw", expiresAt = time(+60), claims = Map("foo" -> JsString("bar")))
      val s = JsonWebToken.write(jwt1, sec)
      val jwt2 = JsonWebToken.read(s, sec)
      jwt2 must beRight(jwt1)
    }

    "reject tokens with invalid signature" in {
      val sec1 = "secret1".getBytes("ASCII")
      val sec2 = "secret2".getBytes("ASCII")

      val jwt1 = JsonWebToken(subject = "jw", claims = Map("foo" -> JsString("bar")))
      val s = JsonWebToken.write(jwt1, sec1)
      val jwt2 = JsonWebToken.read(s, sec2)
      jwt2 must beLeft(JsonWebToken.InvalidSignature)
    }

    "recognize expiration" in {
      val sec = "secret".getBytes("ASCII")

      val jwt1 = JsonWebToken(subject = "jw", expiresAt = time(-60), claims = Map("foo" -> JsString("bar")))
      val s = JsonWebToken.write(jwt1, sec)
      val jwt2 = JsonWebToken.read(s, sec)
      jwt2 must beLeft(JsonWebToken.Expired(jwt1))

      jwt2.left.get.asInstanceOf[JsonWebToken.Expired].token.isExpired === true
    }

    "properly handle malformed tokens" in {
      val sec = "secret".getBytes("ASCII")

      JsonWebToken.read("123h9123h", sec) must beLeft(JsonWebToken.Malformed)
      JsonWebToken.read("123h9123hiuhji.123123123.12323h12i", sec) must beLeft(JsonWebToken.Malformed)
    }
  }
}
