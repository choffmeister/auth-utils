package de.choffmeister.auth.akkahttp

import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.Directive1
import akka.http.scaladsl.server.directives.RouteDirectives._
import akka.http.scaladsl.server.directives._
import akka.pattern.after
import de.choffmeister.auth.akkahttp.util.SimpleScheduler
import de.choffmeister.auth.common.JsonWebToken
import de.choffmeister.auth.common.JsonWebToken._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Success

class Authenticator[Auth](
    realm: String,
    bearerTokenSecret: Array[Byte],
    fromUsernamePassword: (String, String) => Future[Option[Auth]],
    fromBearerToken: JsonWebToken => Future[Option[Auth]])(implicit executor: ExecutionContext) extends SecurityDirectives {
  def apply(): Directive1[Auth] = {
    bearerToken(acceptExpired = false).recover { rejs =>
      basic().recover(rejs2 => reject(rejs ++ rejs2: _*))
    }
  }

  def basic(minDelay: Option[FiniteDuration] = None): AuthenticationDirective[Auth] = {
    authenticateOrRejectWithChallenge[BasicHttpCredentials, Auth] {
      case Some(BasicHttpCredentials(username, password)) =>
        val auth = fromUsernamePassword(username, password)
        val delayedAuth = minDelay match {
          case None => auth
          case Some(delay) =>
            val delayed = after[Option[Auth]](delay, SimpleScheduler.instance)(Future(None))

            val promise = Promise[Option[Auth]]()
            auth.onComplete {
              case Success(Some(user)) => promise.success(Some(user))
              case _ => delayed.onComplete(_ => promise.success(None))
            }
            promise.future
        }

        delayedAuth map {
          case Some(user) => grant(user)
          case None => deny
        }

      case None => Future(deny)
    }
  }

  def bearerToken(acceptExpired: Boolean = false): AuthenticationDirective[Auth] = {
    def resolve(token: JsonWebToken): Future[AuthenticationResult[Auth]] = fromBearerToken(token).map {
      case Some(user) => grant(user)
      case None => deny(None)
    }

    authenticateOrRejectWithChallenge[OAuth2BearerToken, Auth] {
      case Some(OAuth2BearerToken(tokenStr)) =>
        JsonWebToken.read(tokenStr, bearerTokenSecret) match {
          case Right(token) => resolve(token)
          case Left(Expired(token)) if acceptExpired => resolve(token)
          case Left(error) => Future(deny(Some(error)))
        }
      case None => Future(deny(Some(Missing)))
    }
  }

  private def grant(user: Auth) = AuthenticationResult.success(user)
  private def deny = AuthenticationResult.failWithChallenge(createBasicChallenge)
  private def deny(error: Option[Error]) = AuthenticationResult.failWithChallenge(createBearerTokenChallenge(error))

  private def createBasicChallenge: HttpChallenge = {
    HttpChallenge("Basic", realm)
  }

  private def createBearerTokenChallenge(error: Option[Error]): HttpChallenge = {
    val desc = error match {
      case None => None
      case Some(Missing) => None
      case Some(Malformed) => Some("The access token is malformed")
      case Some(InvalidSignature) => Some("The access token has been manipulated")
      case Some(Incomplete) => Some("The token must at least contain the iat and exp claim")
      case Some(Expired(_)) => Some("The access token expired")
      case Some(UnsupportedAlgorithm(algo)) => Some(s"The signature algorithm $algo is not supported")
      case Some(Unknown) => Some("An unknown error occured")
    }
    val params = desc match {
      case Some(msg) => Map("error" -> "invalid_token", "error_description" -> msg)
      case None => Map.empty[String, String]
    }
    HttpChallenge("Bearer", realm, params)
  }
}
