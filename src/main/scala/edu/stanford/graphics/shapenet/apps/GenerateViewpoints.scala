package edu.stanford.graphics.shapenet.apps

import au.com.bytecode.opencsv.CSVWriter
import com.jme3.math.Vector3f
import com.jme3.renderer.Camera
import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.{FullId, CameraInfo}
import edu.stanford.graphics.shapenet.jme3.Jme
import edu.stanford.graphics.shapenet.jme3.loaders.ModelLoadOptions
import edu.stanford.graphics.shapenet.jme3.stringToVector3f
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
  val id = ConfigHelper.getString("input", config.defaultModelId)(configFile)
  val summaryFilename = ConfigHelper.getString("output", Constants.WORK_DIR + "viewpoints.tsv")(configFile)
  val fovy = ConfigHelper.getDouble("fovy", 30.0)(configFile)
  val up = ConfigHelper.getStringOption("up")(configFile).map( x => stringToVector3f(x) )
  val front = ConfigHelper.getStringOption("front")(configFile).map( x => stringToVector3f(x) )
  val unit = ConfigHelper.getDoubleOption("unit")(configFile)

  // Setup camera generator
  var cam = new Camera(config.width.get, config.height.get)
  cam.setFrustumPerspective(fovy.toFloat, cam.getWidth().toFloat / cam.getHeight().toFloat, 1.0F, 1000.0F)
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
  if (this.unit.isDefined || this.up.isDefined || this.front.isDefined) {
    val fullId = FullId(id)
    val loadOpts = jme.dataManager.getModelLoadOptions(fullId, null).copy(unit = this.unit, up = this.up, front = this.front)
    jme.dataManager.registerCustomLoadOptions(id, loadOpts)
  }

  val scene = if (loadModel) jme.loadModelAsAlignedScene(id) else jme.loadAlignedScene(id)
  val cameraPositions = cameraPositionGenerator.generatePositions(scene.node)
  val scenebb = jme.getBoundingBox(scene.node)
  val bbmin = scenebb.getMin(null)
  val bbmax = scenebb.getMax(null)

  println("Opening " + summaryFilename)
  val append = false
  val hasData = IOUtils.isReadableFileWithData(summaryFilename)
  val summaryFile = new CSVWriter(IOUtils.filePrintWriter(summaryFilename, append), '\t', CSVWriter.NO_QUOTE_CHARACTER)
  if (!append || !hasData) {
    // Only output header if empty
    summaryFile.writeNext(Array("scene","image","bbmin","bbmax","camera.position","camera.up","camera.target","camera.direction"))
  }

  def toString[T >: Null](o:T) = if (o == null) "" else o.toString()
  for ((p,i) <- cameraPositions.zipWithIndex) {
    val camInfo = CameraInfo("cam", p.position, p.up, p.direction, p.target)
    val sceneCam = jme.transformCameraInfoFromWorldToScene(camInfo, scene.scene)
    val row = Array(scene.scene.sceneId, i, bbmin, bbmax,
      sceneCam.position, sceneCam.up, sceneCam.target, sceneCam.direction)
    summaryFile.writeNext(row.map( x => if (x.isInstanceOf[Vector3f]) {
       val v = x.asInstanceOf[Vector3f]
       v.x + "," + v.y + "," + v.z
    }  else {
       toString(x)
    }))
  }
  summaryFile.close()
}
