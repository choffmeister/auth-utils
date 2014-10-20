package de.choffmeister.auth.common.util

private[auth] object SequenceUtils {
  def compareConstantTime[T](s1: Seq[T], s2: Seq[T]): Boolean = {
    var res = true
    val l = Math.max(s1.length, s2.length)
    for (i ← 0 until l) {
      if (s1.length <= i || s2.length <= i || s1(i) != s2(i)) {
        res = false
      }
    }
    res
  }
}
