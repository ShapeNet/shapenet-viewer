package edu.stanford.graphics.shapenet.jme3.geom

import com.jme3.bounding.BoundingBox
import com.jme3.math.{Matrix4f, Vector3f, Vector2f}
import edu.stanford.graphics.shapenet.jme3.safeDivide

/**
 * Utility classes for working with bounding boxes
 *  We use the basic coordinate frame of
 *    -X = LEFT
 *     Y = UP
 *    -Z = FRONT
 * @author Angel Chang
 */
object BBFaceType extends Enumeration {
  val XMIN, XMAX, YMIN, YMAX, ZMIN, ZMAX = Value

  def getOppositeFace(face: Int): Int = {
    getOppositeFace(BBFaceType(face)).id
  }

  def getOppositeFace(face: BBFaceType.Value): BBFaceType.Value = {
    face match {
      case XMIN => XMAX
      case XMAX => XMIN
      case YMIN => YMAX
      case YMAX => YMIN
      case ZMIN => ZMAX
      case ZMAX => ZMIN
      case null => null
    }
  }

  // Get the different face centers
  def getCenter(face: Int) = {
    center(face)
  }
  def getCenter(face: BBFaceType.Value) = {
    if (face == null) center(YMIN.id)
    else center(face.id)
  }

  def getFaceCenter(boundingBox: BoundingBox, face: Int): Vector3f = {
    getFaceCenters(boundingBox)(face)
  }

  def getFaceCenter(boundingBox: BoundingBox, face: BBFaceType.Value): Vector3f = {
    getFaceCenters(boundingBox)(face.id)
  }

  def getFaceCenters(boundingBox: BoundingBox): Array[Vector3f] = {
    val bbFaceCenters = Array.ofDim[Vector3f](6)
    val center = new Vector3f()
    val min = new Vector3f()
    val max = new Vector3f()
    boundingBox.getCenter(center)
    boundingBox.getMin(min)
    boundingBox.getMax(max)
    bbFaceCenters(0) = new Vector3f(min.getX, center.getY, center.getZ)
    bbFaceCenters(1) = new Vector3f(max.getX, center.getY, center.getZ)
    bbFaceCenters(2) = new Vector3f(center.getX, min.getY, center.getZ)
    bbFaceCenters(3) = new Vector3f(center.getX, max.getY, center.getZ)
    bbFaceCenters(4) = new Vector3f(center.getX, center.getY, min.getZ)
    bbFaceCenters(5) = new Vector3f(center.getX, center.getY, max.getZ)
    bbFaceCenters
  }

  def getFaceDims(boundingBox: BoundingBox, face: Int): Vector2f = {
    getFaceDims(boundingBox)(face)
  }

  def getFaceSurfaceArea(boundingBox: BoundingBox, face: Int, minExtent: Float = 0.0f): Float = {
    val dims = getFaceDims(boundingBox, face)
    val minDim = minExtent*2
    val x = math.max(dims.getX, minDim)
    val y = math.max(dims.getY, minDim)
    x*y
  }

  def getFaceBB(boundingBox: BoundingBox, face: BBFaceType.Value, d: Float): BoundingBox = {
    val center = getFaceCenter(boundingBox, face)
    var extent = new Vector3f()
    boundingBox.getExtent(extent)
    extent = setFaceDimTo(extent, face, d)
    new BoundingBox(center, extent.x, extent.y, extent.z)
  }

  def getFaceBB(boundingBox: BoundingBox, face: Int, d: Float): BoundingBox = {
    getFaceBB(boundingBox, BBFaceType(face), d)
  }

  // Return dimensions of the face (largest first)
  def getFaceDims(boundingBox: BoundingBox): Array[Vector2f] = {
    val bbFaceDims = Array.ofDim[Vector2f](6)
    val center = new Vector3f()
    val extent = new Vector3f()
    boundingBox.getCenter(center)
    boundingBox.getExtent(extent)
    extent.multLocal(2.0f)
    bbFaceDims(0) = if (extent.getY >= extent.getZ)
      new Vector2f(extent.getY, extent.getZ)
    else new Vector2f(extent.getZ, extent.getY)
    bbFaceDims(1) = bbFaceDims(0)

    bbFaceDims(2) = if (extent.getX >= extent.getZ)
      new Vector2f(extent.getX, extent.getZ)
    else new Vector2f(extent.getZ, extent.getX)
    bbFaceDims(3) = bbFaceDims(2)

    bbFaceDims(4) = if (extent.getY >= extent.getX)
      new Vector2f(extent.getY, extent.getX)
    else new Vector2f(extent.getX, extent.getY)
    bbFaceDims(5) = bbFaceDims(4)

    bbFaceDims
  }

