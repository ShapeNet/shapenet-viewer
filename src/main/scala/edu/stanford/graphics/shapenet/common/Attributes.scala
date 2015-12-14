package edu.stanford.graphics.shapenet.common

/**
  * Attributes
  * @author Angel Chang
  */
trait HasAttributes {
  var attributes: IndexedSeq[(String,String)]

  def addToIndexedSeq[T](s: IndexedSeq[T], t: T, allowDuplicates: Boolean = false): (IndexedSeq[T], Int) = {
    if (s == null) {
      (IndexedSeq(t), 0)
    } else {
      val nextIndex = s.size
      if (allowDuplicates) {
        // Don't check for duplicates, just add it to the end
        (s :+ t, nextIndex)
      } else {
        // Check if there is a existing value that is the same, and only add if there isn't
        val i = s.indexOf(t)
        if (i >= 0) (s,i) else (s :+ t, nextIndex)
      }
    }
  }

  def setValue[K,V](s: IndexedSeq[(K,V)], k: K, v: V): (IndexedSeq[(K,V)], Int) = {
    if (s == null) {
      (IndexedSeq((k,v)), 0)
    } else {
      // Check if there is a existing value that is the same, and only add if there isn't
      val i = s.indexOf(k)
      val nextIndex = s.size
      if (i >= 0) (s.updated(i,(k,v)),i) else (s :+ (k,v), nextIndex)
    }
  }

  def addAttribute(attrType: String, attrValue: String, allowDuplicates: Boolean = false): Int = {
    val (a,i) = addToIndexedSeq(attributes, (attrType, attrValue), allowDuplicates)
    attributes = a
    i
  }

  def setAttribute(attrType: String, attrValue: String): Int = {
    val (a,i) = setValue(attributes, attrType, attrValue)
    attributes = a
    i
  }

  def setAttribute(attrType: String, attrValue: Boolean): Int = {
    val (a,i) = setValue(attributes, attrType, attrValue.toString)
    attributes = a
    i
  }

  def setAttribute(attrType: String, attrValue: Number): Int = {
    val (a,i) = setValue(attributes, attrType, attrValue.toString())
    attributes = a
    i
  }

  def getAttributes(name: String): IndexedSeq[(String,String)] = {
    if (attributes != null) {
      attributes.filter( x => x._1 == name)
    } else {
      IndexedSeq()
    }
  }

  def getAttributeValue(name: String): Option[String] = {
    getAttributes(name).headOption.map( x => x._2 )
  }

  def getAttributeBoolean(name: String): Option[Boolean] = {
    getAttributes(name).headOption.map( x => x._2.toBoolean )
  }

  def getAttributeInt(name: String): Option[Int] = {
    getAttributes(name).headOption.map( x => x._2.toInt )
  }

  def getAttributeDouble(name: String): Option[Double] = {
    getAttributes(name).headOption.map( x => x._2.toDouble )
  }
}

/**
  * Attribute type constants
  * @author Angel Chang
  */
// Constants on possible attribute types (mainly so we don't mistype)
object AttributeTypes  {
  val MODEL_ID = "modelId"
  val OBJECT_INDEX = "objIndex"
  // Unused attributes, but ones we would like to have (maybe these should be promoted to be full relations)
  val COLOR = "color"
  val SHAPE = "shape"
  val MATERIAL = "material"

  // Not quite attribute types, but effectively attributes
  val CATEGORY = "category"
  val DESCRIPTION = "description"
}

