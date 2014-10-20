package de.choffmeister.auth.common

import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

import de.choffmeister.auth.common.util._
import de.choffmeister.auth.common.util.Base64StringConverter._

trait PasswordHashAlgorithm {
  /**
   * A unique identifier for this hash algorithm.
   */
  val name: String

  /**
   * Returns either the hashed password or a list of additional configuration
   * values that should be appended to config before reinvoking this method.
   */
  def hash(config: List[String], password: String): Either[List[String], Array[Byte]]
}

/**
 * plain:(password)
 */
object Plain extends PasswordHashAlgorithm {
  val name = "plain"

  def hash(config: List[String], password: String) = config match {
    case Nil ⇒
      Right(password.getBytes("UTF-8"))
    case _ ⇒
      throw new Exception(s"Invalid config $config")
  }
}

/**
 * pbkdf2:hmac-sha1:10000:128:(salt):(hash)
 */
object PBKDF2 extends PasswordHashAlgorithm {
  val name = "pbkdf2"

  def hash(config: List[String], password: String) = config match {
    case algorithmName :: iterations :: keyLength :: Nil ⇒
      val salt = NonceGenerator.generateBytes(32)
      Left(bytesToBase64(salt) :: Nil)
    case "hmac-sha1" :: UnapplyInt(iterations) :: UnapplyInt(keyLength) :: UnapplyByteArray(salt) :: Nil ⇒
      val spec = new PBEKeySpec(password.toCharArray, salt, iterations, keyLength)
      val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
      Right(skf.generateSecret(spec).getEncoded())
    case _ ⇒
      throw new Exception(s"Invalid config $config")
  }
}
