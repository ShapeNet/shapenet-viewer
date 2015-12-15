package edu.stanford.graphics.shapenet.common

import edu.stanford.graphics.shapenet.util.StringUtils

/**
 * Utility functions for normalizing category names...
 * @author Angel Chang
 */
object CategoryUtils {
  val ROOT = "ROOT"
  val UNKNOWN = "UNKNOWN"

  def normalize(s: String) = {
    StringUtils.toCamelCase(s)
  }

  def toKeyword(s: String) = {
    if (s != null) {
      StringUtils.camelCaseToText(s, lowercase = true)
    } else null
  }

  def getKeywords(category: String, location: String = null): IndexedSeq[String] = {
    if (location != null) {
      IndexedSeq(toKeyword(category), toKeyword(location))
    } else {
      IndexedSeq(toKeyword(category))
    }
  }

  def isSameCategory(cat1: String, cat2: String): Boolean = {
    // TODO: normalize?
    cat1 == cat2
  }

  def getCategories(m: Option[ModelInfo]): Seq[String] = {
    getCategories(m.getOrElse(null))
  }

  def getCategories(m: ModelInfo, allowEmpty: Boolean = true): Seq[String] = {
    if (m != null) {
      val cats = m.category //m.relaxedCategory
      if  (cats != null && cats.nonEmpty) {
        cats.toSeq.map( x => normalize(x) )
      } else if (allowEmpty) Seq() else Seq( CategoryUtils.UNKNOWN )
    } else {
      if (allowEmpty) Seq() else Seq( CategoryUtils.UNKNOWN )
    }
  }

  def hasCategory(m: ModelInfo, category: String): Boolean = {
    getCategories(m).exists( x => x == category)
  }

}

