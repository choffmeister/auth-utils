package de.choffmeister.auth.common

import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import de.choffmeister.auth.common.util.Base64UrlStringConverter._
import de.choffmeister.auth.common.util._
import spray.json._

@deprecated("Please switch to https://github.com/choffmeister/microservice-utils", since = "0.3.2")
class JsonWebTokenException(val message: String) extends Exception(message)

@deprecated("Please switch to https://github.com/choffmeister/microservice-utils", since = "0.3.2")
case class JsonWebToken(
    claims: Map[String, JsValue] = Map.empty,
    createdAt: Instant = Instant.ofEpochSecond(System.currentTimeMillis / 1000L),
    expiresAt: Instant = Instant.ofEpochSecond(System.currentTimeMillis / 1000L)) {
  def claimAsString(claim: String) = claims.find(_._1 == claim) match {
    case Some((_, value: JsString)) => Right(value.value)
    case Some((_, _)) => Left(s"Claim $claim is not of type String")
    case _ => Left(s"Claim $claim is missing")
  }
  def isExpired = expiresAt.toEpochMilli < System.currentTimeMillis
  def nonExpired = !isExpired
}

@deprecated("Please switch to https://github.com/choffmeister/microservice-utils", since = "0.3.2")
object JsonWebToken {
  def read(str: String, secret: Array[Byte]): Either[Error, JsonWebToken] = {
    try {
      str.split("\\.", -1).toList match {
        case headerStr :: tokenStr :: signatureStr :: Nil =>
          JsonParser(base64ToString(headerStr)).asJsObject.getFields("typ", "alg") match {
            case Seq(JsString("JWT"), JsString(algorithm)) =>
              sign(algorithm, (headerStr + "." + tokenStr).getBytes("ASCII"), secret) match {
                case Some(signature) =>
                  if (SequenceUtils.compareConstantTime(base64ToBytes(signatureStr), signature)) {
                    val knownClaimNames = List("exp", "iat")
                    val tokenRaw = JsonParser(base64ToString(tokenStr)).asJsObject
                    tokenRaw.fields.filter(f => knownClaimNames.contains(f._1)).toSeq.sortBy(_._1).map(_._2) match {
                      case Seq(JsNumber(exp), JsNumber(iat)) =>
                        val token = JsonWebToken(
                          createdAt = Instant.ofEpochSecond(iat.toLong),
                          expiresAt = Instant.ofEpochSecond(exp.toLong)
                        ).copy(claims = tokenRaw.fields.filter(f => !knownClaimNames.contains(f._1)))
                        if (token.nonExpired) Right(token)
                        else Left(Expired(token))
                      case _ => Left(Incomplete)
                    }
                  } else Left(InvalidSignature)
                case None => Left(UnsupportedAlgorithm(algorithm))
              }
            case _ => Left(Malformed)
          }
        case _ => Left(Malformed)
      }
    } catch {
      case _: JsonParser.ParsingException => Left(Malformed)
      case _: Exception => Left(Unknown)
    }
  }

  def write(token: JsonWebToken, secret: Array[Byte]): String = {
    val h = JsObject("typ" -> JsString("JWT"), "alg" -> JsString("HS256"))
    val t = JsObject(Map(
      "iat" -> JsNumber(token.createdAt.getEpochSecond),
      "exp" -> JsNumber(token.expiresAt.getEpochSecond)
    ) ++ token.claims)

    val part12 = stringToBase64(h.toString) + "." + stringToBase64(t.toString)
    val part3 = bytesToBase64(sign("HS256", part12.getBytes("ASCII"), secret).get)
    part12 + "." + part3
  }

  private def sign(algorithm: String, data: Array[Byte], secret: Array[Byte]): Option[Array[Byte]] = algorithm match {
    case "HS256" =>
      val hmac = Mac.getInstance("HmacSHA256")
      val key = new SecretKeySpec(secret, algorithm)
      hmac.init(key)
      Some(hmac.doFinal(data))
    case _ => None
  }

  sealed trait Error
  case object Unknown extends Error
  case object Missing extends Error
  case object Malformed extends Error
  case object InvalidSignature extends Error
  case object Incomplete extends Error
  case class Expired(token: JsonWebToken) extends Error
  case class UnsupportedAlgorithm(algorithm: String) extends Error
}
