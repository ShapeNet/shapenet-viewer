package edu.stanford.graphics.shapenet.data

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.common._
import edu.stanford.graphics.shapenet.util.{BatchSampler, CSVFile, Loggable, IOUtils}
import edu.stanford.graphics.shapenet.Constants

import scala.util.Random
import scala.util.matching.Regex

/**
 *  Database of 3D models
 *  @author Angel Chang
 */
trait ModelsDb {
  def getSources(): Set[String]

  def getModelIds(): Seq[String]

  def getModelInfo(id: String): Option[ModelInfo]

  def getModelInfos(): Seq[ModelInfo]

  def getModelInfos(source: String, category: String): Seq[ModelInfo] = {
    val modelInfos = if (source != null) {
      getModelInfos.filter( x => {
        val fullId = FullId(x.fullId)
        fullId.source == source
      })
    } else {
      getModelInfos
    }
    val filtered = if (category != null) {
      modelInfos.filter( modelInfo => {
        CategoryUtils.hasCategory(modelInfo, category)
      })
    } else {
      modelInfos
    }
    filtered
  }

  def getModelIds(source: String, category: String): Seq[String] = {
    getModelInfos(source, category).map( x => x.fullId )
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

}

class CombinedModelsDb(val defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends ModelsDb with Loggable {
  lazy val modelsDbsByName = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ModelsDb]]()
  lazy val modelsDbsBySource = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ModelsDb]]()
  lazy val modelsDbs = new scala.collection.mutable.ArrayBuffer[ModelsDb]

  def registerModelsDb(modelsDb: ModelsDb) = {
    val sources = modelsDb.getSources()
    modelsDbs.append(modelsDb)
    for (source <- sources) {
      val list = modelsDbsBySource.getOrElseUpdate(source, new scala.collection.mutable.ArrayBuffer[ModelsDb]())
      list.append(modelsDb)
    }
  }
  def registerCsvAsModelsDb(modelsCsvFile: String, defaults: DefaultModelInfo = defaultModelInfo): ModelsDb = {
    logger.info("Registering " + modelsCsvFile)
    val db = new ModelsDbWithCsv(modelsCsvFile, defaults)
    db.init()
    registerModelsDb(db)
    db
  }
  def registerSolrQueryAsModelsDb(solrQuerier: SolrQuerier, solrQuery: String, defaults: DefaultModelInfo = defaultModelInfo): ModelsDb = {
    logger.info("Registering query " + solrQuery)
    val db = new ModelsDbWithSolrQuery(solrQuerier, solrQuery, defaults)
    db.init()
    registerModelsDb(db)
    db
  }
  def getModelsDb(source: String): Seq[ModelsDb] = modelsDbsBySource.get(source).map( x => x.toSeq ).getOrElse(Seq())
  def getModelsDbForModel(modelId: String): Seq[ModelsDb] = {
    val fullId = FullId(modelId)
    getModelsDb(fullId.source)
  }

  def getModelInfo(modelId: String): Option[ModelInfo] = {
    val fullId = FullId(modelId)
    val dbs = getModelsDb(fullId.source)
    for (db <- dbs) {
      val modelInfo = db.getModelInfo(modelId)
      if (modelInfo.isDefined) return modelInfo
    }
    None
  }

  def getSources(): Set[String] = modelsDbsBySource.keySet.toSet

  def getModelIds(): Seq[String] = modelsDbs.flatMap( x => x.getModelIds() )

  def getModelInfos(): Seq[ModelInfo] = modelsDbs.flatMap( x => x.getModelInfos() )

  override def getModelInfos(source: String, category: String): Seq[ModelInfo] = {
    if (source != null) {
      if (modelsDbsBySource.contains(source)) {
        modelsDbsBySource(source).flatMap( x => x.getModelInfos(source = source, category = category) )
      } else Seq()
    } else {
      modelsDbs.flatMap( x => x.getModelInfos(source = source, category = category) )
    }
  }

}

