package de.choffmeister.auth.common

import de.choffmeister.auth.common.util._
import de.choffmeister.auth.common.util.Base64StringConverter._

class PasswordHasher(defaultName: String, defaultConfig: List[String], algorithms: Seq[PasswordHashAlgorithm]) {
  def hash(password: String, additionalConfig: List[String] = Nil): String = {
    hashByAlgoName(defaultName, defaultConfig ++ additionalConfig, password) match {
      case Right(hash) =>
        val fields = List(defaultName) ++ defaultConfig ++ additionalConfig ++ List(bytesToBase64(hash))
        fields.mkString(":")
      case Left(c) =>
        hash(password, additionalConfig ++ c)
    }
  }

  def validate(stored: String, password: String): Boolean = {
    val splitted = stored.split(":", -1).toList
    val name = splitted.head
    val config = splitted.tail.init
    val hash = base64ToBytes(splitted.last)
    SequenceUtils.compareConstantTime(hashByAlgoName(name, config, password).right.get, hash)
  }

  private def hashByAlgoName(name: String, config: List[String], password: String): Either[List[String], Array[Byte]] = {
    algorithms.find(_.name == name).get.hash(config, password)
  }
}
