package edu.stanford.graphics.shapenet.data

import java.io.File

import edu.stanford.graphics.shapenet.common.{FullId, CategoryTaxonomy, ModelInfo}
import edu.stanford.graphics.shapenet.jme3.loaders.{ModelLoadOptions, AssetGroups}
import edu.stanford.graphics.shapenet.util.IOUtils

/**
 * Handles data management
  *
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
    val modelsDb = new ModelsDbWithCategoryCsvs("ShapeNetCore", dir) {
      override def getModelLoadOptions(fullId: FullId, format: String) = {
        // customized load path
        val modelInfo = getModelInfo(fullId.fullid)
        var opts = super.getModelLoadOptions(fullId, format)
        logger.info("Format is " + format)
        if (modelInfo.isDefined && modelInfo.get.category.nonEmpty && format != "kmz" && format != "dae") {
          val x = modelInfo.get
          val filename = dir + x.category.head + File.separator + x.id + File.separator + "model.obj"
          if (IOUtils.isReadableFileWithData(filename)) {
            opts = ModelLoadOptions(
              format = "obj",
              path = Some(filename),
              unit = Some(defaultModelInfo.unit),
              up = Some(defaultModelInfo.up),
              front = Some(defaultModelInfo.front),
              //doubleSided = true,
              ignoreZeroRGBs = true,
              defaultColor = Array(0.0, 0.0, 0.0),
              modelIdToPath = (id: String) => {
                dir + x.category.head + File.separator + x.id + File.separator + "model.obj"
              }
            )
          }
        }
        opts
      }
    }
    modelsDb.init(categoryTaxonomy)
    modelsDb.lowercaseCategoryNames = true

    registerModelsDb(modelsDb)
  }

  def registerShapeNetSem(dirpath: String): Unit = {
    val dir = IOUtils.ensureDirname(dirpath)
    val metadataFilename = dir + "metadata.csv"
    //categoryTaxonomy.init(dir + "taxonomy.json", "json")
    val modelsDb = new ModelsDbWithCsv("ShapeNetSem", metadataFilename) {
      // customized load path
      override def getModelLoadOptions(fullId: FullId, format: String) = {
        val modelInfo = getModelInfo(fullId.fullid)
        var opts = AssetGroups.getModelLoadOptions(fullId.source, format)
        if (modelInfo.isDefined) {
          val x = modelInfo.get
          val filename = if (format == "obj") {
            dir + "models" + File.separator + x.id + ".obj"
          } else if (format == "dae" || format == "kmz") {
            dir + "COLLADA" + File.separator + x.id + ".dae"
          } else null
          if (filename != null && IOUtils.isReadableFileWithData(filename)) {
            opts = opts.copy(path = Some(filename))
          }
        }
        opts
      }
    }
    modelsDb.init()
    //modelsDb.init(categoryTaxonomy)
    //modelsDb.lowercaseCategoryNames = true

    registerModelsDb(modelsDb)
  }
}

object DataManager {
  lazy val _dataManager = new DataManager()
  def apply() = _dataManager
}
