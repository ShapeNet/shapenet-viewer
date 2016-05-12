package edu.stanford.graphics.shapenet.jme3.viewer

import java.io.File

import edu.stanford.graphics.shapenet.{Constants, UserDataConstants}
import edu.stanford.graphics.shapenet.common.{CameraState, GeometricScene, CameraInfo}
import edu.stanford.graphics.shapenet.jme3.JmeUtils
import edu.stanford.graphics.shapenet.jme3.geom.BoundingBoxUtils
import edu.stanford.graphics.shapenet.util.{UserData, Loggable}
import com.jme3.bounding.BoundingBox
import com.jme3.math._
import com.jme3.renderer.Camera
import com.jme3.scene.{Node, Spatial}

import scala.concurrent.Promise

/**
 * Optimizes the view by positioning the camera appropriately
 * This is an heuristic based camera position optimizer
 *  that try to optimize the camera position
 *  by maximizing the number of objects visible
 * TODO: Have better interface with
 *   input: target position/target objects, up, height
 *   output: camera location and rotation (with scores)
 *
 * @author Angel Chang
 */
class CameraPositionOptimizer(val viewer: Viewer,
                              val options: CameraPositionOptions = new CameraPositionOptions()) extends Loggable {
  val jme = viewer.jme

  val sceneStats = viewer.getSceneStats
  val offscreen = viewer.getOffScreen
  val basicCameraPositioner = jme.cameraPositioner
//  val largeOffscreenAnalyzer = new OffscreenAnalyzer(
//    viewer.getRenderManager, offscreen.width*2, offscreen.height*2, offscreen.transform)

  /**
   * Try to optimize the camera position
   *  by maximizing the number of objects visible
   *  For now, keep camera distance and height
   * select only horizontal positioning
    *
    * @param nCameraPos - number of camera positions to try
   */
  def optimize(nCameraPos: Int, targetNodes: Spatial*) {
    optimize(nCameraPos, listener = null, targetNodes:_*)
  }

  def optimize(nCameraPos: Int, listener: CameraOptimizationListener, targetNodes: Spatial*) {
    optimizePosition(nCameraPos, listener, targetNodes:_*)
  }

  /**
   * Optimizes camera position for looking at a specified set of targets
   *  (or the entire scene if no targets specified) by:
   * 1. Set camera up to be the scene up
   * 2. Set camera target to be the center of the bounding box containing
   *    all targets (and their children)
   * 3. Consider camera positions distributed evenly (based on nCameraPos)
   *    around the target set back an appropriate distance (distance from bounding box)
   *    (the camera is pushed forward if there are objects in the way)
   * 4. Select camera position based on the number of relevant objects
   *    (either target or child of target) visible
    *
    * @param nCameraPos Number of camera positions to consider
   * @param listener Callback for notifying when optimization is complete
   * @param targetNodes Target nodes to focus on
   */
  def optimizePosition(nCameraPos: Int, listener: CameraOptimizationListener, targetNodes: Spatial*) {
    // Generate positions to examine
    val camPositions = generateCameraStates(nCameraPos, targetNodes:_*)(options)
    // Enqueue this as a task to be handled by rendering and reviewing the rendered image
    viewer.renderTasks.enqueue( new OptimizeCameraPositionsTask(camPositions,listener) )
  }

  def generateDebugPositions(targets: Spatial*): Seq[CameraInfo] = {
    // Predefined debug camera positions
    val camera = viewer.getCamera
    basicCameraPositioner.positionCamera(
      camera, targets, distanceScale = 1.0f,
      camHeightRatio = None, camAngleFromHorizontal = None,
      camPosType = CameraPositioningStrategy.POSITION_TO_FIT)
    val frontView = jme.toCameraInfo(camera, CameraInfo.DEBUG + "0")

    jme.cameraPositioner.positionCamera(
      camera, targets, distanceScale = 1.0f,
      camHeightRatio = Option(1.5f), camAngleFromHorizontal = None,
      camPosType = CameraPositioningStrategy.POSITION_TO_FIT)
    val debugCam1 = jme.toCameraInfo(camera, CameraInfo.DEBUG + "1")

    jme.cameraPositioner.positionCamera(
      camera, targets, distanceScale = 1.0f,
      camHeightRatio = None, camAngleFromHorizontal = Option((Math.PI/4).toFloat),
      camPosType = CameraPositioningStrategy.POSITION_TO_FIT)
    val debugCam2 = jme.toCameraInfo(camera, CameraInfo.DEBUG + "2")
    IndexedSeq(frontView, debugCam1, debugCam2)
  }

  private def getTargetNodesFromScene(scene: GeometricScene[Node], specifiedTargetNodes: Spatial*)(implicit options: CameraPositionOptions): Seq[Spatial] = {
    def isRoot(node: Node) = {
      val index = jme.getModelInstanceIndex(node)
      if (index >= 0) {
        val m = scene.scene.objects(index)
        (m != null && m.supportParentIndex < 0)
      } else {
        false
      }
    }

    // 0. figure out target nodes based on if we want to have root or not
    var targetNodes = specifiedTargetNodes
    if (options.sceneWithoutRoot) {
      // No root
      if (targetNodes.length == 0) {
        // find root model and keep only children
        targetNodes = scene.modelInstances.map(x => if (x != null) x.node else null).filter( x => x != null && !isRoot(x))
      }
    }
    targetNodes
  }

  def generateCameraStates(nCameraPos: Int, specifiedTargetNodes: Spatial*)(implicit options: CameraPositionOptions): CameraStates = {
    val scene = viewer.scene
    val fcscene = new FalseColoredScene(scene)(jme)
    val cam = viewer.getCamera

    // 0. figure out target nodes based on if we want to have root or not
    val targetNodes = getTargetNodesFromScene(scene, specifiedTargetNodes:_*)

    // 1a. Set defaults for when no targets are selected
    // Initial camera from which starting state is copied
    var sourceCamera: Camera = cam
    // Compute bounding box of scene
    var bb: BoundingBox = jme.getBoundingBox(scene.node)
    // How far (in terms of the max extent of the bounding box) to be away
    var distanceScale = options.distanceFromObjectRatio.getOrElse(viewer.defaultSceneDistanceScale)
    var dists: Vector3f = null
    var targetIndices: Set[Int] = null
    // 1b. Compute above for when there are some targets
    if (targetNodes != null && targetNodes.length > 0) {
      distanceScale = options.distanceFromObjectRatio.getOrElse(viewer.defaultModelDistanceScale)
      // Reset to camera something reasonable to start
      sourceCamera = cam.clone()
      dists = basicCameraPositioner.positionCamera(sourceCamera, targetNodes.toSeq, distanceScale,
        camHeightRatio = None, //options.cameraHeightToObjectHeightRatio,
        camAngleFromHorizontal = options.cameraAngleFromHorizontal,
        camPosType = options.cameraPositioningStrategy)
      // Figure out our bounding box
      bb = jme.getBoundingBox(targetNodes:_*)
      // Set scorer
      // Target indices (no children)
      //val targetIndices = targetNodes.map( s => jme.getModelInstanceIndex(s) ).toSet
      // Target indices (includes children)
      targetIndices = targetNodes.map( s => jme.getModelInstanceNodesMap(s).keySet ).flatten.toSet
      if (options.childrenOnly) {
        // Remove target nodes from target indices
        val targetParentIndices = targetNodes.map( s => jme.getModelInstanceIndex(s) ).toSet
        targetIndices = targetIndices.filter( x => !targetParentIndices.contains(x) )
      }
      //      logger.debug("Targets: " + targetIndices)
    } else {
      sourceCamera = cam.clone()
      dists = basicCameraPositioner.positionCamera(sourceCamera, Seq(scene.node), distanceScale,
        camHeightRatio = None, //options.cameraHeightToObjectHeightRatio,
        camAngleFromHorizontal = options.cameraAngleFromHorizontal,
        camPosType = options.cameraPositioningStrategy)
      if (scene.modelInstances.size == 1) {
        distanceScale = options.distanceFromObjectRatio.getOrElse(viewer.defaultModelDistanceScale)
      }
    }
    // Compute max extent of bounding box so we can be a certain distance away
    val bbDims = BoundingBoxUtils.getBBDims(bb)
    val maxExtent = Seq(bbDims.x, bbDims.y, bbDims.z).max
    val dist = distanceScale * maxExtent  // Desired distance from bounding box

    // 2. Get initial starting configuration of camera (should be reasonable)
    val target = jme.userData.getOrElse[Vector3f](sourceCamera, UserDataConstants.CAMERA_TARGET, null)
    val up = jme.userData.getOrElse[Vector3f](sourceCamera, UserDataConstants.CAMERA_UP, null)
    val start = jme.userData.getOrElse[Vector3f](sourceCamera, UserDataConstants.CAMERA_LOCATION, null)

    // 3. Generate positions around target to test
    val positions = Array.ofDim[CameraState](nCameraPos)
    val delta = (math.Pi*2)/nCameraPos
    val bbNode = jme.bbToSpatial(bb)
    val targetCamHeight: Option[Float] = if (options.cameraHeight.isDefined) {
      options.cameraHeight
    } else if (options.cameraHeightToObjectHeightRatio.isDefined) {
      // Get the extent of the current target bb and project onto worldUp
      // Assume Y up
      Option(bb.getMin(null).getY + bbDims.dot(jme.worldUp) * options.cameraHeightToObjectHeightRatio.get)
    } else None
    // rotate around up axis with center at target delta
    val rotTarget = target
    var next = start
    val axisOfRotation = options.axisOfRotation.getOrElse(jme.worldUp)
    if (options.startRotation.isDefined) {
      val r: Float = options.startRotation.get
      val rMatrix = jme.getRotateAroundAxisMatrix(rotTarget, axisOfRotation, r)
      next = rMatrix.mult(next)
    }
    val matrix = jme.getRotateAroundAxisMatrix(rotTarget, axisOfRotation, delta.toFloat)
    for (i <- 0 until nCameraPos) {
      var cameraPosBaseScore = 0.0
      var currentPos = next.clone()
      // Adjust currentPos until it has our target height and distance
      if (targetCamHeight.isDefined) {
        //logger.debug("targetCamHeight is " + targetCamHeight)
        // Compute current height and adjust
        val cur = currentPos.dot(jme.worldUp)
        val d = targetCamHeight.get - cur
        // Adjust so the height is the same as the targetHeight
        currentPos.addLocal( jme.worldUp.mult(d) )
        //logger.debug("next Height is " + next.dot(jme.worldUp))
      }
      //logger.debug( next + ": " + bb.distanceToEdge(next) )
      // Adjust position so we are a good distance away,
      // while making sure there is still nothing between us and the bounding box
      val p = jme.keepDistance(bbNode, currentPos, target, dist )
      val suggestedPoint = p._1
      currentPos = suggestedPoint
      if (options.keepTargetsVisible) {
        val bbpoint = p._2
        // See what is between us and the target
        // do ray from bbpoint to next and get the first contact point
        val cols = jme.collisionsBetween(scene.node, bbpoint, currentPos)
        if (cols.size() > 0) {
          currentPos = cols.getClosestCollision.getContactPoint
          // Have a penalty for having moved the camera from the desired distance
          // TODO: Improve this penalty
          val actualDist = cols.getClosestCollision.getDistance
          val distPenaltyUnscaled = if (dist > 0) math.abs(actualDist - dist)/dist else math.abs(actualDist)
          val distPenalty = -distPenaltyUnscaled*options.distPenalty
          cameraPosBaseScore += distPenalty
        }
      }
      // Save position
      positions(i) = new CameraState(name = i.toString, position = currentPos, up = jme.worldUp, target = target, targetIndices = targetIndices, score = cameraPosBaseScore)
      // Rotate to next position
      next = matrix.mult(suggestedPoint)
    }

    // How to score a particular camera state
    var scorer: () => Double = () => sceneStats.getModelIndexCountsWithPercent().size.toDouble
    if (targetIndices != null && targetIndices.size > 0) {
      scorer = () => {
        val totalPixels = sceneStats.getTotalPixels()
        val allObjCounts = sceneStats.getModelIndexCountsWithPercent()
        val allObjVisible = allObjCounts.size.toDouble / scene.modelInstances.size
        val relevantObjCounts = allObjCounts.filterKeys(i => targetIndices.contains(i))
        val relevantVisible = relevantObjCounts.size.toDouble
        val percentOfScreen = relevantObjCounts.values.sum.toDouble / totalPixels
        val max = 0.4
        val min = 0.2
        val percentOfScreenScore =
          if (percentOfScreen > max) 1
          else if (percentOfScreen < min) 0
          else (percentOfScreen - min) / (max - min)
        relevantVisible + percentOfScreenScore + allObjVisible
      }
    }

    new CameraStates(fcscene, positions, sourceCamera, scorer)
  }

  class OptimizeCameraPositionsTask(val camPositions: CameraStates,
                                    val listener: CameraOptimizationListener) extends RenderTask {
    private var _done: Boolean = false

    override def update(tpf: Float) = {
      if (camPositions.iterator.hasNext) {
        val next = camPositions.iterator.next()
        //viewer.getOffScreenDisplay.setEnabled(true)
        offscreen.viewScene(camPositions.scene.coloredSceneRoot)
        offscreen.camera.copyFrom(camPositions.sourceCamera)
        offscreen.camera.setLocation( next.position )
        offscreen.camera.lookAt( next.target, next.up )
       // largeOffscreenAnalyzer.enableDisplay(true)
       // largeOffscreenAnalyzer.setCameraPosition(offscreen.camera)
       // largeOffscreenAnalyzer.setCamera(offscreen.camera)
        //largeOffscreenAnalyzer.getOffScreen.camera.setViewPort(0.25f,0.75f, 0.25f, 0.75f)
       // largeOffscreenAnalyzer.getOffScreen.viewScene(camPositions.scene.coloredSceneRoot.clone())
       // largeOffscreenAnalyzer.update(tpf)
        offscreen.saveImage(Constants.DEBUG_DIR + "camopt" + File.separator + "cam" + next.name + ".png")
        val callback = () => {
          val score = camPositions.scorer()
          if (next.score.isNaN)
            next.score = score
          else next.score += score
          if (camPositions.bestPosition == null || next.score > camPositions.bestPosition.score) {
            camPositions.bestPosition = next
            logger.info("camPos: " + next + "*")
          } else {
            logger.info("camPos: " + next)
          }
          if (listener != null) {
            listener.evaluated(next)
          }
        }
        sceneStats.analyzeScene("optimize camera", camPositions.scene, callback)
        RenderTaskStatus.Updated
      } else {
        RenderTaskStatus.Done
      }
    }

    override def done() {
      // Done
      val best = camPositions.bestPosition
      if (best != null) {
        viewer.getCamera.setLocation( best.position )
        viewer.getCamera.lookAt( best.target, best.up )
      }
      _done = true
      if (listener != null) {
        val result =
          if (best != null)
            CameraOptimizationResult(best)
          else null
        listener.optimized(result)
      }
    }

    override def isDone: Boolean = _done
  }

}

