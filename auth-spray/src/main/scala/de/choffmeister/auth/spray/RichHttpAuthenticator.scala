package de.choffmeister.auth.spray

import spray.http._
import spray.routing.authentication._
import spray.routing._

import scala.concurrent._

class RichHttpAuthenticator[U](self: HttpAuthenticator[U]) {
  def map[V](f: U => V) = new HttpAuthenticator[V] {
    override implicit def executionContext: ExecutionContext = self.executionContext

    override def getChallengeHeaders(httpRequest: HttpRequest): List[HttpHeader] =
      self.getChallengeHeaders(httpRequest)

    override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): Future[Option[V]] =
      self.authenticate(credentials, ctx).map {
        case Some(u) => Some(f(u))
        case None => None
      }
  }

  def withFallback(other: HttpAuthenticator[U]) = new HttpAuthenticator[U] {
    override implicit def executionContext: ExecutionContext = self.executionContext

    override def getChallengeHeaders(httpRequest: HttpRequest): List[HttpHeader] =
      self.getChallengeHeaders(httpRequest) ++ other.getChallengeHeaders(httpRequest)

    override def authenticate(credentials: Option[HttpCredentials], ctx: RequestContext): Future[Option[U]] =
      self.authenticate(credentials, ctx).flatMap {
        case Some(u1) => Future(Some(u1))
        case None => other.authenticate(credentials, ctx).map {
          case Some(u2) => Some(u2)
          case None => None
        }
      }
  }
}

object RichHttpAuthenticator {
  implicit def toRichHttpAuthenticator[U](inner: HttpAuthenticator[U]) = new RichHttpAuthenticator(inner)
}
