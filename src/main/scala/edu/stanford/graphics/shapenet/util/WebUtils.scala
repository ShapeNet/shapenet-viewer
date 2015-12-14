package edu.stanford.graphics.shapenet.util

import java.io._
import java.net.URL

import dispatch._, Defaults._
import edu.stanford.graphics.shapenet.Constants
import org.json.simple.JSONValue
import scala.concurrent.Await
import scala.concurrent.duration._

/**
 * Web utils
 * @author Angel Chang
 */
object WebUtils {
  val defaultTimeout = Duration.Inf //60 seconds
  var useCache = true
  val cacheSettings = CacheSettings

  //  TODO: Error handling...
  //    val res = Http(req).either()
  //    res match {
  //      case Right(result) => Option(result.getResponseBody)
  //      case Left(e) => {
  //        println("Error " + e)
  //        None
  //      }
  //    }

  private def getCachedFilePath(path: String): String = {
    val url = new URL(path)
    val normalized = url.getHost + File.separator + url.getPath
    Constants.WEB_CACHE_DIR + normalized
  }

  case class PathStatus(path: String, filename: String, isLocal: Boolean, isCached: Boolean) {
    def loadFromFile: Boolean = isLocal || isCached
    def loadFromWeb: Boolean = !loadFromFile
  }
  private def getPathStatus(path: String, params: Map[String,String] = Map.empty, autoCache: Boolean = useCache): PathStatus = {
    if (IOUtils.isWebFile(path)) {
      val cacheSetting = if (useCache) cacheSettings(path) else null
      if (cacheSetting != null && params.isEmpty) {
        val cacheFilepath = getCachedFilePath(path)
        if (autoCache) {
          // Fetch and cache file
          WebCacheUtils.checkedFetchAndSave(path, cacheFilepath, cacheSetting.expiresAt)
        }
        val isCached = IOUtils.isReadableFileWithData(cacheFilepath)
        PathStatus(path, cacheFilepath, false, isCached)
      } else {
        PathStatus(path, null, false, false)
      }
    } else {
//      val fileOkay = IOUtils.isReadableFileWithData(path)
      PathStatus(path, path, true, false)
    }
  }

  def loadJson(path: String, params: Map[String,String] = Map.empty, timeout: Duration = defaultTimeout): Option[Object] = {
    val result = loadString(path, params, timeout)
    result.map(
      s => JSONValue.parse(s)
    )
  }

  def loadBytes(path: String, params: Map[String,String] = Map.empty, timeout: Duration = defaultTimeout): Option[Array[Byte]] = {
    // Check if file or cached
    val pathStatus = getPathStatus(path, params)
    if (pathStatus.loadFromFile)  {
      Some(IOUtils.getByteArrayFromFile(pathStatus.filename))
    } else {
      val bytes = url(path) <<? params
      val resultFuture = Http(bytes OK as.Bytes).option
      Await.result(resultFuture, timeout)
    }
  }

  def loadString(path: String, params: Map[String,String] = Map.empty, timeout: Duration = defaultTimeout): Option[String] = {
    // Check if file or cached
    val pathStatus = getPathStatus(path, params)
    if (pathStatus.loadFromFile)  {
//      val file = new File(pathStatus.filename)
//      io.Source.fromFile(file).mkString
      Some(IOUtils.slurpFile(pathStatus.filename))
    } else {
      val req = url(path) <<? params
      val resultFuture = Http(req OK as.String).option
      Await.result(resultFuture, timeout)
    }
  }

  def inputStream(url: URL): Option[InputStream] = {
    inputStream(url.toString)
  }

  def inputStream(path: String, params: Map[String,String] = Map.empty, timeout: Duration = defaultTimeout): Option[InputStream] = {
    inputStreamWithCachedFile(path, params, timeout).map( x => x._1 )
  }

  def inputStreamWithCachedFile(url: URL): Option[(InputStream,java.io.File)] = {
    inputStreamWithCachedFile(url.toString)
  }

  def inputStreamWithCachedFile(path: String, params: Map[String,String] = Map.empty, timeout: Duration = defaultTimeout): Option[(InputStream,java.io.File)] = {
    // Check if file or cached
    val pathStatus = getPathStatus(path, params)
    if (pathStatus.loadFromFile)  {
      val file = new File(pathStatus.filename)
      val inputStream = new BufferedInputStream(new FileInputStream(file))
      Some((inputStream, file))
    } else {
      val bytes = url(path) <<? params
      val resultFuture = Http(bytes OK as.Bytes).option
      Await.result(resultFuture, timeout).map( x => (new ByteArrayInputStream(x), null) )
    }
  }

  def cachedFile(url: URL): java.io.File = {
    cachedFile(url.toString)
  }

  def cachedFile(path: String, params: Map[String,String] = Map.empty): java.io.File = {
    // Check if file or cached
    val pathStatus = getPathStatus(path, params)
    if (pathStatus.loadFromFile)  {
      val file = new File(pathStatus.filename)
      file
    } else {
      null
    }
  }

  def submit(path: String, data: String, params: Map[String,String] = Map.empty, timeout: Duration = defaultTimeout): Option[String] = {
    val req = url(path).setMethod("POST").setBody(data) <<? params
    val resultFuture = Http(req OK as.String).option
    Await.result(resultFuture, timeout)
  }

  def submitJson(path: String, data: String, params: Map[String,String] = Map.empty, timeout: Duration = defaultTimeout): Option[String] = {
    val req = url(path).setMethod("POST").setBody(data).setHeader("Content-Type", "application/json") <<? params
    val resultFuture = Http(req OK as.String).option
    Await.result(resultFuture, timeout)
  }

  def submitJsonObject(path: String, data: Object, params: Map[String,String] = Map.empty, timeout: Duration = defaultTimeout): Option[String] = {
    // TODO: Be careful of scala json-simple with scala types
    val string = JSONValue.toJSONString(data)
    submitJson(path, string, params, timeout)
  }
}

case class AssetCacheSetting(
  // Filter to see whether the asset path matches this cache setting
  matches: String => Boolean,
  // When this asset will expire
  expiresAt: Long,
  // Is this asset cacheable (typically true, why else are you trying to use a cache)?
  isCacheable: Boolean = true)

object CacheSettings {
  // TODO: Read cache settings from file
  // Recache anything that is older than this...
  val globalCacheExpireDate = StringUtils.toMillis("20150216T160000")
  //val globalCacheExpireDate = Long.MaxValue

  val defaultCacheSetting = AssetCacheSetting(x => true, globalCacheExpireDate)
  // List of cache settings (slightly finer control)
  val assetCacheSettings = Seq[AssetCacheSetting](
  )

  def apply(path: String): AssetCacheSetting = get(path).getOrElse(defaultCacheSetting)

  def get(path: String): Option[AssetCacheSetting] = {
    for (cacheSetting <- assetCacheSettings) {
      if (cacheSetting.matches(path)) {
        return Option(cacheSetting)
      }
    }
    None
  }


}