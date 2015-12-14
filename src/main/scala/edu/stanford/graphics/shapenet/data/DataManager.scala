package edu.stanford.graphics.shapenet.data

import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.{FullId, ModelInfo}

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

}

object DataManager {
  lazy val _dataManager = new DataManager()
  def apply() = _dataManager
}