  // Return dimensions of the face (largest first, together with the extent of this face to the opposite face)
  def getFaceDimsWithExtent(boundingBox: BoundingBox): Array[Vector3f] = {
    val extent = new Vector3f()
    boundingBox.getExtent(extent)
    extent.multLocal(2.0f)
    getFaceDimsWithExtent(extent)
  }

  // Return dimensions of the face (largest first, together with the extent of this face to the opposite face)
  def getFaceDimsWithExtent(extent: Vector3f): Array[Vector3f] = {
    val bbFaceDims = Array.ofDim[Vector3f](6)
    bbFaceDims(0) = if (extent.getY >= extent.getZ)
      new Vector3f(extent.getY, extent.getZ, extent.getX)
    else new Vector3f(extent.getZ, extent.getY, extent.getX)
    bbFaceDims(1) = bbFaceDims(0)

    bbFaceDims(2) = if (extent.getX >= extent.getZ)
      new Vector3f(extent.getX, extent.getZ, extent.getY)
    else new Vector3f(extent.getZ, extent.getX, extent.getY)
    bbFaceDims(3) = bbFaceDims(2)

    bbFaceDims(4) = if (extent.getY >= extent.getX)
      new Vector3f(extent.getY, extent.getX, extent.getZ)
    else new Vector3f(extent.getX, extent.getY, extent.getZ)
    bbFaceDims(5) = bbFaceDims(4)
    bbFaceDims
  }

  def reduceToFace(v: Vector3f, face: Int): Vector2f = {
    val f = projs(face)
    f(v)
  }

  def reduceToFace(v: Vector3f, face: BBFaceType.Value): Vector2f = {
    reduceToFace(v, face.id)
  }

  def setFaceDimTo(v: Vector3f, face: Int, value: Float): Vector3f = {
    val f = setFaceDimOps(face)
    f(v, value)
  }

  def setFaceDimTo(v: Vector3f, face: BBFaceType.Value, value: Float): Vector3f = {
    setFaceDimTo(v, face.id, value)
  }

  def setFaceDimTo(v: Vector3f, face: Int, value: Vector3f): Vector3f = {
    val f = setFaceDimOpsVec(face)
    f(v, value)
  }

  def setFaceDimTo(v: Vector3f, face: BBFaceType.Value, value: Vector3f): Vector3f = {
    setFaceDimTo(v, face.id, value)
  }

  def expandToVector3f(v: Vector2f, face: Int): Vector3f = {
    val f = toVector3f(face)
    f(v)
  }

  def expandToVector3f(v: Vector2f, face: BBFaceType.Value): Vector3f = {
    expandToVector3f(v, face.id)
  }