class BasicCameraPositioner(val worldUp: Vector3f = JmeUtils.worldUp, val userData: UserData = null) extends Loggable {
  val defaultFov = 30f

  /** Returns distance to fit giving bounding box in view */
  def getDistsToFit(bb: BoundingBox, width: Int, height: Int, fovy: Float = defaultFov): Vector3f = {
    val dims = BoundingBoxUtils.getBBDims(bb)
    val aspectRatio = if (width >0 && height > 0) width.toFloat/height.toFloat else 1.0f
    val maxDims = new Vector3f(
      Math.max( dims.z/aspectRatio, dims.y),
      Math.max( dims.x/aspectRatio, dims.z),
      Math.max( dims.x/aspectRatio, dims.y)
    )
    val tanFov = Math.tan(Math.PI/180*fovy/2)
    val c = 0.5f/tanFov.toFloat
    val dists = maxDims.mult( c*1.1f )
    dists
  }

  /** returns sequence of camera infos for canonical views of a a zoomed in view of the target nodes (fitting to given width/height) */
  def getCanonicalViewsToFit(targetNodes: Spatial*)(width: Int, height: Int): IndexedSeq[CameraInfo] = {
    val bb = JmeUtils.getBoundingBox(targetNodes:_*)
    getCanonicalViewsToFit(bb)(width, height)
  }

