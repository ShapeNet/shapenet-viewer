package edu.stanford.graphics.shapenet.common

import java.io.File

import scala.util.matching.Regex

/**
 * Represents a resource/asset with a source and a id
 * @author Angel Chang
 */
case class FullId(source: String, id: String) {
  lazy val fullid = source + "." + id
}

object FullId {
  val fullIdRegex = new Regex("([a-zA-z0-9_-]+)\\.([a-zA-z0-9_-]+)")
  def apply(fullid: String, defaultSource: Option[String] = None): FullId = {
    val dotIndex = fullid.indexOf('.')
    val (source, id) = if (fullid.startsWith("http://") || fullid.startsWith("https://")) {
      ("raw", fullid)
    } else if (fullid.startsWith("file://")) {
      ("raw", fullid.substring(7))
    } else if (fullid.startsWith("/")) {
      ("raw", fullid)
    } else if (new File(fullid).isAbsolute) {
      ("raw", fullid)
    } else if (dotIndex > 0) {
      (fullid.substring(0, dotIndex), fullid.substring(dotIndex + 1))
    } else {
      val s = defaultSource.getOrElse(if (fullid.contains("scene")) "wssScenes" else "3dw")
      (s, fullid)
    }
    new FullId(source,id)
  }
  def matches(id1: String, id2: String): Boolean = {
    val f1 = FullId(id1)
    val f2 = FullId(id2)
    f1 == f2
  }
  def isFullId(s: String): Boolean = {
    fullIdRegex.pattern.matcher(s).matches()
  }
}

