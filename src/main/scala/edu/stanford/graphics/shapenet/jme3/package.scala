package edu.stanford.graphics.shapenet

import edu.stanford.graphics.shapenet.util.WebUtils
import com.jme3.math._
import java.awt.Color
import jme3dae.utilities.MatrixUtils

/**
 * Wrapper around JME3 engine
 * @author Angel Chang
 */
package object jme3 {
  val ZERO_TOLERANCE = 1e-06f
  val HALF_XYZ = new Vector3f(0.5f, 0.5f, 0.5f)

  implicit def javaColorToColorRGBA(color: Color): ColorRGBA = {
    new ColorRGBA(color.getRed/255.0f, color.getGreen/255.0f, color.getBlue/255.0f, color.getAlpha/255.0f)
  }
  implicit def colorRGBAToJavaColor(color: ColorRGBA): Color = {
    new Color(color.asIntRGBA(), true)
  }

  implicit def vector3fToArray(v: Vector3f): Array[Double] = {
    if (v != null)
      Array[Double](v.getX, v.getY, v.getZ)
    else null
  }
  def arrayToVector3f(v: Array[Double]) = {
    if (v != null)
      new Vector3f(v(0).toFloat, v(1).toFloat, v(2).toFloat)
    else null
  }

  implicit def safeDivide(v1: Vector3f, v2: Vector3f): Vector3f = {
    val x = if (v2.x > 0) v1.x/v2.x else v1.x
    val y = if (v2.y > 0) v1.y/v2.y else v1.y
    val z = if (v2.z > 0) v1.z/v2.z else v1.z
    new Vector3f(x,y,z)
  }

  implicit def stringToVector3f(s: String): Vector3f = {
    if (s == null || s.trim.isEmpty) null
    else {
      var vs = s.trim
      if (vs.charAt(0) == '(') vs = vs.substring(1)
      if (vs.charAt(vs.length-1) == ')') vs = vs.substring(0, vs.length-1)
      val nums = vs.split(",").map( x => x.trim.toFloat )
      if (nums.length == 3) {
        new Vector3f(nums(0), nums(1), nums(2))
      } else {
        throw new IllegalArgumentException("Invalid vector3f format: " + s)
      }
    }
  }

  def stringToMatrix4f(str: String): Matrix4f = {
    if (str == null || str.trim.isEmpty) return  null
    val mat = new Matrix4f()
    val rows = str.split("\\n+").filter( s => s.trim().length > 0 )
    assert(rows.size == 4)
    for (i <- 0 until 4) {
      val cols = rows(i).split("\\s+")
      assert(cols.size == 4)
      for (j <- 0 until 4) {
        mat.set(i,j,cols(j).toFloat)
      }
    }
    mat
  }

  def jsonArrayStringToMatrix4f(str: String): Matrix4f = {
    if (str == null || str.trim.isEmpty) return  null
    val trimmed = str.replaceAll("[\\[\\]]","").trim()
    val elements = trimmed.split("\\s*,\\s*")
    assert(elements.size == 16)
    arrayToMatrix(elements.map( x => x.toFloat))
  }

  def loadMatrix4f(filename: String): Option[Matrix4f] = {
    val matrixStr = WebUtils.loadString(filename)
    matrixStr.map( s => stringToMatrix4f(s) )
  }

  def matrixToTransform( m: Matrix4f ): Transform = {
    // There is a slight bug in the matrix4f.toRotationQuat in that it doesn't normalize
    // so we don't use that function
    MatrixUtils.matrix4fToTransform(m)
    //val quatRot = new Quaternion()
    //quatRot.fromRotationMatrix(m.toRotationMatrix.normalizeLocal())
    //new Transform(m.toTranslationVector,quatRot, m.toScaleVector)
  }

  def transformToMatrix( t: Transform ): Matrix4f = {
    MatrixUtils.transformToMatrix4f(t)
//    val m = new Matrix4f()
//    m.setTransform( t.getTranslation, t.getScale, t.getRotation.toRotationMatrix )
//    m
  }

  def arrayToTransform( ta: Array[Float] ): Transform = matrixToTransform( new Matrix4f(ta) )
  def transformToArray(t: Transform): Array[Float] = {
    val m = transformToMatrix(t)
    val array = Array.ofDim[Float](16)
    // array is columnMajor
    m.get(array, /* rowMajor = */ false)
    array
  }
  def matrixToArray( m: Matrix4f): Array[Float] = {
    val array = Array.ofDim[Float](16)
    // array is columnMajor
    m.get(array, /* rowMajor = */ false)
    array
  }
  def arrayToMatrix( v: Array[Float]): Matrix4f = {
    val m = new Matrix4f()
    // array is columnMajor
    m.set(v, /*rowMajor = */ false)
    m
  }

  def getInverse(t: Transform): Transform = {
    val m = new Matrix4f()

    m.setTransform( t.getTranslation, t.getScale, t.getRotation.toRotationMatrix )
    val inv = m.invert()
    matrixToTransform(inv)
  }

  def abs(v: Vector3f): Vector3f = {
    new Vector3f(math.abs(v.x), math.abs(v.y), math.abs(v.z))
  }

  case class EnrichedVector3f(v: Vector3f) {
    def +(o: Vector3f) = v.add(o)
    def -(o: Vector3f) = v.subtract(o)
    def *(s: Number) = v.mult(s.floatValue())
    def /(s: Number) = v.mult(1.0f / s.floatValue())
    def unitCross(o: Vector3f) = {
      v.cross(o).normalizeLocal()
    }
    def abs() = new Vector3f(math.abs(v.x), math.abs(v.y), math.abs(v.z))
  }
  implicit def Vector3fToEnrichedVector3f(v: Vector3f) = EnrichedVector3f(v)
  implicit def EnrichedVector3fToVector3f(v: EnrichedVector3f) = v.v

  case class EnrichedTriangle(t: Triangle) {
    // Returns the edge between vertices i0 and i1 as a segment
    def getEdgeSegment(i0: Int, i1: Int) = {
      val segment = new LineSegment()
      val dir = t.get(i1) - t.get(i0)
      val length = dir.length()
      segment.setOrigin( (t.get(i0) + t.get(i1)).mult(0.5f) )
      segment.setDirection( dir.normalizeLocal() )
      segment.setExtent( 0.5f*length )
      segment
    }
  }
  implicit def TriangleToEnrichedTriangle(t: Triangle) = EnrichedTriangle(t)
  implicit def EnrichedTriangleToTriangle(t: EnrichedTriangle) = t.t
}