  def getCanonicalViewsToFit(bb: BoundingBox)(width: Int, height: Int): IndexedSeq[CameraInfo] = {
    val dists = getDistsToFit(bb, width, height)
    getCanonicalViewsWithDists(bb)(dists)
  }

  def getCanonicalViewsWithDistScale(targetNodes: Spatial*)(distanceScale: Float = 1.0f): IndexedSeq[CameraInfo] = {
    val bb = JmeUtils.getBoundingBox(targetNodes:_*)
    getCanonicalViewsWithDistScale(bb)(distanceScale)
  }

  def getCanonicalViewsWithDistScale(bb: BoundingBox)(distanceScale: Float): IndexedSeq[CameraInfo] = {
    val dims = BoundingBoxUtils.getBBDims(bb)
    val maxDim = Seq(dims.x, dims.y, dims.z).max
    val d = maxDim*distanceScale
    val distances = new Vector3f(d,d,d)
    getCanonicalViewsWithDists(bb)(distances)
  }

  def getCanonicalViewsWithDists(targetNodes: Spatial*)(distances: Vector3f = Vector3f.UNIT_XYZ): IndexedSeq[CameraInfo] = {
    val bb = JmeUtils.getBoundingBox(targetNodes:_*)
    getCanonicalViewsWithDists(bb)(distances)
  }

