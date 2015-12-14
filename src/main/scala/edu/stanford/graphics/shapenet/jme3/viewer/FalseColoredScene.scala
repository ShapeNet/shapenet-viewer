package edu.stanford.graphics.shapenet.jme3.viewer

import edu.stanford.graphics.shapenet.UserDataConstants
import edu.stanford.graphics.shapenet.common.GeometricScene
import edu.stanford.graphics.shapenet.jme3.Jme
import edu.stanford.graphics.shapenet.jme3.viewer.FalseColoredScene.{IndexToPixelsMap, Pixel}
import com.jme3.math.ColorRGBA
import com.jme3.material.Material
import com.jme3.scene.{Geometry, Node}
import java.nio.ByteBuffer
import scala.collection.{mutable, MapProxy}

/**
 * False Coloring for a scene
 * @author Angel Chang
 */
object FalseColoredScene {
  case class Pixel(x: Int, y: Int)
  // Map from a index to a sequence of pixels with that index
  class IndexToPixelsMap(val self: Map[Int, IndexedSeq[Pixel]]) extends MapProxy[Int,IndexedSeq[Pixel]] {
    def pixels(c: Int) = self.getOrElse(c, IndexedSeq())
    def size(c: Int) = pixels(c).size
  }
}

class FalseColorGenerator {
  // Set of predefined colors that we want to avoid
  val backgroundIndex = -1
  val backgroundColor = ColorRGBA.White
  val backsceneColor = ColorRGBA.Gray
  val predefinedColors = Set( colorAsInt(backgroundColor), colorAsInt(backsceneColor) )

  val falseColors = new scala.collection.mutable.ArrayBuffer[ColorRGBA]
  // Map of colors (ColorRGBA stuffed into a Int) to Index (lookup in falseColors)
  val falseColorsToIndex = new scala.collection.mutable.HashMap[Int,Int]()

  protected def populateColors(n: Int) {
    //    val p = (n+2)/3
    //    val d = 255/p
    //    var i = 0
    //    for (r <- 0 to 255 by d; b <- 0 to 255 by d; g <- 0 to 255 by d) {
    //      val c = new ColorRGBA(r/255.0f, b/255.0f, g/255.0f, 1.0f)
    //      falseColors += c
    //      falseColorsToModelInstIndex.put( c.asIntRGBA(), i )
    //      i = i + 1
    //      if (i >= n) return
    //    }
    for (i <- 0 until n) {
      generateColor(i)
    }
  }

  def generateColor(i: Int = falseColors.size): (ColorRGBA, Int) = {
    if (i < falseColors.size) {
      return (falseColors(i),i)
    }
    // Randomly select random colors
    var c = ColorRGBA.randomColor
    var cint = colorAsInt(c)
    while (predefinedColors.contains(cint) || falseColorsToIndex.contains(cint)) {
      c = ColorRGBA.randomColor
      cint = colorAsInt(c)
    }
    falseColors += c
    falseColorsToIndex.put( cint, i )
    (c,cint)
  }

  protected def colorAsBytes(c: ColorRGBA) = {
    val bytes = Array.ofDim[Byte](4)
    bytes(0) = (math.round(c.r * 255) & 0xFF).toByte
    bytes(1) = (math.round(c.g * 255) & 0xFF).toByte
    bytes(2) = (math.round(c.b * 255) & 0xFF).toByte
    bytes(3) = (math.round(c.a * 255) & 0xFF).toByte
    bytes
  }

  protected def colorAsInt(c: ColorRGBA) = {
    val bytes = colorAsBytes(c)
    colorBytesAsInt(bytes(0), bytes(1), bytes(2), bytes(3))
  }

  protected def colorAsRGBA(c: Int) = {
    val color = new ColorRGBA()
    color.fromIntRGBA(c)
    color
  }

  protected def colorBytesAsInt(r: Byte, g: Byte, b: Byte, a: Byte)
    = ((r.toInt & 0xFF) << 24) | ((g.toInt & 0xFF) << 16) | ((b.toInt & 0xFF) << 8) | (a.toInt & 0xFF)
}

/**
 * Basic false colored scene that colors models different colors
 * @param inputScene
 * @param jme
 */
class FalseColoredScene(val inputScene: GeometricScene[Node])(implicit val jme: Jme) extends FalseColorGenerator {

  lazy val coloredSceneRoot = colorModels(inputScene)

  def getIndexCounts(byteBuf: ByteBuffer, width: Int, height: Int) = {
    val counts = getColorCounts(byteBuf, width, height)
    colorMapToIndexMap(counts)
  }

  def getIndexPixels(byteBuf: ByteBuffer, width: Int, height: Int): IndexToPixelsMap = {
    val pixels = getPixels(byteBuf, width, height, colorToIndex, backgroundIndex)
    new IndexToPixelsMap(pixels)
  }

  /**
   * Color the scene using indexed colors (one per model)
   * @param inputScene Input scene to color
   * @return Scene node of colored scene that can be rendered
   */
  private def colorModels(inputScene: GeometricScene[Node]): Node = {
    val node = jme.assetCreator.cloneNode(inputScene.node)
    val modelInstanceNodes = jme.getModelInstanceNodes(node)
    val n = modelInstanceNodes.size
    populateColors(n)
    for ((m,i) <- modelInstanceNodes.zipWithIndex) {
      if (m != null) {
        val mat = jme.getFlatFalseColorMaterial(falseColors(i))
        jme.makeDoubleSided(mat)
        jme.setMaterials(m, mat, true)
      }
    }
    // Don't need any controls...
    jme.removeControls(node)
    node
  }

