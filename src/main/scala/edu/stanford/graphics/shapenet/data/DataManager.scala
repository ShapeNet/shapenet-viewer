package edu.stanford.graphics.shapenet.data

import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.{CategoryTaxonomy, ModelInfo}
import edu.stanford.graphics.shapenet.util.IOUtils

/**
 * Handles data management
 * @author Angel Chang
 */
class DataManager extends CombinedModelsDb {
  lazy val solrQuerier = new SolrQuerier
  lazy val shapeNetCoreModelsDb = registerSolrQueryAsModelsDb(solrQuerier, "datasets:ShapeNetCore")
  /*lazy*/ val wssModelsDb = registerCsvAsModelsDb(Constants.MODELS3D_CSV_FILE)

  /* Model dbs */
  private def getModelInfoFromSolr(modelId: String): Option[ModelInfo] = {
    solrQuerier.getModelInfo(modelId)
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