  def getCanonicalViewsWithDists(bb: BoundingBox)(distances: Vector3f): IndexedSeq[CameraInfo] = {
    val min = bb.getMin(null)
    val max = bb.getMax(null)
    val centroid = bb.getCenter()
    val positions = IndexedSeq(
      new Vector3f( min.x - distances.x, centroid.y, centroid.z),
      new Vector3f( max.x + distances.x, centroid.y, centroid.z),
      new Vector3f( centroid.x, min.y - distances.y, centroid.z),
      new Vector3f( centroid.x, max.y + distances.y, centroid.z),
      new Vector3f( centroid.x, centroid.y, min.z - distances.z),
      new Vector3f( centroid.x, centroid.y, max.z + distances.z)
    )
    val names = IndexedSeq("left", "right", "bottom", "top", "front", "back")
    val cameras = positions.zip(names).map( x => {
      val position = x._1
      val name = x._2
      CameraInfo(name, position, JmeUtils.worldUp, target = centroid).withDirection
    })
    cameras
  }

  def getCameraInfo(targetNodes: Spatial*)(distanceScale: Float = 1, normal: Vector3f = JmeUtils.worldFront): CameraInfo = {
    val bb = JmeUtils.getBoundingBox(targetNodes:_*)
    getCameraInfo(bb)(distanceScale, normal)
  }

