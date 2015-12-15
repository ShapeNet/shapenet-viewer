package edu.stanford.graphics.shapenet.common

import com.jme3.math.Vector3f
import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.data.{DataManager, ModelsDb}
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Conceptual representation of a scene
 * This is the external representation of a scene that is generated
 *   from text and passed to the viewer.
 * It should contain all information necessary to create a
 *   3D representation of the scene
 * @author Angel Chang
 */
class Scene( // Objects in the scene
             var objects: IndexedSeq[SceneObject] = IndexedSeq(),
             // Predefined camera positions
             var cameras: IndexedSeq[CameraInfo] = IndexedSeq(),
             //  val lights: IndexedSeq[Light]
             // Actual geometric of the scene
             var geometricScene: GeometricScene[_] = null
          )
{
  // Scene id
  var sceneId: String = null
  // Name of the scene
  var sceneName: String = null
  // Category of this scene
  var category: IndexedSeq[String] = null

  // Information about the scene up, front, scale
  var up: Vector3f = Constants.DEFAULT_SCENE_UP
  var front: Vector3f = Constants.DEFAULT_SCENE_FRONT
  var unit: Double = Constants.DEFAULT_SCENE_UNIT

  // For camera placement during layout
  var screenWidth: Int = Constants.screenWidth
  var screenHeight: Int = Constants.screenHeight

  def getSceneObject(objIndex: Int): SceneObject = {
    if (objIndex >= 0 && objIndex < objects.size) {
      objects(objIndex)
    } else null
  }
  def getNumberOfObjects(): Int = {
    objects.size
  }

  def hasCategory(c: String) = category != null && category.contains(c)

  override def toString = {
    val fields = new mutable.ArrayBuffer[String]()
    if (sceneId != null) fields.append("id=" + sceneId)
    if (sceneName != null) fields.append("name=" + sceneName)
    if (category != null) fields.append("category=(" + category.mkString(",") + ")")
    "Scene(" + fields.mkString(", ") + ")"
  }

  def roots() = {
    objects.filter( x => x.supportParentIndex < 0 )
  }

  def nonroots() = {
    objects.filter( x => x.supportParentIndex >= 0 )
  }

  def leaves() = {
    objects.filter( x => !x.hasChildren )
  }

  // Get the common ancestor shared by the objects
  // and for each object, find the index of self/ancestor that is
  // child of the common ancestor
  def toChildrenOfCommonAncestor(objIndices: Int*): IndexedSeq[Int] = {
    val commonAncestor = findCommonAncestor(objIndices:_*)
    val res = for (objIndex <- objIndices) yield {
      // Identify child of common ancestor that is ancestor of me
      val anc = ancestors(objIndex)
      if (commonAncestor == -1) {
        if (anc.nonEmpty) anc.last
        else objIndex
      } else {
        val i = anc.indexOf( commonAncestor )
        if (i == 0) objIndex
        else anc(i-1)
      }
    }
    res.toIndexedSeq
  }

  // Returns the common ancestor shared by the objects
  def findCommonAncestor(objIndices: Int*): Int = {
    if (objIndices.size == 0) return -1
    if (objIndices.size == 1) return objects(objIndices.head).supportParentIndex
    var potentialAncestors = ancestors(objIndices.head)
    var remaining = objIndices.tail
    while (potentialAncestors.nonEmpty && remaining.nonEmpty) {
      val objIndex = remaining.head
      val anc = ancestors(objIndex)
      potentialAncestors = potentialAncestors.filter( x => anc.contains(x) )
      remaining = remaining.tail
    }
    if (potentialAncestors.isEmpty) -1
    else potentialAncestors.head
  }

  def ancestors(objIndex: Int): IndexedSeq[Int] = {
    ancestors(objects(objIndex))
  }

  def ancestors(obj: SceneObject): IndexedSeq[Int] = {
    val ancestors = scala.collection.mutable.ArrayBuffer[Int]()
    var p = obj.supportParentIndex
    while (p != -1) {
      val pObject = objects(p)
      ancestors.append(p)
      p = pObject.supportParentIndex
    }
    ancestors.toIndexedSeq
  }

  def getMatchingDescendants(inputObjs: Seq[SceneObject], categories: Seq[String])(implicit dataManager: DataManager): Set[Int] = {
    def filter(x: SceneObject): Boolean = {
      for (c <- categories) {
        if (x.modelInfo != null && x.modelInfo.hasCategoryRelaxed(c).getOrElse(false))
          return true
      }
      false
    }
    getMatchingDescendants(inputObjs, x => filter(x))
  }


  def getMatchingDescendants(inputObjs: Seq[SceneObject], filter: SceneObject => Boolean): Set[Int] = {
    val matching = scala.collection.mutable.HashSet[Int]()
    def _getMatching(objs: Seq[SceneObject]) {
      for (obj <- objs) {
        if (filter(obj)) { matching += obj.index }
        if (obj.childIndices != null) {
          val children = obj.childIndices.toSeq.map( x => objects(x) )
          _getMatching(children)
        }
      }
    }
    _getMatching(inputObjs)
    matching.toSet
  }

  def sceneHierarchyString(implicit dataManager: DataManager): String = {
    def objectString(obj: SceneObject): String = {
      val modelInfo = obj.modelInfo
      val name = if (modelInfo != null) obj.modelInfo.name else "?"
      val cat = if (modelInfo != null) obj.modelInfo.basicCategory else "?"
      "obj" + obj.index + " (name=" + name + ", cat=" + cat + ", id=" + obj.modelID + ")"
    }
    val sb = new mutable.StringBuilder()
    val todo = new mutable.Stack[(SceneObject, String)]()
    val rts = roots()
    for (rt <- rts) {
      todo.push((rt, ""))
    }
    while (todo.nonEmpty) {
      val (obj, prefix) = todo.pop()
      val objString = objectString(obj)
      sb.append(prefix + "-" + objString + "\n")
      val childIndices = obj.childIndices
      if (childIndices != null && childIndices.nonEmpty) {
        for (ci <- childIndices) {
          val cobj = objects(ci)
          todo.push((cobj, prefix + "  "))
        }
      }
    }
    sb.toString()
  }

  def updateChildIndices() = {
    for (obj <- objects) {
      // Clear child indices
      obj.clearChildIndices()
    }
    val roots = new ArrayBuffer[Int]()
    for (i <- 0 until objects.length) {
      val obj = objects(i)
      if (obj.supportParentIndex >= 0) {
        val parent: SceneObject = objects(obj.supportParentIndex)
        parent.addChildIndex(i)
      } else {
        roots.append( i )
      }
    }
    roots.toSeq
  }

  def isValidObjectIndex(objIndex: Int): Boolean = {
    objIndex >= 0 && objIndex < objects.length
  }

  def isParentChild(objIndex1: Int, objIndex2: Int): Boolean = {
    isValidObjectIndex(objIndex1) && isValidObjectIndex(objIndex2) &&
      objIndex1 == objects(objIndex2).supportParentIndex
  }

  def isChildParent(objIndex1: Int, objIndex2: Int): Boolean = {
    isValidObjectIndex(objIndex1) && isValidObjectIndex(objIndex2) &&
      objects(objIndex1).supportParentIndex == objIndex2
  }


  def getCamera(): CameraInfo = {
    getCamera(CameraInfo.CURRENT)
  }

  def getCamera(cameraName: String): CameraInfo = {
    cameras.find( c => c.name == cameraName ).getOrElse( null )
  }

  def getDebugCameras(): Seq[CameraInfo] = {
    cameras.filter( c => c.name.startsWith(CameraInfo.DEBUG) )
  }

  def addCamera(camInfo: CameraInfo) {
    cameras = cameras :+ camInfo
  }

  def replaceCamera(camInfo: CameraInfo) {
    val cams = cameras.filterNot( c => c.name == camInfo.name )
    cameras = cams :+ camInfo
  }
}

