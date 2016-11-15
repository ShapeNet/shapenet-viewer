package edu.stanford.graphics.shapenet.common

import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import edu.stanford.graphics.shapenet.jme3.JmeUtils

/**
 * Information about the position, orientation of a camera in a scene.
 * Also includes targets that the camera is looking at.
 * The targets are semantic information that is typically left out of
 *   most graphics representations.
 * For the camera, we want to save the position, up, and target
 * In some cases, we don't know what the target is and only have a direction
 * The up vector can either be the camera up or the world up
 *   (the camera up should be close to world up)
 * In the case that only a target is specified, the up vector is typically the world up
 * @author Angel Chang
 */
// TODO: Replace CameraInfo with CameraState!!!
case class CameraState(name: String,
                       position: Vector3f,
                       up: Vector3f,
                       target: Vector3f,
                       direction: Vector3f = null,
                       cameraCoordState: CameraCoordState = null,
                       targetIndices: Set[Int] = null,
                       var score: Double = Double.NaN) {

  // Set camera to this
  def setCamera(camera: Camera): Unit = {
    val camState = if (cameraCoordState != null) {
      // Transform camera state if needed
      val camInfo = CameraInfo(name, this.position, this.up, this.direction, this.target)
      val transCamInfo = JmeUtils.transformCameraStateWithUpFrontToWorld(camInfo, cameraCoordState.up, cameraCoordState.front, cameraCoordState.scale)
      CameraState(transCamInfo)
    } else {
      this
    }
    camera.setLocation(camState.position)
    if (camState.target != null) {
      camera.lookAt(camState.target, camState.up)
    } else {
      camera.lookAtDirection(camState.direction, camState.up)
    }
  }
}

object CameraState {
  def apply(cameraInfo: CameraInfo): CameraState = {
    new CameraState(cameraInfo.name, cameraInfo.position, cameraInfo.up, cameraInfo.target, cameraInfo.direction, targetIndices = cameraInfo.targetObjectIndices)
  }
}

// What coordinate system was used for the camera state
case class CameraCoordState(up: Vector3f, front: Vector3f, scale: Float = 1.0f)


case class CameraInfo(name: String,  // name of camera (indicates what it is for)
                      // position of the camera
                      position: Vector3f,
                      // up direction of the camera
                      up: Vector3f,
                      // direction the camera is facing
                      direction: Vector3f = null,
                      // Point the camera is looking at
                      target: Vector3f = null,
                      // Indicies of objects that the camera was looking at
                      targetObjectIndices: Set[Int] = Set()) {
  assert(position != null)
  assert(up != null)
  assert(direction != null || target != null)

  def withDirection: CameraInfo = if (direction == null) {
    val newUpDir = CameraInfo.lookAt(position, target, up)
    CameraInfo(name, position, newUpDir._1, newUpDir._2, target, targetObjectIndices)
  } else {
    this
  }
}

object CameraInfo {
  val INITIAL = "initial"
  val CURRENT = "current"
  val DEBUG = "debug"

  private def getDirection(position: Vector3f, target: Vector3f): Vector3f = {
    val dir = target.subtract(position)
    if (dir.lengthSquared() > 0) {
      dir.normalize()
    } else { dir }
  }

  private def lookAt(position: Vector3f, target: Vector3f, worldUpVector: Vector3f) = {
    val newDirection = new Vector3f()
    val newUp = new Vector3f()
    val newLeft = new Vector3f()
    val d = 0.0000000001
    newDirection.set(target).subtractLocal(position).normalizeLocal
    newUp.set(worldUpVector).normalizeLocal
    if (newUp.distanceSquared(Vector3f.ZERO) < d) newUp.set(Vector3f.UNIT_Y)
    newLeft.set(newUp).crossLocal(newDirection).normalizeLocal
    if (newLeft.distanceSquared(Vector3f.ZERO) < d) {
      if (newDirection.x != 0) newLeft.set(newDirection.y, -newDirection.x, 0f)
      else newLeft.set(0f, newDirection.z, -newDirection.y)
    }
    newUp.set(newDirection).crossLocal(newLeft).normalizeLocal
    // println("up: " + newUp.toString() + ", left: " + newLeft.toString() + ", dir: " + newDirection.toString())
    (newUp, newDirection)
  }
}