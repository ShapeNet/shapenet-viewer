package edu.stanford.graphics.shapenet.data

import java.io.File

import edu.stanford.graphics.shapenet.common.{CategoryTaxonomy, ModelInfo}
import edu.stanford.graphics.shapenet.util.IOUtils

/**
 * Handles data management
 * @author Angel Chang
 */
class DataManager extends CombinedModelsDb("DataManager") {
  lazy val solrQuerier = new SolrQuerier

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
    val modelsDb = new ModelsDbWithCategoryCsvs("ShapeNetCore", dir)
    modelsDb.init(categoryTaxonomy)
    modelsDb.lowercaseCategoryNames = true

    // customized load path
    modelsDb.getModelLoadOptions = (fullId: String, format: String) => {
      val modelInfo = getModelInfo(fullId)
      var opts: Option[LoadOpts] = None
      if (modelInfo.isDefined && modelInfo.get.category.nonEmpty && format != "kmz") {
        val x = modelInfo.get
        val filename = dir + x.category.head + File.separator + x.id + File.separator + "model.obj"
        if (IOUtils.isReadableFileWithData(filename)) {
          opts = Some(LoadOpts(Some(filename),
            Some(defaultModelInfo.unit), Some(defaultModelInfo.up), Some(defaultModelInfo.front)))
        }
      }
      opts
    }

    registerModelsDb(modelsDb)
  }

  def registerShapeNetSem(dirpath: String): Unit = {
    val dir = IOUtils.ensureDirname(dirpath)
    val metadataFilename = dir + "metadata.csv"
    //categoryTaxonomy.init(dir + "taxonomy.json", "json")
    val modelsDb = new ModelsDbWithCsv("ShapeNetSem", metadataFilename)
    modelsDb.init()
    //modelsDb.init(categoryTaxonomy)
    //modelsDb.lowercaseCategoryNames = true

    // customized load path
    modelsDb.getModelLoadOptions = (fullId: String, format: String) => {
      val modelInfo = getModelInfo(fullId)
      var opts: Option[LoadOpts] = None
      if (modelInfo.isDefined) {
        val x = modelInfo.get
        val filename = if (format == "obj") {
          dir + "models" + File.separator + x.id + ".obj"
        } else if (format == "dae") {
          dir + "COLLADA" + File.separator + x.id + ".dae"
        } else null
        if (filename != null && IOUtils.isReadableFileWithData(filename)) {
          opts = Some(LoadOpts(Some(filename)))
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
