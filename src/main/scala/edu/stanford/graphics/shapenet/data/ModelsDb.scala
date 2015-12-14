package edu.stanford.graphics.shapenet.data

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.common.{CategoryTaxonomy, CategoryUtils, FullId, ModelInfo}
import edu.stanford.graphics.shapenet.models3d.{Materials, Material}
import edu.stanford.graphics.shapenet.util.{BatchSampler, CSVFile, Loggable, IOUtils}
import edu.stanford.graphics.shapenet.Constants

import scala.util.Random

/**
 *  Database of 3D models
 *  @author Angel Chang
 */
class ModelsDb(modelsFile: String = Constants.MODELS3D_CSV_FILE) extends Loggable {
  var models: Map[String, ModelInfo] = null
  var categoryTaxonomy: CategoryTaxonomy = null
  var materials: Map[String, Material] = null

  def init(useCategoryTaxonomy:Boolean = false,
           useMaterials:Boolean = false)
  {
    categoryTaxonomy = if (useCategoryTaxonomy) {
      new CategoryTaxonomy()
    } else  { null }
    materials = if (useMaterials) {
      Materials.readMaterials(Constants.MATERIAL_DENSITIES_FILE)
    } else { null }
    models = readModelsCsv(modelsFile, categoryTaxonomy)
  }

  def getModelIds(source: String = null, category: String = null): Seq[String] = {
    val ids = if (source != null) {
      models.keys.toIndexedSeq.filter( x => {
        val fullId = FullId(x)
        fullId.source == source
      })
    } else {
      models.keys.toIndexedSeq
    }
    val filteredIds = if (category != null) {
      ids.filter( id => {
        CategoryUtils.hasCategory(models(id), category)
      })
    } else {
      ids
    }
    filteredIds
  }

  def getRandomModelIds(source: String = null, category: String = null, nModels:Int = 1): Seq[String] = {
    val filteredIds = getModelIds(source, category)
    if (filteredIds.nonEmpty) {
      val sampler = new BatchSampler
      sampler.sampleWithoutReplacement(filteredIds, nModels)
    } else {
      Seq()
    }
  }

  def getRandomModelId(source: String = null, category: String = null): String = {
    val filteredIds = getModelIds(source, category)
    if (filteredIds.nonEmpty) {
      filteredIds(Random.nextInt(filteredIds.size))
    } else {
      null
    }
  }

  def readModelsCsv(filename: String, categoryTaxonomy: CategoryTaxonomy = null): Map[String, ModelInfo] = {
    def toVector3f(str: String): Vector3f = {
      if (str != null && str.length > 0) {
        val f = str.split("\\s*,\\s*").map( s => s.toFloat )
        new Vector3f( f(0), f(1), f(2) )
      } else null
    }
    def toDouble(str: String, default: Double = Double.NaN): Double = {
      if (str != null && str.length > 0) {
        str.toDouble
      } else default
    }
    // Read CSV of models
    val csvFile = new CSVFile(
      fileName = filename,
      includesHeader = true,
      escape = '\\')
      //headerFieldLength = collection.immutable.HashMap("minPoint" -> 3, "maxPoint" -> 3))
    val map = csvFile.toMap(
      row =>  {
        val modelId = csvFile.field(row, "fullId")
        val minPoint = toVector3f(csvFile.field(row, "minPoint"))
        val maxPoint = toVector3f(csvFile.field(row, "maxPoint"))
        val up = toVector3f(csvFile.field(row, "up"))
        val front = toVector3f(csvFile.field(row, "front"))
        val csvCategories = csvFile.splitField(row, "category", ",").map( x => x.intern() )
        val allCategories = if (categoryTaxonomy != null) {
          // Augment model categories with super classes and order categories from fine to least fine
          val (auxCategories, mainCategories) = csvCategories.partition( x => x.startsWith("_"))
          val augmentedCategories = categoryTaxonomy.getCategoriesWithAncestors(mainCategories)
          auxCategories ++ augmentedCategories
        } else {
          csvCategories
        }
        val isRoom = if (allCategories != null && !allCategories.isEmpty)
          allCategories.contains("Room")
        else false
        // Fix hacking unit for rooms... store in DB
        val defaultUnit = if (isRoom && modelId.startsWith("wss."))
          Constants.WSS_SCENE_SCALE
        else Constants.DEFAULT_MODEL_UNIT

        val unit = toDouble(csvFile.field(row,"unit"), defaultUnit)

        val info = new ModelInfo(
          fullId = modelId,
          minPoint = minPoint,
          maxPoint = maxPoint,
          rawbbdims = maxPoint.subtract(minPoint),
          name = csvFile.field(row, "name"),
          tags = csvFile.splitField(row, "tags", ",").filter( t => !t.isEmpty && !t.equals("*")),
          allCategory = allCategories,
          category0 = csvFile.splitField(row, "category0", ",").map( x => x.intern() ),
          scenes = csvFile.splitField(row, "scenes", ",").map( x => x.intern() ),
          datasets = csvFile.splitField(row, "datasets", ",").map( x => x.intern() ),
          unit0 = unit,
          up0 = up,
          front0 = front
        )
        (modelId, info)
      }
    )
    map.filter( x => !x._2.isBad )
  }

  // Output utility
  def printModelCategories(filename: String): Unit = {
    val pw = IOUtils.filePrintWriter(filename)
    pw.println("modelId,category")
    for (model <- models.values) {
      for (cat <- model.category) {
        pw.println(model.fullId + "," + cat)
      }
    }
    pw.close()
  }

}

