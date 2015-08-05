package de.choffmeister.auth.akkahttp

import akka.http.scaladsl.testkit._

import scala.concurrent.duration._

trait Specs2RouteTest extends TestFrameworkInterface with RouteTest {
  implicit val timeout = RouteTestTimeout(5.seconds)

  override def cleanUp(): Unit = {}

  override def failTest(msg: String): Nothing = {
    throw new Exception(msg)
  }
}
