package edu.stanford.graphics.shapenet.jme3.viewer

import com.jme3.math.ColorRGBA
import com.typesafe.config.{ConfigFactory, Config}
import scala.collection.JavaConversions._
import edu.stanford.graphics.shapenet.util.ConfigManager
import edu.stanford.graphics.shapenet.jme3.loaders.LoadFormat

/**
 * Configuration for the viewer
 *
 * @author Angel Chang
 */
class ViewerConfig(config: Config) extends ConfigManager(config) {
  val defaultModelId = getString("viewer.defaultModelId", "3dw.111cb08c8121b8411749672386e0b711")
  val shapeNetCoreDir = getString("viewer.shapeNetCoreDir")
  val width = Option(getIntOption("viewer.width").getOrElse(1024))
  val height = Option(getIntOption("viewer.height").getOrElse(768))
  val showSettings = getBoolean("viewer.showSettings", false)
  val cacheWebFiles = getBoolean("viewer.cacheWebFiles", true)
  val neutralColor = getColor("viewer.neutralColor", new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f))
  val lightColor = getColor("viewer.lightColor", ColorRGBA.White)
  var highlightMode = getStringOption("viewer.highlightMode").map( x => HighlightMode.withName(x) )
    .getOrElse(HighlightMode.HighlightSelectedFalseBkOrig)

  val modelCacheSize = getIntOption("viewer.modelCacheSize")
  val offscreenMode = getBoolean("viewer.offscreen", false)
  //val commandsFile = config.getString("viewer.commands.file")
  //val commands = Seq("load random")
  val commands = getStringList("viewer.commands", Seq()).toIndexedSeq

  val userId = getString("viewer.userId", "shapenetViewer")

  // Mutable configuration
  var useShadow = getBoolean("viewer.useShadow", false)

  var defaultSceneDistanceScale = getFloat("viewer.sceneDistanceScale", 1.0f)
  var defaultModelDistanceScale = getFloat("viewer.modelDistanceScale", 2.0f)
  registerMutable[Float]("defaultModelDistanceScale", "Distance factor away from model",
    x => defaultModelDistanceScale, s => defaultModelDistanceScale = s.toFloat)

  var falseMaterialBlendOld = getFloat("viewer.falseMaterialBlendOld", 0.0f)

  var loadFormat = getStringOption("viewer.loadFormat").map( s => LoadFormat(s) )

  // Next output dir
  var useNestedOutputDir = getBoolean("viewer.useNestedOutputDir", false)
  registerMutableBoolean("useNestedOutputDir", "Whether the output directory should be nested for 3dw models",
    x => useNestedOutputDir, s => useNestedOutputDir = s)

  // Select mode
  var selectMode = getStringOption("viewer.selectMode").map( x => SelectMode.withName(x) ).getOrElse(SelectMode.Object)
  registerMutable[SelectMode.Value]("selectMode", "Select 'Object' or 'Mesh'",
    x => selectMode, s => selectMode = SelectMode.withName(s), supportedValues = Seq("Object", "Mesh"))

  // Whether to use debug positions for scene screenshots
  var useDebugPositionsForSceneImages = getBoolean("viewer.useDebugPositionsForSceneImages", false)
  registerMutableBoolean("useDebugPositionsForSceneImages", "Whether to use debug positions for scene screenshots",
    x => useDebugPositionsForSceneImages, s => useDebugPositionsForSceneImages = s)

  // Whether to optimize camera position for scene screenshots
  var optimizeCameraPositionForSceneImages = getBoolean("viewer.optimizeCameraPositionForSceneImages", false)
  registerMutableBoolean("optimizeCameraPositionForSceneImages", "Whether to optimize camera position for scene screenshots",
    x => optimizeCameraPositionForSceneImages, s => optimizeCameraPositionForSceneImages = s)

  // Number of images per model for model screenshots
  var nImagesPerModel = getInt("viewer.nImagesPerModel", 8)
  registerMutable("nImagesPerModel", "Number of images per model for model screenshots",
    x => nImagesPerModel, s => nImagesPerModel = s.toInt)

  // Number of camera positions to optimize over
  var nCameraPositionsForOptimize = getInt("viewer.nCameraPositionsForOptimize", 12)
  registerMutable("nCameraPositionsForOptimize", "Number of camera positions to optimize over",
    x => nCameraPositionsForOptimize, s => nCameraPositionsForOptimize = s.toInt)

  var cameraStartOrientation = Math.toRadians(getFloat("viewer.cameraStartOrientation", 0)).toFloat
  registerMutable("cameraStartOrientation", "Starting rotation for camera",
    x => cameraStartOrientation, s => cameraStartOrientation = Math.toRadians(s.toFloat).toFloat )

  //Option((math.Pi/6).toFloat
  var cameraAngleFromHorizontal = Math.toRadians(getFloat("viewer.cameraAngleFromHorizontal", (180.0/6.0).toFloat)).toFloat
  registerMutable("cameraAngleFromHorizontal", "Angle from horizontal for camera",
    x => cameraAngleFromHorizontal, s => cameraAngleFromHorizontal = Math.toRadians(s.toFloat).toFloat )

  var includeCanonicalViews = getBoolean("viewer.includeCanonicalViews", true)
  registerMutableBoolean("includeCanonicalViews", "Whether to include the 6 canonical views (left, right, top, bottom, front, back) for screenshots",
    x => includeCanonicalViews, s => includeCanonicalViews = s)

  var cameraPositionStrategy = getStringOption("viewer.cameraPositionStrategy").map( x => CameraPositioningStrategy(x) ).getOrElse(CameraPositioningStrategy.POSITION_TO_FIT)
  registerMutable[CameraPositioningStrategy.Value]("cameraPositionStrategy", "Select 'distance' or 'fit' or 'distance_to_centroid",
    x => cameraPositionStrategy, s => cameraPositionStrategy = CameraPositioningStrategy(s),
    supportedValues = CameraPositioningStrategy.names())

  var randomizeModels = getBoolean("viewer.randomizeModels", true)
  registerMutableBoolean("randomizeModels", "Randomize ordering of models during screenshot generation",
    x => randomizeModels, s => randomizeModels = s )

  var skipExisting = getBoolean("viewer.skipExisting", true)
  registerMutableBoolean("skipExisting", "Skip screenshot generation for model if screenshots already exists",
    x => skipExisting, s => skipExisting = s )

  var showModelLabel = getBoolean("viewer.showModelLabel", false)

  // Add floor or not
  var addFloor = getBoolean("viewer.addFloor", false)

  // Use radial or rectangular floor
  var useRadialFloor = getBoolean("viewer.useRadialFloor", false)

  // Minimum size for the floor (in virtual units - currently cm)
  var minFloorSize = getInt("viewer.minFloorSize", 100)

  // Ratio of floor size to object size
  var floorSizeRatio = getDouble("viewer.floorSizeRatio", 1.5)

  // outline objects
  var useOutline = getBoolean("viewer.useOutline", false)

  // ambient occlusion
  var useAmbientOcclusion = getBoolean("viewer.useAmbientOcclusion", true)

  var modelScreenShotDir = getStringOption("viewer.modelScreenshotDir")
  registerMutable("modelScreenshotDir", "Model screenshots base directory",
    x => modelScreenShotDir.getOrElse(""), s => modelScreenShotDir = Option(s))

  //  registerMutable("autoAlign", "Auto align scenes or not",
//    s => autoAlign = ConfigHelper.parseBoolean(s), supportedValues = Seq("on", "off") /*ConfigHelper.getSupportedBooleanStrings */
//  )
}

object ViewerConfig {
  // Stupid type-config - have to define defaults for everything....
  val defaults = ConfigFactory.parseMap(
    Map(
      "viewer.commands" -> new java.util.ArrayList[String]()
    )
  )
  def apply() = new ViewerConfig(ConfigFactory.empty().withFallback(defaults))
  def apply(config: Config) = new ViewerConfig(if (config == null) defaults else config.withFallback(defaults))
}