object Scene {
  def apply(): Scene = new Scene()
  def apply( sceneId: String ): Scene = {
    val s = new Scene()
    s.sceneId = sceneId
    s
  }
  def apply( objects: IndexedSeq[SceneObject] ): Scene = new Scene(objects)
}

case class SceneBasicInfo(id: String, name: String, category: Seq[String], unit: Option[Double])

/**
 * Object in a scene
 */
// TODO: Should this class be immutable?
case class SceneObject(var index: Int,
                       modelID: String,
                       var supportParentIndex: Int = -1,
                       transform: Transform = Transform(),
                       var objectDescIndex: Int = -1,
                       // TODO: These are wss fields - Do we really want them?
                       var supportParentMeshI: Int = -1,
                       var supportParentTriI: Int = -1,
                       var wssCubeFace: Int = -1,
//                       var wssScale: Double = 1.0,
//                       var wssRotation: Double = 0.0,
                       // childFace (of worldBb - TODO: compute from wssCubeFace and transform)
                       var childFace: Int = -1
                       )
{
  private var _childIndices: mutable.Set[Int] = null
  var isDominantChild: Boolean = false
  var hasSiblings: Boolean = false
  var isRoot: Boolean = false
  var parentIndex: Int = -1
  def scaleTo(s: Double) = { transform.scaleTo(s); this } //For scaling objects!!
  def scaleBy(s: Double) = { transform.scaleBy(s); this }
  def addChildIndex(i: Int) = {
    if (_childIndices == null)
      _childIndices = new mutable.TreeSet[Int]()
    _childIndices.add(i)
  }
  def removeChildIndex(i: Int) = {
    if (_childIndices != null) {
      _childIndices.remove(i)
    }
  }
  def childIndices = {
    if (_childIndices == null)
      Set()
    else
      _childIndices
  }
  def clearChildIndices() {
    if (_childIndices != null) _childIndices.clear()
  }
  def numberOfChildren = if (_childIndices == null) 0 else _childIndices.size
  def hasChildren = numberOfChildren > 0

  def modelInfo(implicit dataManager: DataManager) = {
    if (dataManager != null) dataManager.getModelInfo(FullId(modelID).fullid).getOrElse(null)
    else null
  }
  def up(implicit dataManager: DataManager): Vector3f = {
    val up = if (modelInfo != null) modelInfo.up else Constants.DEFAULT_MODEL_UP
    up
}
  def front(implicit dataManager: DataManager): Vector3f = {
    val front = if (modelInfo != null) modelInfo.front else Constants.DEFAULT_MODEL_FRONT
    front
  }
  def fullId() = {
    FullId(modelID).fullid
  }

}

