package edu.stanford.graphics.shapenet.jme3.viewer

import com.typesafe.config.{ConfigFactory, Config}
import scala.collection.JavaConversions._
import edu.stanford.graphics.shapenet.util.{ConfigManager, MutableConfigHelper, ConfigHelper}
import edu.stanford.graphics.shapenet.jme3.loaders.AssetLoader.LoadFormat

/**
 * Configuration for the viewer
 * @author Angel Chang
 */
class ViewerConfig(config: Config) extends ConfigManager(config) {
  val shapeNetCoreDir = getString("viewer.shapeNetCoreDir")
  val width = Option(getIntOption("viewer.width").getOrElse(1024))
  val height = Option(getIntOption("viewer.height").getOrElse(768))
  val showSettings = getBoolean("viewer.showSettings", false)
  val cacheWebFiles = getBoolean("viewer.cacheWebFiles", true)
  val useShadow = getBoolean("viewer.useShadow", false)

  val modelCacheSize = getIntOption("viewer.modelCacheSize")
  val loadFormat = getStringOption("viewer.loadFormat").map( s => LoadFormat.withName(s) )
  val offscreenMode = config.getBoolean("viewer.offscreen")
  //val commandsFile = config.getString("viewer.commands.file")
  //val commands = Seq("load random")
  val commands = config.getStringList("viewer.commands").toIndexedSeq

  val userId = getString("viewer.userId", "babysherlock")

  // Surface extraction config
  val surfaceExtractionRandomizeModels = getBoolean("surfaceExtraction.randomizeModels")
  val surfaceExtractionSkipDone = getBoolean("surfaceExtraction.skipDone")
  val surfaceExtractionUseVertices = getBoolean("surfaceExtraction.useVertices", true)
  val surfaceExtractionRestrictPlanar = getBoolean("surfaceExtraction.restrictPlanar", false)

  // Mutable configuration
  var defaultSceneDistanceScale = getFloat("viewer.sceneDistanceScale", 1.0f)
  var defaultModelDistanceScale = getFloat("viewer.modelDistanceScale", 2.0f)
  var falseMaterialBlendOld = getFloat("viewer.falseMaterialBlendOld", 0.0f)

  // Select mode
  var selectMode = getStringOption("viewer.selectMode").map( x => SelectMode.withName(x) ).getOrElse(SelectMode.Object)
  registerMutable[SelectMode.Value]("selectMode", "Select 'Object' or 'Surface' or 'Mesh'",
    x => selectMode, s => selectMode = SelectMode.withName(s), supportedValues = Seq("Object", "Surface", "Mesh"))

  // Whether to use debug positions for scene screenshots
  var useDebugPositionsForSceneImages = getBoolean("viewer.useDebugPositionsForSceneImages", false)
  registerMutableBoolean("useDebugPositionsForSceneImages", "Whether to use debug positions for scene screenshots",
    x => useDebugPositionsForSceneImages, s => useDebugPositionsForSceneImages = s)

  // Whether to optimize camera position for scene screenshots
  var optimizeCameraPositionForSceneImages = getBoolean("viewer.optimizeCameraPositionForSceneImages", false)
  registerMutableBoolean("optimizeCameraPositionForSceneImages", "Whether to optimize camera position for scene screenshots",
    x => optimizeCameraPositionForSceneImages, s => optimizeCameraPositionForSceneImages = s)

  // Whether to generate multiple camera positions
  var generateCameraPositionsForSceneImages = getBoolean("viewer.generateCameraPositionsForSceneImages", true)
  registerMutableBoolean("generateCameraPositionsForSceneImages", "Whether to generate multiple camera positions",
    x => generateCameraPositionsForSceneImages, s => generateCameraPositionsForSceneImages = s)

  // Number of images per scene for scene screenshots
  var nImagesPerScene = getInt("viewer.nImagesPerScene", 4)
  registerMutable("nImagesPerScene", "Number of images per scene for scene screenshots",
    x => nImagesPerScene, s => nImagesPerScene = s.toInt)

  // Number of camera positions to optimize over
  var nCameraPositionsForOptimize = getInt("viewer.nCameraPositionsForOptimize", 12)
  registerMutable("nCameraPositionsForOptimize", "Number of camera positions to optimize over",
    x => nCameraPositionsForOptimize, s => nCameraPositionsForOptimize = s.toInt)

  // Number of rotations for reference object
  var nRotationsForRefObject = getInt("viewer.nRotationsForRefObject", 1)
  registerMutable("nRotationsForRefObject", "Number of rotations for the reference object (when generating scenes for object pairs)",
    x => nRotationsForRefObject, s => nRotationsForRefObject = s.toInt)
  var cameraStartOrientation = Math.toRadians(getFloat("viewer.cameraStartOrientation", 0)).toFloat
  registerMutable("cameraStartOrientation", "Starting rotation for camera",
    x => cameraStartOrientation, s => cameraStartOrientation = Math.toRadians(s.toFloat).toFloat )

  var cameraAngleFromHorizontal = Math.toRadians(getFloat("viewer.cameraAngleFromHorizontal", (180.0/8.0).toFloat)).toFloat
  registerMutable("cameraAngleFromHorizontal", "Angle from horizontal for camera",
    x => cameraAngleFromHorizontal, s => cameraAngleFromHorizontal = Math.toRadians(s.toFloat).toFloat )

  var skipRotationsForSymmetricRef = getBoolean("viewer.skipRotationsForSymmetricRef", false)

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

  // number of models to use...
  var defaultModelCount = getInt("viewer.defaultModelCount", 1)
  registerMutable("defaultModelCount", "Number of models to use for various operations",
    x => defaultModelCount, s => defaultModelCount = s.toInt)

  var sceneLayoutType = getString("sceneLayout.type")

  //  registerMutable("autoAlign", "Auto align scenes or not",
//    s => autoAlign = ConfigHelper.parseBoolean(s), supportedValues = Seq("on", "off") /*ConfigHelper.getSupportedBooleanStrings */
//  )
}

object ViewerConfig {
  // Stupid type-config - have to define defaults for everything....
  val defaults = ConfigFactory.parseMap(
    Map(
      "viewer.offscreen" -> java.lang.Boolean.FALSE,
      "viewer.commands" -> new java.util.ArrayList[String]()
    )
  )
  def apply() = new ViewerConfig(ConfigFactory.empty().withFallback(defaults))
  def apply(config: Config) = new ViewerConfig(if (config == null) defaults else config.withFallback(defaults))
}
