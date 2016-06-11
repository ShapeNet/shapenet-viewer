package edu.stanford.graphics.shapenet.jme3

import com.jme3.app.Application
import com.jme3.math._
import edu.stanford.graphics.shapenet.common.Transform
import edu.stanford.graphics.shapenet.data.DataManager
import edu.stanford.graphics.shapenet.{UserDataConstants, Constants}
import edu.stanford.graphics.shapenet.common._
import edu.stanford.graphics.shapenet.colors._
import edu.stanford.graphics.shapenet.jme3.asset.{MyUrlLocator, MyFileLocator}
import edu.stanford.graphics.shapenet.jme3.geom.BoundingBoxUtils
import edu.stanford.graphics.shapenet.jme3.loaders.{AssetLoader, LoadProgress, LoadProgressListener, LoadFormat}
import edu.stanford.graphics.shapenet.jme3.viewer.{FalseColorGenerator, BasicCameraPositioner}
import edu.stanford.graphics.shapenet.util.ConversionUtils._
import edu.stanford.graphics.shapenet.util.Loggable
import com.jme3.asset.{DesktopAssetManager, AssetManager}
import com.jme3.bounding.{BoundingSphere, BoundingBox, BoundingVolume}
import com.jme3.collision.{CollisionResult, CollisionResults}
import com.jme3.light.{AmbientLight, DirectionalLight}
import com.jme3.material.Material
import com.jme3.material.RenderState.{BlendMode, FaceCullMode}
import com.jme3.renderer.queue.RenderQueue.Bucket
import com.jme3.renderer.Camera
import com.jme3.scene._
import com.jme3.scene.debug.{Arrow, WireBox}
import com.jme3.scene.shape.{Sphere, Box}
import com.jme3.scene.Spatial.CullHint
import com.jme3.system.{AppSettings, JmeSystem}
import com.jme3.util.BufferUtils
import java.io.File
import java.net.{MalformedURLException, URL}
import java.nio.FloatBuffer
import scala.collection.mutable
import scala.collection.JavaConversions._

import scala.util.matching.Regex

/**
 * Interface to an underlying graphics engine
 * Some issues when using jmonkeyengine
 *  - FlyByCamera: rise/fall assumes Y is up
 *                 workaround: always rotate scene so Y is up
 *  - BoundingBox.merge: the local (this) bb is changed, returned bbox is always centered at 0
 *                       workaround: use mergeLocal
 *  - Matrix4f.toRotationQuat: Doesn't normalize rotation matrix before getting quaternion
 *                       workaround: see matrixToTransform
 *  - Spatial.collidesWith: Doesn't handle spatial to spatial collisions
 *  - Spatial.rotate(A): if existing rotation is R, and resulting rotation is RA (not AR as would be expected)
 * Jme Transform Matrix M for object to world:
 *   - M = T R S
 * Jme Matrix has methods to populate float array as column or row major
 *   - constructors - mostly rowMajor (except for the one that takes in a float array - that one is columnMajor)
 *   - set/get - rowMajor is the boolean
 *   - fillFloatArray - columnMajor is the boolean
 * @author Angel Chang
 **/
class Jme(val assetManager: AssetManager,
          val dataManager: DataManager,
          modelCacheSize: Option[Int] = None,
          var defaultLoadFormat: Option[LoadFormat.Value] = None,
          val alwaysClearCache: Boolean = true) extends JmeUtils with Loggable {
  lazy val assetCreator = new JmeAssetCreator(assetManager)
  lazy val assetLoader = new AssetLoader(assetCreator,
    dataManager = dataManager,
    modelCacheSize = modelCacheSize,
    defaultLoadFormat = defaultLoadFormat)
  lazy val userData = assetCreator.userData
  lazy val cameraPositioner = new BasicCameraPositioner(worldUp, userData)

  /** Helpers for loading models and scenes */
  class SceneLoadProgressListener(val listener: LoadProgressListener[GeometricScene[Node]]) extends LoadProgressListener[assetLoader.assetCreator.SCENE] {
    override def onProgress(progress: LoadProgress[assetLoader.assetCreator.SCENE]) {
      listener.onProgress(progress.asInstanceOf[LoadProgress[GeometricScene[Node]]])
    }
    override def onDone(result: assetLoader.assetCreator.SCENE) {
      listener.onDone(result.asInstanceOf[GeometricScene[Node]])
    }
  }
  implicit def scene3DLoadProgressListenerConvert(in: LoadProgressListener[GeometricScene[Node]]) =
    if (in != null) new SceneLoadProgressListener(in) else null

  def clearCache(clearAssetCache: Boolean = false) {
    if (assetManager.isInstanceOf[DesktopAssetManager] && alwaysClearCache) {
      assetManager.asInstanceOf[DesktopAssetManager].clearCache()
    }
    if (clearAssetCache) {
      assetLoader.clearCache()
    }
  }
  def loadModel(modelId: String) = assetLoader.loadModel(modelId).asInstanceOf[Model[Node]]
  def loadModelAsScene(modelId: String, transformMatrix: Matrix4f = null, listener: LoadProgressListener[GeometricScene[Node]] = null) = {
    clearCache()
    val transform = if (transformMatrix != null) Transform(matrixToArray(transformMatrix)) else Transform()
    assetLoader.loadModelAsScene(modelId, transform, listener).asInstanceOf[GeometricScene[Node]]
  }
  def loadModelAsAlignedScene(modelId: String, transformMatrix: Matrix4f = null) = {
    clearCache()
    val transform = if (transformMatrix != null) Transform(matrixToArray(transformMatrix)) else Transform()
    val scene = assetLoader.loadModelAsScene(modelId, transform).asInstanceOf[GeometricScene[Node]]
    alignScene(scene)
    scaleScene(scene)
    updateScene(scene)
    scene
  }
  def loadScene(s: (String or Scene),
                useSupportHierarchy: Boolean = assetLoader.defaultUseSupportHierarchy,
                listener: LoadProgressListener[GeometricScene[Node]] = null) = {
    clearCache()
    s match {
      //case Left(sceneId) => assetLoader.loadScene(sceneId, useSupportHierarchy, listener).asInstanceOf[GeometricScene[Node]]\
      case Left(sceneId) => ???
      case Right(scene) => assetLoader.loadScene(scene, useSupportHierarchy, listener).asInstanceOf[GeometricScene[Node]]
    }
  }
  def loadAlignedScene(s: (String or Scene),
                useSupportHierarchy: Boolean = assetLoader.defaultUseSupportHierarchy) = {
    val scene = loadScene(s, useSupportHierarchy)
    alignScene(scene)
    scaleScene(scene)
    updateScene(scene)
    scene
  }
  def createScene(modelInstances: ModelInstance[Node]*): GeometricScene[Node] = {
    clearCache()
    val scene = assetCreator.createScene("Scene")
    for (mi <- modelInstances) {
      // NOTE: modelinstance index will be changed!!!
      scene.insert(mi)
    }
    scene
  }

  /**
   * Set material for this spatial
   * @param s - Spatial for which materials should be set
   * @param material - New material to use
   * @param saveOldMaterial - Saves the old material away
   * @param recursive - Recurse into other model instances
   * @param blendOld - Percentage of old material to keep
   */
  def setMaterials(s: Spatial, material: Material, saveOldMaterial: Boolean = false, recursive: Boolean = false, blendOld: Float = 0.0f) {
    val visitor = getGeomVisitor(
      geomVisitor = (geom: Geometry) => {
        if (saveOldMaterial) {
          val oldMaterials = userData.getOrElseUpdate[mutable.Stack[Material]](
            geom, UserDataConstants.ORIG_MATERIALS, new mutable.Stack[Material]())
          oldMaterials.push(geom.getMaterial)
        }
        if (blendOld > 0.0) {
          val newMat = material.clone()
          val oldTextureParam = geom.getMaterial.getTextureParam("DiffuseMap")
          if (oldTextureParam != null) {
            newMat.setTexture("DiffuseMap", oldTextureParam.getTextureValue)
          }
          //val blendNew = 1.0 - blendOld
          val blendColors = Seq("Color", "Diffuse", "Ambient")
          for (p <- blendColors) {
            val pnew = newMat.getParam(p)
            val pold = geom.getMaterial.getParam(p)
            if (pnew != null && pold != null) {
              val vnew = pnew.getValue.asInstanceOf[ColorRGBA]
              val vold = pold.getValue.asInstanceOf[ColorRGBA]
              val colornew = new ColorRGBA()
              colornew.interpolateLocal(vnew, vold, blendOld)
              newMat.setColor(p, colornew)
            }
          }
          geom.setMaterial(newMat)
        } else {
          geom.setMaterial(material)
        }
      },
      maxDepth = if (recursive) -1 else 1
    )_
    depthFirstTraversalForModelInstanceNodes(s, visitor)
  }

  def revertMaterials(s: Spatial, recursive: Boolean = false) {
    val visitor = getGeomVisitor(
      geomVisitor = (geom: Geometry) => {
        val oldMaterials = userData.getOrElse[mutable.Stack[Material]](
          geom, UserDataConstants.ORIG_MATERIALS, null)
        if (oldMaterials != null && !oldMaterials.isEmpty) {
          val material = oldMaterials.pop()
          geom.setMaterial(material)
        }
      },
      maxDepth = if (recursive) -1 else 1
    )_
    depthFirstTraversalForModelInstanceNodes(s, visitor)
  }

  /** Functions for creating materials **/

  def getWireFrameMaterial(r: Double, g: Double, b: Double): Material = {
    getWireFrameMaterial( new ColorRGBA(r.toFloat,g.toFloat,b.toFloat,1.0f))
  }

  def getWireFrameMaterial(color: ColorRGBA): Material = {
    val mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
    mat.getAdditionalRenderState().setWireframe(true)
    mat.setColor("Color", color)
    mat
  }

  def getWireFrameMaterial(colorId: Int): Material = {
    val c = getColor(colorId)
    getSimpleFalseColorMaterial(c.getRed/255.0, c.getGreen/255.0, c.getBlue/255.0)
  }

  def getSimpleFalseColorMaterial(r: Double, g: Double, b: Double, opacity: Double = 1.0): Material = {
    val diffuse = Array(r,g,b)
    val materialInfo = new MaterialInfo(
      diffuse = diffuse,
      opacity = opacity,
      ambient = diffuse.map( x => x/4),
      specular = Array(0.18, 0.18, 0.18),
      shininess = 64
    )
    assetCreator.createMaterial(materialInfo)
  }

  def getSimpleFalseColorMaterial(color: ColorRGBA): Material = {
    getSimpleFalseColorMaterial( color.r, color.g, color.b, color.a )
  }

  def getSimpleFalseColorMaterial(id: Int): Material = {
    val c = getColor(id)
    getSimpleFalseColorMaterial(c.getRed/255.0, c.getGreen/255.0, c.getBlue/255.0)
  }

  def getFlatMaterial(): Material = {
    // No coloring
    new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md")
  }

  def getFlatFalseColorMaterial(r: Double, g: Double, b: Double): Material = {
    val diffuse = Array(r,g,b)
    val materialInfo = new MaterialInfo(
      diffuse = diffuse,
      shadeless = true
    )
    assetCreator.createMaterial(materialInfo)
  }

  def getFlatFalseColorMaterial(color: ColorRGBA): Material = {
    val diffuse = color.getColorArray.take(3).map( x => x.toDouble )
    getFlatFalseColorMaterial( diffuse(0), diffuse(1), diffuse(2))
  }

  def getFlatFalseColorMaterial(id: Int): Material = {
    val c = getColor(id)
    getFlatFalseColorMaterial(c.getRed/255.0, c.getGreen/255.0, c.getBlue/255.0)
  }

  def bbToSpatial(bb: BoundingBox, wirebox: Boolean = false, color: ColorRGBA = ColorRGBA.White): Spatial = {
    if (wirebox) {
      val bbNode = WireBox.makeGeometry(bb)
      val mat = getWireFrameMaterial(color)
      bbNode.setMaterial(mat)
      bbNode.setLocalTranslation(bb.getCenter)
      updateNode(bbNode)
      bbNode
    } else {
      val box = new Box(bb.getXExtent, bb.getYExtent, bb.getZExtent)
      val bbNode = new Geometry("bb",box)
      val mat = getFlatFalseColorMaterial(color)
      bbNode.setMaterial(mat)
      bbNode.setLocalTranslation(bb.getCenter)
      updateNode(bbNode)
      bbNode
    }
  }

  def createBall(name: String, position: Vector3f, radius: Float, colorId: Int = 0): Geometry = {
    val sphere = new Sphere(32,32, radius)
    val geo = new Geometry(name, sphere)
    geo.setLocalTranslation(position)
    val mat = getFlatFalseColorMaterial(colorId)
    geo.setMaterial(mat)
    geo
  }

  def createArrow(name: String, position: Vector3f, dir: Vector3f, lineWidth: Float, colorId: Int = 0): Geometry = {
    val arrow = new Arrow(dir)
    arrow.setLineWidth(lineWidth); // make arrow thicker
    val g = new Geometry(name, arrow)
    val mat = getWireFrameMaterial(colorId)
    g.setMaterial(mat)
    g.setLocalTranslation(position)
    g
  }
}