  // Normals pointing to the inside of the bb
  def inNormal(i: Int): Vector3f = {
    if (i >= 0 && i < 6) inNormals(i) else null
  }
  val inNormals = Array(
    new Vector3f(1.0f, 0.0f, 0.0f),
    new Vector3f(-1.0f, 0.0f, 0.0f),
    new Vector3f(0.0f, 1.0f, 0.0f),
    new Vector3f(0.0f, -1.0f, 0.0f),
    new Vector3f(0.0f, 0.0f, 1.0f),
    new Vector3f(0.0f, 0.0f, -1.0f)
  )
  // Normals pointing to the outside of the bb
  def outNormal(i: Int): Vector3f = {
    if (i >= 0 && i < 6) outNormals(i) else null
  }
  val outNormals = Array(
    new Vector3f(-1.0f, 0.0f, 0.0f),
    new Vector3f(1.0f, 0.0f, 0.0f),
    new Vector3f(0.0f, -1.0f, 0.0f),
    new Vector3f(0.0f, 1.0f, 0.0f),
    new Vector3f(0.0f, 0.0f, -1.0f),
    new Vector3f(0.0f, 0.0f, 1.0f)
  )
  def center(i: Int): Vector3f = {
    if (i >= 0 && i < 6) centers(i) else null
  }
  val centers = Array(
    new Vector3f(0.0f, 0.5f, 0.5f),
    new Vector3f(1.0f, 0.5f, 0.5f),
    new Vector3f(0.5f, 0.0f, 0.5f),
    new Vector3f(0.5f, 1.0f, 0.5f),
    new Vector3f(0.5f, 0.5f, 0.0f),
    new Vector3f(0.5f, 0.5f, 1.0f)
  )
  val projs: Array[Vector3f => Vector2f] = Array(
    v => new Vector2f(v.getZ, v.getY),
    v => new Vector2f(v.getZ, v.getY),
    v => new Vector2f(v.getX, v.getZ),
    v => new Vector2f(v.getX, v.getZ),
    v => new Vector2f(v.getX, v.getY),
    v => new Vector2f(v.getX, v.getY)
  )
  val setFaceDimOps: Array[(Vector3f,Float) => Vector3f] = Array(
    (v,x) => new Vector3f(x, v.getY, v.getZ),
    (v,x) => new Vector3f(x, v.getY, v.getZ),
    (v,x) => new Vector3f(v.getX, x, v.getZ),
    (v,x) => new Vector3f(v.getX, x, v.getZ),
    (v,x) => new Vector3f(v.getX, v.getY, x),
    (v,x) => new Vector3f(v.getX, v.getY, x)
  )
  val setFaceDimOpsVec: Array[(Vector3f,Vector3f) => Vector3f] = Array(
    (v,x) => new Vector3f(x.getX, v.getY, v.getZ),
    (v,x) => new Vector3f(x.getX, v.getY, v.getZ),
    (v,x) => new Vector3f(v.getX, x.getY, v.getZ),
    (v,x) => new Vector3f(v.getX, x.getY, v.getZ),
    (v,x) => new Vector3f(v.getX, v.getY, x.getZ),
    (v,x) => new Vector3f(v.getX, v.getY, x.getZ)
  )
  val toVector3f: Array[Vector2f => Vector3f] = Array(
    v => new Vector3f(0, v.getY, v.getX),
    v => new Vector3f(0, v.getY, v.getX),
    v => new Vector3f(v.getX, 0, v.getY),
    v => new Vector3f(v.getX, 0, v.getY),
    v => new Vector3f(v.getX, v.getY, 0),
    v => new Vector3f(v.getX, v.getY, 0)
  )

  def findClosestByOutNormal(outNorm: Vector3f, threshold: Double = 0.99): Int = {
    for (i <- 0 until 6) {
      val norm = BBFaceType.outNormal(i)
      if (outNorm.dot(norm) >= threshold) {
        return i
      }
    }
    return -1
  }

  def findClosestByInNormal(inNorm: Vector3f, threshold: Double = 0.99): Int = {
    for (i <- 0 until 6) {
      val norm = BBFaceType.inNormal(i)
      if (inNorm.dot(norm) >= threshold) {
        return i
      }
    }
    return -1
  }

  def bbFaceTypeToWssCubeFace(bbFaceType: BBFaceType.Value): Int = {
    bbFaceType match {
      case BBFaceType.ZMIN => 0
      case BBFaceType.XMAX => 1
      case BBFaceType.YMAX => 2
      case BBFaceType.ZMAX => 3
      case BBFaceType.XMIN => 4
      case BBFaceType.YMIN => 5
      case _ => -1
    }
  }

  def wssCubeFaceToBBFaceType(wssCubeFace: Int): BBFaceType.Value = {
    wssCubeFace match {
      // Account for rotation...
      case 0 => BBFaceType.YMIN//ZMIN
      case 1 => BBFaceType.XMAX//XMAX
      case 2 => BBFaceType.ZMIN//YMAX
      case 3 => BBFaceType.YMAX//ZMAX
      case 4 => BBFaceType.XMIN//XMIN
      case 5 => BBFaceType.ZMAX//YMIN
      case _ => null
    }
  }

  def wssCubeFaceToBBFaceId(wssCubeFace: Int): Int = {
    val faceType = wssCubeFaceToBBFaceType(wssCubeFace)
    if (faceType != null) faceType.id
    else -1
  }

}

case class BBFaceRel(outNorm: Vector3f) {
  val relType: BBFaceRelType.Value = {
    val i = BBFaceType.findClosestByOutNormal(outNorm)
    if (i >= 0) BBFaceRelType(i)
    else BBFaceRelType.OTHER
  }
  def inNorm = outNorm.negate()
}

object BBFaceRelType extends Enumeration {
  val LEFT, RIGHT, BOTTOM, TOP, FRONT, BACK, OTHER = Value

