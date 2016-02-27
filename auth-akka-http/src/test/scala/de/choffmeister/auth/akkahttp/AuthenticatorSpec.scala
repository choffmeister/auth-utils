package de.choffmeister.auth.akkahttp

import akka.http.scaladsl.model.headers.{BasicHttpCredentials, HttpChallenge, OAuth2BearerToken}
import akka.http.scaladsl.server.AuthenticationFailedRejection
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}
import akka.http.scaladsl.server.Directives._
import org.specs2.mutable._
import spray.json.JsString

import scala.concurrent._
import scala.concurrent.duration._

case class User(id: Int, username: String, password: String)

class AuthenticatorSpec extends Specification with Specs2RouteTest {
  val users = User(1, "user1", "pass1") :: User(2, "user2", "pass2") :: Nil

  // generate with http://jwt.io/
  val jwtUser1Exp1970 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNDM4ODA0MTY4MjkyLCJleHAiOjB9.ntBAmd6DFfhrsOHyBHeM4uZFNjQwu6Fd-SVfFFE1khs"
  val jwtUser1Exp2170 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNDM4ODA0MTY4MjkyLCJleHAiOjYzMTE0MzM2MDAwMDB9.UB8qiTHGaZWs2PE3Py6PWo_aX_w4Or7th1Czs_a0XVg"
  val jwtUser2Exp1970 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwiaWF0IjoxNDM4ODA0MTY4MjkyLCJleHAiOjB9.JMEh_Crn4hd2YlO5f_MJRMToow_A0vcn5GP2vV4OaOQ"
  val jwtUser2Exp2170 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIyIiwiaWF0IjoxNDM4ODA0MTY4MjkyLCJleHAiOjYzMTE0MzM2MDAwMDB9.NnTF1tq_r8BS5fOgR5IZQUGh09kGHW5qvGv_74O3vxU"
  val jwtUserUnknownExp1970 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1bmtub3duIiwiaWF0IjoxNDM4ODA0MTY4MjkyLCJleHAiOjB9.st5j0nvk1RTHZzDaci6C3ZGypDLEPfEJn2E9xDOYbCc"
  val jwtUserUnknownExp2170 = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1bmtub3duIiwiaWF0IjoxNDM4ODA0MTY4MjkyLCJleHAiOjYzMTE0MzM2MDAwMDB9.W-FFZoX0Ep7QDf-HHHfrYmaRclS3kXGC3MEhMS954oY"
  val jwtIncomplete = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1bmtub3duIiwiZXhwIjowfQ.7bMVM53kCY5nNAsjpap2eNteDnP-_aaD4hADx5Yb30M"
  val jwtInvalidSignature = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNDM4ODA0MTY4MjkyLCJleHAiOjB9.XtBAmd6DFfhrsOHyBHeM4uZFNjQwu6Fd-SVfFFE1khs"
  val jwtMalformed = "EyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwiaWF0IjoxNDM4ODA0MTY4MjkyLCJleHAiOjB9.ntBAmd6DFfhrsOHyBHeM4uZFNjQwu6Fd-SVfFFE1khs"
  val jwtRsa = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1bmtub3duIiwiZXhwIjowfQ.2dtTcDpntzQJEtRuFKOkruSy7eOGUdBsWTKttHuU5q0"

  val authenticator = new Authenticator[User](
    realm = "realm",
    bearerTokenSecret = "secret".getBytes(),
    fromUsernamePassword = (username, password) => Future {
      users.find(_.username == username) match {
        case Some(user) =>
          user.password == password match {
            case true => Some(user)
            case false => None
          }
        case None =>
          None
      }
    },
    fromBearerToken = token => Future {
      users.find(u => token.claimAsString("sub") == Right(u.id.toString))
    }
  )

  val routes =
    path("basic") {
      parameter('delay.as[Int].?) { delay =>
        authenticator.basic(delay.map(_.milliseconds)) { user =>
          complete(user.id.toString)
        }
      }
    } ~
    path("bearer") {
      parameter('acceptExpired.as[Boolean].?) { acceptExpired =>
        authenticator.bearerToken(acceptExpired.getOrElse(false)) { user =>
          complete(user.id.toString)
        }
      }
    } ~
    path("combined") {
      authenticator() { user =>
        complete(user.id.toString)
      }
    }

  "Authenticator" should {
    "basic - grant valid credentials" in new TestActorSystem {
      Get("/basic") ~> addCredentials(BasicHttpCredentials("user1", "pass1")) ~> routes ~> check {
        responseAs[String] === "1"
      }

      Get("/basic") ~> addCredentials(BasicHttpCredentials("user2", "pass2")) ~> routes ~> check {
        responseAs[String] === "2"
      }
    }

    "basic - reject missing credentials" in new TestActorSystem {
      Get("/basic") ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Basic", "realm"))
      }
    }

