package edu.stanford.graphics.shapenet.jme3

import edu.stanford.graphics.shapenet.{jme3, UserDataConstants}
import edu.stanford.graphics.shapenet.common.{ModelInstance, GeometricScene, Model, MaterialInfo}
import edu.stanford.graphics.shapenet.jme3.asset.{EnhancedModelKey, CompressedAssetKey}
import edu.stanford.graphics.shapenet.jme3.loaders.{ModelLoadOptions, UTF8Decoder, AssetCreator}
import edu.stanford.graphics.shapenet.util.Loggable
import com.jme3.asset.{ModelKey, AssetManager}
import com.jme3.bounding.BoundingVolume
import com.jme3.material.Material
import com.jme3.material.RenderState.{FaceCullMode, BlendMode}
import com.jme3.math.{Matrix4f, ColorRGBA, Transform}
import com.jme3.renderer.queue.RenderQueue.Bucket
import com.jme3.scene._
import com.jme3.scene.Mesh.Mode
import com.jme3.texture.Texture
import com.jme3.texture.Texture.WrapMode
import jme3tools.optimize.GeometryBatchFactory

/**
 * Creates assets with JMonkeyEngine
 *
 * @author Angel Chang
 */
class JmeAssetCreator(val assetManager: AssetManager, val nolights: Boolean = false) extends AssetCreator with Loggable {
  type BBOX = BoundingVolume
  type SPATIAL = Spatial
  type NODE = Node
  type MESH = Geometry
  type MATERIAL = Material
  type TRANSFORM = Transform

  val defaultDiffuse = ColorRGBA.White
  val defaultAmbient = ColorRGBA.LightGray
  val defaultSpecular = ColorRGBA.DarkGray
  val optimize = false

  def getColor(v: Array[Double], default: ColorRGBA): ColorRGBA = {
    if (v != null) {
      if (v.length >= 4) {
        new ColorRGBA(v(0).toFloat, v(1).toFloat, v(2).toFloat, v(3).toFloat)
      } else {
        new ColorRGBA(v(0).toFloat, v(1).toFloat, v(2).toFloat, 1.0f)
      }
    } else default
  }

  def getTexture(path: String): Texture = {
    try {
      if (path != null) {
        val texture = assetManager.loadTexture(path)
        texture.setWrap(WrapMode.Repeat)
        texture
      } else null
    } catch {
      case e: Exception => {
        logger.warn("Cannot load texture: " + path, e)
        null
      }
    }
  }

  def createMaterial(mi: MaterialInfo) = {
    val ambient = getColor(mi.ambient, defaultAmbient)
    var diffuse = getColor(mi.diffuse, defaultDiffuse)
    if (mi.opacity < 1f && mi.transparent){
      diffuse = new ColorRGBA(diffuse)
      diffuse.a = mi.opacity.toFloat
    }
    val specular = getColor(mi.specular, defaultSpecular)
    val shininess = if (!mi.shininess.isNaN) mi.shininess.toFloat else 16

    var material: Material = null
    if (nolights || mi.shadeless){
      material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
      material.setName("unshaded material")
      material.setColor("Color", diffuse)
      val diffuseMap = getTexture(mi.diffuseMap)
      material.setTexture("ColorMap", diffuseMap)
    } else {
      material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md")
      material.setName("shaded material")
      material.setBoolean("UseMaterialColors", true)
      //material.setBoolean("UseAlpha", false)
      material.setColor("Ambient", ambient)
      material.setColor("Diffuse", diffuse)
      material.setColor("Specular", specular)
      material.setFloat("Shininess", shininess)

      val diffuseMap = getTexture(mi.diffuseMap)
      if (diffuseMap != null)  material.setTexture("DiffuseMap", diffuseMap)
      //      if (specularMap != null) material.setTexture("SpecularMap", specularMap)
      //      if (normalMap != null)   material.setTexture("NormalMap", normalMap)
      //      if (alphaMap != null)    material.setTexture("AlphaMap", alphaMap)
    }

    if (mi.transparent){
      material.setTransparent(true)
      material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha)
      material.getAdditionalRenderState().setAlphaTest(true)
      material.getAdditionalRenderState().setAlphaFallOff(0.01f)
    }

