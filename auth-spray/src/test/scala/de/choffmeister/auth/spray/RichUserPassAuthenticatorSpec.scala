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
    "delayed" in {
      check(None, new TestUserPassAuthenticator(Some(1)).delayed(duration.FiniteDuration(2, TimeUnit.SECONDS))) === Some(1)
      check(Some(UserPass("u", "p")), new TestUserPassAuthenticator(Some(1)).delayed(duration.FiniteDuration(2, TimeUnit.SECONDS))) === Some(1)

      check(None, new TestUserPassAuthenticator(None).delayed(duration.FiniteDuration(2, TimeUnit.SECONDS))) === None
      check(Some(UserPass("u", "p")), new TestUserPassAuthenticator(None).delayed(duration.FiniteDuration(2, TimeUnit.SECONDS))) must throwA[TimeoutException]

      check(None, new Test2UserPassAuthenticator().delayed(duration.FiniteDuration(2, TimeUnit.SECONDS))) must throwA[TimeoutException]
      check(Some(UserPass("u", "p")), new Test2UserPassAuthenticator().delayed(duration.FiniteDuration(2, TimeUnit.SECONDS))) must throwA[TimeoutException]
    }
  }

  def check[U](userPass: Option[UserPass], authenticator: UserPassAuthenticator[U]): Option[U] = {
    Await.result(authenticator(userPass), duration.Duration(1, TimeUnit.SECONDS))
  }
}

class TestUserPassAuthenticator[U](result: Option[U]) extends UserPassAuthenticator[U] {
  def apply(userPass: Option[UserPass]): Future[Option[U]] = Future(result)
}

class Test2UserPassAuthenticator[U] extends UserPassAuthenticator[U] {
  def apply(userPass: Option[UserPass]): Future[Option[U]] = Future.failed(new Exception("foo"))
}