  def getCameraInfo(bb: BoundingBox)(distanceScale: Float, normal: Vector3f): CameraInfo = {
    val dims = BoundingBoxUtils.getBBDims(bb)
    val centroid = bb.getCenter()
    val maxDim = Seq(dims.x, dims.y, dims.z).max
    val delta = normal.mult( dims.mult(0.5f) ).add(normal.mult(maxDim*distanceScale))
    val position = centroid.add(delta)
    CameraInfo("cam", position, JmeUtils.worldUp, target = centroid).withDirection
  }

  def positionCamera(camera: Camera,
                     target: Spatial,
                     distanceScale: Float = 1,
                     camHeightRatio: Option[Float] = None,
                     camPosType: CameraPositioningStrategy.Value = CameraPositioningStrategy.DEFAULT) {
    positionCamera(camera, Seq(target), distanceScale, camHeightRatio, None, camPosType)
  }

  def positionCamera(camera: Camera, targets: Seq[Spatial]) {
    positionCamera(camera, targets, 1.0f, None,  None, CameraPositioningStrategy.DEFAULT)
  }

  def positionCamera(camera: Camera, targets: Seq[Spatial], camHeightRatio: Option[Float]) {
    positionCamera(camera, targets, 1.0f, camHeightRatio, None, CameraPositioningStrategy.DEFAULT)
  }

  def positionCamera(camera: Camera, targets: Seq[Spatial],
                     distanceScale: Float,
                     camPosType: CameraPositioningStrategy.Value) {
    positionCamera(camera, targets, distanceScale, camHeightRatio = None, camAngleFromHorizontal = None, camPosType)
  }

