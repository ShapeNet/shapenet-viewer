package edu.stanford.graphics.shapenet.jme3.loaders

import edu.stanford.graphics.shapenet.common.{MaterialInfo, Model}
import edu.stanford.graphics.shapenet.util.IOUtils
import org.json.simple.JSONObject
import scala.collection.JavaConversions._

/**
 * Loads model from UTF8 json format
 * @author Angel Chang
 */
class UTF8Loader(val assetLoader: AssetLoader)  {
  val assetCreator = assetLoader.assetCreator
  val decoder = new UTF8Decoder()

  case class ModelLoadInfo(
    decodeParams: UTF8Decoder.DecodeParams,
    materials: Map[String,Map[String,String]] = Map(),
    urls: Map[String,Array[UTF8Decoder.MeshParams]] = Map()
  )
  case class LazyMaterial (
    baseDir: String,
    params: Map[String,Object]
  )(implicit options: ModelLoadOptions) {
    lazy val material = assetLoader.loadMaterial(convertMaterialParams(params, baseDir))
  }

  def convertMaterialParams(params: Map[String,Object],
                            baseDir: String)(implicit options: ModelLoadOptions): MaterialInfo = {
    def getPath(s: String) =
      if (s.startsWith("http:")) s else options.materialsPath.getOrElse(baseDir) + "/" + s

    def getColor(obj: Object) = {
      var v = Conversions.javaListAsDoubleArray(obj)
      if (v.length == 3) {
        if (options != null && options.normalizeRGB) {
          v = v.map( x => x/255.0 )
        }
        if (options != null && options.ignoreZeroRGBs && v.forall( x => x == 0.0)) null
        else v
      } else {
        null
      }
    }

    var res = new MaterialInfo()
    for ( (prop,value) <- params ) {
      prop.toLowerCase match {
        // Diffuse color (color under white light) using RGB values
        case "kd" => res = res.copy(diffuse = getColor(value))
        // Ambient color (color under shadow) using RGB values
        case "ka" => res = res.copy(ambient = getColor(value))
        // Specular color (color when light is reflected from shiny surface) using RGB values
        case "ks" => res = res.copy(specular = getColor(value))
        // Diffuse texture map
        case "map_kd" => res = res.copy(diffuseMap = getPath(value.asInstanceOf[String]))
        // The specular exponent (defines the focus of the specular highlight)
        // A high exponent results in a tight, concentrated highlight. Ns values normally range from 0 to 1000.
        case "ns" => res = res.copy(shininess = value.asInstanceOf[Double])

        // According to MTL format (http://paulbourke.net/dataformats/mtl/):
        //   d is dissolve for current material
        //   factor of 1.0 is fully opaque, a factor of 0 is fully dissolved (completely transparent)
        case "d" => {
          var v = value.asInstanceOf[Number].doubleValue()
          if (options.invertTransparency) v = 1 - v
          if (v < 1) {
            res = res.copy(transparent = true)
            res = res.copy(opacity = v)
          }
        }
        case _ => {}
      }
    }
    if (options != null && options.defaultColor != null) {
      val lowercaseKeys = params.keySet.map( x => x.toLowerCase )
      val hasKd = lowercaseKeys.contains("kd")
      val hasColorOrMaterial = res.diffuse != null || res.diffuseMap != null
      if (!hasColorOrMaterial && hasKd) {
        res = res.copy(diffuse = options.defaultColor)
      }
    }
    if (options != null) {
      res = res.copy(doubleSided = options.doubleSided)
    }
    res
  }

  def loadModel(fullId: String, path: String, options: ModelLoadOptions = new ModelLoadOptions()): Model[_] =
  {
    System.out.println("load json " + path)
    val base = IOUtils.getParentDir(path)
    val modelName = fullId
    val json = assetLoader.loadJson(path)
    if (json != null) {
      val obj = json.asInstanceOf[JSONObject]
      val decodeParamsJson = obj.get("decodeParams").asInstanceOf[JSONObject]
      val decodeOffsets = Conversions.javaListAsIntArray(decodeParamsJson.get("decodeOffsets"))
      val decodeScales = Conversions.javaListAsFloatArray(decodeParamsJson.get("decodeScales"))
      val urls = obj.get("urls").asInstanceOf[java.util.Map[String,Object]].mapValues( x =>
        x.asInstanceOf[java.util.List[Object]].map( m => {
          val p = m.asInstanceOf[JSONObject]
          new UTF8Decoder.MeshParams(
            p.get("material").toString,
            p.get("attribRange").asInstanceOf[java.util.List[Object]].map( x => x.asInstanceOf[Long].toInt).toArray,
            p.get("indexRange").asInstanceOf[java.util.List[Object]].map( x => x.asInstanceOf[Long].toInt).toArray )
        } ).toArray
      ).toMap
      val materialInfos: Map[String,LazyMaterial] = obj.get("materials").asInstanceOf[java.util.Map[String,Object]]
        .mapValues( x => {
         new LazyMaterial(
            base,
            x.asInstanceOf[java.util.Map[String,Object]].toMap )(options)
        }).toMap
      val decodeParams = new UTF8Decoder.DecodeParams(decodeOffsets.toArray, decodeScales.toArray)
      val model = assetCreator.createModel(modelName)
      var meshNum = 0
      // FIXME: The urls are from a map so they may not be ordered
      // For consistent ordering, we should order by something....
      // Okay for now since there is only one url per model....
      for ((url, urlmmis) <- urls) {
        // Fetch geometries
        val geomBytes = assetLoader.loadBytes(url, options.geometryPath.getOrElse(base) )
        if (geomBytes != null) {
          val geomStr = new String(geomBytes,"UTF-8")
          for (idx <- 0 until urlmmis.length) {
            val name = url + "." + idx  // TODO: Get unique name
            val urlmmi = urlmmis(idx)
            val decodedMesh = decoder.decode(geomStr, urlmmi, decodeParams, name, idx)
            val material = if (options.loadMaterials) {
              val mi = materialInfos.get(urlmmi.material)
              mi.map( x => x.material ).getOrElse(null)
            } else {
              null
            }
            val mesh = assetCreator.createMesh(Map(
              "geometry" -> decodedMesh,
              "material" -> material,
              "index" -> meshNum
            ))
            meshNum += 1
            assetCreator.attachMesh(model.node, mesh)
          }
        }
      }
      assetCreator.finalizeModel(model, options)
      model
    } else {
      null
    }
  }

}

object Conversions {
  def javaListAsLongArray(a: Any) =
    a.asInstanceOf[java.util.List[Object]].map( x => x.asInstanceOf[Long] ).toArray
  def javaListAsIntArray(a: Any) =
    javaListAsLongArray(a).map( x => x.toInt )
  def javaListAsDoubleArray(a: Any) =
    a.asInstanceOf[java.util.List[Object]].map( x => x.asInstanceOf[Number].doubleValue() ).toArray
  def javaListAsFloatArray(a: Any) =
    javaListAsDoubleArray(a).map( x => x.toFloat )
}

