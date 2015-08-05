package de.choffmeister.auth.common

import spray.json._

case class OAuth2AccessTokenResponse(tokenType: String, accessToken: String, expiresIn: Long)

object OAuth2AccessTokenResponseFormat extends RootJsonFormat[OAuth2AccessTokenResponse] {
  def write(obj: OAuth2AccessTokenResponse) = JsObject(
    "token_type" -> JsString(obj.tokenType),
    "access_token" -> JsString(obj.accessToken),
    "expires_in" -> JsNumber(obj.expiresIn)
  )
  def read(value: JsValue) =
    value.asJsObject.getFields("token_type", "access_token", "expires_in") match {
      case Seq(JsString(tokenType), JsString(accessToken), JsNumber(expiresIn)) =>
        OAuth2AccessTokenResponse(tokenType, accessToken, expiresIn.toLong)
      case _ =>
        throw new DeserializationException("OAuth2 token response expected")
    }
}