  def getOppositeFace(face: Int): Int = {
    getOppositeFace(BBFaceRelType(face)).id
  }

  def getOppositeFace(face: BBFaceRelType.Value): BBFaceRelType.Value = {
    face match {
      case LEFT => RIGHT
      case RIGHT => LEFT
      case BOTTOM => TOP
      case TOP => BOTTOM
      case FRONT => BACK
      case BACK => FRONT
      case OTHER => OTHER
      case null => null
    }
  }

}

object BoundingBoxUtils {
  lazy val faceNormals = Array(Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_Z)

  def getMaxExtentWithIndex(bb: BoundingBox): (Float,Int) = {
    val extents = Array(bb.getXExtent, bb.getYExtent, bb.getZExtent)
    extents.zipWithIndex.maxBy( x => x._1 )
  }

  def getMaxExtent(bb: BoundingBox): Float = {
    val extents = Array(bb.getXExtent, bb.getYExtent, bb.getZExtent)
    extents.max
  }

  /**
   * Returns the relative position of point in the bounding box
   * @param bb Bounding box
   * @param point Absolute position of the point
   * @return Relative position of the point ( bbmin = 0, bbmax = 1 )
   */
  def getRelativePointFromMin(bb: BoundingBox, point: Vector3f): Vector3f = {
    val (bbmin,bbmax) = getBBMinMax(bb)
    val extent = bbmax.subtract(bbmin)
    val rel = safeDivide(point.subtract(bbmin), extent)
    rel
  }

  /**
   * Returns the relative position of point in the bounding box
   * @param bb Bounding box
   * @param point Absolute position of the point
   * @return Relative position of the point ( bbmin = -1, bbmax = 1 )
   */
  def getRelativePointFromCenter(bb: BoundingBox, point: Vector3f): Vector3f = {
    val center = bb.getCenter
    val extent = bb.getExtent(null)
    val rel = safeDivide(point.subtract(center), extent)
    rel
  }

  def getRelativePoint(bb: BoundingBox, point: Vector3f, fromMin: Boolean = false): Vector3f = {
    if (fromMin) {
      getRelativePointFromMin(bb, point)
    } else {
      getRelativePointFromCenter(bb, point)
    }
  }

  /**
   * Returns the absolute position of a relative point (wrt to min)
   * @param bb Bounding box
   * @param bbPoint Relative position of the point ( bbmin = 0, bbmax = 1 )
   * @return Absolute position of the point
   */
  def getPointFromMin(bb: BoundingBox, bbPoint: Vector3f): Vector3f = {
    val bbmin = new Vector3f()
    bb.getMin(bbmin)
    val x = bbmin.getX + bb.getXExtent*2*bbPoint.getX
    val y = bbmin.getY + bb.getYExtent*2*bbPoint.getY
    val z = bbmin.getZ + bb.getZExtent*2*bbPoint.getZ
    new Vector3f( x, y, z )
  }

  /**
   * Returns the absolute position of a relative point (wrt to center)
   * @param bb Bounding box
   * @param bbPoint Relative position of the point ( bbmin = -1, bbmax = 1, bbcenter = 0 )
   * @return Absolute position of the point
   */
  def getPointFromCenter(bb: BoundingBox, bbPoint: Vector3f): Vector3f = {
    val center = bb.getCenter
    val x = bbPoint.getX*bb.getXExtent + center.getX
    val y = bbPoint.getY*bb.getYExtent + center.getY
    val z = bbPoint.getZ*bb.getZExtent + center.getZ
    new Vector3f( x, y, z )
  }

  def getBoundingBoxAreas(bb: BoundingBox): Vector3f = {
    // Returns bounding box areas for the yz plane, xz plane and xy plane
    val extent = bb.getExtent(null)
    extent.multLocal(2)
    val yz = extent.getY*extent.getZ
    val xz = extent.getX*extent.getY
    val xy = extent.getX*extent.getZ
    new Vector3f(yz, xz, xy)
  }

  def getBoundingBoxSurfaceArea(bb: BoundingBox): Float = {
    if (bb != null) {
      val bbAreas = getBoundingBoxAreas(bb)
      (bbAreas.x + bbAreas.y + bbAreas.z) * 2
    } else { 0.0f }
  }

  def getBoundingBoxVolume(bb: BoundingBox): Float = {
    if (bb != null) {
      val extent = bb.getExtent(null)
      extent.getX * extent.getY * extent.getZ
    } else { 0.0f }
  }

