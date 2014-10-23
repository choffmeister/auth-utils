package de.choffmeister.auth.common

import java.util.Date

import org.specs2.mutable._
import spray.json._

class JsonWebTokenSpec extends Specification {
  def time(delta: Long) = new Date((System.currentTimeMillis + delta) / 1000L * 1000L)

  "JsonWebToken" should {
    "read and write tokens" in {
      val sec = "secret1".getBytes("ASCII")

      val jwt1 = JsonWebToken(subject = "jw", expiresAt = time(10000), claims = Map("foo" -> JsString("bar")))
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
      val sec = "secret1".getBytes("ASCII")

      val jwt1 = JsonWebToken(subject = "jw", expiresAt = time(-10000), claims = Map("foo" -> JsString("bar")))
      val s = JsonWebToken.write(jwt1, sec)
      val jwt2 = JsonWebToken.read(s, sec)
      jwt2 must beLeft(JsonWebToken.Expired(jwt1))

      jwt2.left.get.asInstanceOf[JsonWebToken.Expired].token.isExpired === true
    }
  }
}