trait JmeUtils {
  // Which direction should we use for up
  //  we do either y (jmonkeyengine default)
  //   (what the wss models are modeled with z up - to have y model up, properly should convert the models)
  //  NOTE: much code assumes y is up
  val worldLeft = Vector3f.UNIT_X.negate()
  val worldRight = Vector3f.UNIT_X
  val worldUp = Vector3f.UNIT_Y
  val worldDown = Vector3f.UNIT_Y.negate()
  val worldFront = Vector3f.UNIT_Z.negate()
  val worldBack = Vector3f.UNIT_Z

  val defaultSemanticLeft = Constants.SEMANTIC_LEFT
  val defaultSemanticUp = Constants.SEMANTIC_UP
  val defaultSemanticFront = Constants.SEMANTIC_FRONT

  // We will work assuming 1 virtual unit = 1 cm
  //  (most of the objects we are concerned with are best measured in cms)

  // How much to multiply stored units (in meters) by to get virtual units (cms)
  //  and vice versa
  val metersToVirtualUnit = 100
  val virtualUnitToMeters = 0.01

  def removeControls(spatial: Spatial) = {
    val visitor = new SceneGraphVisitor() {
      def visit(s: Spatial) {
        val controls = for (i <- 0 until spatial.getNumControls) yield {
          spatial.getControl(i)
        }
        for (c <- controls) {
          spatial.removeControl(c)
        }
      }
    }
    spatial.depthFirstTraversal(visitor)
  }

  def toCameraInfo(cam: Camera, name: String): CameraInfo = {
    val position = cam.getLocation
    val up = cam.getUp
    // The target cannot be retrieved from the camera
    // (we can only get the direction)
    val direction = cam.getDirection
    CameraInfo(name, position, up, direction = direction)
  }

  def transformCameraInfo(camInfo: CameraInfo, rotMatrix: Matrix3f, scale: Float = 1.0f): CameraInfo = {
    def transform(v: Vector3f): Vector3f = {
      if (v != null) rotMatrix.mult(v).mult(scale) else null
    }
    def rotate(v: Vector3f): Vector3f = {
      if (v != null) rotMatrix.mult(v) else null
    }
    camInfo.copy(
      position = transform(camInfo.position),
      up = rotate(camInfo.up),
      direction = rotate(camInfo.direction),
      target = transform(camInfo.target)
    )
  }

  def transformCameraStateWithUpFrontToWorld(camInfo: CameraInfo, up: Vector3f, front: Vector3f, scale: Float = 1.0f): CameraInfo = {
    if (camInfo != null) {
      val matrix = getAlignToUpFrontAxesMatrix3f(up, front, worldUp, worldFront)
      transformCameraInfo(camInfo, matrix, scale)
    } else null
  }

  def transformCameraInfoFromSceneToWorld(camInfo: CameraInfo, scene: Scene): CameraInfo = {
    if (camInfo != null) {
      val matrix = getAlignToUpFrontAxesMatrix3f(scene.up, scene.front, worldUp, worldFront)
      val scale = scene.unit * metersToVirtualUnit
      transformCameraInfo(camInfo, matrix, scale.toFloat)
    } else null
  }

  def transformCameraInfoFromWorldToScene(camInfo: CameraInfo, scene: Scene): CameraInfo = {
    if (camInfo != null) {
      val matrix = getAlignToUpFrontAxesMatrix3f(worldUp, worldFront, scene.up, scene.front)
      val scale = 1.0 / (scene.unit * metersToVirtualUnit)
      transformCameraInfo(camInfo, matrix, scale.toFloat)
    } else null
  }

