package edu.stanford.graphics.shapenet.jme3.loaders

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.DefaultModelInfo

/**
  * Load options
  *
  * @author Angel Chang
  */
case class ModelLoadOptions(
                             format: String = null,
                             path: Option[String] = None,
                             unit: Option[Double] = None,
                             up: Option[Vector3f] = None,
                             front: Option[Vector3f] = None,
                             geometryPath: Option[String] = None,
                             materialsPath: Option[String] = None,
                             loadMaterials: Boolean = true,
                             doubleSided: Boolean = false,
                             normalizeRGB: Boolean = false,
                             ignoreZeroRGBs: Boolean = false,
                             invertTransparency: Boolean = false,
                             defaultColor: Array[Double] = null,
                             compressionExt: String = null,
                             modelIdToPath: String => String = null
                       )

case class ModelAssetGroup(name: String,
                           source: String,
                           defaultUnit: Double = Constants.DEFAULT_MODEL_UNIT,
                           defaultUp: Vector3f = Constants.DEFAULT_MODEL_UP,
                           defaultFront: Vector3f = Constants.DEFAULT_MODEL_FRONT,
                           loadOptions: Seq[ModelLoadOptions] = Seq()) {
  lazy val loadOptionsByFormat: Map[String, ModelLoadOptions] =
    loadOptions.map( x => x.format -> x ).toMap
}

object AssetGroups {
  private val _useObjGz: Boolean = false
  private val _wssObjDir: String = if (_useObjGz) Constants.WSS_OBJGZ_DIR else Constants.WSS_OBJ_DIR
  // Predefined model load options
  val defaultModelAssetGroups: Seq[ModelAssetGroup] = Seq(
    ModelAssetGroup("3dw", "3dw", defaultUp = Vector3f.UNIT_Z, defaultFront = Vector3f.UNIT_Y.negate(),
      loadOptions = Seq(
        ModelLoadOptions(
          format = "kmz",
          //doubleSided = true,
          ignoreZeroRGBs = true,
          defaultColor = Array(0.0, 0.0, 0.0),
          modelIdToPath = (id: String) => {
            val (id1,id2) = id.splitAt(6)
            val prefix = id1.mkString("/") + id2
            Seq(Constants.SHAPENET_DATA_DIR, prefix, id, "Collada", id + ".kmz").mkString("/")
            //        Seq(Constants.SHAPENET_DATA_DIR, prefix, id, "OBJ", id + ".obj").mkString("/")
        }
      ))
    ),
    ModelAssetGroup("wss", "wss", defaultUp = Vector3f.UNIT_Z, defaultFront = Vector3f.UNIT_Y.negate(),
      loadOptions = Seq(
        ModelLoadOptions(
          format = "obj",
          normalizeRGB = false,
          ignoreZeroRGBs = true,
          invertTransparency = true,
          defaultColor = Array(0.0, 0.0, 0.0),
          // Because the mtl and obj are not stored in the same directory,
          // we need to have the asset locator look hard for where the mtl/obj files are
          // (this incurs overhead of 404 errors with default jme asset loader)
          // work around by hacking around and specifying the materialsPath...
          geometryPath = Option(_wssObjDir),
          materialsPath = Option(Constants.WSS_TEXTURE_DIR),
          compressionExt = if (_useObjGz) "gz" else null,
          modelIdToPath = (id: String) => _wssObjDir + "/" + id + ".obj"
        ),
        ModelLoadOptions(
          format = "utf8",
          geometryPath = Option(Seq(Constants.WSS_DATA_DIR, "geometry").mkString("/")),
          materialsPath = Option(Seq(Constants.WSS_DATA_DIR, "texture").mkString("/")),
          normalizeRGB = false,
          ignoreZeroRGBs = true,
          invertTransparency = true,
          defaultColor = Array(0.0, 0.0, 0.0),
          modelIdToPath = (id: String) => Seq(Constants.WSS_DATA_DIR, "model",id + ".json").mkString("/")
        ))
    ),
    ModelAssetGroup("archive3d", "archive3d", defaultUp = Vector3f.UNIT_Z, defaultFront = Vector3f.UNIT_Y.negate(),
      loadOptions = Seq(
        ModelLoadOptions(
          format = "obj",
          doubleSided = true,
          modelIdToPath = (id: String) => Seq(Constants.ARCHIVE3D_DATA_DIR, "models",id, id + ".obj").mkString("/")
        ),
        new ModelLoadOptions(
          format = "utf8",
          doubleSided = true,
          ignoreZeroRGBs = true,
          defaultColor = Array(0.0, 0.0, 0.0),
          modelIdToPath = (id: String) => Seq(Constants.ARCHIVE3D_DATA_DIR, "models",id, id + ".json").mkString("/")
        ))
    ),
    ModelAssetGroup("vf", "vf", defaultUp = Vector3f.UNIT_Z, defaultFront = Vector3f.UNIT_Y.negate(),
      loadOptions = Seq(
        new ModelLoadOptions(
          format = "ply",
          doubleSided = true,
          modelIdToPath = (id: String) => Seq(Constants.VF_DATA_DIR, id + "d.ply").mkString("/")
        ),
        new ModelLoadOptions(
          format = "utf8",
          doubleSided = true,
          defaultColor = Array(0.0, 0.0, 0.0),
          modelIdToPath = (id: String) => Seq(Constants.VF_DATA_DIR, id + ".json").mkString("/")
        ))
    ),
    ModelAssetGroup("yobi3d", "yobi3d", defaultUp = Vector3f.UNIT_Y, defaultFront = Vector3f.UNIT_X,
      loadOptions = Seq(
        ModelLoadOptions(
          format = "obj",
          doubleSided = true,
          modelIdToPath = (id: String) => Seq("data", "models", "bikes", "02834778", id, "model.obj").mkString("/")
        ))
    ),
    ModelAssetGroup("raw", "raw", loadOptions = Seq(
      new ModelLoadOptions(format = "all", modelIdToPath = (id: String) => id))
    )
  )