    if (mi.doubleSided) {
      material.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off)
    }
    material
  }

  override def createScene(name: String) = {
    val node = new Node(name)
    val scene = new JmeScene(node)
    scene
  }

  override def finalizeModel(model: Model[Node], options: ModelLoadOptions = null) {
    if (optimize) {
      GeometryBatchFactory.optimize(model.node)
    }
  }

  override def finalizeScene(scene: GeometricScene[Node]) {
    scene.node.updateGeometricState()
    scene.node.updateModelBound()
  }

  def loadModel(name: String, inpath: String, options: ModelLoadOptions = null) = {
    // Replace \ with / so JME ModelKey will have correct folder (JME AssetKey looks for char 47 '/' as folder separator)
    val path = inpath.replaceAll("\\\\", "/")
    logger.info("load model " + name + " from " + path)
    val node = new Node(name)
    val modelKey = if (options.geometryPath.isDefined || options.materialsPath.isDefined) {
      new EnhancedModelKey(path, options.geometryPath.getOrElse(null), options.materialsPath.getOrElse(null))
    } else new ModelKey(path)
    val spatial = if (options.compressionExt != null) {
      val assetKey = new CompressedAssetKey(modelKey, options.compressionExt)
      assetManager.loadAsset(assetKey)
    } else {
      assetManager.loadAsset(modelKey)
    }
    node.attachChild(spatial)

    val defaultColor = if (options != null) getColor(options.defaultColor, defaultDiffuse) else defaultDiffuse
    // Fix our materials and number our meshes
    var meshIndex = 0
    val geomVisitor = new SceneGraphVisitorAdapter {
      def setToDefaultIfNotSet(material: Material, param: String,  defaultValue: ColorRGBA): Unit = {
        if (material.getParam(param) == null) {
          try {
            material.setColor(param, defaultValue)
          } catch {
            case ex: Exception => logger.warn("Error setting default material " + param, ex)
          }
        }
      }
      def setToDefaultIfZero(material: Material, param: String,  defaultValue: ColorRGBA) {
        if (material.getParam(param) != null) {
          try {
            val origValue = material.getParam(param).getValue.asInstanceOf[ColorRGBA]
            if (ColorRGBA.Black.equals(origValue)) {
              material.setColor(param, defaultValue)
            }
          } catch {
            case ex: Exception => logger.warn("Error setting default material " + param)
          }
        }
      }
      override def visit(geom: Geometry) {
        val material = geom.getMaterial
        if (material.isTransparent()) {
          geom.setQueueBucket(Bucket.Transparent)
        } else { geom.setQueueBucket(Bucket.Opaque) }
        if (options.invertTransparency)  {
          // TODO: anything to do here?
        }

        val vertexColorParam = material.getParam("VertexColor")
        if (vertexColorParam == null || vertexColorParam.getValue.asInstanceOf[Boolean] == false) {
          // Not vertex color, set diffuse
          setToDefaultIfNotSet(material, "Diffuse", defaultColor)
        }
        val diffuseColor = if (material.getParam("Diffuse") != null) {
          material.getParam("Diffuse").getValue.asInstanceOf[ColorRGBA]
        } else null
        val ambientColor = if (diffuseColor != null) {
          val amb = diffuseColor.mult(0.25f)
          setToDefaultIfNotSet(material, "Ambient", amb)
          setToDefaultIfNotSet(material, "Specular", defaultSpecular)
          amb
        } else defaultAmbient
        if (options.ignoreZeroRGBs) {
          // Check material info
          if (material.getParam("DiffuseMap") != null) {
            setToDefaultIfZero(material, "Diffuse", defaultDiffuse)
            setToDefaultIfZero(material, "Ambient", defaultAmbient)
            setToDefaultIfZero(material, "Specular", defaultSpecular)
          } else if (material.getParam("Diffuse") != null) {
            val diffuse = material.getParam("Diffuse").getValue.asInstanceOf[ColorRGBA]
            if (!ColorRGBA.Black.equals(diffuse)) {
              setToDefaultIfZero(material, "Ambient", ambientColor)
              setToDefaultIfZero(material, "Specular", defaultSpecular)
            }
          }
        }
        if (options.doubleSided) {
          material.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off)
        }
        // TODO: check order of meshes...
        geom.setUserData(UserDataConstants.MESH_INDEX, meshIndex)
        meshIndex += 1
      }
    }
    spatial.depthFirstTraversal(geomVisitor)

    val model = new Model[Node](node)
    model
  }

  override def createModel(name: String) = {
    val node = new Node(name)
    node.setUserData(UserDataConstants.MODEL_ID, name)
    val model = new Model[Node](node)
    model
  }

  override def createModelInstance(name: String, index: Int, transform: TRANSFORM) = {
    val node = createNode(name, transform)
    node.setUserData(UserDataConstants.MODEL_INDEX, index)
    new ModelInstance(node, index)
  }

  override def createNode(name: String, transform: TRANSFORM) = {
    val node = new Node(name)
    if (transform != null) {
      node.setLocalTransform(transform)
    }
    node
  }

  def matrixToTransform( m: Matrix4f ): TRANSFORM = jme3.matrixToTransform(m)
  def transformToMatrix( t: TRANSFORM ): Matrix4f = jme3.transformToMatrix(t)
  def arrayToTransform( ta: Array[Float] ): TRANSFORM = jme3.arrayToTransform(ta)
  def transformToArray(t: TRANSFORM): Array[Float] = jme3.transformToArray(t)
  def matrixToArray( m: Matrix4f): Array[Float] = jme3.matrixToArray(m)
  def arrayToMatrix( v: Array[Float]): Matrix4f = jme3.arrayToMatrix(v)

  def applyTransform(node: NODE, t: TRANSFORM): NODE = {
    node.getLocalTransform.combineWithParent(t)
    node
  }

  def setTransform(node: NODE, t: TRANSFORM): NODE = {
    node.setLocalTransform(t)
    node
  }
  def getWorldTransform(node: NODE): TRANSFORM = {
    node.getWorldTransform().clone()
  }
  def getLocalTransform(node: NODE): TRANSFORM = {
    node.getLocalTransform().clone()
  }

  def getInverse(t: TRANSFORM): TRANSFORM = jme3.getInverse(t)

  def cloneNode(node: NODE, skipChildren: Boolean) = {
    val res = node.clone().asInstanceOf[NODE]
    if (skipChildren) {
      // remove children from clone
      val children = new scala.collection.mutable.ArrayBuffer[Spatial]()
      val visitor = new SceneGraphVisitor() {
        override def visit(s: Spatial) {
          if (res != s && s.getUserData(UserDataConstants.MODEL_INDEX) != null) {
            children.append(s)
          }
        }
      }
      res.depthFirstTraversal(visitor)
      for (c <- children) {
        c.removeFromParent()
      }
    }
    res
  }

  def attachChild(node: NODE, child: NODE) = {
    node.attachChild(child)
    node
  }

  def attachMesh(node: NODE, child: MESH) = {
    node.attachChild(child)
    node
  }

  def createMesh(params: Map[String,Any]) = {
    val geometry = params.getOrElse("geometry", null)
    val material = params.get("material").map( m => m.asInstanceOf[Material] ).getOrElse(null)
    val index = params.get("index").map( x => x.asInstanceOf[Int] )
    if (geometry != null && geometry.isInstanceOf[UTF8Decoder.Mesh]) {
      val mesh = createMesh(geometry.asInstanceOf[UTF8Decoder.Mesh], material)
      if (index.isDefined) mesh.setUserData(UserDataConstants.MESH_INDEX, index.get)
      mesh
    } else {
      null
    }
  }

  private def createMesh(decodedMesh: UTF8Decoder.Mesh, material: Material) = {
    val geometry = new Geometry(decodedMesh.getName)
    if (material != null) {
      geometry.setMaterial(material)
    }
    val mesh = new Mesh()
    mesh.setMode(Mode.Triangles)
    geometry.setMesh(mesh)

    mesh.setBuffer(VertexBuffer.Type.Index, 3, decodedMesh.getIndices)
    mesh.setBuffer(VertexBuffer.Type.Position, 3, decodedMesh.getVertices)
    mesh.setBuffer(VertexBuffer.Type.Normal, 3, decodedMesh.getNormals)
    mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, decodedMesh.getUVs)

    mesh.setStatic()
    mesh.updateBound()
    mesh.updateCounts()

    geometry
  }
}
