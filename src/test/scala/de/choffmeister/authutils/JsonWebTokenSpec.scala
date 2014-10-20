package de.choffmeister.authutils

import java.util.Date

import org.specs2.mutable._
import spray.json._

class JsonWebTokenSpec extends Specification {
  def time(delta: Long) = new Date(System.currentTimeMillis + delta)

  "JsonWebToken" should {
    "read and write tokens" in {
      val sec1 = "secret1".getBytes("ASCII")
      val sec2 = "secret2".getBytes("ASCII")

      val jwt1 = JsonWebToken(subject = "jw", claims = Map("foo" -> JsString("bar")))
      val s = JsonWebToken.write(jwt1, sec1)
      val jwt2 = JsonWebToken.read(s, sec1)
      jwt1 === jwt2
    }

    "reject tokens with invalid signature" in {
      val sec1 = "secret1".getBytes("ASCII")
      val sec2 = "secret2".getBytes("ASCII")

      val jwt1 = JsonWebToken(subject = "jw", claims = Map("foo" -> JsString("bar")))
      val s = JsonWebToken.write(jwt1, sec1)
      JsonWebToken.read(s, sec2) must throwA("signature is invalid")
    }

    "recognize expiration" in {
      val jwt1 = JsonWebToken(subject = "jw", expiresAt = time(-10000))
      val jwt2 = JsonWebToken(subject = "jw", expiresAt = time(+10000))

      jwt1.isExpired === true
      jwt2.isExpired === false
    }
  }
}
