package de.choffmeister.auth.common

import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import de.choffmeister.auth.common.util._
import de.choffmeister.auth.common.util.Base64UrlStringConverter._
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
  def read(str: String, secret: Array[Byte]): Either[Error, JsonWebToken] = str.split("\\.", -1).toList match {
    case headerStr :: tokenStr :: signatureStr :: Nil ⇒
      JsonParser(base64ToString(headerStr)).asJsObject.getFields("typ", "alg") match {
        case Seq(JsString("JWT"), JsString(algorithm)) ⇒
          sign(algorithm, (headerStr + "." + tokenStr).getBytes("ASCII"), secret) match {
            case Some(signature) ⇒
              if (SequenceUtils.compareConstantTime(base64ToBytes(signatureStr), signature)) {
                val knownClaimNames = List("iat", "exp", "sub")
                val tokenRaw = JsonParser(base64ToString(tokenStr)).asJsObject
                tokenRaw.fields.filter(f ⇒ knownClaimNames.contains(f._1)).map(_._2) match {
                  case Seq(JsNumber(iat), JsNumber(exp), JsString(sub)) ⇒
                    val token = JsonWebToken(
                      createdAt = new Date(iat.toLong * 1000L),
                      expiresAt = new Date(exp.toLong * 1000L),
                      subject = sub).copy(claims = tokenRaw.fields.filter(f ⇒ !knownClaimNames.contains(f._1)))
                    if (token.nonExpired) Right(token)
                    else Left(Expired(token))
                  case _ ⇒ Left(Incomplete)
                }
              } else Left(InvalidSignature)
            case None ⇒ Left(UnsupportedAlgorithm(algorithm))
          }
        case _ ⇒ Left(Malformed)
      }
    case _ ⇒ Left(Malformed)
  }

  def write(token: JsonWebToken, secret: Array[Byte]): String = {
    val h = JsObject("typ" -> JsString("JWT"), "alg" -> JsString("HS256"))
    val t = JsObject(Map(
      "iat" -> JsNumber(token.createdAt.getTime / 1000L),
      "exp" -> JsNumber(token.expiresAt.getTime / 1000L),
      "sub" -> JsString(token.subject)) ++ token.claims)

    val part12 = stringToBase64(h.toString) + "." + stringToBase64(t.toString)
    val part3 = bytesToBase64(sign("HS256", part12.getBytes("ASCII"), secret).get)
    part12 + "." + part3
  }

  private def sign(algorithm: String, data: Array[Byte], secret: Array[Byte]): Option[Array[Byte]] = algorithm match {
    case "HS256" ⇒
      val hmac = Mac.getInstance("HmacSHA256")
      val key = new SecretKeySpec(secret, algorithm)
      hmac.init(key)
      Some(hmac.doFinal(data))
    case _ ⇒ None
  }

  sealed trait Error
  case object Missing extends Error
  case object Malformed extends Error
  case object InvalidSignature extends Error
  case object Incomplete extends Error
  case class Expired(token: JsonWebToken) extends Error
  case class UnsupportedAlgorithm(algorithm: String) extends Error
}