  lazy val defaultModelAssetGroupsMap: Map[String, ModelAssetGroup] =
    defaultModelAssetGroups.map( x => x.name -> x ).toMap

  def getModelLoadOptions(source: String, defaultFormat: String = null): ModelLoadOptions = {
    defaultModelAssetGroupsMap.get(source).map( x =>
      x.loadOptionsByFormat.getOrElse(defaultFormat, x.loadOptions.head)).getOrElse(null)
  }

  def getDefaultModelInfo(source: String, defaultFormat: String = null): DefaultModelInfo = {
    val modelLoadOptions = getModelLoadOptions(source, defaultFormat)
    if (modelLoadOptions != null) {
      val assetGroupDefaults = defaultModelAssetGroupsMap.get(source)
      DefaultModelInfo(
        unit = modelLoadOptions.unit.getOrElse(
          assetGroupDefaults.map(x => x.defaultUnit).getOrElse(Constants.DEFAULT_MODEL_UNIT)),
        up = modelLoadOptions.up.getOrElse(
          assetGroupDefaults.map(x => x.defaultUp).getOrElse(Constants.DEFAULT_MODEL_UP)),
        front = modelLoadOptions.front.getOrElse(
          assetGroupDefaults.map(x => x.defaultFront).getOrElse(Constants.DEFAULT_MODEL_FRONT))
      )
    } else {
      DefaultModelInfo()
    }
  }

}

object LoadFormat extends Enumeration {
  val OBJ_FORMAT, UTF8_FORMAT, KMZ_FORMAT, DAE_FORMAT = Value
  def shortName(format: LoadFormat.Value): String = {
    format match {
      case OBJ_FORMAT => "obj"
      case UTF8_FORMAT => "utf8"
      case KMZ_FORMAT => "kmz"
      case DAE_FORMAT => "dae"
    }
  }
  def apply(f: String): LoadFormat.Value = {
    f match {
      case "obj" => OBJ_FORMAT
      case "utf8" => UTF8_FORMAT
      case "kmz" => KMZ_FORMAT
      case "dae" => DAE_FORMAT
      case _ => withName(f)
    }
  }
}

object LoadStatus extends Enumeration {
  type LoadStatus = Value
  val LOADING, DONE = Value
}

case class LoadProgress[T >: Null](name: String,
                                   stage: String,   // Load stage
                                   total: Int = 1,  // Total to load
                                   trackParts: Boolean = false) {
  var status: LoadStatus.Value = LoadStatus.LOADING
  var loaded: Int = 0 // Loaded
  var result: T = null
  val partsProgress: Array[LoadProgress[_]] = // Load progress for each part
    if (trackParts) Array.ofDim[LoadProgress[_]](total) else null
  private var partsPercents: Array[Float] = null // How much each part contributes to the total
  def this(name: String, stage: String, result: T) {
    this(name, stage)
    this.loaded = total
    this.result = result
    this.status = LoadStatus.DONE
  }
  def setPartPercents(weights: Array[Float]) = {
    if (weights.length != total) throw new IllegalArgumentException("Invalid number of weights")
    if (!weights.forall( x => x > 0)) throw new IllegalArgumentException("Weights must be positive")
    val sum = weights.sum
    partsPercents = weights.map( x => x/sum)
  }
  def percentDone: Float = {
    if (this.status == LoadStatus.DONE) 1.0f
    else if (partsPercents == null) {
      if (partsProgress == null) {
        this.loaded.toFloat / this.total
      } else {
        this.partsProgress.map( x => if (x != null) x.percentDone else 0 ).sum / this.total
      }
    } else {
      if (partsProgress == null) {
        partsPercents.take(this.loaded).sum
      } else {
        this.partsProgress.map( x => if (x != null) x.percentDone else 0.0f )
          .zip(partsPercents).map( p => p._1 * p._2 ).sum
      }
    }
  }
  def done(result: T) {
    this.synchronized {
      this.result = result
      this.status = LoadStatus.DONE
    }
  }
  def incLoaded() {
    this.synchronized {
      this.loaded = this.loaded + 1
    }
  }
}

trait LoadProgressListener[T >: Null] {
  def onProgress(progress: LoadProgress[T])
  def onDone(result: T)
}

class PartLoadProgressListener[T >: Null,C >: Null](val parent: LoadProgress[T],
                                                    val parentListener: LoadProgressListener[T],
                                                    val childIndex: Int) extends LoadProgressListener[C] {
  def onProgress(progress: LoadProgress[C]) {
    if (parent.partsProgress != null) parent.partsProgress(childIndex) = progress
    parentListener.onProgress(parent)
  }
  def onDone(result: C) {
    parent.incLoaded()
    parentListener.onProgress(parent)
  }
}
