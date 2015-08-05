package de.choffmeister.auth.spray

import de.choffmeister.auth.common._
import spray.http.HttpHeaders._
import spray.http._
import spray.routing.AuthenticationFailedRejection.{ CredentialsMissing => Missing, CredentialsRejected => Rejected }
import spray.routing.authentication._
import spray.routing.{ AuthenticationFailedRejection => AuthRejection, _ }
import spray.util._

import scala.concurrent._

/** See http://tools.ietf.org/html/rfc6750 */
class OAuth2BearerTokenHttpAuthenticator[U](val realm: String, val secret: Array[Byte], user: String => Future[Option[U]])(implicit val executionContext: ExecutionContext)
    extends HttpAuthenticator[U] { self =>
  import OAuth2BearerTokenHttpAuthenticator._

  override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): Future[Option[U]] = credentials match {
    case Some(bt: OAuth2BearerToken) => JsonWebToken.read(bt.token, secret) match {
      case Right(token) => user(token.subject)
      case Left(err) => Future(None)
    }
    case _ => Future(None)
  }

  override def getChallengeHeaders(req: HttpRequest): List[HttpHeader] = extractJsonWebToken(req) match {
    case Left(JsonWebToken.InvalidSignature) => convertToChallengeHeaders(TokenManipulated)
    case Left(JsonWebToken.Expired(_)) => convertToChallengeHeaders(TokenExpired)
    case Left(JsonWebToken.Missing) => convertToChallengeHeaders(TokenMissing)
    case _ => convertToChallengeHeaders(TokenMalformed)
  }

  def extractJsonWebToken(req: HttpRequest): Either[JsonWebToken.Error, JsonWebToken] = {
    val authHeader = req.headers.findByType[`Authorization`]
    val credentials = authHeader.map { case Authorization(creds) => creds }
    credentials match {
      case Some(bt: OAuth2BearerToken) => JsonWebToken.read(bt.token, secret)
      case _ => Left(JsonWebToken.Missing)
    }
  }

  private def convertToChallengeHeaders(error: Error): List[HttpHeader] = {
    val desc = error match {
      case TokenMissing => None
      case TokenMalformed => Some("The access token is malformed")
      case TokenManipulated => Some("The access token has been manipulated")
      case TokenExpired => Some("The access token expired")
      case TokenSubjectRejected => Some("The access token subject has been rejected")
      case _ => Some("An unknown error occured")
    }
    val params = desc match {
      case Some(msg) => Map("error" -> "invalid_token", "error_description" -> msg)
      case None => Map.empty[String, String]
    }
    `WWW-Authenticate`(HttpChallenge(scheme = "Bearer", realm = realm, params = params)) :: Nil
  }

  def withoutExpiration = new HttpAuthenticator[U] {
    implicit val executionContext: ExecutionContext = self.executionContext

    def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): Future[Option[U]] = credentials match {
      case Some(bt: OAuth2BearerToken) => JsonWebToken.read(bt.token, secret) match {
        case Right(token) => user(token.subject)
        case Left(JsonWebToken.Expired(token)) => user(token.subject)
        case Left(err) => Future(None)
      }
      case _ => Future(None)
    }

    def getChallengeHeaders(req: spray.http.HttpRequest): List[HttpHeader] = self.getChallengeHeaders(req)
  }
}

object OAuth2BearerTokenHttpAuthenticator {
  abstract sealed class Error
  case object TokenMalformed extends Error
  case object TokenManipulated extends Error
  case object TokenExpired extends Error
  case object TokenMissing extends Error
  case object TokenSubjectRejected extends Error
}