  protected def getPixels[T](byteBuf: ByteBuffer, width: Int, height: Int, colorToIndex: Int => T, ignoreIndex: T): Map[T, IndexedSeq[Pixel]] = {
    val pixelMap = new scala.collection.mutable.HashMap[T,mutable.ArrayBuffer[Pixel]]()
    for (y <- 0 until height; x <- 0 until width) {
      val ptr: Int = (y * width + x) * 4
      val b: Byte = byteBuf.get(ptr + 0)
      val g: Byte = byteBuf.get(ptr + 1)
      val r: Byte = byteBuf.get(ptr + 2)
      val a: Byte = byteBuf.get(ptr + 3)

      // Figure what color this point is associated with
      val c: Int = colorBytesAsInt(r,g,b,a)
      val mi = colorToIndex(c)
      if (mi != ignoreIndex) {
        // Not part of the background
        val pixels = pixelMap.getOrElseUpdate(mi, new mutable.ArrayBuffer[Pixel]())
        pixels.append(Pixel(x,y))
      }
    }
    pixelMap.mapValues( x => x.toIndexedSeq ).toMap
  }

  protected def getColorCounts(byteBuf: ByteBuffer, width: Int, height: Int): Map[Int,Int] = {
    val colorMap = new scala.collection.mutable.HashMap[Int,Int]()
    for (y <- 0 until height; x <- 0 until width) {
      val ptr: Int = (y * width + x) * 4
      val b: Byte = byteBuf.get(ptr + 0)
      val g: Byte = byteBuf.get(ptr + 1)
      val r: Byte = byteBuf.get(ptr + 2)
      val a: Byte = byteBuf.get(ptr + 3)

      // Figure what color this point is associated with
      val c: Int = colorBytesAsInt(r,g,b,a)
      val oldCount = colorMap.getOrElseUpdate(c, 0)
      colorMap.put(c, oldCount+1)
    }
    colorMap.toMap
  }

  protected def colorToIndex(c: Int) = {
    falseColorsToIndex.getOrElse(c, backgroundIndex)
  }

  protected def colorMapToIndexMap(colorMap: Map[Int,Int]): Map[Int,Int] = {
    colorMap.map( p => (falseColorsToIndex.getOrElse(p._1, backgroundIndex) -> p._2) ).toMap
  }
}

/**
 * False colored scene that colors meshes different colors
 * @param inputScene
 * @param jme
 */
class FalseColoredMeshScene(inputScene: GeometricScene[Node],
                            selected: Seq[Node],
                            meshIndexName: String = UserDataConstants.MESH_INDEX)(implicit jme: Jme)
  extends FalseColoredScene(inputScene)(jme)
{
  val falseColorToMeshIndex = new scala.collection.mutable.HashMap[Int,Seq[(Int,Int)]]()
  val falseColorByMeshIndex = new scala.collection.mutable.HashMap[(Int,Int),Int]()

  override lazy val coloredSceneRoot = colorMeshes(inputScene, selected:_*)

  /**
   * Color the scene using indexed colors (one per mesh)
   * nodes that are not among the target nodes are colored gray
   * @param inputScene Input scene to color
   * @return Scene node of colored scene that can be rendered
   */
  private def colorMeshes(inputScene: GeometricScene[Node], targetNodes: Node*): Node = {
    colorMeshes(inputScene, meshIndexName, targetNodes:_*)
  }

  /**
   * Color the scene using indexed colors
   * - all meshes with the same mesh index are colored with the same color
   * - the mesh index can be unique for mesh or indicate the part the mesh belongs to
   * nodes that are not among the target nodes are colored gray
   * @param inputScene Input scene to color
   * @return Scene node of colored scene that can be rendered
   */
  def colorMeshes(inputScene: GeometricScene[Node], indexName: String, targetNodes: Node*): Node = {
    val targetIndices = targetNodes.map( x => jme.getModelInstanceIndex(x) ).toSet
    val node = jme.assetCreator.cloneNode(inputScene.node)
    val modelInstanceNodes = jme.getModelInstanceNodes(node)
    val saveOldMaterial = true
    val backsceneMat = jme.getFlatFalseColorMaterial(backsceneColor)
    for ((m,i) <- modelInstanceNodes.zipWithIndex) {
      val modelIndex = i
      if (targetNodes.length == 0 || targetIndices.contains(modelIndex)) {
        val visitor = jme.getGeomVisitor(
          geomVisitor = (geom: Geometry) => {
            val meshIndex = geom.getUserData[Int](indexName)
            val c = falseColorForMesh(modelIndex,meshIndex)
            val material = jme.getFlatFalseColorMaterial(c)
            jme.makeDoubleSided(material)
            if (saveOldMaterial) {
              val oldMaterials = jme.userData.getOrElseUpdate[mutable.Stack[Material]](
                geom, UserDataConstants.ORIG_MATERIALS, new mutable.Stack[Material]())
              oldMaterials.push(geom.getMaterial)
            }
            geom.setMaterial(material)
          },
          maxDepth = 1
        )_
        jme.depthFirstTraversalForModelInstanceNodes(m, visitor)
      } else {
        jme.setMaterials(m, backsceneMat, true)
      }
    }
    // Don't need any controls...
    jme.removeControls(node)
    node
  }

  protected def falseColorForMesh(modelIndex: Int, meshIndex: Int): ColorRGBA = {
    val cOption = falseColorByMeshIndex.get((modelIndex, meshIndex))
    if (cOption.isEmpty) {
      // Not in list of known colors, let's generate a new color
      val (c,ci) = generateColor()
      falseColorByMeshIndex.update((modelIndex,meshIndex), ci)
      val meshIndices = falseColorToMeshIndex.getOrElseUpdate(ci, Seq())
      falseColorToMeshIndex.update(ci, meshIndices :+ (modelIndex,meshIndex))
      c
    } else {
      colorAsRGBA(cOption.get)
    }
  }

}