  def positionCamera(camera: Camera,
                     targets: Seq[Spatial],
                     distanceScale: Float,
                     camHeightRatio: Option[Float],
                     camAngleFromHorizontal: Option[Float],  // angle from horizontal
                     camPosType: CameraPositioningStrategy.Value): Vector3f = {
    val bb = JmeUtils.getBoundingBox(targets:_*)
    logger.debug("PositionCamera TargetBB: " + bb)
    val centroid = bb.getCenter

    val dims = BoundingBoxUtils.getBBDims(bb)
    val min = bb.getMin(null)
    val max = bb.getMax(null)
    val maxDim = Seq(dims.x, dims.y, dims.z).max
    setCameraFrustum(camera, defaultFov, maxDim/200.0f, maxDim*5.0f)

    // TODO: Generalize - for now assume worldUp is Y up
    def positionToViewFront(distZ: Float) {
      val camY = if (camHeightRatio.isDefined) {
        val r = camHeightRatio.get
        min.getY*(1-r) + max.getY*r
      } else centroid.y
      val target = centroid
      camera.setLocation( new Vector3f(centroid.x, camY, min.z - distZ) )
      camera.lookAt( centroid, worldUp )

      if (userData != null) {
        userData.set(camera, UserDataConstants.CAMERA_LOCATION, camera.getLocation.clone())
        userData.set(camera, UserDataConstants.CAMERA_TARGET, target)
        userData.set(camera, UserDataConstants.CAMERA_UP, camera.getUp())  // the camera up should be the same as world up...
        userData.set(camera, UserDataConstants.CAMERA_LEFT, camera.getLeft())
      }
    }

    def positionToViewFrontTheta(distZ: Float, distY: Float, theta: Float) {
      val ry = dims.y/2.0f + distY
      val rz = dims.z/2.0f + distZ
      val camY = centroid.y + ry*math.sin(theta).toFloat
      val camZ = centroid.z - rz*math.cos(theta).toFloat
      val target = centroid
      camera.setLocation( new Vector3f(centroid.x, camY, camZ) )
      camera.lookAt( centroid, worldUp )

      if (userData != null) {
        userData.set(camera, UserDataConstants.CAMERA_LOCATION, camera.getLocation.clone())
        userData.set(camera, UserDataConstants.CAMERA_TARGET, target)
        userData.set(camera, UserDataConstants.CAMERA_UP, camera.getUp())  // the camera up should be the same as world up...
        userData.set(camera, UserDataConstants.CAMERA_LEFT, camera.getLeft())
      }
    }

    // Position the camera.
    val dists = _getCameraDistances(camera, bb, dims, maxDim*distanceScale, camPosType)
    if (camHeightRatio.isDefined) {
      positionToViewFront(dists.z)
    } else {
      positionToViewFrontTheta(dists.z, dists.y, camAngleFromHorizontal.getOrElse(0.0f))
    }
    dists
  }

  private def _getCameraDistances(camera: Camera,
                                  bb: BoundingBox,
                                  dims: Vector3f,
                                  distance: Float,
                                  camPosType: CameraPositioningStrategy.Value): Vector3f = {
    val dists = camPosType match {
      case CameraPositioningStrategy.POSITION_TO_FIT => {
        getDistsToFit(bb, camera.getWidth(), camera.getHeight(), defaultFov)
      }
      case CameraPositioningStrategy.POSITION_BY_DISTANCE => {
        val d = distance
        new Vector3f(d,d,d)
      }
      case CameraPositioningStrategy.POSITION_BY_DISTANCE_TO_CENTROID => {
        val d = distance
        new Vector3f(d-dims.x/2.0f,d-dims.y/2.0f,d-dims.z/2.0f)
      }
    }
    dists
  }

