package edu.stanford.graphics.shapenet.common

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D
import org.apache.commons.math3.linear.{LUDecomposition, MatrixUtils, RealMatrix}

/**
 * Transformation matrix for scenes
 * @param m Transform in column-major
 * @author Angel Chang
 */
class Transform(var m: Array[Double]) {
  var scale = 1.0
  var M: RealMatrix = MatrixUtils.createRealMatrix(4, 4)
  setM(m)

  def toArray() = m
  def toFloatArray() = m.map( x => x.toFloat )
  def getScale() = scale
  def scaleTo(s: Double) { scaleBy(s/scale) }
  def scaleBy(s: Double) {
    m = m.zipWithIndex.map( x => if (x._2 < 12) x._1*s else x._1 )
    //scale = scale*s
    setM(m)
  }
  def set(m: Array[Double]) {
    if (m.size != 16) throw new IllegalArgumentException("Invalid length for transform: " + m.size)
    this.m = m
    setM(m)
  }
  def set(m: Array[Float]) {
    if (m.size != 16) throw new IllegalArgumentException("Invalid length for transform: " + m.size)
    this.m = m.map( x => x.toDouble )
    setM(this.m)
  }
  def apply(v: Vector3D): Vector3D = {
    val res = M.preMultiply(v.toArray :+ 1.0)
    new Vector3D(res.take(3))
  }
  def getInverse(): Transform = {
    val inv = Transform.invert(M)
    Transform(inv)
  }
  private def setM(m: Array[Double]) {
    for (i <- 0 to 3) M.setColumn(i, m.slice(4*i, 4*i+4))
    val col = M.getColumn(0)
    scale = math.sqrt(col.take(3).map( x => x*x ).sum)
  }
  override def toString = "Transform(" + m.mkString(",") + ")"
}

object Transform {
  def apply() = new Transform(Array(1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1))
  def apply(m: Array[Double]) = {
    if (m.size != 16) throw new IllegalArgumentException("Invalid length for transform: " + m.size)
    new Transform(m)
  }
  def apply(m: Array[Float]) = {
    if (m.size != 16) throw new IllegalArgumentException("Invalid length for transform: " + m.size)
    new Transform(m.map( x => x.toDouble ))
  }
  def apply(m: Seq[Double]) = {
    if (m.size != 16) throw new IllegalArgumentException("Invalid length for transform: " + m.size)
    new Transform(m.toArray)
  }
  def apply(m: RealMatrix) = {
    if (m.getColumnDimension != 4 && m.getRowDimension != 4)
      throw new IllegalArgumentException("Invalid matrix dimensions for transform: " + m.getRowDimension + "x" + m.getColumnDimension)
    val array = Array.ofDim[Double](16)
    for (i <- 0 until 4; j <- 0 until 4) {
      array(4*j + i) = m.getEntry(i,j)
    }
    new Transform(array)
  }

//  def getAlignToUpFrontAxesMatrix3(objectUp: Vector3D, objectFront: Vector3D,
//                                   targetUp: Vector3D, targetFront: Vector3D) = {
//    // Figure out what transform to apply to matrix
//    val objM = axisPairToOrthoMatrix3(objectUp, objectFront)
//    val targetM = axisPairToOrthoMatrix3(targetUp, targetFront)
//    val objMinv = invert(objM)
//    val rotation = targetM.multiply(objMinv)
//    rotation
//  }
//  def getAlignToUpFrontAxesMatrix4(objectUp: Vector3D, objectFront: Vector3D,
//                                   targetUp: Vector3D, targetFront: Vector3D) = {
//    // Figure out what transform to apply to matrix
//    val objM = axisPairToOrthoMatrix4(objectUp, objectFront)
//    val targetM = axisPairToOrthoMatrix4(targetUp, targetFront)
//    val objMinv = invert(objM)
//    val rotation = targetM.multiply(objMinv)
//    rotation
//  }
//
//  def getSemanticCoordinateFrameMatrix4(semanticUp: Vector3D, semanticFront: Vector3D) = {
//    getAlignToUpFrontAxesMatrix4(semanticUp, semanticFront, Constants.SEMANTIC_UP, Constants.SEMANTIC_FRONT)
//  }
//  def getSemanticCoordinateFrameMatrix3(semanticUp: Vector3D, semanticFront: Vector3D) = {
//    getAlignToUpFrontAxesMatrix3(semanticUp, semanticFront, Constants.SEMANTIC_UP, Constants.SEMANTIC_FRONT)
//  }
//
//  def getSemanticToObjectCoordinateMatrix4f(objPosition: Vector3D, objWorldUp: Vector3D, objWorldFront: Vector3D): RealMatrix = {
//    // Offset so objPosition is 0,0,0
//    val worldToAlignedObjRotation = getAlignToUpFrontAxesMatrix4(objWorldUp, objWorldFront, Constants.SEMANTIC_UP, Constants.SEMANTIC_FRONT)
//    val offset = objPosition
//    val offsetMatrix = createMatrix4(Array(
//      1.0, 0.0, 0.0, -offset.getX,
//      0.0, 1.0, 0.0, -offset.getY,
//      0.0, 0.0, 1.0, -offset.getZ,
//      0.0, 0.0, 0.0,     1.0
//    ), rowMajor = true)
//
//    val M = worldToAlignedObjRotation.multiply(offsetMatrix)
//    M
//  }
//
//  def axisPairToOrthoMatrix3(v1: Vector3D, v2: Vector3D): RealMatrix = {
//    val v1n = v1.normalize()
//    val v2n = v2.normalize()
//    val v3 = v1n.crossProduct(v2n)
//    val m = createMatrix3(Array(
//      v1n.getX, v2n.getX, v3.getX,
//      v1n.getY, v2n.getY, v3.getY,
//      v1n.getZ, v2n.getZ, v3.getZ
//    ), rowMajor = true)
//    m
//  }
//  def axisPairToOrthoMatrix4(v1: Vector3D, v2: Vector3D): RealMatrix = {
//    val v1n = v1.normalize()
//    val v2n = v2.normalize()
//    val v3 = v1n.crossProduct(v2n)
//    val m = createMatrix4(Array(
//      v1n.getX, v2n.getX, v3.getX, 0.0,
//      v1n.getY, v2n.getY, v3.getY, 0.0,
//      v1n.getZ, v2n.getZ, v3.getZ, 0.0,
//      0.0,     0.0,     0.0,     1.0
//    ), rowMajor = true)
//    m
//  }
  def invert(M: RealMatrix): RealMatrix = {
    new LUDecomposition(M).getSolver.getInverse
  }
//  def createMatrix3(values: Array[Double], rowMajor: Boolean = false): RealMatrix = {
//    assert(values.length == 9)
//    val M = MatrixUtils.createRealMatrix(3, 3)
//    if (rowMajor) {
//      for (i <- 0 to 2) {
//        M.setRow(i, values.slice(3 * i, 3 * i + 3))
//      }
//    } else {
//      for (i <- 0 to 2) {
//        M.setColumn(i, values.slice(3 * i, 3 * i + 3))
//      }
//    }
//    M
//  }
//  def createMatrix4(values: Array[Double], rowMajor: Boolean = false): RealMatrix = {
//    assert(values.length == 16)
//    val M = MatrixUtils.createRealMatrix(4,4)
//    if (rowMajor) {
//      for (i <- 0 to 3) {
//        M.setRow(i, values.slice(4 * i, 4 * i + 4))
//      }
//    } else {
//      for (i <- 0 to 3) {
//        M.setColumn(i, values.slice(4 * i, 4 * i + 4))
//      }
//    }
//    M
//  }
}