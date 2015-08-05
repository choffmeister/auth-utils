package de.choffmeister.auth.spray.util

import akka.actor._

import scala.collection.mutable.PriorityQueue
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

private[auth] class SimpleScheduler(override val maxFrequency: Double) extends Scheduler {
  private val lock = new AnyRef()
  private val queue = PriorityQueue.empty[(Runnable, Long, Option[Long], ExecutionContext)](ExecuteOrdering)
  private def now = System.currentTimeMillis

  override def schedule(initialDelay: FiniteDuration, interval: FiniteDuration, runnable: Runnable)(implicit ec: ExecutionContext): Cancellable = {
    lock.synchronized {
      queue.enqueue((runnable, now + initialDelay.toMillis, Some(interval.toMillis), ec))
      NullCancellable
    }
  }

  override def scheduleOnce(delay: FiniteDuration, runnable: Runnable)(implicit ec: ExecutionContext): Cancellable = {
    lock.synchronized {
      queue.enqueue((runnable, now + delay.toMillis, None, ec))
      NullCancellable
    }
  }

  private val thread = new Thread(new Runnable {
    def run() {
      while (true) {
        lock.synchronized {
          while (queue.length > 0 && queue.head._2 <= now) {
            val head = queue.dequeue()
            head._4.execute(head._1)
            head._3 match {
              case Some(next) => queue.enqueue((head._1, head._2 + next, Some(next), head._4))
              case _ => {}
            }
          }
        }
        val millis = 1.0 / maxFrequency * 1000
        Thread.sleep(millis.toLong)
      }
    }
  }).start()

  object NullCancellable extends Cancellable {
    override def cancel(): Boolean = false

    override def isCancelled: Boolean = false
  }

  object ExecuteOrdering extends Ordering[(Runnable, Long, Option[Long], ExecutionContext)] {
    override def compare(x: (Runnable, Long, Option[Long], ExecutionContext), y: (Runnable, Long, Option[Long], ExecutionContext)): Int = x._2.compareTo(y._2)
  }
}

private[auth] object SimpleScheduler {
  lazy val instance = new SimpleScheduler(60.0)
}