  def addDefaultLights(root: Node, target: Node = null, camera: Camera = null,
                       color: ColorRGBA = ColorRGBA.White): DirectionalLight = {
    val ambient = new AmbientLight()
    ambient.getColor.set(color).multLocal(0.4f)
    root.addLight(ambient)

    val secondSun = new DirectionalLight() //new PointLight()
    secondSun.setDirection(new Vector3f(-0.5f,0.5f,-0.5f).normalizeLocal())
    secondSun.getColor.set(color).multLocal(0.5f)
    root.addLight(secondSun)
    val targetSun = new DirectionalLight() //new PointLight()
    targetSun.setDirection(new Vector3f(0.5f,-1.0f,0.5f).normalizeLocal())
    targetSun.getColor.set(color)
    root.addLight(targetSun)
    targetSun
  }

 // Returns scene graph visitor that will do something for geometry nodes up to the maximum depth
 def getGeomVisitor(geomVisitor: Geometry => _, maxDepth: Int = -1)(s: Spatial, depth: Int): Boolean = {
   s match {
     case g:Geometry => { geomVisitor(g); true }
     case n:Node => {
       if (maxDepth > 0 && depth > maxDepth) false
       else true
     }
     case _ => true
   }
  }

  // Returns true if a spatial corresponds to a model instance node
  def isModelInstance(s: Spatial) = (s != null) && (s.getUserData(UserDataConstants.MODEL_INDEX) != null)

  /**
   * Returns the model instance index associated with a spatial
   * Returns -1 if the node is not associated with a model instance
   * @param s 
   * @return
   */
  def getModelInstanceIndex(s: Spatial) = {
    if (s == null) -1
    else Option(s.getUserData(UserDataConstants.MODEL_INDEX)).getOrElse(-1)
  }
  def getAncestorModelInstanceIndices(node: Spatial): Seq[Int] = {
    val parentIndices = new mutable.ArrayBuffer[Int]
    var s = node
    while (s != null) {
      if ((node != s) && isModelInstance(s)) {
        val i = Option(s.getUserData(UserDataConstants.MODEL_INDEX)).getOrElse(-1)
        if (i >= 0) parentIndices.append(i)
      }
      s = s.getParent
    }
    parentIndices.toSeq
  }

  /**
   * Returns the sequence of nodes associated with model instances
   *  found under this spatial
   * @param s
   * @return
   */
  def getModelInstanceNodes(s: Spatial): IndexedSeq[Node] = {
    val buf = new mutable.ArrayBuffer[Node]
    for ((i,s) <- getModelInstanceNodesMap(s)) {
      for (j <- buf.length to i) {
        buf+=null
      }
      buf(i) = s
    }
    buf.toIndexedSeq
  }
  def getModelInstanceNodesMap(s: Spatial): Map[Int,Node] = {
    val map = new mutable.HashMap[Int, Node]
    def visitNode(x: Spatial, depth: Int) = {
      x match {
        case node:Node => {
          val i = getModelInstanceIndex(node)
          if (i >= 0) map.put(i, node)
        }
        case _ => {}
      }
      true
    }
    depthFirstTraversalForModelInstanceNodes(s, visitNode)
    map.toMap
  }
  def getModelInstanceNode(start: Spatial): Spatial = {
    var s = start
    var selectedModelInstNode: Spatial = null
    while ((s != null) && selectedModelInstNode == null) {
      if (isModelInstance(s)) {
        selectedModelInstNode = s
      } else {
        s = s.getParent
      }
    }
    selectedModelInstNode
  }

  def filterOutChildModels(nodes: Seq[Spatial]): Seq[Spatial] = {
    val modelIndices = nodes.map( n => getModelInstanceIndex(n) ).toSet
    val res = new mutable.ArrayBuffer[Spatial](nodes.size)
    for (n <- nodes) {
      val ancestors = getAncestorModelInstanceIndices(n)
      val isChild = ancestors.exists( a => modelIndices.contains(a) )
      if (!isChild) {
        res.append(n)
      }
    }
    res.toSeq
  }

  // Infer target by finding first object in the camera direction and picking the center
  def inferTarget(camera: Camera, shootable: Node): CollisionResult = {
    val ray = new Ray(camera.getLocation, camera.getDirection)
    val results = new CollisionResults()
    shootable.collideWith(ray, results)
    val result = getClosestCollision(ray, results)
    result
  }

  /**
   * Does depth first traversal of a scene graph starting at the specified spatial
   *  where depth is measured by the number of model instances
   * @param s Spatial at which the traversal should start
   * @param visitor What to do at each node traversed 
   *                Input is the spatial being visited and the depth of the spatial
   *                Returns true if traversal should continue
   * @param depth The current depth of the node being traversed
   */
  def depthFirstTraversalForModelInstanceNodes(s: Spatial, visitor: (Spatial,Int) => Boolean, depth: Int = 0) {
    val isModelRoot = isModelInstance(s)
    val d = if (isModelRoot) depth+1 else depth
    val processChildren = visitor( s, d )
    if (processChildren) {
      s match {
        case node:Node => {
          for ( child <- node.getChildren) {
            depthFirstTraversalForModelInstanceNodes( child, visitor, d )
          }
        }
        case _ => {}
      }
    }
  }

  def depthFirstTraversal(s: Spatial, visitor: (Spatial,Int) => Boolean, depth: Int = 0) {
    val d = depth+1
    val processChildren = visitor( s, d )
    if (processChildren) {
      s match {
        case node:Node => {
          for ( child <- node.getChildren) {
            depthFirstTraversal( child, visitor, d )
          }
        }
        case _ => {}
      }
    }
  }

  def depthFirstTraversal[T](s: Spatial, visitor: (Spatial,T) => Boolean, data: T, incr: (Spatial,T) => T) {
    val d = incr(s,data)
    val processChildren = visitor( s, d )
    if (processChildren) {
      s match {
        case node:Node => {
          for ( child <- node.getChildren) {
            depthFirstTraversal( child, visitor, d, incr )
          }
        }
        case _ => {}
      }
    }
  }

  /** Alignment functions **/

  def alignScene(scene: GeometricScene[Node]) {
    alignLocalToUpFrontAxes(scene.node, scene.scene.up, scene.scene.front, worldUp, worldFront)
  }

  def scaleScene(scene: GeometricScene[Node]) {
    val scale = scene.scene.unit * metersToVirtualUnit
    scene.node.setLocalScale(scale.toFloat)
  }

  def unscaleScene(scene: GeometricScene[Node]) {
    val scale = 1.0f
    scene.node.setLocalScale(scale)
  }

  // Realigns scene to scene up and front
  def clearAlignment(scene3D: GeometricScene[Node]) {
    scene3D.node.setLocalRotation(Matrix3f.IDENTITY)
    updateNode(scene3D.node)
  }

  def updateScene(scene: GeometricScene[Node]) {
    updateNode(scene.node)
  }

  def updateNode(node: Spatial) {
    node.updateGeometricState()
    node.updateModelBound()
  }

  def bbToSpatialNoMaterial(bb: BoundingBox, wirebox: Boolean = false): Spatial = {
    if (wirebox) {
      val bbNode = WireBox.makeGeometry(bb)
      bbNode.setLocalTranslation(bb.getCenter)
      updateNode(bbNode)
      bbNode
    } else {
      val box = new Box(bb.getXExtent, bb.getYExtent, bb.getZExtent)
      val bbNode = new Geometry("bb",box)
      bbNode.setLocalTranslation(bb.getCenter)
      updateNode(bbNode)
      bbNode
    }
  }

  def toBoundingBox(bound: BoundingVolume): BoundingBox = {
    bound match {
      case x:BoundingBox => x
      case x:BoundingSphere => new BoundingBox(x.getCenter, x.getRadius, x.getRadius, x.getRadius)
    }
  }

  def getFastBoundingBox(targets: Spatial*): BoundingBox = {
    var bb: BoundingVolume = null
    for (target <- targets) {
      val b = target.getWorldBound
      if (bb == null) {
        if (b != null) bb = b.clone()
      }
      // FIXME: bb.merge(b) always returns bbbox with 0 extent at origin
      else bb = bb.mergeLocal(b)
    }
    if (bb != null) toBoundingBox(bb)
    else null
  }

  def getScreenBoundingBox(camera: Camera, target: Spatial): BoundingBox = {
    val transform: Vector3f => Vector3f = x => camera.getScreenCoordinates(x)
    getComputedBoundingBox(transform, target)
  }

  def getScreenBoundingBox(camera: Camera, targets: Spatial*): BoundingBox = {
    val transform: Vector3f => Vector3f = x => camera.getScreenCoordinates(x)
    getComputedBoundingBox(transform, targets:_*)
  }

  def getBoundingBox(targets: Spatial*): BoundingBox = getComputedBoundingBox(transform = null, targets:_*)

  def getBoundingBox(transformMatrix: Matrix3f, targets: Spatial*): BoundingBox = {
    val transform: Vector3f => Vector3f = x => transformMatrix.mult(x)
    getComputedBoundingBox(transform, targets:_*)
  }

