package edu.stanford.graphics.shapenet

import java.io.File

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.util.IOUtils

/**
 * Global parameters/constants
 * @author Angel Chang
 */
trait Constants {
  protected def prop(namesToTry:Seq[String], defaultValue:String):String = namesToTry.foldLeft(None:Option[String]) { case (valueSoFar:Option[String], name:String) =>
    valueSoFar.orElse( Option(System.getProperty(name)) ).orElse( Option(System.getenv(name)) )
  }.getOrElse(defaultValue)
  protected def prop(name:String, defaultValue:String):String = prop(List(name), defaultValue)
  protected def ensureDir(str:String):String = if (str.endsWith(File.separator)) str else str + File.separator

  // Paths to our data...
  val WORKING_DIR = new java.io.File(".").getCanonicalPath
  val ON_WINDOWS = WORKING_DIR.charAt(1) == ':'
  val HOME_DIR = ensureDir(prop(List("HOME_DIR", "HOME"), if (ON_WINDOWS) WORKING_DIR.charAt(0) + ":" else System.getenv("HOME")))
  val ROOT_DIR = if (ON_WINDOWS) WORKING_DIR.charAt(0) + ":\\" else "/"
  val DATA_DIR = ensureDir(prop("DATA_DIR", HOME_DIR + "data" + File.separator))
  val CODE_DIR = ensureDir(prop("CODE_DIR", HOME_DIR + "code" + File.separator))
  val WORK_DIR = ensureDir(prop("WORK_DIR", HOME_DIR + "work" + File.separator))
  val TEST_DIR = ensureDir(prop("TEST_DIR", WORK_DIR + "test" + File.separator))
  val LOG_DIR = ensureDir(prop("LOG_DIR", WORK_DIR + "log" + File.separator))
  val CACHE_DIR = ensureDir(prop("CACHE_DIR", WORK_DIR + "cache" + File.separator))
  val WEB_CACHE_DIR = CACHE_DIR + "web" + File.separator

  val SHAPENET_VIEWER_DIR = ensureDir(prop("SHAPENET_VIEWER_DIR", CODE_DIR + "shapenet-viewer"))
  val SHAPENET_HOST = prop("SHAPENET_HOST", "https://www.shapenet.org")
  val ASSETS_DIR = ensureDir(prop("ASSETS_DIR", SHAPENET_VIEWER_DIR + "/assets"))
  val MISC_DATA_HOST = prop("DATA_HOST", "http://dovahkiin.stanford.edu")
  val TEXT2SCENE_DIR = if (USE_LOCAL_DATA) DATA_DIR + "text2scene" + separator else MISC_DATA_HOST + "/text2scene/"

  //  val JUNIT_REF_DIR = SHAPENET_VIEWER_DIR + "junit" + File.separator + "ref" + File.separator
  //  val JUNIT_INPUT_DIR = SHAPENET_VIEWER_DIR + "junit" + File.separator + "input" + File.separator
  //  val JUNIT_OUT_DIR = TEST_DIR + "junit" + File.separator

  // Try to use local data directory (use if models are already copied locally) ...
  val USE_LOCAL_DATA = prop("USE_LOCAL_DATA", "false").toBoolean
  val separator = if (USE_LOCAL_DATA) File.separator else "/"

  val SHAPENET_SYNSETS_SOLR_URL = SHAPENET_HOST + "/shapenet-synsets/solr"
  val WORK_SCREENSHOTS_DIR = WORK_DIR + "screenshots" + File.separator

  // Some arbitrary constants for screen width and height
  var screenWidth: Int = 600
  var screenHeight: Int = 400

  val DEFAULT_ATTRIBUTE_TYPES = Seq("colors", "material", "category")
  val DEFAULT_MODEL_UNIT = 0.0254
  val DEFAULT_SCENE_UNIT = 1.0 * DEFAULT_MODEL_UNIT

  val useSemanticCoordFront: Boolean = true
  val WSS_SCENE_SCALE = 0.0254
  val WSS_SCENE_UP = Vector3f.UNIT_Z
  val WSS_SCENE_FRONT = Vector3f.UNIT_Y.negate()
  val WSS_MODEL_UP = Vector3f.UNIT_Z
  val WSS_MODEL_FRONT = Vector3f.UNIT_Y.negate()
  val DEFAULT_SCENE_UP = Vector3f.UNIT_Z
  val DEFAULT_SCENE_FRONT = Vector3f.UNIT_Y.negate()
  val DEFAULT_MODEL_UP = Vector3f.UNIT_Y
  val DEFAULT_MODEL_FRONT = Vector3f.UNIT_X
  // Our semantic coordinate frame
  val SEMANTIC_LEFT = Vector3f.UNIT_X.negate()
  val SEMANTIC_UP = Vector3f.UNIT_Y
  val SEMANTIC_FRONT = Vector3f.UNIT_Z.negate()