  def getBBMinMax(bb: BoundingBox) = {
    val bbmin = new Vector3f()
    bb.getMin(bbmin)
    val bbmax = new Vector3f()
    bb.getMax(bbmax)
    (bbmin, bbmax)
  }

  def getBBExtent(bb: BoundingBox) = {
    val extent = new Vector3f()
    bb.getExtent(extent)
  }

  def getBBDims(bb: BoundingBox) = {
    val extent = new Vector3f()
    assert(bb != null)
    bb.getExtent(extent)
    extent.multLocal(2.0f)
  }

  def getUnion(bb1: BoundingBox, bb2: BoundingBox): BoundingBox = {
    val bb = bb1.clone().asInstanceOf[BoundingBox]
    bb.mergeLocal(bb2)
    bb
  }

  def getIntersection(bb1: BoundingBox, bb2: BoundingBox): BoundingBox = {
    val (bb1min,bb1max) = getBBMinMax(bb1)
    val (bb2min,bb2max) = getBBMinMax(bb2)
    val min = bb1min.clone()
    min.maxLocal(bb2min)
    val max = bb1max.clone()
    max.minLocal(bb2max)
    if (min.getX <= max.getX && min.getY <= max.getY && min.getZ <= max.getZ) {
      new BoundingBox(min, max)
    } else {
      null
    }
  }

  def getIntersection(bb1: BoundingBox, bb2: (Vector3f,Vector3f)): BoundingBox = {
    val (bb1min,bb1max) = getBBMinMax(bb1)
    val (bb2min,bb2max) = bb2
    val min = bb1min.clone()
    min.maxLocal(bb2min)
    val max = bb1max.clone()
    max.minLocal(bb2max)
    if (min.getX <= max.getX && min.getY <= max.getY && min.getZ <= max.getZ) {
      new BoundingBox(min, max)
    } else {
      null
    }
  }

  def getRelativeMinMaxBounded(bb: BoundingBox, bbFace: BBFaceType.Value): (Vector3f,Vector3f) = {
    val point = bb.getCenter
    val bound = BBFaceType.getFaceCenter(bb, bbFace)

    val p = bbFace match {
      case BBFaceType.XMAX =>
        (new Vector3f(point.getX, Float.NegativeInfinity, Float.NegativeInfinity),
         new Vector3f(bound.getX, Float.PositiveInfinity, Float.PositiveInfinity))
      case BBFaceType.XMIN =>
        (new Vector3f(bound.getX, Float.NegativeInfinity, Float.NegativeInfinity),
          new Vector3f(point.getX, Float.PositiveInfinity, Float.PositiveInfinity))
      case BBFaceType.YMAX =>
        (new Vector3f(Float.NegativeInfinity, point.getY, Float.NegativeInfinity),
          new Vector3f(Float.PositiveInfinity, bound.getY, Float.PositiveInfinity))
      case BBFaceType.YMIN =>
        (new Vector3f(Float.NegativeInfinity, bound.getY, Float.NegativeInfinity),
          new Vector3f(Float.PositiveInfinity, point.getY, Float.PositiveInfinity))
      case BBFaceType.ZMAX =>
        (new Vector3f(Float.NegativeInfinity, Float.NegativeInfinity, point.getZ),
          new Vector3f(Float.PositiveInfinity, Float.PositiveInfinity, bound.getZ))
      case BBFaceType.ZMIN =>
        (new Vector3f(Float.NegativeInfinity, Float.NegativeInfinity, bound.getZ),
          new Vector3f(Float.PositiveInfinity, Float.PositiveInfinity, point.getZ))
    }
    p
  }

  def getRelativeMinMaxUnbounded(bb: BoundingBox, bbFace: BBFaceType.Value, internal: Boolean = false): (Vector3f,Vector3f) = {
    val point = if (internal) {
      bb.getCenter
    } else {
      BBFaceType.getFaceCenter(bb, bbFace)
    }
    getRelativeMinMaxUnbounded(point, bbFace)
  }