  def getBoundingBox(transformMatrix: Matrix4f, targets: Spatial*): BoundingBox = {
    val transform: Vector3f => Vector3f = x => transformMatrix.mult(x)
    getComputedBoundingBox(transform, targets:_*)
  }

  def getBoundingBox(transform: Vector3f => Vector3f, targets: Spatial*): BoundingBox = getComputedBoundingBox(transform, targets:_*)

  def getComputedBoundingBox(targets: Spatial*): BoundingBox = getComputedBoundingBox(transform = null, targets:_*)

  def getComputedBoundingBox(transform: Vector3f => Vector3f, targets: Spatial*): BoundingBox = {
    var bb: BoundingVolume = null
    for (target <- targets) {
      // JMonkeyEngine world bound is not very accurate.... (but do this to make sure bb updated)
      val bTemp = target.getWorldBound
      val b = computeBoundingBox(target, transform)
      if (bb == null) {
        if (b != null) bb = b.clone()
      }
      // FIXME: bb.merge(b) always returns bbbox with 0 extent at origin
      else bb = bb.mergeLocal(b)
    }
    if (bb != null) toBoundingBox(bb)
    else null
  }

  def computeBoundingBox(spatial: Spatial,
                         transform: Vector3f => Vector3f = null,
                         includeChildren: Boolean = false): BoundingBox = {
    var res: BoundingBox = null
    def visitGeom = (geom:Geometry) => {
      val bb = computeBoundingBox(geom, transform)
      if (bb != null) {
        if (res == null) res = bb
        else res.mergeLocal(bb)
      }
    }
    val maxDepth = if (includeChildren) -1 else 1
    val visitor = getGeomVisitor(visitGeom, maxDepth)_
    depthFirstTraversalForModelInstanceNodes(spatial, visitor)
    res
  }

  def computeBoundingBox(geom: Geometry): BoundingBox = {
    val vertices = getWorldVertices(geom)
    computeBoundingBox(vertices)
  }

  def computeBoundingBox(geom: Geometry, transform: Vector3f => Vector3f): BoundingBox = {
    var vertices = getWorldVertices(geom)
    if (transform != null) {
      vertices = vertices.map( x => transform(x) )
    }
    computeBoundingBox(vertices)
  }

  def computeBoundingBox(vertices: Seq[Vector3f]): BoundingBox = {
    if (vertices.nonEmpty) {
      val bb = new BoundingBox()
      val min = Vector3f.POSITIVE_INFINITY.clone()
      val max = Vector3f.NEGATIVE_INFINITY.clone()
      for (v <- vertices) {
        if (v.getX < min.getX) min.setX(v.getX)
        if (v.getY < min.getY) min.setY(v.getY)
        if (v.getZ < min.getZ) min.setZ(v.getZ)
        if (v.getX > max.getX) max.setX(v.getX)
        if (v.getY > max.getY) max.setY(v.getY)
        if (v.getZ > max.getZ) max.setZ(v.getZ)
      }
      bb.setMinMax(min,max)
      bb
    } else null
  }

  // For now, just return volume of bbbox
  def getVolume(node: Spatial): Float = {
    node.getWorldBound.getVolume
  }

  def recenter(node: Spatial, centerTo: Vector3f = Vector3f.ZERO,
               bbBoxPointToCenter: Vector3f = HALF_XYZ) {
    val bb = getBoundingBox(node)
    val bbmin = bb.getMin(null)
    val bbmax = bb.getMax(null)
    val smin = new Vector3f(1.0f, 1.0f, 1.0f)
    smin.subtractLocal(bbBoxPointToCenter)
    smin.multLocal(bbmin)
    val shift = bbBoxPointToCenter.mult(bbmax)
    shift.addLocal(smin).negateLocal()

    node.move(shift)
    node.move(centerTo)
  }

  def alignLocalToUpFrontAxes(v: Vector3f, objectUp: Vector3f, objectFront: Vector3f,
                              targetUp: Vector3f, targetFront: Vector3f) {
    // Figure out what transform to apply to matrix
    val rotation = getAlignToUpFrontAxesMatrix3f(objectUp, objectFront, targetUp, targetFront)
    // Apply rotation to vector
    rotation.multLocal(v)
  }

  def alignLocalToUpFrontAxes(node: Spatial, objectUp: Vector3f, objectFront: Vector3f,
                              targetUp: Vector3f, targetFront: Vector3f) {
    // Figure out what transform to apply to matrix
    val rotation = getAlignToUpFrontAxesMatrix3f(objectUp, objectFront, targetUp, targetFront)
    // Apply rotation to node
    node.setLocalRotation(rotation)
  }

  def getAlignToUpFrontAxesMatrix3f(objectUp: Vector3f, objectFront: Vector3f,
                                    targetUp: Vector3f, targetFront: Vector3f) = {
    // Figure out what transform to apply to matrix
    val objM = axisPairToOrthoMatrix3f(objectUp, objectFront)
    val targetM = axisPairToOrthoMatrix3f(targetUp, targetFront)
    val objMinv = objM.invert()
    val rotation = targetM.mult(objMinv)
    rotation
  }

  /**
   * Returns transformation matrix that takes objectUp to targetUp and objectFront to targetFront
   */
  def getAlignToUpFrontAxesMatrix4f(objectUp: Vector3f, objectFront: Vector3f,
                                    targetUp: Vector3f, targetFront: Vector3f) = {
    // Figure out what transform to apply to matrix
    val objM = axisPairToOrthoMatrix4f(objectUp, objectFront)
    val targetM = axisPairToOrthoMatrix4f(targetUp, targetFront)
    val objMinv = objM.invert()
    val rotation = targetM.mult(objMinv)
    rotation
  }

  /**
   * Returns transform matrix for converting to semantic space
   * @param semanticUp semanticUp in world/model space
   * @param semanticFront semanticFront in world/model space
   * @return Matrix that takes from world/model space to semantic space
   */
  def getSemanticCoordinateFrameMatrix4f(semanticUp: Vector3f, semanticFront: Vector3f) = {
    getAlignToUpFrontAxesMatrix4f(semanticUp, semanticFront, defaultSemanticUp, defaultSemanticFront)
  }

  def getSemanticCoordinateFrameMatrix3f(semanticUp: Vector3f, semanticFront: Vector3f) = {
    getAlignToUpFrontAxesMatrix3f(semanticUp, semanticFront, defaultSemanticUp, defaultSemanticFront)
  }

  /**
   * Returns a matrix for converting from world coordinates into the modelInstance's semantic coordinate frame
   * @param modelInstance
   * @return
   */
  def getSemanticCoordinateFrameMatrix4f(modelInstance: ModelInstance[Node]): Matrix4f = {
    val modelInstanceWorldUp = getWorldUp(modelInstance)
    val modelInstanceWorldFront = getWorldFront(modelInstance)
    val alignToModelUpFrontMatrix = getAlignToUpFrontAxesMatrix4f(modelInstanceWorldUp, modelInstanceWorldFront, defaultSemanticUp, defaultSemanticFront)
    val bbWorld = getBoundingBox(modelInstance.node)
    val bbWorldCenter = bbWorld.getCenter
    val translate = bbWorldCenter.negate()
    val matrixTranslate = new Matrix4f()
    matrixTranslate.setTranslation(translate)
    var matrix = alignToModelUpFrontMatrix.mult(matrixTranslate)

    // Figure out the AABB in this semantic coordinate frame
    val bbSemantic = getBoundingBox(matrix, modelInstance.node)
    val bbSemanticExtent = BoundingBoxUtils.getBBExtent(bbSemantic)
    val scale = safeDivide(Vector3f.UNIT_XYZ,bbSemanticExtent)
    val matrixScale = new Matrix4f()
    matrixScale.setScale(scale)
    matrix = matrixScale.mult(matrix)
    matrix
  }

  def getSemanticCoordinateFrameMatrix4f(objTransform: Matrix4f, objExtents: Vector3f): Matrix4f = {
    // Assumes worldUp/WorldFront same as defaultSemanticUp/defaultSemanticFront
    var matrix = objTransform.invert()
    val scale = safeDivide(Vector3f.UNIT_XYZ,objExtents)
    val matrixScale = new Matrix4f()
    matrixScale.setScale(scale)
    matrix = matrixScale.mult(matrix)
    matrix
  }

