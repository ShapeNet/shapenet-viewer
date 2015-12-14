package edu.stanford.graphics.shapenet.util

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap
import scala.ref.SoftReference

// Simple LRU - wrapper around concurrentlinkedhashmap (can use spray-caching in future)
case class LRUCache[A, B](maxCapacity: Int, initialCapacity: Int = 16)
{
  val maxWeightedCapacity = if (maxCapacity < 0) Long.MaxValue else maxCapacity
  protected val cache = new ConcurrentLinkedHashMap.Builder[A, B]
    .initialCapacity(initialCapacity)
    .maximumWeightedCapacity(maxWeightedCapacity)
    .build()
  //LruCache[A,B](maxCapacity = MAX_ENTRIES)

  def getOrElse(key: A)(fn: => B): B = {
    get(key).getOrElse {
      val result = fn
      put(key, result)
      result
    }
  }

  def get(key: A): Option[B] = Option(cache.get(key))

  def put(key: A, value: B) = cache.put(key, value)

  def remove(key: A) = cache.remove(key)

  def clear() = cache.clear()
}

case class SoftLRUCache[A,B <: AnyRef](maxCapacity: Int, initialCapacity: Int = 16)
{
  val lruCache = LRUCache[A,SoftReference[B]](maxCapacity, initialCapacity)

  def getOrElse(key: A)(fn: => B): B = {
    val v = get(key)
    if (v.isEmpty) {
      val result = fn
      lruCache.put(key, new SoftReference(result))
      result
    } else v.get
  }

  def get(key: A): Option[B] = {
    val v = lruCache.get(key)
    if (v.isEmpty || v.get.get.isEmpty) {
      None
    } else v.get.get
  }

  def put(key: A, value: B) = lruCache.put(key, new SoftReference(value))

  def remove(key: A) = lruCache.remove(key)

  def clear() = lruCache.clear()
}

