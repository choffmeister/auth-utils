package de.choffmeister.authutils

import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import de.choffmeister.authutils.util._
import de.choffmeister.authutils.util.Base64UrlStringConverter._
import spray.json._

class JsonWebTokenException(val message: String) extends Exception(message)

case class JsonWebToken(
    subject: String,
    claims: Map[String, JsValue] = Map.empty,
    createdAt: Date = new Date(System.currentTimeMillis / 1000L * 1000L),
    expiresAt: Date = new Date(System.currentTimeMillis / 1000L * 1000L)) {
  def isExpired = expiresAt.getTime < System.currentTimeMillis
  def nonExpired = !isExpired
}

object JsonWebToken {
  def read(str: String, secret: Array[Byte]): JsonWebToken = {
    val parts = str.split("\\.", -1)
    if (parts.length != 3) throw new JsonWebTokenException("Expected Json Web Token")

    val algo = JsonParser(base64ToString(parts(0))).asJsObject.getFields("typ", "alg") match {
      case Seq(JsString("JWT"), JsString("HS256")) ⇒
        "HmacSHA256"
      case _ ⇒
        throw new JsonWebTokenException("Expected Json Web Token with HMAC-SHA256 signature")
    }

    val s1 = base64ToBytes(parts(2))
    val s2 = hmac(algo, (parts(0) + "." + parts(1)).getBytes("ASCII"), secret)
    if (SequenceUtils.compareConstantTime(s1, s2) == false)
      throw new JsonWebTokenException("Json Web Token signature is invalid")

    val knownClaimNames = List("iat", "exp", "sub")
    val t1 = JsonParser(base64ToString(parts(1))).asJsObject
    val t2 = t1.fields.filter(f ⇒ knownClaimNames.contains(f._1)).map(_._2) match {
      case Seq(JsNumber(iat), JsNumber(exp), JsString(sub)) ⇒
        JsonWebToken(
          createdAt = new Date(iat.toLong * 1000L),
          expiresAt = new Date(exp.toLong * 1000L),
          subject = sub)
      case _ ⇒
        throw new JsonWebTokenException("Json Web Token must at least contain " + knownClaimNames.mkString(", ") + " claims")
    }

    t2.copy(claims = t1.fields.filter(f ⇒ !knownClaimNames.contains(f._1)))
  }

  def write(token: JsonWebToken, secret: Array[Byte]): String = {
    val h = JsObject("typ" -> JsString("JWT"), "alg" -> JsString("HS256"))
    val t = JsObject(Map(
      "iat" -> JsNumber(token.createdAt.getTime / 1000L),
      "exp" -> JsNumber(token.expiresAt.getTime / 1000L),
      "sub" -> JsString(token.subject)) ++ token.claims)

    val part12 = stringToBase64(h.toString) + "." + stringToBase64(t.toString)
    val part3 = bytesToBase64(hmac("HmacSHA256", part12.getBytes("ASCII"), secret))
    println(part12 + "." + part3)
    part12 + "." + part3
  }

  private def hmac(algorithm: String, data: Array[Byte], secret: Array[Byte]): Array[Byte] = {
    val hmac = Mac.getInstance(algorithm)
    val key = new SecretKeySpec(secret, algorithm)
    hmac.init(key)
    hmac.doFinal(data)
  }
}
