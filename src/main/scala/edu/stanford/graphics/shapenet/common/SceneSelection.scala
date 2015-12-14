package edu.stanford.graphics.shapenet.common

import scala.collection.SeqProxy
import scala.collection.mutable.ArrayBuffer

/**
  * Represents a selection in a scene
  * @author Angel Chang
  */
case class SceneSelection(objectIndex: Int,
                          partId: PartId = null) {
  def partType = if (partId != null) partId.partType else null
  def partIndex = if (partId != null) partId.partIndex else -1
  def partIdString = if (partId != null) partId.toString else null
  def id = if (partId != null) objectIndex + "." + partId else objectIndex
}

class SceneSelections(val self: Seq[SceneSelection]) extends SeqProxy[SceneSelection] {
  def getSelectedParts(partType: PartType.Value): Seq[SceneSelection] = {
    self.filter(x => x.partType == partType )
  }

  def getSelectedObjects: Seq[SceneSelection] = {
    self.filter(x => x.partType == null )
  }

  def getSelectedForObject(objectIndex: Int): Seq[SceneSelection] = {
    self.filter(x => x.objectIndex == objectIndex)
  }

  def findSelectionIndex(objectIndex: Int): Int = {
    self.indexWhere(x => x.objectIndex == objectIndex && x.partType == null)
  }

  def findSelectionIndex(ss: SceneSelection): Int = {
    self.indexOf(ss)
  }
}

class MutableSceneSelections(buffer: ArrayBuffer[SceneSelection] = new ArrayBuffer) extends SceneSelections(buffer) {
  def remove(i: Int) = buffer.remove(i)
  def add(ss: SceneSelection) = buffer.append(ss)
  def clear() = buffer.clear()
  def select(ss: SceneSelection): Int = {
    val index = findSelectionIndex(ss)
    if (index >= 0) index
    else {
      buffer.append(ss)
      buffer.length-1
    }
  }
  def unselect(ss: SceneSelection) {
    val index = findSelectionIndex(ss)
    if (index >= 0) {
      remove(index)
    }
  }
}