  /**
   * Returns transform matrix for converting from semantic space into world/model space
   * @param semanticUp semanticUp in world/model space
   * @param semanticFront semanticFront in world/model space
   * @return Matrix that takes from semantic space into world/model space
   */
  def getSemanticCoordinateFrameInverseMatrix4f(semanticUp: Vector3f, semanticFront: Vector3f) = {
    getAlignToUpFrontAxesMatrix4f(defaultSemanticUp, defaultSemanticFront, semanticUp, semanticFront)
  }

  def getSemanticCoordinateFrameInverseMatrix3f(semanticUp: Vector3f, semanticFront: Vector3f) = {
    getAlignToUpFrontAxesMatrix3f(defaultSemanticUp, defaultSemanticFront, semanticUp, semanticFront)
  }

  def toMatrix4f(matrix3f: Matrix3f): Matrix4f = {
    val rot = new Matrix4f(
      matrix3f.get(0,0), matrix3f.get(0,1), matrix3f.get(0,2), 0,
      matrix3f.get(1,0), matrix3f.get(1,1), matrix3f.get(1,2), 0,
      matrix3f.get(2,0), matrix3f.get(2,1), matrix3f.get(2,2), 0,
      0,0,0,1
    )
    rot
  }

  def axisPairToOrthoMatrix3f(v1: Vector3f, v2: Vector3f): Matrix3f = {
    val v1n = v1.normalize()
    val v2n = v2.normalize()
    val v3 = v1n.cross(v2n)
    val m = new Matrix3f(
      v1n.x, v2n.x, v3.x,
      v1n.y, v2n.y, v3.y,
      v1n.z, v2n.z, v3.z
    )
    m
  }

  def axisPairToOrthoMatrix4f(v1: Vector3f, v2: Vector3f): Matrix4f = {
    val v1n = v1.normalize()
    val v2n = v2.normalize()
    val v3 = v1n.cross(v2n)
    // Matrix4f is populated in rowMajor
    val m = new Matrix4f(
      v1n.x, v2n.x, v3.x, 0.0f,
      v1n.y, v2n.y, v3.y, 0.0f,
      v1n.z, v2n.z, v3.z, 0.0f,
      0.0f, 0.0f, 0.0f, 1.0f
    )
    m
  }

  def getWorldToObjectCoordinateMatrix4f(objPosition: Vector3f, objWorldUp: Vector3f, objWorldFront: Vector3f): Matrix4f = {
    // Offset so objPosition is 0,0,0
    val worldToAlignedObjRotation = getAlignToUpFrontAxesMatrix4f(objWorldUp, objWorldFront, worldUp, worldFront)
    val offset = objPosition
    val offsetMatrix4f = Matrix4f.IDENTITY.clone()
    offsetMatrix4f.set(0,3,-offset.x)
    offsetMatrix4f.set(1,3,-offset.y)
    offsetMatrix4f.set(2,3,-offset.z)
    val M = worldToAlignedObjRotation.mult(offsetMatrix4f)
    M
  }

  def getWorldToObjectCoordinateMatrix4f(node: Node, up: Vector3f, front: Vector3f): Matrix4f = {
    // Figure out the transform

    // Transform that converts from local object into world transform
    val objWorldTransform = node.getWorldTransform
    // Get rotation transform that converts from world into aligned object
    val objUp = objWorldTransform.transformVector(up, null)
    val objFront = objWorldTransform.transformVector(front, null)
    // Offset so center is at center of object
    val offset = node.getWorldBound.getCenter
    val M = getWorldToObjectCoordinateMatrix4f(offset, objUp, objFront)
    M
  }

  def getRotateAroundAxisMatrix(target: Vector3f, up: Vector3f, delta: Float) = {
    val rotmatrix = new Matrix4f()
    rotmatrix.fromAngleAxis(delta.toFloat, up)
    val transmatrix2 = new Matrix4f()
    transmatrix2.setTranslation(target)
    val transmatrix1 = new Matrix4f()
    transmatrix1.setTranslation(target.negate())
    val matrix = transmatrix2.mult(rotmatrix.mult(transmatrix1))
    matrix
  }

  def getRotateAroundAxisMatrix(up: Vector3f, delta: Float) = {
    val rotmatrix = new Matrix4f()
    rotmatrix.fromAngleAxis(delta.toFloat, up)
    rotmatrix
  }

  // Rotates node around the center of the node, using the worldAxisDir as the direction for the axis of rotation
  def rotate(node: Node, delta: Float, worldAxisDir: Vector3f) {
    // Get rotation matrix in world space
    val bb = getBoundingBox(node)
    val worldCenter = bb.getCenter
    val rotMatrix = getRotateAroundAxisMatrix(worldCenter, worldAxisDir, delta)
    // Get new transform matrix
    // M' M_p M_l = M_p M_l'
    // M_p^{-1} M' M_p M_l = M_l'
    val worldTransform = node.getLocalToWorldMatrix(null)
    val parentTransformInv = if (node.getParent != null) {
      node.getParent.getLocalToWorldMatrix(null).invertLocal()
    } else Matrix4f.IDENTITY
    val m = parentTransformInv.mult(rotMatrix.mult(worldTransform))
    val t = matrixToTransform(m)
    node.setLocalTransform(t)
  }

  def getClosestCollision(ray: Ray, collisions: CollisionResults): CollisionResult = {
    var closest: CollisionResult = null
    val negRayNorm = ray.getDirection.normalize().negateLocal()
    var closestNormSim = 0.0f
    for (i <- 0 until collisions.size) {
      val collision = collisions.getCollision(i)
      if (closest != null && collision.getDistance > closest.getDistance) {
        return closest
      } else {
        // Compare normals
        val normSim = negRayNorm.dot(collision.getContactNormal)
        if (closest == null || normSim > closestNormSim) {
          closestNormSim = normSim
          closest = collision
        }
      }
    }
    closest
  }

  def nearestCollision(s: Spatial, v: Vector3f, dir: Vector3f): CollisionResult = {
    // 1. Aim the ray from point to dir.
    val ray = new Ray(v, dir)
    // 2. Collect intersections between Ray and spatial in results list.
    val results = new CollisionResults()
    s.collideWith(ray, results)
    if (results.size() > 0) {
      results.head
    } else null
  }

  // Returns collision from v to target with some filtering
  def collisionsBetween(s: Spatial, v: Vector3f, target: Vector3f, filter: CollisionResult => Boolean = null): CollisionResults = {
    val distanceToTarget = v.distance(target)
    val myfilter = (r: CollisionResult) => {
      if (r.getDistance > ZERO_TOLERANCE && r.getDistance < distanceToTarget) {
        (filter == null || filter(r))
      } else false
    }
    collisionsFrom(s, v, target, myfilter)
  }

  // Returns collision from v to target
  def collisionsFrom(s: Spatial, v: Vector3f, target: Vector3f, filter: CollisionResult => Boolean = null): CollisionResults = {
    val dir = target.subtract(v).normalizeLocal()
    val ray = new Ray(v, dir)
    // 2. Collect intersections between Ray and spatial in results list.
    val results = new CollisionResults()
    s.collideWith(ray, results)
    if (filter != null) {
      // 3. Filter out results
      val filteredResults = new CollisionResults()
      val distanceToTarget = target.distance(v)
      for (r <- results) {
        if (r.getDistance <= distanceToTarget) {
          if (filter(r)) {
            filteredResults.addCollision(r)
          }
        }
      }
      filteredResults
    } else results
  }

  // Make sure we maintain a certain distance from the spatial s
  // Return the new point at the specified distance and the contact point
  def keepDistance(s: Spatial, v: Vector3f, target: Vector3f, d: Float): (Vector3f, Vector3f) = {
    // direction toward target
    val dir = target.subtract(v).normalizeLocal()
    val c = nearestCollision(s, v, dir)
    val curDistance =
      if (c != null) {
        c.getDistance
      } else {
        // let's just be d distance away from target point
        // (regardless of where this spatial is)
        v.distance(target)
      }
    // Need to move this much in direction toward target
    val delta = curDistance - d
    val res = v.add(dir.multLocal(delta))
    // Debugging
    val p = if (c != null) c.getContactPoint else target
//    println("selected=" + res + ", closest= " + p
//      + ", distance=" + p.distance(res) + ", goal=" + d)
    (res,p)
  }

  def getVertex(geom: Geometry, index: Int) = {
    val pb = geom.getMesh.getBuffer(VertexBuffer.Type.Position)
    val v = new Vector3f()
    if (pb != null && pb.getFormat() == VertexBuffer.Format.Float && pb.getNumComponents() == 3) {
      val fpb = pb.getData().asInstanceOf[FloatBuffer]
      val vertIndex = index*3
      BufferUtils.populateFromBuffer(v, fpb, vertIndex)
      v
    } else {
      throw new UnsupportedOperationException("Position buffer not set or "
        + " has incompatible format")
    }
  }

