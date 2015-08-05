package de.choffmeister.auth.akkahttp

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{ImplicitSender, TestKitBase}
import org.specs2.mutable.After

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

abstract class TestActorSystem extends { implicit val system = ActorSystem(UUID.randomUUID().toString) }
    with TestKitBase
    with After
    with ImplicitSender {
  implicit val executor = system.dispatcher
  implicit val materializer = ActorMaterializer()

  def await[T](f: => Future[T]): T =
    Await.result(f, Duration.Inf)

  def after: Unit = {
  }
}
