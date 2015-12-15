package edu.stanford.graphics.shapenet.data

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.{DefaultModelInfo, AttributeTypes, ModelInfo, FullId}
import edu.stanford.graphics.shapenet.util.{SolrUtils, StringUtils}
import org.apache.solr.common.SolrDocument
import org.apache.solr.client.solrj.{SolrQuery, SolrClient}
import org.apache.solr.client.solrj.impl.HttpSolrClient
import scala.collection.JavaConversions._

/**
 * Queries Solr for scenes and models
 * @author Angel Chang
 */
class SolrQuerier(modelSources: Seq[String] = Seq(/*"archive3d",*/)) {
  lazy val modelsServer: SolrClient = new HttpSolrClient( Constants.MODELS3D_SOLR_URL )

  implicit val defaultModelFilterQueries = getModelFilterQueries()
  val defaultRandomize: Boolean = false

  private def getModelFilterQueries(sources: Seq[String] = modelSources): Seq[String] = {
    val s = Seq(
      makeFieldQueryString("+hasModel", "true"),
      makeFieldQueryString("-category", "_BAD")
    )
    if (sources.nonEmpty) {
      s :+ makeFieldQueryString("+source", sources, "OR")
    } else s
  }

  // Query for models using keywords along with match score
  def queryModelIds(keywords: IndexedSeq[String], category: String = null, attributes: IndexedSeq[(String,String)] = null,
                    limit: Int = -1,
                    modelFilterQueries: Seq[String] = defaultModelFilterQueries): Seq[(String,Double)] = {
    if (category == null && (keywords == null || keywords.isEmpty) && (attributes == null || attributes.isEmpty)) Seq()
    else {
      // TODO: This probably shouldn't be here, but somewhere in the SceneObjectSelector
      // Check if the model id is already specified
      if (attributes != null) {
        val modelIdAttr = attributes.find(x => x._1 == AttributeTypes.MODEL_ID).getOrElse(null)
        if (modelIdAttr != null) {
          return Seq((FullId(modelIdAttr._2).fullid, 1.0))
        }
      }
      val catQueryStr = if (category != null) {
        val cat0Query = makeFieldQueryString("category0", StringUtils.toLowercase(category))
        val cat1Query = makeFieldQueryString("category", StringUtils.toCamelCase(category))
        "(" + cat0Query + " OR " + cat1Query + ")"
      }
      else null
      val keywordsQueryStr = if (keywords != null && keywords.nonEmpty) {
        makeFieldQueryString("text", keywords, "AND")
      } else null
      val queryStr = if (catQueryStr != null) {
        if (keywordsQueryStr != null) {
          catQueryStr + " AND " + keywordsQueryStr
        } else {
          catQueryStr
        }
      } else {
        keywordsQueryStr
      }
      var res = queryModelIds(queryStr, limit, modelFilterQueries, defaultRandomize)
      if (res.isEmpty) {
        res = queryModelIds(catQueryStr, limit, modelFilterQueries, defaultRandomize)
        if (res.isEmpty && keywordsQueryStr != null) {
          res = queryModelIds(keywordsQueryStr, limit, modelFilterQueries, defaultRandomize)
        }
      }
      res
    }
  }

  // Query for models using solr query format
  // Returns list of model ids along with match score
  def queryModelIds(solrQueryStr: String, limit: Int): Seq[(String,Double)] = {
    queryModelIds(solrQueryStr, limit, defaultModelFilterQueries, defaultRandomize)
  }

  def queryModelIds(solrQueryStr: String, limit: Int, modelFilterQueries: Seq[String], randomize: Boolean): Seq[(String,Double)] = {
    val solrQuery = new SolrQuery( solrQueryStr )
    solrQuery.setFields("fullId","score")
    solrQuery.setFilterQueries(modelFilterQueries:_*)
    solrQuery.addSort("score", SolrQuery.ORDER.desc)
    if (randomize) {
      val seed = Math.floor((Math.random() * 1000000000) + 1)
      solrQuery.addSort("random_" + seed, SolrQuery.ORDER.desc)
    }
    solrQuery.addSort("nfaces", SolrQuery.ORDER.asc)
    if (limit > 0) solrQuery.setParam("rows", limit.toString)
    val response = modelsServer.query(solrQuery)
    val results = response.getResults()
    results.map( x => (x.get("fullId").asInstanceOf[String], x.get("score").asInstanceOf[Float].toDouble))
  }

