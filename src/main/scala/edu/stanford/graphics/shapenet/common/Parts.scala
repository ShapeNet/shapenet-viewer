package edu.stanford.graphics.shapenet.common

import edu.stanford.graphics.shapenet.util.StringUtils

/**
  * Data structure for parts
  * @author Angel Chang
  */
object PartType extends Enumeration {
  val MESH, SGPATH, OBJECT, CLUSTER = Value
}

case class PartId(partType: PartType.Value, partIndex: Int, partIdValue: String) {
  override def toString(): String = {
    if (partType != null) partType + "-" + partIdValue else null
  }
}

object PartId {
  def parse(partId: String): PartId = {
    if (partId != null) {
      val x = partId.split("-", 2)
      val pt = PartType.withName(x(0))
      val pi: Int = if (StringUtils.isAllDigits(x(1))) {
        x(1).toInt
      } else {
        -1
      }
      PartId(pt, pi, x(1))
    } else null
  }
  def apply(partType: PartType.Value, partIndex: Int): PartId =
    if (partType != null && partIndex >= 0)
      PartId(partType, partIndex, partIndex.toString)
    else null
  def toString(partType: PartType.Value, partIndex: Int) = if (partType != null) partType + "-" + partIndex else null
  def toString(partType: PartType.Value, partIdValue: String) = if (partType != null) partType + "-" + partIdValue else null
}