  // Categories files
  val CATEGORIES_DATA_DIR = SHAPENET_VIEWER_DIR + "categories" + File.separator
  val CATEGORY_MATERIALS_FILE = CATEGORIES_DATA_DIR + "materials.csv"
  val CATEGORY_ISCONTAINER_FILE = CATEGORIES_DATA_DIR + "isContainer.csv"
  //val SUBCATEGORIES_FILE = CATEGORIES_DATA_DIR + "subcategories.txt"
  val CATEGORIES_FILE = CATEGORIES_DATA_DIR + "categories.csv"
  val MATERIAL_DENSITIES_FILE = CATEGORIES_DATA_DIR + "densities.csv"

  val MODELS_DIR = if (USE_LOCAL_DATA) DATA_DIR + "models" + separator else MISC_DATA_HOST + "/models/"
  val MODELS_DATA_DIR = if (USE_LOCAL_DATA) DATA_DIR + "models/data-tables/" else TEXT2SCENE_DIR  + "models/data-tables/"
  // Computed stats files
  val COMPUTED_MODEL_STATS_FILE = MODELS_DATA_DIR + "computedModelStats.csv"

  val DEBUG_DIR = WORK_DIR + "debug" + File.separator

  val ARCHIVE3D_DATA_DIR = SHAPENET_HOST + "/data/archive3d/"
  val WSS_DATA_DIR = SHAPENET_HOST + "/data/wss/"
  val VF_DATA_DIR = SHAPENET_HOST + "/data/vf/"
  val SHAPENET_DATA_DIR = SHAPENET_HOST + "/shapenet/data/"
  val YOBI3D_DATA_DIR = SHAPENET_HOST + "/shapenet/data/"
  val WSS_OBJ_DIR = if (USE_LOCAL_DATA) "/models/repositories/g3dw/models/" else MISC_DATA_HOST + "/g3dw/models/"
  val WSS_OBJGZ_DIR = if (USE_LOCAL_DATA) "/models/repositories/g3dw/models/" else TEXT2SCENE_DIR  + "/models/repositories/g3dw/models/"
  val WSS_TEXTURE_DIR = if (USE_LOCAL_DATA) "/models/repositories/g3dw/textures/" else MISC_DATA_HOST + "/g3dw/textures/"

  // Models and scenes solr configuration
  val MODELS3D_SOLR_URL = SHAPENET_HOST + "/solr/models3d"
  val SCENES_SOLR_URL = SHAPENET_HOST + "/solr/scenes"

  // Constants for scenes
  val SCENES_DIR = DATA_DIR + "scenes" + File.separator
  val SCENES_DATA_DIR = if (USE_LOCAL_DATA) SCENES_DIR + "data-tables/" else TEXT2SCENE_DIR  + "scenes/data-tables/"

  val phi = (1 + math.sqrt(5))/2
}

object Constants extends Constants

object SizeBy extends Enumeration {
  val width, depth, height, max, volumeCubeRoot, diagonal2d, diagonal = Value

  def sizeWithZUp(sizeBy: SizeBy.Value, dims: Vector3f): Double = {
    sizeBy match {
      case SizeBy.width => dims.getX
      case SizeBy.depth => dims.getY
      case SizeBy.height => dims.getZ  // Z up
      case SizeBy.max => Array(dims.getX, dims.getY, dims.getZ).max
      case SizeBy.diagonal => dims.length()
      case SizeBy.diagonal2d => math.sqrt(dims.getX*dims.getX + dims.getY + dims.getY)
      case SizeBy.volumeCubeRoot => math.pow(dims.getX * dims.getY * dims.getZ, 1.0/3.0)
      case _ => throw new UnsupportedOperationException("Unknown size by: " + sizeBy)
    }
  }

  def sizeWithYUp(sizeBy: SizeBy.Value, dims: Vector3f): Double = {
    sizeBy match {
      case SizeBy.width => dims.getX
      case SizeBy.depth => dims.getZ
      case SizeBy.height => dims.getY  // Y up
      case SizeBy.max => Array(dims.getX, dims.getY, dims.getZ).max
      case SizeBy.diagonal => dims.length()
      case SizeBy.diagonal2d => math.sqrt(dims.getX*dims.getX + dims.getZ + dims.getZ)
      case SizeBy.volumeCubeRoot => math.pow(dims.getX * dims.getY * dims.getZ, 1.0/3.0)
      case _ => throw new UnsupportedOperationException("Unknown size by: " + sizeBy)
    }
  }
}
