package edu.stanford.graphics.shapenet.common

import java.io.File

import au.com.bytecode.opencsv.CSVWriter
import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.data.ModelsDb
import edu.stanford.graphics.shapenet.util.{BatchSampler, CSVFile, IOUtils}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Simple category taxonomy with mapping to WordNet.
 * @param categoriesFile with category hierarchy
 * @author Angel Chang
 */
class CategoryTaxonomy(val categoriesFile: String = Constants.CATEGORIES_FILE,
                       val materialsFile: String = Constants.CATEGORY_MATERIALS_FILE,
                       val isContainerFile: String = Constants.CATEGORY_ISCONTAINER_FILE) {
  val categoryMap: Map[String, CategoryInfo] = initCategories()
  var synsetCategoriesMap: Map[String, Set[String]] = null

  def initCategories(): Map[String, CategoryInfo] = {
    val map = new mutable.HashMap[String, CategoryInfo]()
    readCategories(map, categoriesFile)
    readMaterialsFile(map, materialsFile)
    readIsContainerFile(map, isContainerFile)
    map.toMap
  }

  def getContainerCategories(): Set[String] = {
    categoryMap.values.filter(x => x.isContainer).map(x => x.name).toSet
  }

  def getLeafCategories(): Set[String] = {
    categoryMap.values.filter(x => !x.hasChildren).map(x => x.name).toSet
  }

  /** Read in simple 2 level category hierarchy
    * @param categoriesFile with category hierarchy definitions: "category subcategory0 subcategory1"
    */
  @deprecated
  private def readSubcategories(map: mutable.HashMap[String, CategoryInfo], categoriesFile: String) {
    val lines = IOUtils.fileSource(categoriesFile).getLines()
    for (s <- lines) {
      if (!s.startsWith("#")) {
        val fields: Array[String] = s.split("\\s+", 2)
        val category = fields(0)
        val subcategories: Set[String] =
          if (fields.length > 1) {
            fields(1).split("\\s*,\\s*").toSet
          } else {
            Set()
          }
        val existing = map.getOrElseUpdate(category, new CategoryInfo(category))
        existing.children ++= subcategories
        for (sc <- subcategories) {
          map.getOrElseUpdate(sc, new CategoryInfo(sc, category)).parent = category
        }
      }
    }
  }

  private def readCategories(map: mutable.HashMap[String, CategoryInfo], categoriesFile: String): Unit = {
    //wnsynsetid,children,symmetries,attachmentSide,hasFront,viewingSide,isContainer,isAnimate,
    // isManmade,foundIn,roughShape,roughSize,inShapeNetCore,exampleModel,text
    val csvfile = new CSVFile(categoriesFile, includesHeader = true)
    val iCategory: Int = csvfile.index("category")
    val iChildren: Int = csvfile.index("children")
    val iSynsetId: Int = csvfile.index("wnsynsetid")
    val knownColumns = Set(iCategory, iChildren, iSynsetId)
    val header = csvfile.getHeader()
    for (row <- csvfile) {
      val category: String = row(iCategory).trim
      val children: String = row(iChildren).trim
      val synsetId: String = row(iSynsetId).trim

      val subcategories: Set[String] =
        if (children.length > 0) {
          children.split("\\s*,\\s*").toSet
        } else {
          Set()
        }
      val existing = map.getOrElseUpdate(category, new CategoryInfo(category))
      existing.children ++= subcategories
      existing.synsetId = synsetId
      for (sc <- subcategories) {
        map.getOrElseUpdate(sc, new CategoryInfo(sc, category)).parent = category
      }

      // Go over other fields
      for ((fieldvalue, i) <- row.zipWithIndex) {
        val fv = fieldvalue.trim()
        if (!knownColumns.contains(i) && fv.length > 0) {
          val fieldname = header(i)
          existing.addAttribute(fieldname, fv)
        }
      }
    }
  }

  def updateCategoriesFile(categoriesFile: String,
                           backupFile: String = categoriesFile.replace(".csv", ".backup.csv"))(modelsDb: ModelsDb = null): Unit = {
    // Update categories file with synsets
    val tempFile = File.createTempFile("categories", "txt")
    val csvWriter: CSVWriter = new CSVWriter(IOUtils.filePrintWriter(tempFile.getAbsolutePath))
    val csvfile = new CSVFile(categoriesFile, includesHeader = true)
    val iCategory: Int = csvfile.index("category")
    val iModelId: Int = csvfile.index("exampleModel")
    val iSynsetId: Int = csvfile.index("wnsynsetid")
    val header = csvfile.getHeader()
    val sampler = new BatchSampler()
    csvWriter.writeNext(header)
    for (row <- csvfile) {
      val category: String = row(iCategory).trim
      val modelId: String = row(iModelId).trim
      val existing = categoryMap.getOrElse(category, null)

      // Output row
      row.update(iSynsetId, existing.synsetId)
      if (modelsDb != null) {
        if (modelId.isEmpty || "none".equals(modelId)) {
          // Try to sample from our models
          val candidates = modelsDb.getModelInfos("wss", category)
          val sampled = sampler.sampleOne(candidates.toIndexedSeq)
          if (sampled.isDefined) {
            row.update(iModelId, sampled.get.fullId)
          }
        }
      }
      csvWriter.writeNext(row)
    }
    csvWriter.close()
    IOUtils.moveFile(categoriesFile, backupFile)
    IOUtils.moveFile(tempFile.getAbsolutePath(), categoriesFile)
  }

  private def readMaterialsFile(map: mutable.HashMap[String, CategoryInfo], filename: String): Unit = {
    val csvfile = new CSVFile(filename, includesHeader = true)
    val iCategory: Int = csvfile.index("Category")
    val iMaterial: Int = csvfile.index("Material")
    val iRatio: Int = csvfile.index("Ratio")
    for (row <- csvfile) {
      val category: String = row(iCategory).trim
      val material: String = row(iMaterial).trim
      val ratio: Double = row(iRatio).trim.toDouble
      val catInfo = map.getOrElseUpdate(category, new CategoryInfo(category))
      if (catInfo.materials == null) {
        catInfo.materials = Map(material -> ratio)
      } else {
        catInfo.materials += (material -> ratio)
      }
    }
  }

  private def readIsContainerFile(map: mutable.HashMap[String, CategoryInfo], filename: String): Unit = {
    val csvfile = new CSVFile(filename, includesHeader = true)
    val iCategory: Int = csvfile.index("category")
    val iIsContainer: Int = csvfile.index("isContainer")
    for (row <- csvfile) {
      val category: String = row(iCategory).trim
      val isContainerStr: String = row(iIsContainer).trim
      val isContainer: Boolean = isContainerStr == "1"
      val catInfo = map.getOrElseUpdate(category, new CategoryInfo(category))
      catInfo.isContainer = isContainer
    }
  }

  def getCategoryInfo(category: String): CategoryInfo = {
    categoryMap.getOrElse(category, null)
  }

  def getCategories(synset: String): Set[String] = {
    synsetCategoriesMap.getOrElse(synset, null)
  }

  def getSynsetId(category: String): String = {
    val cat = categoryMap.getOrElse(category, null)
    if (cat != null) cat.synsetId else null
  }

  def getParent(category: String): String = {
    val cat = categoryMap.getOrElse(category, null)
    if (cat != null) cat.parent else null
  }

  def getSubCategoryLevel(subCat: String, cat: String): Int = {
    var level = 0
    var parent = subCat
    while (parent != null) {
      if (parent == cat) return level
      level += 1
      parent = getParent(parent)
    }
    -1
  }

  def getAncestors(category: String): Seq[String] = {
    val ancestors = ArrayBuffer[String]()
    var parent = category
    while (parent != null) {
      ancestors.append(parent)
      parent = getParent(parent)
    }
    ancestors.toSeq
  }

  // Returns list of categories including ancestors, from finest to coarsest
  def getCategoriesWithAncestors(categories: Seq[String]): Seq[String] = {
    val ancestorsWithDepth: Seq[Seq[(String, Int)]] = categories.map(
      category => {
        val anc = getAncestors(category)
        val s = anc.size
        anc.zipWithIndex.map(x => (x._1, s - x._2))
      }
    )
    ancestorsWithDepth.flatten.sortBy(x => -x._2).map(x => x._1).distinct
  }
}

/**
 * Node in category hierarchy with information about a category.
 * @param name of category
 * @param parent of category
 */
class CategoryInfo(val name: String, var parent: String = null) extends HasAttributes {
  val children: mutable.Set[String] = mutable.Set[String]()
  var synsetId: String = null
  var materials: Map[String, Double] = null // Priors on what an object of this category is made of
  var isContainer: Boolean = false // is this category typically a container?
  var attributes: IndexedSeq[(String,String)] = IndexedSeq()

  def hasParent = parent != null
  def hasChildren = !children.isEmpty
  def hasNoOne = parent == null && children.isEmpty

  override def toString = {
    name + ", parent: " + parent + ", children: " + children + ", synset: " + synsetId
  }
}