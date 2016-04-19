package edu.stanford.graphics.shapenet.data

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.common._
import edu.stanford.graphics.shapenet.jme3.loaders.{AssetGroups, ModelLoadOptions}
import edu.stanford.graphics.shapenet.util.{BatchSampler, CSVFile, Loggable, IOUtils}

import scala.util.Random
import scala.util.matching.Regex

/**
 *  Database of 3D models
  *
  *  @author Angel Chang
 */
trait ModelsDb {
  val name: String

  def getName(): String = name

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

  def getModelLoadOptions(fullId:FullId, format:String): ModelLoadOptions = {
    val modelInfo = getModelInfo(fullId.fullid)
    var x = AssetGroups.getModelLoadOptions(fullId.source, format)
    if (modelInfo.isDefined) {
      x = x.copy( unit = Some(modelInfo.get.unit),
                  up = if (modelInfo.get.up != null) Some(modelInfo.get.up) else x.up,
                  front = if (modelInfo.get.front != null) Some(modelInfo.get.front) else x.front )
    }
    x
  }

  def getDefaultModelInfo(fullId:FullId, format:String = null): DefaultModelInfo = {
    AssetGroups.getDefaultModelInfo(fullId.source)
  }
}

class CombinedModelsDb(override val name: String,
                       val defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends ModelsDb with Loggable {
  lazy val modelsDbsByName = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ModelsDb]]()
  lazy val modelsDbsBySource = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ModelsDb]]()
  lazy val modelsDbs = new scala.collection.mutable.ArrayBuffer[ModelsDb]
  override def getModelLoadOptions(fullId:FullId, format:String) = {
    var opts: ModelLoadOptions = null
    for (db <- modelsDbs) {
      val dbOpts = db.getModelLoadOptions(fullId, format)
      if (dbOpts != null) {
        if (opts == null || (dbOpts.format != opts.format && dbOpts.format == format)
          || (dbOpts.format == opts.format && dbOpts.path.isDefined && opts.path.isEmpty)) {
          // this one is better
          opts = dbOpts
        }
      }
    }
    if (opts == null) {
      opts = super.getModelLoadOptions(fullId, format)
    }
    opts
  }

  def registerModelsDb(modelsDb: ModelsDb) = {
    val sources = modelsDb.getSources()
    modelsDbs.append(modelsDb)
    for (source <- sources) {
      val list = modelsDbsBySource.getOrElseUpdate(source, new scala.collection.mutable.ArrayBuffer[ModelsDb]())
      list.append(modelsDb)
    }
    val nameList = modelsDbsBySource.getOrElseUpdate(modelsDb.name, new scala.collection.mutable.ArrayBuffer[ModelsDb]())
    nameList.append(modelsDb)
  }
  def registerCsvAsModelsDb(name: String, modelsCsvFile: String, defaults: DefaultModelInfo = defaultModelInfo): ModelsDb = {
    logger.info("Registering " + modelsCsvFile)
    val db = new ModelsDbWithCsv(name, modelsCsvFile, defaults)
    db.init()
    registerModelsDb(db)
    db
  }
  def registerSolrQueryAsModelsDb(name: String, solrQuerier: SolrQuerier, solrQuery: String, defaults: DefaultModelInfo = defaultModelInfo): ModelsDb = {
    logger.info("Registering query " + solrQuery)
    val db = new ModelsDbWithSolrQuery(name, solrQuerier, solrQuery, defaults)
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

abstract class ModelsDbWithMap(override val name: String,
                               val defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends ModelsDb with Loggable {
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

class ModelsDbWithSolrQuery(name: String,
                            solrQuerier: SolrQuerier, query: String,
                            defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends ModelsDbWithMap(name, defaultModelInfo) with Loggable {
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

class ModelsDbWithCsv(name: String, modelsFile: String, defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends ModelsDbWithMap(name, defaultModelInfo) with Loggable {
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
    val map = csvFile.toMap(
      row =>  {
        val modelId = csvFile.field(row, "fullId")
        val fullId = FullId(modelId)
        // NOTE: Default for model has good default unit,up,front, but not categories
        val defaultsForModel = getDefaultModelInfo(fullId)
        val minPoint = toVector3f(csvFile.field(row, "minPoint"))
        val maxPoint = toVector3f(csvFile.field(row, "maxPoint"))
        val up = toVector3f(csvFile.field(row, "up"))
        val front = toVector3f(csvFile.field(row, "front"))
        // defaultModelInfo has default categories
        val csvCategories = csvFile.splitField(row, "category", ",").map( x => x.intern() ) ++ defaultModelInfo.categories
        val allCategories = if (categoryTaxonomy != null && csvCategories.nonEmpty) {
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
          defaultsForModel.copy( unit = Constants.WSS_SCENE_SCALE )
        else defaultsForModel

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

class ModelsDbWithCategoryCsvs(name: String, dir: String, defaultModelInfo: DefaultModelInfo = DefaultModelInfo()) extends CombinedModelsDb(name, defaultModelInfo) with Loggable {
  lazy val modelsDbsByCategory = new scala.collection.mutable.HashMap[String, scala.collection.mutable.ArrayBuffer[ModelsDb]]()
  var categoryTaxonomy: CategoryTaxonomy = null
  var lowercaseCategoryNames: Boolean = false

  def init(catTaxonomy: CategoryTaxonomy = null) {
    // Get csvs from dir
    categoryTaxonomy = catTaxonomy
    val csvFiles = IOUtils.listFiles(dir, new Regex(".*\\.csv$"))
    for (categoryCsv <- csvFiles) {
      val category = IOUtils.stripExtension(categoryCsv.getName)
      val modelsDb = new ModelsDbWithCsv(name + "-" + category, categoryCsv.getAbsolutePath,
        defaultModelInfo.copy( categories = defaultModelInfo.categories :+ category ))
      modelsDb.init(catTaxonomy)
      registerModelsDb(modelsDb)
      val list = modelsDbsByCategory.getOrElseUpdate(category, new scala.collection.mutable.ArrayBuffer[ModelsDb]())
      list.append(modelsDb)
    }
  }

  override def getModelInfos(source: String, category: String): Seq[ModelInfo] = {
    if (category != null) {
      val norm = if (lowercaseCategoryNames) category.toLowerCase() else category
      val categories = categoryTaxonomy.getCategoriesByName(norm).filter( x => !x.hasParent )
      val names = categories.map( x => x.name ).toSeq
      logger.info("Top level matching categories for '" + category + "':" + names.mkString(","))
      val modelInfos = for (cat <- names) yield {
        if (modelsDbsByCategory.contains(cat)) {
          modelsDbsByCategory(cat).flatMap(x => x.getModelInfos(source = source, null))
        } else Seq()
      }
      modelInfos.flatten
    } else {
      super.getModelInfos(source, category)
    }
  }
}