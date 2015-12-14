package edu.stanford.graphics.shapenet.jme3.viewer

import com.jme3.app.state.AbstractAppState
import scala.util.control.Breaks
import java.util.concurrent._
import java.util.concurrent.locks.{Condition, ReentrantLock}
import scala.Some

/**
 * A task that requires doing rendering, potentially going through multiple render cycles
 * At each update step, only one RenderTask is executed
 * @author Angel Chang
 */
trait RenderTask {
  // Update for rendering
  def update(tpf: Float): RenderTaskStatus.Value
  // Done
  def done() = {}
  // is done?
  def isDone: Boolean = false
}

object RenderTaskStatus extends Enumeration {
  type RenderTaskStatus = Value
  val Updated, Waiting, Cancelled, Done = Value
}

trait RenderTaskListener[T] {
  def updated(result: T) = {}
  def done(result: T) = {}
}

trait RenderTaskListenerWithResult[T] extends RenderTaskListener[T] with Future[Option[T]] {
  private var resultOption: Option[T] = null

  private var exception: ExecutionException = null
  private var cancelled: Boolean = false
  private var finished: Boolean = false
  private final val stateLock: ReentrantLock = new ReentrantLock
  private final val finishedCondition: Condition = stateLock.newCondition

  override def done(result: T): Unit = {
    stateLock.lock()
    try {
      finished = true
      resultOption = Some(result)
      finishedCondition.signalAll()
    } finally {
      stateLock.unlock()
    }
  }

  // Implements future....
  def get(): Option[T] = {
    stateLock.lock()
    try {
      while (!isDone) {
        finishedCondition.await()
      }
      if (exception != null) {
        throw exception
      }
      resultOption
    } finally {
      stateLock.unlock()
    }
  }
  def get(timeout: Long, unit: TimeUnit): Option[T] = {
    stateLock.lock()
    try {
      if (!isDone) {
        finishedCondition.await(timeout, unit)
      }
      if (exception != null) {
        throw exception
      }
      if (isDone) {
        resultOption
      } else {
        throw new TimeoutException("Object not returned in time allocated.")
      }
    } finally {
      stateLock.unlock()
    }
  }

  override def isDone: Boolean = {
    stateLock.lock()
    try {
      finished || cancelled || (exception != null)
    } finally {
      stateLock.unlock()
    }
  }

  def isCancelled: Boolean = {
    stateLock.lock()
    try {
      cancelled
    } finally {
      stateLock.unlock()
    }
  }

  def cancel(mayInterruptIfRunning: Boolean): Boolean = {
    stateLock.lock()
    try {
      if (isDone) {
        return false
      }
      cancelled = true
      finishedCondition.signalAll()
      return cancelled
    }
    finally {
      stateLock.unlock()
    }
  }
}

class RenderTaskQueue extends AbstractAppState {
  val taskQueue = new scala.collection.mutable.Queue[RenderTask]
  val mybreaks = new Breaks
  import mybreaks.{break, breakable}

  override def update(tpf: Float) {
    // Go through taskQueue until we have executed something
    var toRemove: RenderTask = null
    breakable {
      for (t <- taskQueue) {
        if (!t.isDone) {
          val status = t.update(tpf)
          status match {
            case RenderTaskStatus.Updated => {
              break()
            }
            case RenderTaskStatus.Done => {
              t.done()
              toRemove = t
              break()
            }
            case _ => {}
          }
        } else {
          toRemove = t
          break()
        }
      }
    }
    if (toRemove != null) {
      taskQueue.dequeueFirst( x => toRemove == x )
    }
  }
  def enqueue(tasks: RenderTask*) = taskQueue.enqueue(tasks:_*)
  def isEmpty() = {
    taskQueue.isEmpty
  }
}