  def getVertices(geom: Geometry): Seq[Vector3f] = {
    val pb = geom.getMesh.getBuffer(VertexBuffer.Type.Position)
    if (pb != null && pb.getFormat() == VertexBuffer.Format.Float && pb.getNumComponents() == 3) {
      val fpb = pb.getData().asInstanceOf[FloatBuffer]
      BufferUtils.getVector3Array(fpb)
    } else {
      throw new UnsupportedOperationException("Position buffer not set or "
        + " has incompatible format")
    }
  }

  def getNormals(geom: Geometry): Seq[Vector3f] = {
    val pb = geom.getMesh.getBuffer(VertexBuffer.Type.Normal)
    if (pb != null && pb.getFormat() == VertexBuffer.Format.Float && pb.getNumComponents() == 3) {
      val fpb = pb.getData().asInstanceOf[FloatBuffer]
      BufferUtils.getVector3Array(fpb)
    } else {
      throw new UnsupportedOperationException("Normal buffer not set or "
        + " has incompatible format")
    }
  }

  def getTexCoords(geom: Geometry): Seq[Vector2f] = {
    val pb = geom.getMesh.getBuffer(VertexBuffer.Type.TexCoord)
    if (pb != null && pb.getFormat() == VertexBuffer.Format.Float && pb.getNumComponents() == 2) {
      val fpb = pb.getData().asInstanceOf[FloatBuffer]
      BufferUtils.getVector2Array(fpb)
    } else if (pb != null) {
      throw new UnsupportedOperationException("TexCoord buffer has incompatible format")
    } else null
  }

  def getWorldVertices(geom: Geometry): Seq[Vector3f] = {
    val vertices = getVertices(geom)
    val m = geom.getWorldMatrix
    vertices.map( x => m.mult(x) )
  }

  def getWorldVertices(s: Spatial): Stream[Vector3f] = {
    val geometries = getGeometries(s)
    // Get all points of s as a stream
    def _getWorldVerticesHelper(buffered: Seq[Vector3f], todo: Seq[Geometry]): Stream[Vector3f] = {
      if (buffered.nonEmpty) {
        Stream.cons(buffered.head, _getWorldVerticesHelper(buffered.tail, todo))
      } else if (todo.isEmpty) {
        Stream()
      } else {
        val geom = todo.head
        val geomVertices = getWorldVertices(geom)
        _getWorldVerticesHelper(geomVertices, todo.tail)
      }
    }
    _getWorldVerticesHelper(Seq(), geometries)
  }

  def getTriangles(geom: Geometry): Stream[Triangle] = {
    val mesh = geom.getMesh()
    def _getTrianglesStream(i: Int): Stream[Triangle] = {
      if (i >= mesh.getTriangleCount) {
        Stream()
      } else {
        val tri = new Triangle()
        mesh.getTriangle(i,tri)
        tri #:: _getTrianglesStream(i+1)
      }
    }
    _getTrianglesStream(0)
  }

  def getTransformedTriangle(t: Triangle, m: Matrix4f): Triangle = {
    val wt = new Triangle(m.mult(t.get1()), m.mult(t.get2()), m.mult(t.get3()) )
    wt.setIndex(t.getIndex())
    wt
  }

  def getWorldTriangle(geom: Geometry, triIndex: Int): Triangle = {
    val t = new Triangle()
    val m = geom.getWorldMatrix
    geom.getMesh.getTriangle(triIndex, t)
    m.mult(t.get1(), t.get1())
    m.mult(t.get2(), t.get2())
    m.mult(t.get3(), t.get3())
    t
  }

  def getWorldTriangles(geom: Geometry): Stream[Triangle] = {
    val triangles = getTriangles(geom)
    val m = geom.getWorldMatrix
    for (t <- triangles) yield {
      val wt = new Triangle(m.mult(t.get1()), m.mult(t.get2()), m.mult(t.get3()) )
      wt.setIndex(t.getIndex())
      wt
    }
  }

  def getWorldTriangles(s: Spatial): Seq[Triangle] = {
    val geometries = getGeometries(s)
    // Get all points of s as a stream
    def _getWorldTrianglesHelper(buffered: Seq[Triangle], todo: Seq[Geometry]): Stream[Triangle] = {
      if (buffered.nonEmpty) {
        Stream.cons(buffered.head, _getWorldTrianglesHelper(buffered.tail, todo))
      } else if (todo.isEmpty) {
        Stream()
      } else {
        val geom = todo.head
        val geomTriangles = getWorldTriangles(geom)
        _getWorldTrianglesHelper(geomTriangles, todo.tail)
      }
    }
    _getWorldTrianglesHelper(Seq(), geometries)
  }

  def getWorldTrianglesWithGeometry(s: Spatial): Seq[(Geometry, Triangle)] = {
    val geometries = getGeometries(s)
    // Get all points of s as a stream
    def _getWorldTrianglesHelper(buffered: Seq[(Geometry, Triangle)], todo: Seq[Geometry]): Stream[(Geometry, Triangle)] = {
      if (buffered.nonEmpty) {
        Stream.cons(buffered.head, _getWorldTrianglesHelper(buffered.tail, todo))
      } else if (todo.isEmpty) {
        Stream()
      } else {
        val geom = todo.head
        val geomTriangles = getWorldTriangles(geom).map( x => (geom, x))
        _getWorldTrianglesHelper(geomTriangles, todo.tail)
      }
    }
    _getWorldTrianglesHelper(Seq(), geometries)
  }

  // Returns all the leaf geometries of this spatial
  def getGeometriesUnordered(s: Spatial, includeChildren: Boolean = false): Seq[Geometry] = {
    val geometries = new mutable.ArrayBuffer[Geometry]()
    def visitGeom = (geom:Geometry) => {
      geometries.append(geom)
    }
    val maxDepth = if (includeChildren) -1 else 1
    val visitor = getGeomVisitor(visitGeom, maxDepth)_
    depthFirstTraversalForModelInstanceNodes(s, visitor)
    geometries.toSeq
  }

  def getGeometries(s: Spatial, includeChildren: Boolean = false): IndexedSeq[Geometry] = {
    val geometries = getGeometriesUnordered(s, includeChildren)
    geometries.sortBy( x => getMeshIndex(x) ).toIndexedSeq
  }

  def getWorldUp(modelInstance: ModelInstance[Node]) = {
    val rotate = modelInstance.node.getWorldRotation
    val up: Vector3f = if (rotate != null) rotate.mult(modelInstance.up, null) else modelInstance.up
    up.normalize()
  }

  def getWorldFront(modelInstance: ModelInstance[Node]) = {
    val rotate = modelInstance.node.getWorldRotation
    val front: Vector3f = if (rotate != null) rotate.mult(modelInstance.front, null) else modelInstance.front
    front.normalize()
  }

  // Return surface area of Triangle t
  def area(t: Triangle): Float = {
    ((t.get2 - t.get1).cross(t.get3 - t.get1) * 0.5f).length()
  }

  def getWireframeMode(spatial: Spatial): Boolean = {
    val wireframeMode = spatial.getUserData("wireframeMode").asInstanceOf[java.lang.Boolean]
    if (wireframeMode == null) false else wireframeMode
  }

  def setWireframeMode(spatial: Spatial, wireframeMode: Boolean = true) {
    spatial.setUserData("wireframeMode", wireframeMode)
    val geos = getGeometries(spatial)
    geos.foreach(g => g.getMaterial.getAdditionalRenderState.setWireframe(wireframeMode))
  }

  def getMaterialColor(material: Material, key: String): ColorRGBA = {
    val p = material.getParam(key)
    if (p != null) p.getValue.asInstanceOf[ColorRGBA]
    else null
  }

  def makeTransparent(material: Material, alpha: Float) {
    var c = getMaterialColor(material, "Color")
    if (c != null) {
      val diffuse = new ColorRGBA(c)
      diffuse.a = alpha
      material.setColor("Color", diffuse)
    } else {
      c = getMaterialColor(material, "Diffuse")
      if (c != null) {
        val diffuse = new ColorRGBA(c)
        diffuse.a = alpha
        material.setColor("Diffuse", diffuse)
      }
    }
    material.setTransparent(true)
    material.getAdditionalRenderState().setBlendMode(BlendMode.Alpha)
    material.getAdditionalRenderState().setAlphaTest(true)
    material.getAdditionalRenderState().setAlphaFallOff(0.01f)
  }

