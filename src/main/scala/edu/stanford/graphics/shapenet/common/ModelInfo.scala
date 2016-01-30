package edu.stanford.graphics.shapenet.common

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.jme3.loaders.AssetGroups
import edu.stanford.graphics.shapenet.{SizeBy, Constants}
import edu.stanford.graphics.shapenet.util.StringUtils

import scala.collection.mutable

case class DefaultModelInfo(unit: Double = Constants.DEFAULT_MODEL_UNIT,
                            up: Vector3f = Constants.DEFAULT_MODEL_UP,
                            front: Vector3f = Constants.DEFAULT_MODEL_FRONT,
                            categories: Seq[String] = Seq())

/**
 * ModelInfo
  *
  * @author Angel Chang
 */
class ModelInfo(
                 val fullId: String = null,
                 val minPoint: Vector3f = null,
                 val maxPoint: Vector3f = null,
                 var rawbbdims: Vector3f = null,
                 var name: String = null,
                 var tags: Array[String] = null,
                 val allCategory: Array[String] = null,
                 val category0: Array[String] = null,
                 val scenes: Array[String] = null,
                 val datasets: Array[String] = null,
                 val unit0: Option[Double] = None,
                 val up0: Option[Vector3f] = None,
                 val front0: Option[Vector3f] = None,
                 val defaults: DefaultModelInfo// = DefaultModelInfo()
                 ) {
  var unit = unit0.getOrElse(defaults.unit)
  var up = up0.getOrElse(defaults.up)
  var front = front0.getOrElse(defaults.front)

  // Computed statistics
  var materials: Seq[(String,Double)] = null // What is this model made of
  var materialsCategory: String = null // Category used for materials
  var isContainer: Option[Boolean] = None
  var solidVolume: Option[Double] = None
  var surfaceVolume: Option[Double] = None
  var supportSurfArea: Option[Double] = None
  var volume: Option[Double] = None
  var weight: Option[Double] = None
  var staticFrictionForce: Option[Double] = None

  // Attributes
  var attributes = new mutable.ArrayBuffer[(String,String)]()

  // Text info
  var description: String = null
  var uploadDate: String = null
  var author: String = null
  lazy val category: Array[String] = allCategory.filterNot(s => s.contains("_"))
  lazy val modelSets: Array[String] = allCategory.filter(s => s.contains("_"))
  var metadata: Map[String,String] = null

  // WordNet
  var wnsynset: Array[String] = null
  var wnhypersynset: Array[String] = null

  def hasCategory(c: String): Option[Boolean] = {
    var res: Option[Boolean] = None
    if (category != null && !category.isEmpty) res = Option(category.contains(c))
    res
  }

  def hasCategoryRelaxed(c: String): Option[Boolean] = {
    var res: Option[Boolean] = None
    if (category != null && !category.isEmpty) res = Option(category.contains(StringUtils.toCamelCase(c)))
    if (!res.getOrElse(false)) {
      if (category0 != null && !category0.isEmpty) res = Option(category0.contains(StringUtils.toLowercase(c)))
    }
    res
  }

  def relaxedCategory: Seq[String] = {
    val cats = if (source == "wss") {
      category ++ category0.map( x => StringUtils.toCamelCase(x))
    } else category
    cats.distinct
  }

  def basicCategory: String = {
    val cat0 = if (source == "wss" && category0 != null && !category0.isEmpty) category0.head else null
    val cat1 = if (category != null && !category.isEmpty) category.head else null
    if (cat0 != null && cat1 != null) {
      if (cat0.length < cat1.length) cat0
      else cat1
    } else if (cat0 != null) cat0
    else if (cat1 != null) cat1
    else null
  }

  def hasAttribute(attrType: String, attrValue: String): Option[Boolean] = {
    var res: Option[Boolean] = None
    val filtered = getAttributes(attrType)
    if (!filtered.isEmpty) res = Option(filtered.contains((attrType,attrValue)))
    res
  }

  def getAttributes(attrType:String): Seq[(String,String)] = {
    attributes.filter( p => attrType.equals(p._1))
  }

  def getAttributes(): Seq[(String,String)] = {
    attributes
  }

  def setMetadata(name: String, value: String): Option[String] = {
    if (metadata == null) {
      metadata = Map(name -> value)
      None
    } else {
      val prev = metadata.get(name)
      metadata = metadata + (name -> value)
      prev
    }
  }

  def getMetadata(name:String): Option[String] = {
    metadata.get(name)
  }

  def getMetadata(): Map[String,String] = {
    metadata
  }

  def trueunit: Double = {
    unit
  }

  def bbdims: Vector3f = {
    rawbbdims
  }

  def physdims: Vector3f = bbdims.mult(unit.toFloat)

  def size(sizeBy: SizeBy.Value): Double = {
    val dims: Vector3f = bbdims
    unit * SizeBy.sizeWithZUp(sizeBy, dims)
  }

  def logSize(sizeBy: SizeBy.Value): Double = {
    math.log(size(sizeBy))
  }

  def defaultSize(sizeBy: SizeBy.Value): Double = {
    val dims: Vector3f = bbdims
    Constants.DEFAULT_MODEL_UNIT * SizeBy.sizeWithZUp(sizeBy, dims)
  }

  def source: String = {
    if (fullId == null) return null
    val pos = fullId.indexOf('.')
    if (pos >= 0) fullId.substring(0, pos) else null
  }

  def id: String = {
    if (fullId == null) return null
    val pos = fullId.indexOf('.')
    if (pos >= 0) fullId.substring(pos+1) else fullId
  }


  def isRoom: Boolean = {
    hasCategory("Room").getOrElse(false)
  }
  def isBad: Boolean = {
    modelSets.contains("_BAD")
  }

  def toDetailedString(): String = {
    val sb = new StringBuilder()
    sb.append("fullId: " + fullId + "\n")
    if (category != null) {
      sb.append("category: " + category.mkString(",") + "\n")
    }
    sb.toString
  }

  override def toString(): String = {
    fullId
  }
}