  def getRelativeMinMaxUnbounded(point: Vector3f, dir: BBFaceType.Value): (Vector3f,Vector3f) = {
    val p = dir match {
      case BBFaceType.XMAX => (new Vector3f(point.getX, Float.NegativeInfinity, Float.NegativeInfinity), Vector3f.POSITIVE_INFINITY)
      case BBFaceType.XMIN => (Vector3f.NEGATIVE_INFINITY, new Vector3f(point.getX, Float.PositiveInfinity, Float.PositiveInfinity))
      case BBFaceType.YMAX => (new Vector3f(Float.NegativeInfinity, point.getY, Float.NegativeInfinity), Vector3f.POSITIVE_INFINITY)
      case BBFaceType.YMIN => (Vector3f.NEGATIVE_INFINITY, new Vector3f(Float.PositiveInfinity, point.getY, Float.PositiveInfinity))
      case BBFaceType.ZMAX => (new Vector3f(Float.NegativeInfinity, Float.NegativeInfinity, point.getZ), Vector3f.POSITIVE_INFINITY)
      case BBFaceType.ZMIN => (Vector3f.NEGATIVE_INFINITY, new Vector3f(Float.PositiveInfinity, Float.PositiveInfinity, point.getZ))
    }
    p
  }

  def getExtendedBoundingBoxMinMax(boundingBox: BoundingBox, face: Int, bidirExtension: Boolean): (Vector3f, Vector3f) = {
    getExtendedBoundingBoxMinMax(boundingBox, BBFaceType(face), bidirExtension)
  }

  def getExtendedBoundingBoxMinMax(boundingBox: BoundingBox, face: BBFaceType.Value, bidirExtension: Boolean): (Vector3f, Vector3f) = {
    // BB extended from the specified face
    val (min,max) = getBBMinMax(boundingBox)
    val p = if (bidirExtension) {
      face match {
        case BBFaceType.XMAX => (new Vector3f(Float.NegativeInfinity, min.getY, min.getZ), new Vector3f(Float.PositiveInfinity, max.getY, max.getZ))
        case BBFaceType.XMIN => (new Vector3f(Float.NegativeInfinity, min.getY, min.getZ), new Vector3f(Float.PositiveInfinity, max.getY, max.getZ))
        case BBFaceType.YMAX => (new Vector3f(min.getX, Float.NegativeInfinity, min.getZ), new Vector3f(max.getX, Float.PositiveInfinity, max.getZ))
        case BBFaceType.YMIN => (new Vector3f(min.getX, Float.NegativeInfinity, min.getZ), new Vector3f(max.getX, Float.PositiveInfinity, max.getZ))
        case BBFaceType.ZMAX => (new Vector3f(min.getX, min.getY, Float.NegativeInfinity), new Vector3f(max.getX, max.getY, Float.PositiveInfinity))
        case BBFaceType.ZMIN => (new Vector3f(min.getX, min.getY, Float.NegativeInfinity), new Vector3f(max.getX, max.getY, Float.PositiveInfinity))
      }
    } else {
      face match {
        case BBFaceType.XMAX => (min, new Vector3f(Float.PositiveInfinity, max.getY, max.getZ))
        case BBFaceType.XMIN => (new Vector3f(Float.NegativeInfinity, min.getY, min.getZ), max)
        case BBFaceType.YMAX => (min, new Vector3f(max.getX, Float.PositiveInfinity, max.getZ))
        case BBFaceType.YMIN => (new Vector3f(min.getX, Float.NegativeInfinity, min.getZ), max)
        case BBFaceType.ZMAX => (min, new Vector3f(max.getX, max.getY, Float.PositiveInfinity))
        case BBFaceType.ZMIN => (new Vector3f(min.getX, min.getY, Float.NegativeInfinity), max)
      }
    }
    p
  }

  def getOverlap(bb1: BoundingBox, bb2: (Vector3f,Vector3f)): Float = {
    val intersection = getIntersection(bb1, bb2)
    if (intersection != null) {
      val intersectVolume = getBBVolume(intersection)
      val bb1Volume = getBBVolume(bb1, 0.01f)
      intersectVolume/bb1Volume
    } else {
      0.0f
    }
  }

  def getNonOverlap(bb1: BoundingBox, bb2: (Vector3f,Vector3f)): Float = {
    1.0f - getOverlap(bb1, bb2)
  }

  def getOverlap(bb1: BoundingBox, bb2: BoundingBox): Float = {
    val intersection = getIntersection(bb1, bb2)
    if (intersection != null) {
      val intersectVolume = getBBVolume(intersection)
      val bb1Volume = getBBVolume(bb1, 0.01f)
      intersectVolume/bb1Volume
    } else {
      0.0f
    }
  }

  def getNonOverlap(bb1: BoundingBox, bb2: BoundingBox): Float = {
    1.0f - getOverlap(bb1, bb2)
  }