    "basic - reject wrong password" in new TestActorSystem {
      Get("/basic") ~> addCredentials(BasicHttpCredentials("user1", "pass2")) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Basic", "realm"))
      }

      Get("/basic") ~> addCredentials(BasicHttpCredentials("user2", "pass1")) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Basic", "realm"))
      }
    }

    "basic - reject unknown username" in new TestActorSystem {
      Get("/basic") ~> addCredentials(BasicHttpCredentials("user-unknown", "pass-unknown")) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Basic", "realm"))
      }
    }

    "basic - delay response" in new TestActorSystem {
      Get("/basic?delay=250") ~> addCredentials(BasicHttpCredentials("user1", "pass1")) ~> routes ~> check {
        responseAs[String] === "1"
      }

      within(1000.milliseconds) {
        Get("/basic?delay=10000") ~> routes ~> check {
          rejection === AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Basic", "realm"))
        }
      }

      Get("/basic?delay=250") ~> addCredentials(BasicHttpCredentials("user1", "pass2")) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Basic", "realm"))
      }
    }

    "bearer - grant valid token" in new TestActorSystem {
      Get("/bearer") ~> addCredentials(OAuth2BearerToken(jwtUser1Exp2170)) ~> routes ~> check {
        responseAs[String] === "1"
      }

      Get("/bearer") ~> addCredentials(OAuth2BearerToken(jwtUser2Exp2170)) ~> routes ~> check {
        responseAs[String] === "2"
      }
    }

    "bearer - reject missing token" in new TestActorSystem {
      Get("/bearer") ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", "realm"))
      }
    }

    "bearer - reject expired token" in new TestActorSystem {
      Get("/bearer") ~> addCredentials(OAuth2BearerToken(jwtUser1Exp1970)) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "realm",
          Map("error" -> "invalid_token", "error_description" -> "The access token expired")))
      }

      Get("/bearer?acceptExpired=true") ~> addCredentials(OAuth2BearerToken(jwtUser1Exp1970)) ~> routes ~> check {
        responseAs[String] === "1"
      }

      Get("/bearer?acceptExpired=true") ~> addCredentials(OAuth2BearerToken(jwtUser2Exp1970)) ~> routes ~> check {
        responseAs[String] === "2"
      }
    }

    "bearer - reject token with unknown subject" in new TestActorSystem {
      Get("/bearer") ~> addCredentials(OAuth2BearerToken(jwtUserUnknownExp2170)) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "realm"))
      }
    }

    "bearer - reject token with formal errors" in new TestActorSystem {
      Get("/bearer") ~> addCredentials(OAuth2BearerToken(jwtIncomplete)) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "realm",
          Map("error" -> "invalid_token", "error_description" -> "The token must at least contain the iat and exp claim")))
      }

      Get("/bearer") ~> addCredentials(OAuth2BearerToken(jwtInvalidSignature)) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "realm",
          Map("error" -> "invalid_token", "error_description" -> "The access token has been manipulated")))
      }

      Get("/bearer") ~> addCredentials(OAuth2BearerToken(jwtMalformed)) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "realm",
          Map("error" -> "invalid_token", "error_description" -> "The access token is malformed")))
      }

      Get("/bearer") ~> addCredentials(OAuth2BearerToken(jwtRsa)) ~> routes ~> check {
        rejection === AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "realm",
          Map("error" -> "invalid_token", "error_description" -> "The signature algorithm RS256 is not supported")))
      }
    }

    "combined - work" in new TestActorSystem {
      Get("/combined") ~> addCredentials(BasicHttpCredentials("user1", "pass1")) ~> routes ~> check {
        responseAs[String] === "1"
      }

      Get("/combined") ~> addCredentials(OAuth2BearerToken(jwtUser2Exp2170)) ~> routes ~> check {
        responseAs[String] === "2"
      }

      Get("/combined") ~> routes ~> check {
        rejections === List(
          AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Bearer", "realm")),
          AuthenticationFailedRejection(CredentialsMissing, HttpChallenge("Basic", "realm")))
      }

      Get("/combined") ~> addCredentials(BasicHttpCredentials("user1", "pass2")) ~> routes ~> check {
        rejections === List(
          AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "realm")),
          AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Basic", "realm")))
      }

      Get("/combined") ~> addCredentials(OAuth2BearerToken(jwtInvalidSignature)) ~> routes ~> check {
        rejections === List(
          AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Bearer", "realm",
            Map("error" -> "invalid_token", "error_description" -> "The access token has been manipulated"))),
          AuthenticationFailedRejection(CredentialsRejected, HttpChallenge("Basic", "realm")))
      }
    }
  }
}