  def makeTransparent(spatial: Spatial, alpha: Float = 0.1f) {
    val visitor = new SceneGraphVisitor() {
      def visit(s: Spatial) {
        s match {
          case g:Geometry => { makeTransparent(g.getMaterial, alpha) }
          case _ => {}
        }
      }
    }
    spatial.depthFirstTraversal(visitor)
    spatial.setQueueBucket(Bucket.Transparent)
  }

  def makeDoubleSided(material: Material) {
    material.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off)
  }

  def makeDoubleSided(spatial: Spatial) {
    val visitor = new SceneGraphVisitor() {
      def visit(s: Spatial) {
        s match {
          case g:Geometry => { makeDoubleSided(g.getMaterial) }
          case _ => {}
        }
      }
    }
    spatial.depthFirstTraversal(visitor)
  }

  def setPolyOffset(material: Material, v: Float) {
    material.getAdditionalRenderState().setPolyOffset(v, 1.0f)
  }

  def setPolyOffset(spatial: Spatial, v: Float) {
    val visitor = new SceneGraphVisitor() {
      def visit(s: Spatial) {
        s match {
          case g:Geometry => { setPolyOffset(g.getMaterial, v) }
          case _ => {}
        }
      }
    }
    spatial.depthFirstTraversal(visitor)
  }

  def setVisible(spatial: Spatial, visible: Boolean = true) {
    spatial.setUserData("visible", visible)
    //val geos = getGeometries(spatial)
    if (visible) {
      spatial.setCullHint(CullHint.Dynamic)
    } else {
      spatial.setCullHint(CullHint.Always)
    }
  }


  def getMeshIndex(geom: Geometry): Int = {
    val mi = geom.getUserData(UserDataConstants.MESH_INDEX).asInstanceOf[java.lang.Integer]
    if (mi != null) mi.toInt else -1
  }

  def getMeshIndexMap(modelInstance: ModelInstance[Node]) = {
    buildMeshIndexMap(modelInstance.node)
  }

  private def buildMeshIndexMap(node: Node): Map[Int, Geometry] = {
    // build a map of mesh index to geometry
    val meshes = new mutable.HashMap[Int, Geometry]()
    val geomVisitor = (geom: Geometry) => {
      // Get meshindex
      val meshIndex = getMeshIndex(geom)
      if (!meshes.contains(meshIndex)) {
        meshes.update(meshIndex, geom)
      } else {
        throw new IllegalStateException("Error building mesh index: Duplicate mesh index " + meshIndex)
      }
    }
    val visitor = getGeomVisitor(geomVisitor, 1)_
    depthFirstTraversalForModelInstanceNodes(node, visitor)
    meshes.toMap
  }

  def lookupNodesBySceneGraphPath(root: Spatial, sgpath: String): Seq[Spatial] = {
    if (sgpath == "" || sgpath == "/") {
      return Seq(root)
    }
    val nodeRE = new Regex("(.*)\\[(\\d+)\\]")
    // traverse root and find first instance matching start and traverse from there...
    val matching = new scala.collection.mutable.ArrayBuffer[Spatial]
    var pathParts = sgpath.split("/")
    if (pathParts.head == "") pathParts = pathParts.tail
    // parse pathParts into pairs of (name,index)
    val pathPairs: Seq[(String,Int)] = pathParts.map( x => {
      x match {
        case nodeRE(name, index) => (name, index.toInt)
        case _ => (x, -1)
      }
    })
    // Look for all instances matching the name
    import scala.util.control.Breaks._
    val startNodes = findNodesByNameOrId(root, pathPairs.head._1)
    // Traverse down matching the specified path...
    for (start <- startNodes) {
      // Use index to locate child and check that the id matches
      var node = start
      var checked = 0
      breakable {
        for ((name, index) <- pathPairs) {
          // Check name
          val nodeId: String = node.getUserData(UserDataConstants.NODE_ID)
          val nodeName = if (nodeId != null) nodeId else node.getName()
          if (nodeName != name) {
            break
          }
          checked = checked + 1
          // Get child
          if (index >= 0 && node.isInstanceOf[Node]) {
            node = node.asInstanceOf[Node].getChild(index)
          } else {
            break
          }
        }
      }
      if (checked == pathPairs.size) {
        matching.append(node)
      }
    }
    matching.toSeq
  }

  def findNodesByName(root: Spatial, name: String): Seq[Spatial] = {
    findNodes(root, s => s.getName() == name)
  }

  def findNodesByNameOrId(root: Spatial, name: String): Seq[Spatial] = {
    findNodes(root, s => s.getUserData[String](UserDataConstants.NODE_ID) == name || s.getName() == name)
  }

  def findNodes(root: Spatial, matchCond: Spatial => Boolean): Seq[Spatial] = {
    val matching = new scala.collection.mutable.ArrayBuffer[Spatial]
    depthFirstTraversal(root, (s,d) => {
      val m = matchCond(s)
      if (m) {
        matching.append(s)
      }
      !m
    })
    matching.toSeq
  }

  def getSceneGraphPathForCollada(node: Spatial): String = {
    val isRootParent: Spatial => Boolean = n => (n == null || n.getName() == "COLLADA SCENE")
    return getSceneGraphPath(node, isRootParent)
  }

  def getSceneGraphPath(node: Spatial, isRootParent: Spatial => Boolean = x => false): String = {
    var parent = node
    var child: Spatial = null
    var path = List[String]()
    while (parent != null && !isRootParent(parent)) {
      val id: String = parent.getUserData(UserDataConstants.NODE_ID)
      var name = if (id != null) id else parent.getName()
      if (child != null && parent.isInstanceOf[Node]) {
        val index = parent.asInstanceOf[Node].getChildIndex(child)
        name = name + "[" + index + "]"
      }
      path = name +: path
      child = parent
      parent = parent.getParent
    }
    "/" + path.mkString("/")
  }

  def buildFalseColoredSpatials(spatials: Seq[Spatial], colorGenerator: FalseColorGenerator = new FalseColorGenerator())(implicit jme: Jme): Node = {
    val coloredNode = new Node("spatials-colored")
    val colorsStart = colorGenerator.falseColors.size
    for ((s,i) <- spatials.zipWithIndex) {
      val coloredSpatial: Spatial = s.clone()
      coloredNode.attachChild(coloredSpatial)

      val (color,cint) = colorGenerator.generateColor(i+colorsStart)
      val material = jme.getFlatFalseColorMaterial(color)
      coloredSpatial.setMaterial(material)
      coloredSpatial.setLocalTransform(s.getWorldTransform)
    }
    coloredNode
  }

  def buildFalseColoredMesh(name: String, modelInstance: ModelInstance[Node],
                            meshIndex: Int, colorGenerator: FalseColorGenerator = new FalseColorGenerator())(implicit jme: Jme): Node = {
    val meshes = getMeshIndexMap(modelInstance)
    val geom = meshes.getOrElse(meshIndex, null)
    if (geom != null) {
      val colorsStart = colorGenerator.falseColors.size

      // Create a mesh
      val coloredGeom = geom.clone()
      val coloredNode = new Node(name)
      coloredNode.attachChild(coloredGeom)

      val (color,cint) = colorGenerator.generateColor(meshIndex+colorsStart)
      val material = jme.getFlatFalseColorMaterial(color)
      coloredGeom.setMaterial(material)
      coloredGeom.setLocalTransform(geom.getWorldTransform)
      coloredNode
    } else {
      null
    }
  }
  def buildFalseColoredMeshes(s: Spatial, colorGenerator: FalseColorGenerator = new FalseColorGenerator())(implicit jme: Jme): Node = {
    val colorsStart = colorGenerator.falseColors.size
    val coloredNode = new Node(s.getName + "-colored")
    val geoms = getGeometries(s)
    for ((geom,i) <- geoms.zipWithIndex) {
      // Create a mesh
      val coloredGeom = geom.clone()
      coloredNode.attachChild(coloredGeom)

      val (color,cint) = colorGenerator.generateColor(i+colorsStart)
      val material = jme.getFlatFalseColorMaterial(color)
      coloredGeom.setMaterial(material)
      coloredGeom.setLocalTransform(geom.getWorldTransform)
    }
    coloredNode
  }

  def buildFalseColoredMeshes(s: Spatial, material: Material)(implicit jme: Jme): Node = {
    buildFalseColoredMeshes(s, material, colorChildren = false)
  }

  def buildFalseColoredMeshes(s: Spatial, material: Material, colorChildren: Boolean)(implicit jme: Jme): Node = {
    val coloredNode = new Node(s.getName + "-colored")
    val geoms = getGeometries(s, colorChildren)
    for ((geom,i) <- geoms.zipWithIndex) {
      // Create a mesh
      val coloredGeom = geom.clone()
      coloredNode.attachChild(coloredGeom)

      coloredGeom.setMaterial(material)
      coloredGeom.setLocalTransform(geom.getWorldTransform)
    }
    coloredNode
  }

  def createAxesUpFront(center: Vector3f, up: Vector3f, front: Vector3f)(implicit jme: Jme): Node = {
    val left = up.normalize().cross(front.normalize())
    val leftScale = math.sqrt(up.lengthSquared() + front.lengthSquared())
    left.multLocal(leftScale.toFloat)
    createAxes(center, left, up, front)
  }

  def createAxes(center: Vector3f, x: Vector3f, y: Vector3f, z: Vector3f)(implicit jme: Jme): Node = {
    createAxes(center, (x, ColorRGBA.Red), (y, ColorRGBA.Green), (z, ColorRGBA.Blue))
  }

  def createAxes(center: Vector3f, axes: (Vector3f, ColorRGBA)*)(implicit jme: Jme): Node = {
    val axesNode = new Node("Axes")
    for ((axis,color) <- axes) {
      val arrow = new Arrow(axis)
      arrow.setLineWidth(4); // make arrow thicker
      val g = new Geometry("coordinate axis", arrow)
      val mat = jme.getWireFrameMaterial(color)
      g.setMaterial(mat)
      g.setLocalTranslation(center)
      axesNode.attachChild(g)
    }
    axesNode
  }

  // Move in world coordinate
  def moveTo(node: Spatial, worldTarget: Vector3f) {
    // X = M_p M_l' 0
    // M_p^{-1} X = M_l' 0 = t' R S 0
    val localTarget = if (node.getParent != null) {
      node.getParent.worldToLocal(worldTarget, null)
    } else worldTarget
    node.setLocalTranslation(localTarget)
  }

  def move(node: Spatial, worldDelta: Vector3f) {
    // TODO: This function needs to be fixed!
    // X + M_p M_l 0 = M_p M_l' 0
    // M_P^{-1} X + M_l 0 = M_l' 0
    // M_P^{-1} X = (M_l'-M_l) 0 = (t'-t) R S 0
    val localDelta = if (node.getParent != null)
      node.getParent.worldToLocal(worldDelta, null)
    else worldDelta
    node.move(localDelta)
  }

}

