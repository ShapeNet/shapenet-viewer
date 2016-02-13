package edu.stanford.graphics.shapenet.apps

import au.com.bytecode.opencsv.CSVWriter
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.CameraInfo
import edu.stanford.graphics.shapenet.jme3.Jme
import edu.stanford.graphics.shapenet.jme3.viewer._
import edu.stanford.graphics.shapenet.util.{IOUtils, ConfigHelper}
import edu.stanford.graphics.shapenet.util.ConversionUtils.l2Or

/**
  * Program to generate viewpoints for looking at a object
  *
  * @author Angel Chang
  */
object GenerateViewpoints extends App {
  val configFile = ConfigHelper.fromOptions(args:_*)
  val config = ViewerConfig(configFile)

  val loadModel = true
  val id = config.defaultModelId
  val summaryFilename = Constants.WORK_DIR + "viewpoints.csv"

  // Setup camera generator
  var cam = new Camera(config.width.get, config.height.get)
  cam.setFrustumPerspective(45.0F, cam.getWidth().toFloat / cam.getHeight().toFloat, 1.0F, 1000.0F)
  cam.setLocation(new Vector3f(0.0F, 0.0F, 10.0F))
  cam.lookAt(new Vector3f(0.0F, 0.0F, 0.0F), Vector3f.UNIT_Y)

  val cameraPositionOptions = new CameraPositionOptions(
    cameraPositioningStrategy = config.cameraPositionStrategy,
    cameraAngleFromHorizontal = Option(config.cameraAngleFromHorizontal),
    startRotation = Option(config.cameraStartOrientation),
    distanceFromObjectRatio = Option(config.defaultModelDistanceScale)
  )

  val cameraPositionGenerator = if (config.includeCanonicalViews) {
    // Create 6 canonical views + 8 views around at height xxx
    val camPosGen1 = CameraPositionGenerator.canonicalViewsToFit(cam)
    val camPosGen2 = new RotatingCameraPositionGenerator(cam, cameraPositionOptions, nPositions = config.nImagesPerModel)
    new CombinedCameraPositionGenerator(camPosGen1, camPosGen2)
  } else {
    new RotatingCameraPositionGenerator(cam, cameraPositionOptions, nPositions = config.nImagesPerModel)
  }

  // Load scene
  val jme = Jme()
  val scene = if (loadModel) jme.loadModelAsAlignedScene(id) else jme.loadAlignedScene(id)
  val cameraPositions = cameraPositionGenerator.generatePositions(scene.node)
  val scenebb = jme.getBoundingBox(scene.node)
  val bbmin = scenebb.getMin(null)
  val bbmax = scenebb.getMax(null)

  println("Opening " + summaryFilename)
  val append = false
  val hasData = IOUtils.isReadableFileWithData(summaryFilename)
  val summaryFile = new CSVWriter(IOUtils.filePrintWriter(summaryFilename, append))
  if (!append || !hasData) {
    // Only output header if empty
    summaryFile.writeNext(Array("scene","image","bbmin","bbmax","camera.position","camera.up","camera.target","camera.direction"))
  }

  def toString[T >: Null](o:T) = if (o == null) "" else o.toString()
  for ((p,i) <- cameraPositions.zipWithIndex) {
    val camInfo = CameraInfo("cam", p.position, p.up, p.direction, p.target)
    val sceneCam = jme.transformCameraInfoFromWorldToScene(camInfo, scene.scene)
    val row = Array(scene.scene.sceneId, scene.scene.sceneId + "_" + i, bbmin, bbmax,
      sceneCam.position, sceneCam.up, sceneCam.target, sceneCam.direction)
    summaryFile.writeNext(row.map( x => toString(x)))
  }
  summaryFile.close()
}