  private def solrResultsToModelInfo(results: Seq[SolrDocument], defaults: DefaultModelInfo): Seq[ModelInfo] = {
    def toVector3D(str: String): Vector3f = {
      if (str != null && str.length > 0) {
        val f = str.split("\\s*,\\s*").map( s => s.toFloat )
        new Vector3f( f(0), f(1), f(2) )
      } else null
    }
    def toDouble(v: AnyRef, default: Double = Double.NaN): Double = {
      if (v != null) {
        v.asInstanceOf[Double]
      } else default
    }
    def toDoubleOption(v: AnyRef): Option[Double] = {
      if (v != null) {
        Some(v.asInstanceOf[Double])
      } else None
    }
    def toStringArray(list: java.util.List[_], default: Array[String] = Array()): Array[String] = {
      if (list != null) {
        list.map(y => y.toString).toArray
      } else default
    }

    results.map( x => {
      val m = new ModelInfo(
        fullId = x.get("fullId").asInstanceOf[String],
        name = x.get("name").asInstanceOf[String],
        tags = toStringArray(x.get("tags").asInstanceOf[java.util.List[_]]),
        allCategory = toStringArray(x.get("category").asInstanceOf[java.util.List[_]]),
        category0 = toStringArray(x.get("category0").asInstanceOf[java.util.List[_]]),
        datasets = toStringArray(x.get("datasets").asInstanceOf[java.util.List[_]]),
        unit0 = toDoubleOption(x.get("unit")),
        up0 = Option(toVector3D(x.get("up").asInstanceOf[java.lang.String])),
        front0 = Option(toVector3D(x.get("front").asInstanceOf[java.lang.String])),
        defaults = defaults
      )
      m.wnsynset = toStringArray(x.get("wnsynset").asInstanceOf[java.util.List[_]])
      m.wnhypersynset = toStringArray(x.get("wnhypersynsets").asInstanceOf[java.util.List[_]])
//      if (m.source == "3dw" && m.wnsynset != null) {
//        // Convert from original Shapenet wnsynset to WordNet 3.1
//        m.wnsynset = m.wnsynset.map( x => WordNet3xSenseMapping.wn30To31("n" + x) ).filter( x => x != null )
//      }
      m
    })
  }

  def getModelInfo(modelId: String, defaults: DefaultModelInfo): Option[ModelInfo] = {
    val solrQuery = new SolrQuery( makeFieldQueryString("fullId", modelId) )
    solrQuery.setFields("fullId","name","tags", "category","category0", "unit","front","up", "pcaDim", "wnsynset", "wnhypersynsets", "datasets")
    val response = modelsServer.query(solrQuery)
    val results = response.getResults()
    solrResultsToModelInfo(results, defaults).headOption
  }

  def getModelInfos(solrQueryStr: String, defaults: DefaultModelInfo): Seq[ModelInfo] = {
    val solrQuery = new SolrQuery( solrQueryStr )
    val modelFilterQueries = getModelFilterQueries()
    solrQuery.setFilterQueries(modelFilterQueries:_*)
    getModelInfos(solrQuery, defaults)
  }

  def getModelInfos(solrQuery: SolrQuery, defaults: DefaultModelInfo): Seq[ModelInfo] = {
    solrQuery.setFields("fullId","name","tags", "category","category0", "unit","front","up", "pcaDim", "wnsynset", "datasets")
    val stream = SolrUtils.query(modelsServer, solrQuery)
    solrResultsToModelInfo(stream, defaults)
  }

  def getModelCategoryCounts(): Map[String,Long] = {
    SolrUtils.getFieldCount(modelsServer, "category")
  }

  def getModelSynsetCounts(filterQueries: Seq[String]): Map[String,Long] = {
    SolrUtils.getFieldCount(modelsServer, "wnhypersynsets", "*:*", filterQueries).filter( x => x._2 > 0)
  }

  private def makeFieldQueryString(fieldName: String, value: String) = SolrUtils.makeFieldQueryString(fieldName, value)
  private def makeFieldQueryString(fieldName: String, parts: Seq[String], conj: String) = SolrUtils.makeFieldQueryString(fieldName, parts, conj)
}

