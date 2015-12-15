package edu.stanford.graphics.shapenet.data

import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.{DefaultModelInfo, CategoryTaxonomy, ModelInfo}
import edu.stanford.graphics.shapenet.util.{WebCacheUtils, IOUtils}

/**
 * Handles data management
 * @author Angel Chang
 */
class DataManager extends CombinedModelsDb {
  lazy val solrQuerier = new SolrQuerier
  //val shapeNetCoreModelsDb = registerSolrQueryAsModelsDb(solrQuerier, "datasets:ShapeNetCore")
  val shapeNetCoreModelsDb = {
    WebCacheUtils.checkedFetchAndSave(Constants.SHAPENET_CORE_MODELS3D_CSV_SOLR_URL, Constants.SHAPENET_CORE_MODELS3D_CSV_FILE, 10000)
    registerCsvAsModelsDb(Constants.SHAPENET_CORE_MODELS3D_CSV_FILE)
  }
  lazy val wssModelsDb = registerCsvAsModelsDb(Constants.WSS_MODELS3D_CSV_FILE,
    DefaultModelInfo(Constants.DEFAULT_MODEL_UNIT, Constants.WSS_MODEL_UP, Constants.WSS_MODEL_FRONT))

  /* Model dbs */
  private def getModelInfoFromSolr(modelId: String): Option[ModelInfo] = {
    solrQuerier.getModelInfo(modelId, defaultModelInfo)
  }
  override def getModelInfo(modelId: String): Option[ModelInfo] = {
    val modelInfo = super.getModelInfo(modelId)
    if (modelInfo.isDefined) modelInfo
    else getModelInfoFromSolr(modelId)
  }

  def registerShapeNetCore(dirpath: String): Unit = {
    val categoryTaxonomy = new CategoryTaxonomy()
    val dir = IOUtils.ensureDirname(dirpath)
    categoryTaxonomy.init(dir + "taxonomy.json", "json")
    val modelsDb = new ModelsDbWithCategoryCsvs(dir)
    registerModelsDb(modelsDb)
  }
}

object DataManager {
  lazy val _dataManager = new DataManager()
  def apply() = _dataManager
}