object JmeUtils extends JmeUtils

class JmeScene(node: Node) extends GeometricScene[Node](node) {
  override def applyTransformToScene(targetScene: Scene, undoWorldAlignAndScale: Boolean) {
    if (targetScene.objects.size != this.modelInstances.size) {
      throw new IllegalStateException("Number of objects in scene and arrangement does not match!")
    }
    val transformMatrix = if (undoWorldAlignAndScale) {
      val t = node.getWorldTransform
      transformToMatrix(t).invert()
    } else null
    for (mi <- 0 until this.modelInstances.size) {
      val m = this.modelInstances(mi)
      if (undoWorldAlignAndScale) {
        val worldMat = transformToMatrix( m.nodeSelf.getWorldTransform )
        val sceneMat = transformMatrix.mult( worldMat )
        val transformArray = matrixToArray( sceneMat )
        targetScene.objects(mi).transform.set( transformArray )
      } else {
        val transformArray = transformToArray( m.nodeSelf.getWorldTransform )
        targetScene.objects(mi).transform.set( transformArray )
      }
    }
  }

  // Removes the specified model from the scene
  def delete(modelInstIndex: Int): Unit = {
    if (modelInstances != null && modelInstIndex >= 0 && modelInstIndex < modelInstances.size) {
      val modelInst = modelInstances.remove(modelInstIndex)

      // Update scene graph
      node.detachChild(modelInst.node)
      modelInst.node.setUserData(UserDataConstants.MODEL_INDEX, -1)

      // Adjust indices for other models
      for (index <- modelInstIndex until modelInstances.size) {
        modelInst.index = index
        modelInst.node.setUserData(UserDataConstants.MODEL_INDEX, index)
      }
    }
  }

  // Add the specified model instance to the scene
  def insert(modelInst: ModelInstance[Node]): Unit = {
    if (modelInstances == null) modelInstances = new mutable.ArrayBuffer[ModelInstance[Node]]()
    val index = modelInstances.size
    modelInstances.append(modelInst)
    modelInst.index = index
    modelInst.node.setUserData(UserDataConstants.MODEL_INDEX, index)
    node.attachChild(modelInst.node)
  }

  def copy(): JmeScene = {
    val copy = new JmeScene(node.clone().asInstanceOf[Node])
    copy.scene = scene
    copy.modelInstances = new mutable.ArrayBuffer(modelInstances.size)
    val modelInstancesMap = JmeUtils.getModelInstanceNodesMap(copy.node)
    for (i <- 0 until modelInstances.size) {
      val node = modelInstancesMap.getOrElse(i, null)
      val mi = new ModelInstance(node, i)
      mi.model = modelInstances(i).model
      mi.nodeSelf = node.getChild(0).asInstanceOf[Node]
      copy.modelInstances.append(mi)
    }
    copy
  }
}

object Jme extends Loggable {
  private var _defaultJme: Jme = null

  def setDefault(jme: Jme) { _defaultJme = jme }
  def getDefault() = _defaultJme

  def initAssetManager(assetManager: AssetManager, useViewerAssets: Boolean = false, useDataDir: Boolean = Constants.USE_LOCAL_DATA, useCustomObjLoader: Boolean = false) {
    assetManager.registerLocator("/", classOf[MyFileLocator])
    if (useViewerAssets) {
      // Register locator for viewer.xml
      assetManager.registerLocator(
        Constants.SHAPENET_VIEWER_DIR, classOf[MyFileLocator])
      // Register locator for various other assets (e.g. progress bar images)
      assetManager.registerLocator(
        Constants.ASSETS_DIR, classOf[MyFileLocator])
    }
    if (useDataDir) {
      assetManager.registerLocator(Constants.DATA_DIR, classOf[MyFileLocator])
    }
    assetManager.registerLocator("/", classOf[MyUrlLocator])
    // replace the default obj loader with ours (TODO: specify in config)
    if (useCustomObjLoader) {
      assetManager.unregisterLoader(classOf[com.jme3.scene.plugins.OBJLoader])
      assetManager.registerLoader(classOf[edu.stanford.graphics.shapenet.jme3.plugins.OBJLoader], "obj")
      assetManager.unregisterLoader(classOf[com.jme3.scene.plugins.MTLLoader])
      assetManager.registerLoader(classOf[edu.stanford.graphics.shapenet.jme3.plugins.MTLLoader], "mtl")
    }
    assetManager.registerLoader(classOf[edu.stanford.graphics.shapenet.jme3.plugins.KMZLoader], "kmz")
    assetManager.registerLoader(classOf[edu.stanford.graphics.shapenet.jme3.plugins.PLYLoader], "ply")
    // TODO: textures that are relative cannot be loaded for now...
    assetManager.registerLoader(classOf[jme3dae.ColladaLoader], "dae")
  }

  def getDefaultAssetManager() = {
    val assetManager = createAssetManager()
    initAssetManager(assetManager, useCustomObjLoader = true)
    assetManager
  }
  def apply(): Jme = {
    if (_defaultJme != null) _defaultJme
    else apply(getDefaultAssetManager())
  }
  def apply(config: JmeConfig): Jme = {
    val assetManager = getDefaultAssetManager()
    new Jme(assetManager, DataManager(), config.modelCacheSize, config.defaultLoadFormat)
  }
  def apply(assetManager: AssetManager = getDefaultAssetManager(),
            modelCacheSize: Option[Int] = None,
            defaultLoadFormat: Option[LoadFormat.Value] = None,
            alwaysClearCache: Boolean = true): Jme = new Jme(assetManager, DataManager(), modelCacheSize, defaultLoadFormat, alwaysClearCache)
  private def createAssetManager(settings: AppSettings = null) = {
    var asm: AssetManager = null
    if (settings != null) {
      val assetCfg: String = settings.getString("AssetConfigURL")
      if (assetCfg != null) {
        var url: URL = null
        try {
          url = new URL(assetCfg)
        }
        catch {
          case ex: MalformedURLException => {
          }
        }
        if (url == null) {
          url = classOf[Application].getClassLoader.getResource(assetCfg)
          if (url == null) {
            logger.error("Unable to access AssetConfigURL in asset config:{0}", assetCfg)
          }
        }
        if (url != null) asm = JmeSystem.newAssetManager(url)
      }
    }
    if (asm == null) {
      asm = JmeSystem.newAssetManager(Thread.currentThread.getContextClassLoader.getResource("com/jme3/asset/Desktop.cfg"))
    }
    asm
  }

}