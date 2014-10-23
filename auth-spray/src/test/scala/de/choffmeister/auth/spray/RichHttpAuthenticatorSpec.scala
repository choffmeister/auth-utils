package de.choffmeister.auth.spray

import java.util.concurrent.TimeUnit

import org.specs2.mutable._
import spray.http._
import spray.routing.RequestContext
import spray.routing.authentication.HttpAuthenticator

import scala.concurrent._

class RichHttpAuthenticatorSpec extends Specification {
  import RichHttpAuthenticator._

  "RichHttpAuthenticator" should {
    "map" in {
      authenticate(new TestHttpAuthenticator(Some(1)).map(_ * 2)) == Some(2)
    }

    "withFallback" in {
      authenticate(new TestHttpAuthenticator(Some(1)).withFallback(new TestHttpAuthenticator(Some(2)))) == Some(1)
      authenticate(new TestHttpAuthenticator(Option.empty[Int]).withFallback(new TestHttpAuthenticator(Some(2)))) == Some(2)
      authenticate(new TestHttpAuthenticator(Some(1)).withFallback(new TestHttpAuthenticator(Option.empty[Int]))) == Some(1)
      authenticate(new TestHttpAuthenticator(Option.empty[Int]).withFallback(new TestHttpAuthenticator(Option.empty[Int]))) == Option.empty[Int]
    }
  }

  def authenticate[U](authenticator: HttpAuthenticator[U]): Option[U] = {
    Await.result(authenticator.authenticate(None, null), duration.Duration(1, TimeUnit.SECONDS))
  }
}

class TestHttpAuthenticator[U](result: Option[U]) extends HttpAuthenticator[U] {
  override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): Future[Option[U]] = Future(result)

  override def getChallengeHeaders(httpRequest: HttpRequest): List[HttpHeader] = ???

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
}
