package edu.stanford.graphics.shapenet.util

import scala.collection.mutable

/**
 * Utility class for providing user data
 * @author Angel Chang
 */
class UserData {
  val userData = new mutable.WeakHashMap[Any, mutable.HashMap[String,Any]]()

  def get(s: Any) = userData.get(s)
  def clear(s: Any) = userData.get(s).map( x => x.clear() )
  def copy(s: Any, t: Any) {
    clear(t)
    val sData = userData.getOrElse(s, null)
    if (sData != null) {
      val tData = userData.getOrElseUpdate(t, new mutable.HashMap[String,Any]())
      for ((k,v) <- sData)
        tData.put(k,v)
    }
  }

  def get[T](s: Any, key: String): Option[T] = {
    val sData = userData.getOrElse(s, null)
    if (sData != null)
      sData.get(key).map( x => x.asInstanceOf[T] )
    else None
  }

  def getOrElse[T](s: Any, key: String, default: => T): T = {
    val sData = userData.getOrElseUpdate(s, new mutable.HashMap[String,Any]())
    sData.getOrElse(key, default).asInstanceOf[T]
  }

  def getOrElseUpdate[T](s: Any, key: String, default: => T): T = {
    val sData = userData.getOrElseUpdate(s, new mutable.HashMap[String,Any]())
    sData.getOrElseUpdate(key, default).asInstanceOf[T]
  }

  def set[T](s: Any, key: String, data:T ): Option[T] = {
    val sData = userData.getOrElseUpdate(s, new mutable.HashMap[String,Any]())
    sData.put(key, data).map( x => x.asInstanceOf[T] )
  }

  def put[T](s: Any, key: String, data:T ): Option[T] = set(s,key,data)

  def remove[T](s: Any, key: String): Option[T] = {
    val sData = userData.get(s)
    sData.map( x => x.remove(key).map( y => y.asInstanceOf[T]) ).flatten
  }

  def contains(s: Any, key: String): Boolean = {
    val sData = userData.get(s)
    if (sData.isDefined) sData.get.contains(key)
    else false
  }


}
