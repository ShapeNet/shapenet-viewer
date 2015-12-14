package edu.stanford.graphics.shapenet.common

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.Constants

/**
 * Represents a model of an object
 * Includes the geometry, and materials for the model
 * @author Angel Chang
 */
class Model[NODE](val node: NODE) {
  /**
   * Semantic information associated with a model
   */
  var modelInfo: ModelInfo = null
  
  def fullId: String = if (modelInfo != null) modelInfo.fullId else null
  def name: String = if (modelInfo != null) modelInfo.name else null
  def up: Vector3f = {
    val up = if (modelInfo != null) modelInfo.up else null
    if (up != null) up else Constants.DEFAULT_MODEL_UP
  }
  def front: Vector3f = {
    val front = if (modelInfo != null) modelInfo.front else null
    if (front != null) front else Constants.DEFAULT_MODEL_FRONT
  }
  def isRoom: Boolean = {
    if (modelInfo != null) modelInfo.isRoom else false
  }
}

/**
 * Represents a instantiation of a model in a scene
 */
class ModelInstance[NODE](val node: NODE, var index: Int) {
  // Information about the model this node is derived from
  var model: Model[NODE] = null
  // my self (no children)
  var nodeSelf: NODE = node
  def up: Vector3f = {
    if (Constants.useSemanticCoordFront) {
      Constants.SEMANTIC_UP
    } else {
      val up = if (model != null) model.up else null
      if (up != null) up else Constants.DEFAULT_MODEL_UP
    }
  }
  def front: Vector3f = {
    if (Constants.useSemanticCoordFront) {
      Constants.SEMANTIC_FRONT
    } else {
      val front = if (model != null) model.front else null
      if (front != null) front else Constants.DEFAULT_MODEL_FRONT
    }
  }
  def withChildren = node
  def withoutChildren = nodeSelf
  def getLabel: String = {
    toString
  }
  override def toString() = {
    model.name + "(" + index + ")"
  }
}

case class MaterialInfo (
                          diffuse: Array[Double] = null,  // Color as RGB (0 to 1)
                          ambient: Array[Double] = null,
                          specular: Array[Double] = null,
                          diffuseMap: String = null,
                          shininess: Double = Double.NaN,
                          transparent: Boolean = false,
                          opacity: Double = 1,
                          shadeless: Boolean = false,
                          doubleSided: Boolean = false
                        )

