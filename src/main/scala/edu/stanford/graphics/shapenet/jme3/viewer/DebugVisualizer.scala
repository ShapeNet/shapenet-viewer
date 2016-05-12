package edu.stanford.graphics.shapenet.jme3.viewer

import com.jme3.math.{ColorRGBA, Vector3f}
import com.jme3.scene._
import com.jme3.bounding.BoundingBox
import edu.stanford.graphics.shapenet.Constants
import edu.stanford.graphics.shapenet.colors.ColorBar
import edu.stanford.graphics.shapenet.common._
import edu.stanford.graphics.shapenet.jme3.Jme
import edu.stanford.graphics.shapenet.util.Loggable
import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._

/**
 * Visualization for debugging
 * @author Angel Chang
 */
class DebugVisualizer(viewer: Viewer) extends Loggable {
  implicit lazy val jme: Jme = viewer.jme
  implicit lazy val dataManager = viewer.dataManager
  //implicit lazy val modelsDb = dataManager.modelsDb

  lazy val colorbar = ColorBar.coolwarmBar
  var debugNode = new Node("Debug Node")

  def clear() {
    debugNode.detachAllChildren()
  }

  def output(message: String, printToConsole: Boolean = false) {
    viewer.output(message, printToConsole)
  }

  def getScene = viewer.scene

  def visualize(any: AnyRef, screenShotFilename: String = null,
                waitForKeyPress: Boolean = false, defaultSceneId: String = "unknown",
                message: String = null, camera: CameraInfo = null): Unit = {
    def ensureSceneId(ss: SceneState): SceneState = {
      if (ss.scene.sceneId == null) {
        ss.scene.sceneId = defaultSceneId
      }
      ss
    }
    val promise = Promise[String]
    val done = () => {
      if (camera != null) {
        viewer.setSceneCamera(camera)
      }
      promise.success("ok")
    }
    any match {
      case scene: Scene => {
        viewer.setSceneState(ensureSceneId(SceneState(scene)), callback = done)
      }
      case sceneState: SceneState => {
        viewer.setSceneState(ensureSceneId(sceneState), callback = done)
      }
      case _ => {
        logger.warn("Cannot visualize type " + any.getClass.getName)
      }
    }
    // Wait for viewer to acknowledge that it loaded our scene
    try {
      logger.debug("Waiting for viewer...")
      Await.ready(promise.future, Duration.Inf)
      logger.debug("Done waiting for viewer")

      if (waitForKeyPress) {
        // TODO: Wait for a key press
      }

      // Do screen capture
      if (screenShotFilename != null) {
        val id = viewer.scene.scene.sceneId
        val genFuture = viewer.generateImagesState.processCurrent(screenShotFilename)
        Await.ready(genFuture, Duration.Inf)
      }

    } catch {
      case ex: Exception => {
        logger.warn("Error during visualization ", ex)
      }
    }
  }

  def showMeshes(setWireframe: Boolean = false) {
    val falseColors = new FalseColorGenerator()
    val meshesNode = new Node()
    val selected =
      if (viewer.selections.isEmpty) viewer.scene.modelInstances
      else viewer.getSelectedModelInstances
    for (modelInstance <- selected) {
      val coloredNode = jme.buildFalseColoredMeshes(modelInstance.node, falseColors)
      meshesNode.attachChild(coloredNode)
      jme.setVisible(modelInstance.node, false)
      jme.setWireframeMode(modelInstance.node, setWireframe)
    }
    debugNode.attachChild(meshesNode)
  }

  def showSpatial(name: String, s: Spatial) {
    if (s == null) return
    // Remove any old spatial with the same name
    val existingMeshNode = debugNode.getChild(name)
    if (existingMeshNode != null) {
      existingMeshNode.removeFromParent()
    }

    val falseColors = new FalseColorGenerator()
    val meshesNode = new Node(name)
    val coloredNode = jme.buildFalseColoredMeshes(s, falseColors)
    meshesNode.attachChild(coloredNode)
    val modelInstNode = jme.getModelInstanceNode(s)
    jme.setWireframeMode(modelInstNode, true)
    debugNode.attachChild(meshesNode)
  }

