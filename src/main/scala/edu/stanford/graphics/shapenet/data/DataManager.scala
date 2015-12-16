package edu.stanford.graphics.shapenet.data

import java.io.File

import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common.{DefaultModelInfo, CategoryTaxonomy, ModelInfo}
import edu.stanford.graphics.shapenet.util.{WebCacheUtils, IOUtils}

/**
 * Handles data management
 * @author Angel Chang
 */
class DataManager extends CombinedModelsDb("DataManager") {
  lazy val solrQuerier = new SolrQuerier
  //val shapeNetCoreModelsDb = registerSolrQueryAsModelsDb(solrQuerier, "datasets:ShapeNetCore")
  lazy val shapeNetCoreModelsDb = {
    WebCacheUtils.checkedFetchAndSave(Constants.SHAPENET_CORE_MODELS3D_CSV_SOLR_URL, Constants.SHAPENET_CORE_MODELS3D_CSV_FILE, 10000)
    registerCsvAsModelsDb("ShapeNetCore", Constants.SHAPENET_CORE_MODELS3D_CSV_FILE)
  }
  lazy val wssModelsDb = registerCsvAsModelsDb("wss", Constants.WSS_MODELS3D_CSV_FILE,
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

  def registerShapeNetCore(dirpath: String, loadFormat: String = "obj"): Unit = {
    val categoryTaxonomy = new CategoryTaxonomy()
    val dir = IOUtils.ensureDirname(dirpath)
    categoryTaxonomy.init(dir + "taxonomy.json", "json")
    val modelsDb = new ModelsDbWithCategoryCsvs("ShapeNetCore", dir)
    modelsDb.init(categoryTaxonomy)
    modelsDb.lowercaseCategoryNames = true

    // customized load path
    modelsDb.getModelLoadOptions = (fullId: String, format: String) => {
      val modelInfo = getModelInfo(fullId)
      var opts: Option[LoadOpts] = None
      if (modelInfo.isDefined && modelInfo.get.category.nonEmpty) {
        val x = modelInfo.get
        val filename = dir + x.category.head + File.separator + x.id + File.separator + "model.obj"
        if (IOUtils.isReadableFileWithData(filename)) {
          opts = Some(LoadOpts(Some(filename),
            Some(defaultModelInfo.unit), Some(defaultModelInfo.up), Some(defaultModelInfo.front),
            Some(loadFormat)))
        }
      }
      opts
    }

    registerModelsDb(modelsDb)
  }
}

object DataManager {
  lazy val _dataManager = new DataManager()
  def apply() = _dataManager
}