  def getSurfaceOverlap(bb1: BoundingBox, bb2: BoundingBox, face: Int): Float = {
    val extendedBb2 = getExtendedBoundingBoxMinMax(bb2,face, bidirExtension = true)
    val intersection = getIntersection(bb1, extendedBb2)
    if (intersection != null) {
      val intersectSurface = BBFaceType.getFaceSurfaceArea(intersection, face)
      val bb1Surface = BBFaceType.getFaceSurfaceArea(bb1, face, 0.01f)
      intersectSurface/bb1Surface
    } else {
      0.0f
    }
  }

  def getSurfaceNonOverlap(bb1: BoundingBox, bb2: BoundingBox, face: Int): Float = {
    1.0f - getSurfaceOverlap(bb1, bb2, face)
  }

  def mergeBbs(bbs: Seq[BoundingBox]): BoundingBox = {
    if (bbs.nonEmpty) {
      val res = bbs.head.clone().asInstanceOf[BoundingBox]
      for (bb <- bbs.tail) {
        res.mergeLocal(bb)
      }
      res
    } else {
      null
    }
  }

  def isBBSame(bb1: BoundingBox, bb2: BoundingBox): Boolean = {
    val bb1Extent = getBBExtent(bb1)
    val bb2Extent = getBBExtent(bb2)
    (bb1.getCenter == bb2.getCenter) && (bb1Extent == bb2Extent)
  }

//  private def getTopoRelFlags(bb1: BoundingBox, bb2: BoundingBox): Int = {
//    val intersect = getIntersection(bb1, bb2)
//    var flags = 0
//    if (intersect == null) {
//      flags |= BBRelationFlags.TOPO_DISJOINT
//    } else {
//      flags |= BBRelationFlags.TOPO_OVERLAPS
//      val bb1Same = isBBSame(bb1, intersect)
//      val bb2Same = isBBSame(bb2, intersect)
//      if (bb1Same && bb2Same) {
//        flags |= BBRelationFlags.TOPO_EQUALS
//      } else if (bb1Same) {
//        flags |= BBRelationFlags.TOPO_CONTAINS
//      } else if (bb2Same) {
//        flags |= BBRelationFlags.TOPO_WITHIN
//      }
//      val (bb1min, bb1max) = getBBMinMax(bb1)
//      val (bb2min, bb2max) = getBBMinMax(bb2)
//      if ((bb1min.getX == bb2max.getX || bb1max.getX == bb2min.getX) ||
//          (bb1min.getY == bb2max.getY || bb1max.getY == bb2min.getY) ||
//          (bb1min.getZ == bb2max.getZ || bb1max.getZ == bb2min.getZ)) {
//        flags |= BBRelationFlags.TOPO_TOUCHES
//      }
//    }
//    flags
//  }

  def getEdgeDifferences(bb1: BoundingBox, bb2: BoundingBox): Array[Float] = {
    val minBB1 = bb1.getMin(null)
    val maxBB1 = bb1.getMax(null)
    val minBB2 = bb2.getMin(null)
    val maxBB2 = bb2.getMax(null)

    // Return a bunch of edge distances
    Array(
      minBB1.getX - minBB2.getX,
      minBB1.getX - maxBB2.getX,
      maxBB1.getX - minBB2.getX,
      maxBB1.getX - maxBB2.getX,

      minBB1.getY - minBB2.getY,
      minBB1.getY - maxBB2.getY,
      maxBB1.getY - minBB2.getY,
      maxBB1.getY - maxBB2.getY,

      minBB1.getZ - minBB2.getZ,
      minBB1.getZ - maxBB2.getZ,
      maxBB1.getZ - minBB2.getZ,
      maxBB1.getZ - maxBB2.getZ
    )
  }

  // Returns a 'distance' to a interval
  // + if outside of the interval
  // - if inside the interval
  def getDistanceToInterval(p: Float, min: Float, max: Float) = {
    if (p < min) min - p
    else if (p > max) p - max
    else -math.min( p-min, max-p )
  }