  def showOrientation(modelInstances: Seq[ModelInstance[Node]] = null, show: Boolean = false) {
    var axesNode = debugNode.getChild("OrientationNode").asInstanceOf[Node]
    if (show) {
      if (axesNode == null) {
        axesNode = new Node("OrientationNode")
      }
      val selected = if (modelInstances != null) { modelInstances }
      else if (viewer.selections.isEmpty) viewer.scene.modelInstances
      else viewer.getSelectedModelInstances
      for (modelInstance <- selected) {
        if (modelInstance != null) {
          val bb = jme.getBoundingBox(modelInstance.node)
          val bbCenter = bb.getCenter
          val scale = bb.getExtent(null).length()/2.0f
          val up: Vector3f = jme.getWorldUp(modelInstance)
          val front: Vector3f = jme.getWorldFront(modelInstance)
          val coloredNode = jme.createAxesUpFront(bbCenter, up.mult(scale), front.mult(scale))
          axesNode.attachChild(coloredNode)
          jme.setWireframeMode(modelInstance.node, true)
        }
      }
      debugNode.attachChild(axesNode)
    } else {
      if (axesNode != null) {
        debugNode.detachChild(axesNode)
      }
    }
  }

  def toggleWireframeMode() {
    val selected =
      if (viewer.selections.isEmpty) viewer.scene.modelInstances
      else viewer.getSelectedModelInstances
    for (modelInstance <- selected) {
      if (modelInstance != null) {
        val node = modelInstance.node
        val wireframeMode = jme.getWireframeMode(node)
        if (!wireframeMode) {
          showOrientation(Seq(modelInstance), true)
          jme.setWireframeMode(node, true)
        } else {
          showOrientation(Seq(modelInstance), false)
          jme.setWireframeMode(node, false)
        }
      }
    }
  }

  def showBoundingBox(bb: BoundingBox, color: ColorRGBA = ColorRGBA.White) = {
    val bbNode = jme.bbToSpatial(bb, wirebox = true, color)
    debugNode.attachChild(bbNode)
  }

  def showBoundingBox(nodes: Seq[_ <: Spatial]) = {
    if (nodes.length > 0) {
      val bb = jme.getComputedBoundingBox(nodes: _*)
      val bbNode = jme.bbToSpatial(bb, wirebox = true)
      debugNode.attachChild(bbNode)
      // Print debug information about bb
      logger.info(bb.toString)
      bbNode
    } else null
  }

  def showMesh(modelInstance: ModelInstance[Node], meshIndex: Int) {
    val meshNodeName = "Part-" + modelInstance.index + "-" + PartId.toString(PartType.MESH, meshIndex)
    val existingMeshNode = debugNode.getChild(meshNodeName)
    if (existingMeshNode == null) {
      val meshNode = jme.buildFalseColoredMesh(meshNodeName, modelInstance, meshIndex)
      jme.setWireframeMode(modelInstance.node, true)
      debugNode.attachChild(meshNode)
    } else {
      jme.setWireframeMode(modelInstance.node, true)
    }
  }

  def showPart(modelInstance: ModelInstance[Node], partId: String): Unit = {
    val partNodeName = getPartNodeName(modelInstance, partId)
    val existingMeshNode = debugNode.getChild(partNodeName)
    if (existingMeshNode == null) {
      val meshNode = buildColoredNodeForPart(modelInstance, partId, partNodeName)
      jme.setWireframeMode(modelInstance.node, true)
      debugNode.attachChild(meshNode)
    } else {
      jme.setWireframeMode(modelInstance.node, true)
    }
  }

  private def getPartNodeName(modelInst: ModelInstance[Node], partId: String): String = {
    "Part-" + modelInst.index + "-" + partId
  }

  private def buildColoredNodeForPart(modelInst: ModelInstance[Node], partId: String, partNodeName: String = null): Spatial = {
    val pi = PartId.parse(partId)
    val nodeName = if (partNodeName != null) partNodeName else getPartNodeName(modelInst, partId)
    val meshNode = pi.partType match {
      case PartType.MESH => {
        jme.buildFalseColoredMesh(partNodeName, modelInst, pi.partIndex)
      }
      case PartType.SGPATH => {
        // Identify the node and then build false colored mesh
        val nodes = jme.lookupNodesBySceneGraphPath(modelInst.nodeSelf, pi.partIdValue)
        jme.buildFalseColoredSpatials(nodes)
      }
    }
    meshNode
  }

}