  def generateCameraPositions(camera: Camera,
                              targets: Seq[Spatial],
                              distanceScale: Float,
                              camAngleFromHorizontal: Option[Float],  // angle from horizontal
                              camPosType: CameraPositioningStrategy.Value)
                             (nCameras: Int, startAngle: Option[Float] = None, endAngle: Option[Float] = None): Seq[CameraState] = {
    val bb = JmeUtils.getBoundingBox(targets:_*)
    val centroid = bb.getCenter

    val dims = BoundingBoxUtils.getBBDims(bb)
    val maxDim = Seq(dims.x, dims.y, dims.z).max
    setCameraFrustum(camera, defaultFov, maxDim/200.0f, maxDim*5.0f)

    // theta is angleFromHorizontal
    // phi is rotation from front
    def positionToView(name: String, dists: Vector3f, theta: Float, phi: Float): CameraState = {
      val ry = dims.y/2.0f + dists.y
      val rz = (dims.z/2.0f + dists.z)*math.cos(phi)*(-1)
      val rx = (dims.x/2.0f + dists.x)*math.sin(phi)
      val camX = centroid.x + (rx*math.cos(theta)).toFloat
      val camY = centroid.y + (ry*math.sin(theta)).toFloat
      val camZ = centroid.z + (rz*math.cos(theta)).toFloat
      CameraState(name, new Vector3f(camX, camY, camZ), worldUp, target = centroid )
    }

    // Position the camera.
    val dists = _getCameraDistances(camera, bb, dims, maxDim*distanceScale, camPosType)
    val start = startAngle.getOrElse(0.0f)
    val end = endAngle.getOrElse((math.Pi*2).toFloat + start)
    val phiDelta = (end - start)/nCameras
    for (i <- 0 until nCameras) yield {
      positionToView("view" + i, dists, camAngleFromHorizontal.getOrElse(0), start+i*phiDelta)
    }
  }

  def positionCamera(camera: Camera, cameraInfo: CameraInfo) {
    logger.debug("positionCamera: " + cameraInfo)
    camera.setLocation(cameraInfo.position)
    if (cameraInfo.target != null) {
      camera.lookAt(cameraInfo.target, cameraInfo.up)
    } else {
      camera.lookAtDirection(cameraInfo.direction, cameraInfo.up)
    }
    // TODO: Set camera far and near (currently reuse from existing camera)
    //jme.setCameraFrustum(camera, 45f)
 }

  def positionCamera(camera: Camera, viewMatrix: Matrix4f) {
    logger.debug("positionCamera: " + viewMatrix)
    val viewMatrixInv = viewMatrix.invert()
    val cameraLocation = viewMatrixInv.toTranslationVector
    val up = new Vector3f(viewMatrixInv.get(0,1), viewMatrixInv.get(1,1), viewMatrixInv.get(2,1))
    val dir = new Vector3f(-viewMatrixInv.get(0,2), -viewMatrixInv.get(1,2), -viewMatrixInv.get(2,2))
    camera.lookAtDirection(dir, up)
    camera.setLocation(cameraLocation)
    // TODO: Set camera far and near (currently reuse from existing camera)
    //jme.setCameraFrustum(camera, 45f)
  }

  def setCameraFrustum(camera: Camera, fov: Float) {
    camera.setFrustumPerspective(fov, camera.getWidth().toFloat / camera.getHeight(), camera.getFrustumNear, camera.getFrustumFar)
  }

  def setCameraFrustum(camera: Camera, fov: Float, near: Float, far: Float) {
    camera.setFrustumPerspective(fov, camera.getWidth().toFloat / camera.getHeight(), near, far)
  }

}

case class CameraPositionOptions (
  cameraPositioningStrategy: CameraPositioningStrategy.Value = CameraPositioningStrategy.DEFAULT,
  cameraHeight: Option[Float] = None,       // Fix camera height
  cameraHeightToObjectHeightRatio: Option[Float] = None,  // Use this as a ratio of where we want to look
  cameraAngleFromHorizontal: Option[Float] = None,  // angle from horizontal
  startRotation: Option[Float] = None, // What rotation to start at
  axisOfRotation: Option[Vector3f] = None, // What axis to rotate around
  distanceFromObjectRatio: Option[Float] = None, // How far to be
  keepTargetsVisible: Boolean = false, // Should we try to move forward so we can keep the targets visible?
  childrenOnly: Boolean = false,
  sceneWithoutRoot: Boolean = false, // Don't care about root node if optimizing for whole scene
  distPenalty: Double = 0.5  // Penalty for being not at the right distance
)

object CameraPositioningStrategy extends Enumeration {
  type CameraPositioningStrategy = Value
  val POSITION_BY_DISTANCE, POSITION_TO_FIT, POSITION_BY_DISTANCE_TO_CENTROID = Value
  val DEFAULT = POSITION_BY_DISTANCE
  def apply(name: String): CameraPositioningStrategy.Value = {
    if (name == "distance") CameraPositioningStrategy.POSITION_BY_DISTANCE
    else if (name == "fit") CameraPositioningStrategy.POSITION_TO_FIT
    else if (name == "distance_to_centroid") CameraPositioningStrategy.POSITION_BY_DISTANCE_TO_CENTROID
    else CameraPositioningStrategy.withName(name)
  }
  def names() = Seq("distance", "fit", "distance_to_centroid")
}

