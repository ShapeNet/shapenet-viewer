package edu.stanford.graphics.shapenet.data

import edu.stanford.graphics.shapenet.common.ModelInfo

/**
 * Handles data management
 * @author Angel Chang
 */
class DataManager {
  lazy val solrQuerier = new SolrQuerier
  lazy val modelsDb = {
    val db = new ModelsDb()
    db.init()
    db
  }

  private def getModelInfoFromSolr(modelId: String): Option[ModelInfo] = {
    solrQuerier.getModelInfo(modelId)
  }

  def getModelInfo(modelId: String): Option[ModelInfo] = {
    val modelInfo = modelsDb.models.get(modelId)
    if (modelInfo.isDefined) modelInfo
    else getModelInfoFromSolr(modelId)
  }
}

object DataManager {
  lazy val _dataManager = new DataManager()
  def apply() = _dataManager
}
