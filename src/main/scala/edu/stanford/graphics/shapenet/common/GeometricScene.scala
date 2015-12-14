package edu.stanford.graphics.shapenet.common

import scala.collection.mutable
import com.jme3.bounding.BoundingBox

/**
 * Geometric representation of a scene
 * @author Angel Chang
 */
abstract class GeometricScene[NODE](val node: NODE) {
  var scene: Scene = null
  var modelInstances: mutable.ArrayBuffer[ModelInstance[NODE]] = null //multiple instances of same model contained here
  // Cached information
  // TODO: Refactor!!!
  var cachedBoundingBoxes: IndexedSeq[BoundingBox] = null
  var cachedBoundingBoxesWithChildren: IndexedSeq[BoundingBox] = null
  // Was the geometric scene modified without updating the underlying scene?
  var geometricSceneModified: Boolean = false

  def getModelInstance(modelInstIndex: Int): ModelInstance[NODE] = {
    if (modelInstIndex >= 0 && modelInstIndex < modelInstances.size) {
      modelInstances(modelInstIndex)
    } else null
  }
  def getNumberOfObjects(): Int = {
    modelInstances.size
  }
  // Removes the specified model from the scene
  def delete(modelInstIndex: Int)
  // Add the specified model instance to the scene
  def insert(modelInst: ModelInstance[NODE])
  def copy(): GeometricScene[NODE]
  // Synchronizes the transforms in the geometric scene to the actual scene
  def syncToScene() {
    applyTransformToScene(this.scene, undoWorldAlignAndScale = true)
    geometricSceneModified = false
  }
  // Apply the transforms in the geometric scene to a target scene
  def applyTransformToScene(targetScene: Scene, undoWorldAlignAndScale: Boolean = false) {
  }

}

