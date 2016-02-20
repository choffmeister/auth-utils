import java.util.Date

import akka.actor._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import de.choffmeister.auth.akkahttp.Authenticator
import de.choffmeister.auth.common._
import spray.json.JsString

import scala.concurrent._
import scala.concurrent.duration._

case class User(id: Int, userName: String, passwordHash: String)
object UserDatabase {
  // This hasher per default hashes new passwords with PBKDF2-HMAC-SHA1, 10000
  // rounds, 128 bit hash output and supports validating passwords that were
  // stored with either PBKDF2 or Plain hashing
  private val hasher = new PasswordHasher(
    "pbkdf2", "hmac-sha1" :: "10000" :: "128" :: Nil,
    List(PBKDF2, Plain))

  private val users = List(
    // user1 with pass1
    User(1, "user1", "plain:cGFzczE="),
    // user2 with pass2
    User(2, "user2", "pbkdf2:hmac-sha1:10000:128:MfdJXQsZjB40B9yhoWw7hVkNkAK9qd4Dt5y1JTPaRDw=:gxnD5GLjZljqp9ybgpFlvQ=="))

  def findById(id: String)(implicit ec: ExecutionContext) =
    Future(users.find(_.id.toString == id))
  def findByUserName(userName: String)(implicit ec: ExecutionContext) =
    Future(users.find(_.userName == userName))
  def validatePassword(user: User, password: String)(implicit ec: ExecutionContext) =
    Future(hasher.validate(user.passwordHash, password))
}

class UsageExample(implicit val system: ActorSystem, val exec: ExecutionContext,
    val materializer: Materializer) {
  val bearerTokenSecret = "secret-no-one-knows".getBytes
  val bearerTokenLifetime = 5.minutes

  val authenticator = new Authenticator[User](
    realm = "Example realm",
    bearerTokenSecret = bearerTokenSecret,
    findUserById = UserDatabase.findById,
    findUserByUserName = UserDatabase.findByUserName,
    validateUserPassword = UserDatabase.validatePassword)

  val route =
    path("token" / "create") {
      get {
        // Here we can send valid username/password HTTP basic authentication
        // and get a JWT for it. If wrong credentials were given, then this
        // route is not completed before 1 second has passed. This makes timing
        // attacks harder, since an attacker cannot distinguish between wrong
        // username and existing username, but wrong password.
        authenticator.basic(Some(1000.millis))(user => completeWithToken(user))
      }
    } ~
    path("token" / "renew") {
      get {
        // Here we can send an expired JWT via HTTP bearer authentication and
        // get a renewed JWT for it.
        authenticator.bearerToken(acceptExpired = true)(user => completeWithToken(user))
      }
    } ~
    path("state") {
      get {
        // Here we get greeted, if we have either a valid username/password or
        // JWT sent via HTTP basic resp. HTTP bearer.
        authenticator()(user => complete(s"Welcome, ${user.userName}!"))
      }
    }

  private def completeWithToken(user: User): Route = {
    val secret = bearerTokenSecret
    val lifetime = bearerTokenLifetime.toSeconds
    val now = System.currentTimeMillis / 1000L * 1000L

    val token = JsonWebToken(
      createdAt = new Date(now),
      expiresAt = new Date(now + lifetime * 1000L),
      subject = user.id.toString,
      claims = Map("name" -> JsString(user.userName))
    )
    val tokenStr = JsonWebToken.write(token, secret)

    val response = OAuth2AccessTokenResponse("bearer", tokenStr, lifetime)
    complete(OAuth2AccessTokenResponseFormat.write(response).compactPrint)
  }
}