// Generate camera positions for consideration
trait CameraPositionGenerator {
  def generatePositions(targets: Spatial*): Seq[CameraState]
  def nViews: Int
}

object CameraPositionGenerator {
  val defaultCameraPositionOptions = new CameraPositionOptions(
    cameraHeightToObjectHeightRatio = Option(Constants.phi.toFloat),
    startRotation = Option(0)
  )

  def canonicalViews(distScale: Float) = new CanonicalViewsCameraPositionGenerator(distScale, 0, 0)
  def canonicalViewsToFit(cam: Camera) = new CanonicalViewsCameraPositionGenerator(0.0f, cam.getWidth, cam.getHeight)

  def apply(viewer: Viewer,
            cameraPositionOptions: CameraPositionOptions = defaultCameraPositionOptions,
            nViews: Int = 1,
            useDebugPosition: Boolean = false) = {
    val camOptimizer = new CameraPositionOptimizer(viewer, cameraPositionOptions)
    new BasicCameraPositionGenerator(camOptimizer, nViews, useDebugPosition)
  }

  def apply(generators: CameraPositionGenerator*) = {
    new CombinedCameraPositionGenerator(generators:_*)
  }

}

protected class CanonicalViewsCameraPositionGenerator(val distScale: Float, val width: Int, val height: Int) extends CameraPositionGenerator {
  lazy val cameraPositioner = new BasicCameraPositioner()
  def generatePositions(targets: Spatial*): Seq[CameraState] = {
    val cams = if (distScale > 0.0) {
      cameraPositioner.getCanonicalViewsWithDistScale(targets:_*)(distScale)
    } else {
      cameraPositioner.getCanonicalViewsToFit(targets:_*)(width, height)
    }
    cams.map( x => CameraState(x) )
  }
  def nViews = 6
}

class BasicCameraPositionGenerator(val cameraOptimizer: CameraPositionOptimizer, val nPositions: Int, val useDebugPositions: Boolean) extends CameraPositionGenerator {
  def generatePositions(targets: Spatial*): Seq[CameraState] = {
    if (useDebugPositions) {
      cameraOptimizer.generateDebugPositions(targets:_*).map( x => CameraState(x) )
    } else {
      cameraOptimizer.generateCameraStates(nPositions, targets:_*)(cameraOptimizer.options).positions.toSeq
    }
  }
  def nViews = if (useDebugPositions) 3 else nPositions
}

class RotatingCameraPositionGenerator(val camera: Camera, val cameraOptions: CameraPositionOptions, val nPositions: Int) extends CameraPositionGenerator {
  lazy val cameraPositioner = new BasicCameraPositioner()

  def generatePositions(targets: Spatial*): Seq[CameraState] = {
    cameraPositioner.generateCameraPositions(camera, targets,
      cameraOptions.distanceFromObjectRatio.getOrElse(1.0f),
      cameraOptions.cameraAngleFromHorizontal,
      cameraOptions.cameraPositioningStrategy)(nPositions, cameraOptions.startRotation)
  }
  def nViews = nPositions
}

class CombinedCameraPositionGenerator(val generators: CameraPositionGenerator*) extends CameraPositionGenerator {
  def generatePositions(targets: Spatial*): Seq[CameraState] = {
    (for (g <- generators) yield {
      g.generatePositions(targets:_*)
    }).flatten
  }
  def nViews = generators.map( x => x.nViews ).sum
}

trait CameraOptimizationListener {
  def evaluated(result: CameraState) = {}
  def optimized(result: CameraOptimizationResult) = {}
}

class CameraOptimizationPromiseListener() extends CameraOptimizationListener {
  val promise = Promise[CameraOptimizationResult]
  override def optimized(result: CameraOptimizationResult): Unit = {
    promise.success(result)
  }
}

// Camera states to try
case class CameraStates(
                         scene: FalseColoredScene,
                         positions: Iterable[CameraState],
                         sourceCamera: Camera,
                         scorer: () => Double = () => 0.0,
                         var bestPosition: CameraState = null
                         ) {
  lazy val iterator = positions.toIterator
}
case class CameraOptimizationResult(cameraState: CameraState)