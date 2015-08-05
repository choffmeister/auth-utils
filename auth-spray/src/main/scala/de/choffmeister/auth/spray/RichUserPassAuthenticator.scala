package de.choffmeister.auth.spray

import akka.pattern.after
import de.choffmeister.auth.spray.util.SimpleScheduler
import spray.routing.authentication._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util._

class RichUserPassAuthenticator[U](self: UserPassAuthenticator[U]) {
  def delayed(delay: FiniteDuration)(implicit ec: ExecutionContext) = new UserPassAuthenticator[U] {
    def apply(userPass: Option[UserPass]): Future[Option[U]] = {
      val auth = self(userPass)
      val delayed = after[Option[U]](delay, SimpleScheduler.instance)(future(None))

      val promise = Promise[Option[U]]()
      auth.onComplete {
        case Success(Some(user)) => promise.success(Some(user))
        case Success(None) if userPass.isEmpty => promise.success(None)
        case _ => delayed.onComplete(_ => promise.success(None))
      }
      promise.future
    }
  }
}

object RichUserPassAuthenticator {
  implicit def toRichUserPassAuthenticator[U](inner: UserPassAuthenticator[U]) = new RichUserPassAuthenticator(inner)
}