abstract class ModelsDbWithMap(val defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends ModelsDb with Loggable {
  var models: Map[String, ModelInfo] = null
  var sources: Set[String] = null
  var categoryTaxonomy: CategoryTaxonomy = null

  def getSources(): Set[String] = sources

  def getModelInfos(): Seq[ModelInfo] = {
    models.values.toIndexedSeq
  }

  def getModelInfo(id: String): Option[ModelInfo] = {
    models.get(id)
  }

  def getModelIds(): Seq[String] = {
    models.keys.toIndexedSeq
  }
}

class ModelsDbWithSolrQuery(solrQuerier: SolrQuerier, query: String,
                            defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends ModelsDbWithMap(defaultModelInfo) with Loggable {
  def init(catTaxonomy: CategoryTaxonomy = null)
  {
    categoryTaxonomy = catTaxonomy
    models = queryModels(query)
    sources = models.values.map( x => x.source ).toSet
  }

  def queryModels(query: String, categoryTaxonomy: CategoryTaxonomy = null): Map[String, ModelInfo] = {
    val modelInfos = solrQuerier.getModelInfos(query, defaultModelInfo)
    modelInfos.map( x => x.fullId -> x ).toMap
  }
}

class ModelsDbWithCsv(modelsFile: String, defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends ModelsDbWithMap(defaultModelInfo) with Loggable {
  def init(catTaxonomy: CategoryTaxonomy = null)
  {
    categoryTaxonomy = catTaxonomy
    models = readModelsCsv(modelsFile, categoryTaxonomy)
    sources = models.values.map( x => x.source ).toSet
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
    def toDoubleOption(str: String): Option[Double] = {
      if (str != null && str.length > 0) {
        Some(str.toDouble)
      } else None
    }
    // Read CSV of models
    val csvFile = new CSVFile(
      fileName = filename,
      includesHeader = true,
      escape = '\\')
      //headerFieldLength = collection.immutable.HashMap("minPoint" -> 3, "maxPoint" -> 3))
    val defaultsForWssRoom = defaultModelInfo.copy(unit = Constants.WSS_SCENE_SCALE)
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
        val isRoom = if (allCategories != null && !allCategories.isEmpty) {
          allCategories.contains("Room")
        } else {
          false
        }
        // Fix hacking unit for rooms... store in DB
        val defaults = if (isRoom && modelId.startsWith("wss."))
          defaultsForWssRoom
        else defaultModelInfo

        val unit = toDoubleOption(csvFile.field(row,"unit"))

        val info = new ModelInfo(
          fullId = modelId,
          minPoint = minPoint,
          maxPoint = maxPoint,
          rawbbdims = if (maxPoint != null && minPoint != null) maxPoint.subtract(minPoint) else null,
          name = csvFile.field(row, "name"),
          tags = csvFile.splitField(row, "tags", ",").filter( t => !t.isEmpty && !t.equals("*")),
          allCategory = allCategories,
          category0 = csvFile.splitField(row, "category0", ",").map( x => x.intern() ),
          scenes = csvFile.splitField(row, "scenes", ",").map( x => x.intern() ),
          datasets = csvFile.splitField(row, "datasets", ",").map( x => x.intern() ),
          unit0 = unit,
          up0 = Option(up),
          front0 = Option(front),
          defaults
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

class ModelsDbWithCategoryCsvs(dir: String, defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends CombinedModelsDb(defaultModelInfo) with Loggable {
  lazy val modelsDbsByCategory = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ModelsDb]]()

  def init(catTaxonomy: CategoryTaxonomy = null) {
    // Get csvs from dir
    val csvFiles = IOUtils.listFiles(dir, new Regex("*.csv$"))
    for (categoryCsv <- csvFiles) {
      val modelsDb = new ModelsDbWithCsv(categoryCsv.getAbsolutePath, defaultModelInfo)
      modelsDb.init(catTaxonomy)
      registerModelsDb(modelsDb)
      val category = IOUtils.stripExtension(categoryCsv.getName)
      val list = modelsDbsByCategory.getOrElseUpdate(category, new scala.collection.mutable.ArrayBuffer[ModelsDb]())
      list.append(modelsDb)
    }
  }
}


