package de.choffmeister.auth.spray

import java.util.concurrent.TimeUnit

import org.specs2.mutable._
import spray.http._
import spray.routing.authentication._
import spray.routing.RequestContext

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class RichUserPassAuthenticatorSpec extends Specification {
  import RichUserPassAuthenticator._

  "RichUserPassAuthenticator" should {
    "delay checking of invalid credentials" in {
      check(new TestUserPassAuthenticator(Some(1)).delayed(duration.FiniteDuration(2, TimeUnit.SECONDS))) === Some(1)
    }

    "not delay checking of valid credentials" in {
      check(new TestUserPassAuthenticator(None).delayed(duration.FiniteDuration(2, TimeUnit.SECONDS))) must throwA[TimeoutException]
    }
  }

  def check[U](authenticator: UserPassAuthenticator[U]): Option[U] = {
    Await.result(authenticator(None), duration.Duration(1, TimeUnit.SECONDS))
  }
}

class TestUserPassAuthenticator[U](result: Option[U]) extends UserPassAuthenticator[U] {
  def apply(userPass: Option[UserPass]): Future[Option[U]] = Future(result)
}