  // Return a bunch of edge distances
  // from bb1.minx, maxx to bb2, bb1.miny, maxy to bb2,...
  // + if outside of the interval
  // - if inside the interval
  def getEdgeIntervalDistances(bb1: BoundingBox, bb2: BoundingBox): Array[Float] = {
    val minBB1 = bb1.getMin(null)
    val maxBB1 = bb1.getMax(null)
    val minBB2 = bb2.getMin(null)
    val maxBB2 = bb2.getMax(null)

    Array(
      getDistanceToInterval(minBB1.getX, minBB2.getX, maxBB2.getX),
      getDistanceToInterval(maxBB1.getX, minBB2.getX, maxBB2.getX),

      getDistanceToInterval(minBB1.getY, minBB2.getY, maxBB2.getY),
      getDistanceToInterval(maxBB1.getY, minBB2.getY, maxBB2.getY),

      getDistanceToInterval(minBB1.getZ, minBB2.getZ, maxBB2.getZ),
      getDistanceToInterval(maxBB1.getZ, minBB2.getZ, maxBB2.getZ)
    )
  }

  // Returns a bunch of edge distances
  // distances inside the interval (-) followed by distances outside the interval (+)...
  def getEdgeInOutDistances(bb1: BoundingBox, bb2: BoundingBox): Array[Float] = {
    val distances = getEdgeIntervalDistances(bb1, bb2)
    val inDistances = distances.grouped(2).map( d => {
      val v1 = math.min(d(0), 0.0f)
      val v2 = math.min(d(1), 0.0f)
      math.max(v1,v2)
    })
    val outDistances = distances.grouped(2).map( d => {
      val v1 = math.max(d(0), 0.0f)
      val v2 = math.max(d(1), 0.0f)
      math.min(v1,v2)
    })
    (inDistances ++ outDistances).toArray
  }

  def getSurfaceOverlapsAndEdgeClearances(bb1: BoundingBox, bb2: BoundingBox, face: BBFaceType.Value): Array[Float] = {
    val overlap12 = getSurfaceOverlap(bb1, bb2, face.id)
    val overlap21 = getSurfaceOverlap(bb2, bb1, face.id)
    val d = getEdgeIntervalDistances(bb1, bb2)
    val (i1,j1) = face match {
      case BBFaceType.XMIN | BBFaceType.XMAX => (4,2)
      case BBFaceType.YMIN | BBFaceType.YMAX => (0,4)
      case BBFaceType.ZMIN | BBFaceType.ZMAX => (0,2)
    }
    val j2 = j1+1
    val i2 = i1+1
    val i1_in = d(i1) <= -0.01
    val i2_in = d(i2) <= -0.01
    val j1_in = d(j1) <= -0.01
    val j2_in = d(j2) <= -0.01
    val (c1,c2) = if (i1_in && i2_in && j1_in && j2_in) {
      // All inside
      ( math.max(d(i1), d(i2)), math.max(d(j1), d(j2)) )
    } else if (overlap12 <= 0.0 && overlap21 <= 0.0) {
      // Disjoint...
      ( math.min( math.max(d(i1), 0.0f), math.max(d(i2), 0.0f)),
        math.min( math.max(d(j1), 0.0f), math.max(d(j2), 0.0f)) )
    } else {
      // TODO: have more reasonable clearances???
      (0.0f, 0.0f)
    }
    val dist = math.sqrt( c1*c1 + c2*c2 ).toFloat
    Array(overlap12, overlap21, c1, c2, dist)
  }

  def getBBVolume(bb: BoundingBox, minExtent: Float = 0.0f) = {
    val x = math.max(bb.getXExtent, minExtent)
    val y = math.max(bb.getYExtent, minExtent)
    val z = math.max(bb.getZExtent, minExtent)
    8*x*y*z
  }

  def isHorizontalPlane(bb: BoundingBox): Boolean = {
    // FIXME: Assumes y up
    // Check that the x and z dimensions are much larger than the y dimension
    val extent = new Vector3f()
    bb.getExtent(extent)
    extent.getY/extent.getX < 0.0001f && extent.getY/extent.getZ < 0.0001f
  }

  def isVerticalPlane(bb: BoundingBox): Boolean = {
    // FIXME: Assumes y up
    // Check that the y and x or z dimensions are much larger than the last dimension
    val extent = new Vector3f()
    bb.getExtent(extent)
    (extent.getX/extent.getZ < 0.0001f && extent.getX/extent.getY < 0.0001f) ||
      (extent.getZ/extent.getX < 0.0001f && extent.getZ/extent.getY < 0.0001f)
  }

  def getTransformedBoundingBox(bb: BoundingBox, transform: Matrix4f): BoundingBox = {
    val transformedBb = new BoundingBox()
    bb.transform(transform, transformedBb)
    transformedBb
  }
}
