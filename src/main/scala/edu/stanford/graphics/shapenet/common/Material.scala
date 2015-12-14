package edu.stanford.graphics.shapenet.models3d

import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.util.CSVFile

import scala.collection.mutable

/**
 * Material
 * @author Angel Chang
 */
case class Material(name: String, density: Double, staticFrictionCoeff: Double) {
}

object Materials {
  lazy val materials: Map[String, Material] = Materials.readMaterials(Constants.MATERIAL_DENSITIES_FILE)

  def readMaterials(filename: String): Map[String, Material] = {
    val map = new mutable.HashMap[String, Material]()
    val csvfile = new CSVFile(filename, includesHeader = true)
    val iMaterial: Int = csvfile.index("Material")
    val iDensity: Int = csvfile.index("Density")
    val iStaticFrictionCoeff: Int = csvfile.index("StaticFrictionCoeff")
    for (row <- csvfile) {
      val material: String = row(iMaterial).trim
      val density: Double = row(iDensity).trim.toDouble
      val staticFrictionCoeff: Double = row(iStaticFrictionCoeff).trim.toDouble
      val matInfo = map.getOrElseUpdate(material, Material(material, density, staticFrictionCoeff))
    }
    map.toMap
  }
}