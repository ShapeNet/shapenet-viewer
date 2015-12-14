package edu.stanford.graphics.shapenet.util

import java.util.concurrent.{Future, Executors}

object Threads extends Loggable {
  lazy val threadPool = Executors.newCachedThreadPool()
  def execute(runnable: Runnable, logger: org.slf4j.Logger = this.logger, desc: String = ""): Future[_] = {
    val wrappedRunnable = new RunnableWithLogging(runnable, logger, desc)
    threadPool.submit(wrappedRunnable)
  }
}

/**
 * Simple wrapper for runnable that catches and logs exceptions
 * @author Angel Chang
 */
class RunnableWithLogging(val runnable: Runnable, val logger: org.slf4j.Logger, val desc: String) extends Runnable {
  override def run(): Unit = {
    try {
      runnable.run()
    } catch {
      case ex: Throwable => {
        logger.error("Error running " + desc, ex)
      }
    }
  }
}
