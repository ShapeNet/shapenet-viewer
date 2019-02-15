package edu.stanford.graphics.shapenet.jme3.viewer

import java.io.File
import java.util.concurrent.Callable
import javax.swing.tree.DefaultMutableTreeNode

import au.com.bytecode.opencsv.CSVWriter
import com.jme3.app.SimpleApplication
import com.jme3.bounding.BoundingBox
import com.jme3.collision.CollisionResults
import com.jme3.input.controls._
import com.jme3.input.{KeyInput, MouseInput}
import com.jme3.light.DirectionalLight
import com.jme3.material.Material
import com.jme3.math.{ColorRGBA, Matrix4f, Ray, Vector2f, Vector3f}
import com.jme3.niftygui.NiftyJmeDisplay
import com.jme3.post.filters.CartoonEdgeFilter
import com.jme3.post.ssao.SSAOFilter
import com.jme3.post.{Filter, FilterPostProcessor}
import com.jme3.renderer.Camera
import com.jme3.renderer.queue.RenderQueue.ShadowMode
import com.jme3.scene.shape.{Box, Cylinder}
import com.jme3.scene.{Geometry, Node, Spatial}
import com.jme3.shadow.{DirectionalLightShadowFilter, DirectionalLightShadowRenderer, EdgeFilteringMode}
import com.jme3.system.{AppSettings, JmeContext, JmeSystem}
import edu.stanford.graphics.shapenet.{Constants, UserDataConstants}
import edu.stanford.graphics.shapenet.common._
import edu.stanford.graphics.shapenet.gui.{MeshTreePanel, SceneTreePanel, TreeNodeInfo}
import edu.stanford.graphics.shapenet.jme3.app.ModelInfoAppState
import edu.stanford.graphics.shapenet.jme3.geom.BoundingBoxUtils
import edu.stanford.graphics.shapenet.jme3.loaders.{LoadFormat, LoadProgress, LoadProgressListener}
import edu.stanford.graphics.shapenet.jme3._
import edu.stanford.graphics.shapenet.jme3.{Jme, JmeUtils}
import edu.stanford.graphics.shapenet.util.ConversionUtils._
import edu.stanford.graphics.shapenet.util._

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.matching.Regex

class Viewer(val config: ViewerConfig = ViewerConfig()) extends SimpleApplication with Loggable {
  val useCustomObjLoader = true
  val exitOnStop = true
  val guiXml: String = "edu/stanford/graphics/shapenet/jme3/viewer/Viewer.xml"
  val startScreen: String = "start"
  val startText: String = ""

  // List of actions
  val ACTION_SELECT = "Viewer_Select"
  val ACTION_SHOW_BOUNDINGBOX = "Viewer_ShowBoundingBox"
  val ACTION_TOGGLE_HIGHLIGHT = "Viewer_ToggleHighlight"
  val ACTION_RESET_CAMERA = "Viewer_ResetCamera"
  val ACTION_CLEAR_SELECTED = "Viewer_ClearSelected"
  val ACTION_TOGGLE_GUI = "Viewer_ToggleGui"
  val ACTION_TOGGLE_INSTRUCTIONS = "Viewer_ToggleInstructions"
  val ACTION_TOGGLE_CONSOLE = "Viewer_ToggleConsole"
  val ACTION_TOGGLE_WIREFRAME = "Viewer_ToggleWireframe"
  val ACTION_TAKE_SCREENSHOT = "Viewer_TakeScreenshot"
  val ACTION_HIDE_MENU = "Viewer_HideMenu"

  val ACTION_DELETE_SELECTED = "Viewer_DeleteSelected"

  val ACTION_ROTATE_AROUND_TARGET_LEFT = "Viewer_RotAroundTargetLeft"
  val ACTION_ROTATE_AROUND_TARGET_RIGHT = "Viewer_RotAroundTargetRight"
  val ACTION_ROTATE_AROUND_TARGET_UP = "Viewer_RotAroundTargetUp"
  val ACTION_ROTATE_AROUND_TARGET_DOWN = "Viewer_RotAroundTargetDown"

  val KEY_LEFT = "LEFT"
  val KEY_RIGHT = "RIGHT"
  val KEY_UP = "UP"
  val KEY_DOWN = "DOWN"
  val KEY_PAGEUP = "PAGEUP"
  val KEY_PAGEDOWN = "PAGEDOWN"

  val KEY_LCTRL = "LCTRL"
  val KEY_RCTRL = "RCTRL"
  val KEY_LSHIFT = "LSHIFT"
  val KEY_RSHIFT = "RSHIFT"
  val KEY_Q = "Q"

  // set of scene interaction actions
  val sceneInteractionActions = Set(
    ACTION_SELECT, ACTION_RESET_CAMERA, ACTION_CLEAR_SELECTED, ACTION_DELETE_SELECTED,
    ACTION_ROTATE_AROUND_TARGET_LEFT, ACTION_ROTATE_AROUND_TARGET_RIGHT,
    ACTION_ROTATE_AROUND_TARGET_UP, ACTION_ROTATE_AROUND_TARGET_DOWN
  )
  // TODO: List of bindings of actions to keys
  //  case class InputBinding(
  //    action: String,
  //    trigger: Seq[Trigger],
  //    actionType: String
  //  )

  var state = ViewerState.INIT

  implicit var jme: Jme = null
  var shootables = new Node()
  //  val selectedNodes = new ArrayBuffer[Node]()
  val selections = new MutableSceneSelections()
  var autoAlign = true

  // Shadow stuff
  private var directionalLightForShadow: DirectionalLight = null
  private var dlsr: DirectionalLightShadowRenderer = null
  private var dlsf: DirectionalLightShadowFilter = null
  private var fpp: FilterPostProcessor = null

  // If floor need to be added
  def needFloor: Boolean = config.addFloor
  var floor: Spatial = null

  var screenShotDir = Constants.WORK_SCREENSHOTS_DIR
  var screenShotState: ScreenshotAppState = null
  var generateImagesState: GenerateImagesAppState = null

  var maxFalseColors = 0 // limit on number of false colors
  var falseBkMaterial: Material = null
  val falseMaterials = new mutable.ArrayBuffer[Material]()
  var scene: GeometricScene[Node] = null

  var modelInfoAppState: ModelInfoAppState = null

  // Main scene node to which the scene is attached
  var rootSceneNode = new Node("Root Scene Node")
  // Various debugging visualizations
  val debugVisualizer = new DebugVisualizer(this)

  var savedCam: Camera = null

  def defaultSceneDistanceScale = config.defaultSceneDistanceScale
  def defaultModelDistanceScale = config.defaultModelDistanceScale
  def falseMaterialBlendOld = config.falseMaterialBlendOld
  def dataManager = jme.dataManager

  def highlightMode = config.highlightMode

  val asyncLoading = true

  var enableSceneInput = true

  val userId = config.userId

  private var niftyController: ViewerController = null
  private val pressedMappings = new mutable.HashSet[String]()

  // Member variables for offscreen analysis
  private var offscreenAnalyzer: OffscreenAnalyzer = null

  val renderTasks = new RenderTaskQueue()
  // Future command to execute...
  val commands = new mutable.Queue[String]()

  def initConfig(): Unit = {
    config.registerMutableBoolean("useShadow", "Use shadows or not",
      x => config.useShadow, s => {
        config.useShadow = s
        // Update shadows...
        this.configShadowProcessors()
        this.updateSceneForShadows(config.useShadow)
      })

    config.registerMutableBoolean("addFloor", "Use floor or not",
      x => config.addFloor, s => {
        config.addFloor = s
        if (config.addFloor) {
          if (floor == null && scene != null) {
            addFloor(jme.getBoundingBox(scene.node))
          }
        }
        // show/hide floor
        JmeUtils.setVisible(floor, config.addFloor)
      })

    config.registerMutableBoolean("useRadialFloor", "Use radial or rectangular floor",
      x => config.useRadialFloor, s => {
        config.useRadialFloor = s
        updateFloor()
      })
    config.registerMutable("minFloorSize", "Minimum size for the floor (in virtual units - currently cm",
      x => config.minFloorSize, s => {
        config.minFloorSize = s.toInt
        updateFloor()
      })
    config.registerMutable("floorSizeRatio", "Ratio of floor size to object size",
      x => config.floorSizeRatio, s => {
        config.floorSizeRatio = s.toDouble
        updateFloor()
      })
    config.registerMutableBoolean("useOutline", "Outline objects",
      x => config.useOutline, s => {
        config.useOutline = s
        configOutlineFilter()
      })
    config.registerMutableBoolean("useAmbientOcclusion", "Ambient occlusion",
      x => config.useAmbientOcclusion, s => {
        config.useAmbientOcclusion = s
        configAmbientOcclusionFilter()
      })
    config.registerMutableBoolean("showModelLabel", "Show labels of models",
      x => config.showModelLabel, s => {
        config.showModelLabel = s
        modelInfoAppState.setShowModelLabel(config.showModelLabel)
      }
    )
    config.registerMutable("loadFormat", "Set default load format for models",
      x => config.loadFormat, s => {
        config.loadFormat = Some(LoadFormat(s))
        jme.defaultLoadFormat = config.loadFormat
        jme.assetLoader.defaultLoadFormat = config.loadFormat
      })
    config.registerMutable("screenshotDir", "Set base directory to save screenshots",
      x => screenShotDir, s => {
        screenShotDir = s + "/"
      })
  }

  def hideMenu() {
    niftyController.hideMenu()
  }

  def setupViewProcessors(directionalLight: DirectionalLight): Unit = {
    // TODO: Sometimes there are artifacts like seen in http://dovahkiin.stanford.edu/text2scene/eval/full-layout/seedsents-sempre/summary.html
    // We may need to remove old processors to prevent artifacts like remaining shadows in a new empty room
    // Things seems okay again for some reason....
    //      if (dlsr != null) {
    //        //viewPort.removeProcessor(dlsr)
    //      }
    //      if (fpp != null) {
    //        //viewPort.removeProcessor(fpp)
    //        //fpp.cleanup()
    //        //fpp.removeAllFilters()
    //      }
    this.directionalLightForShadow = directionalLight
    configShadowProcessors(directionalLight)
    configAmbientOcclusionFilter()
    configOutlineFilter()
  }

  def updateSceneForShadows(useShadow: Boolean) = {
    val shadowMode = if (useShadow) ShadowMode.CastAndReceive else ShadowMode.Off
    for (i <- 0 until scene.getNumberOfObjects()) {
      val m = scene.getModelInstance(i)
      if (m != null && m.node != null) {
        m.nodeSelf.setShadowMode(shadowMode)
      }
    }
    if (this.floor != null) {
      val floorShadowMode = if (useShadow) ShadowMode.Receive else ShadowMode.Off
      this.floor.setShadowMode(floorShadowMode)
    }
  }

  def configShadowProcessors(directionalLight: DirectionalLight = this.directionalLightForShadow, flag: Boolean = config.useShadow): Unit = {
    if (flag) {
      // Shadows on!
      // Actual light is set after the light is created
      val shadowMapSize = 2048
      if (dlsr == null) {
        dlsr = new DirectionalLightShadowRenderer(assetManager, shadowMapSize, 3)
        //dlsr.setLight(l)
        dlsr.setLambda(0.55f)
        dlsr.setShadowIntensity(0.35f)  // How dark a shadow
        dlsr.setEdgeFilteringMode(EdgeFilteringMode.Bilinear)
        //dlsr.displayFrustum()
        viewPort.addProcessor(dlsr)
      }

      if (dlsf == null) {
        dlsf = new DirectionalLightShadowFilter(assetManager, shadowMapSize, 3)
        //dlsf.setLight(l)
        dlsf.setLambda(0.55f)
        dlsf.setShadowIntensity(0.35f)  // How dark a shadow
        dlsf.setEdgeFilteringMode(EdgeFilteringMode.Bilinear)
      }
      dlsf.setEnabled(true)

      _configFilter(dlsf, true)

      dlsr.setLight(directionalLight)
      dlsf.setLight(directionalLight)
    } else {
      // Remove shadow processing
      if (dlsf != null) {
        _configFilter(dlsf, false)
      }
    }
  }

  private lazy val ambientOcclusionFilter = {
    new SSAOFilter(12.94f, 43.92f, 0.33f, 0.61f)
  }
  def configAmbientOcclusionFilter(): Unit = {
    _configFilter(ambientOcclusionFilter, config.useAmbientOcclusion)
  }

  private lazy val outlineFilter = {
    new CartoonEdgeFilter()
  }
  def configOutlineFilter(): Unit = {
    _configFilter(outlineFilter, config.useOutline)
  }
  private def _configFilter(filter: =>Filter, enable: Boolean): Unit = {
    filter.setEnabled(enable)
    if (enable) {
      if (fpp == null) {
        fpp = new FilterPostProcessor(assetManager)
        fpp.addFilter(filter)
        viewPort.addProcessor(fpp)
      } else {
        // See if there is the filter already there
        val existingFilter = fpp.getFilter(filter.getClass)
        if (existingFilter == null) {
          fpp.addFilter( filter )
        }
      }
    } else {
      if (fpp != null) {
        val existingFilter = fpp.getFilter(filter.getClass)
        if (existingFilter != null) {
          fpp.removeFilter(existingFilter)
        }
      }
    }
  }

  def dumpMeshInfo(spatial: Spatial): Unit = {
    case class DepthTransform(depth: Int, parentMatrix: Matrix4f)
    def incr(s: Spatial, dt: DepthTransform): DepthTransform = {
      DepthTransform(dt.depth + 1, dt.parentMatrix.mult(transformToMatrix(s.getLocalTransform)))
    }
    def visitor(s: Spatial, dt: DepthTransform): Boolean = {
      val pad = Array.fill[String](dt.depth)(" ")
      val padStr = pad.mkString("")
      println(padStr + "- Node: " + s.getName)
      s match {
        case geom: Geometry => {
          println(padStr + " mesh: " + geom.getMesh.getTriangleCount)
          println(padStr + " material: " + geom.getMaterial.getName)
        }
        case _ => {}
      }
      // transpose matrix so our array will end up row-major (don't use matrix to string so we fit on one line)
      val local = transformToMatrix(s.getLocalTransform)
      val world = transformToMatrix(s.getWorldTransform)
      val recomputedWorld = dt.parentMatrix.mult(local)
      println(padStr + " transform: [" + matrixToArray(local.transpose()).mkString(",") + "]\n"
        + padStr + " bb: [" + jme.getBoundingBox(s) + "]")
      //            + padStr + " world: [" + matrixToArray(world.transpose()).mkString(",") + "]\n"
      //            + padStr + " worldCheck: [" + matrixToArray(recomputedWorld.transpose()).mkString(",") + "]")
      true
    }
    val initial: DepthTransform = new DepthTransform(0, transformToMatrix(spatial.getParent.getWorldTransform))
    jme.depthFirstTraversal(spatial, visitor _, initial, incr)
  }

  def showMeshTreePanel(spatial: Spatial): Unit = {
    showMeshTreePanel(Seq(spatial))
  }
  def showMeshTreePanel(spatials: Seq[Spatial]): Unit = {
    val treeNodes = spatials.map( spatial => createMeshTree(spatial))
    if (treeNodes.length == 1) {
      MeshTreePanel.create(treeNodes.head)
    } else {
      val tree = new DefaultMutableTreeNode()
      for (t <- treeNodes) {
        tree.add(t)
      }
      MeshTreePanel.create(tree)
    }
  }
  def createMeshTree(spatial: Spatial, dump: Boolean = false): DefaultMutableTreeNode = {
    case class DepthTransform(depth: Int, parentMatrix: Matrix4f)
    def incr(s: Spatial, dt: DepthTransform): DepthTransform = {
      DepthTransform(dt.depth + 1, dt.parentMatrix.mult(transformToMatrix(s.getLocalTransform)))
    }
    val treeNodes = new mutable.HashMap[Spatial,DefaultMutableTreeNode]
    def visitor(s: Spatial, dt: DepthTransform): Boolean = {
      val id: String = s.getUserData(UserDataConstants.NODE_ID)
      val name =
        if (s.getName() != null && !s.getName().isEmpty) s.getName()
        else if (s.isInstanceOf[Geometry]) { "Mesh-" + s.asInstanceOf[Geometry].getMesh.getId() }
        else "NONAME"
      val info = new TreeNodeInfo(name, s)
      info.put("viewer", this)
      val treenode = new DefaultMutableTreeNode(info)
      treeNodes.put(s, treenode)
      val parentTreeNode = treeNodes.getOrElse(s.getParent, null)
      if (parentTreeNode != null) {
        parentTreeNode.add(treenode)
      }

      val pad = Array.fill[String](dt.depth)(" ")
      val padStr = pad.mkString("")

      val lines = new ArrayBuffer[String]
      lines.append("Node: " + name)
      if (id != null) {
        lines.append("id: " + id)
      }
      val sgpath = JmeUtils.getSceneGraphPath(s, x => x == spatial /* || x.getUserDataKeys.contains("modelId") */)
      lines.append("sgpath: " + sgpath)
      s match {
        case geom: Geometry => {
          lines.append("mesh: id=" + geom.getMesh.getId + ", tris=" + geom.getMesh.getTriangleCount)
          lines.append("material: " + geom.getMaterial.getName)
        }
        case _ => {}
      }
      // transpose matrix so our array will end up row-major (don't use matrix to string so we fit on one line)
      val local = transformToMatrix(s.getLocalTransform)
      val world = transformToMatrix(s.getWorldTransform)
      lines.append("localTransform: [" + matrixToArray(local.transpose()).mkString(",") + "]")
      lines.append("worldTransform: [" + matrixToArray(world.transpose()).mkString(",") + "]")

      val sb = new StringBuilder()
      for (line <- lines) {
        sb.append(line + "\n")
      }
      info.put("info", sb.toString)

      if (dump) {
        println(padStr + "- " + lines.head)
        for (line <- lines.tail) {
          println(padStr + " " + line)
        }
        // Additional stuff to dump
        println(padStr + " bb: [" + jme.getBoundingBox(s) + "]")
      }
      true
    }
    val initial: DepthTransform = new DepthTransform(0, transformToMatrix(spatial.getParent.getWorldTransform))
    jme.depthFirstTraversal(spatial, visitor _, initial, incr)
    treeNodes(spatial)
  }

  def showSceneTreePanel(): Unit = {
    if (scene != null && scene.scene != null) {
      val tree = createSceneHierarchyTree()
      SceneTreePanel.create(tree)
    } else {
      output("No scene is loaded", true)
    }
  }
  def createSceneHierarchyTree(): DefaultMutableTreeNode = {
    val top = new DefaultMutableTreeNode(new TreeNodeInfo(scene.scene.sceneName, null))
    val treeNodes = new Array[DefaultMutableTreeNode](scene.modelInstances.size)
    for (modelInstance <- scene.modelInstances) {
      if (modelInstance != null) {
        val modelInstanceIndex = modelInstance.index
        val info = new TreeNodeInfo(modelInstance.toString, modelInstance)
        val treenode = new DefaultMutableTreeNode(info)
        info.put("viewer", this)
        treeNodes(modelInstanceIndex) = treenode
      }
    }
    for (modelInstance <- scene.modelInstances) {
      if (modelInstance != null) {
        val obj = scene.scene.objects(modelInstance.index)
        val parentIndex = obj.supportParentIndex
        val treenode = treeNodes(modelInstance.index)
        if (parentIndex >= 0) {
          val parentTreeNode = treeNodes(parentIndex)
          parentTreeNode.add(treenode)
        } else {
          top.add(treenode)
        }
      }
    }
    top
  }

  def getSelectedNodes: Seq[Node] = {
    selections.filter(x => x.partType == null).map( s => scene.modelInstances(s.objectIndex).node )
  }
  def getSelectedModelInstances: Seq[ModelInstance[Node]] = {
    selections.filter(x => x.partType == null).map( s => scene.modelInstances(s.objectIndex) )
  }
  def getUnselectedModelInstances: Seq[ModelInstance[Node]] = {
    val selected = getSelectedModelIndices.toSet
    scene.modelInstances.filter( x => !selected.contains(x.index) )
  }
  def getSelectedModelIndices: Seq[Int] = {
    selections.filter(x => x.partType == null).map( s => s.objectIndex )
  }
  def getUnselectedModelIndices: Seq[Int] = {
    val selected = getSelectedModelIndices.toSet
    scene.modelInstances.filter( x => !selected.contains(x.index) ).map( m => m.index )
  }
  def getSceneState: SceneState = {
    if (scene != null) {
      if (scene.geometricSceneModified) {
        scene.syncToScene()
      }
      // Convert from world up/front to scene up/front
      val sceneCam = jme.transformCameraInfoFromWorldToScene( getCurrentCameraInfo(CameraInfo.CURRENT), scene.scene )
      scene.scene.replaceCamera( sceneCam )
      SceneState(scene.scene, selections)
    } else null
  }
  def setSceneState(ss: SceneState, callback: () => _) {
    loadSceneState(ss, onloaded = callback)
  }

  def getSceneStats = offscreenAnalyzer.getSceneStats
  def getOffScreen = offscreenAnalyzer.getOffScreen
  def getOffScreenDisplay = offscreenAnalyzer.getOffScreenDisplay
  def getOffScreenAnalyzer = offscreenAnalyzer
  def getConsole = niftyController.commandConsole.console

  def getCurrentCameraState: CameraState = {
    val position = cam.getLocation
    val up = cam.getUp
    // The target cannot be retrieved from the camera
    // (we can only get the direction)
    val direction = cam.getDirection
    CameraState("current", position, up, null, direction)
  }

  def getCurrentCameraInfo(name: String): CameraInfo = {
    jme.toCameraInfo(cam, name)
  }

  def adjustFlyCamSpeed() {
    this.flyCam.setMoveSpeed(this.cam.getFrustumFar/5)
    this.flyCam.setZoomSpeed(this.cam.getFrustumFar/75)
  }

  def showMesh(modelIndex: Int, meshIndex: Int) {
    showMesh(modelIndex, meshIndex, printToConsole = false)
  }
  def showMesh(modelIndex: Int, meshIndex: Int, printToConsole: Boolean) {
    if (scene != null) {
      val modelInstance = scene.modelInstances(modelIndex)
      debugVisualizer.showMesh(modelInstance, meshIndex)
    } else {
      output("No scene loaded - cannot show mesh", printToConsole)
    }
  }
  def showPart(modelIndex: Int, partId: String, printToConsole: Boolean) {
    if (scene != null) {
      val modelInstance = scene.modelInstances(modelIndex)
      debugVisualizer.showPart(modelInstance, partId)
    } else {
      output("No scene loaded - cannot show part", printToConsole)
    }
  }
  def toggleSelectMesh(modelIndex: Int, meshIndex: Int, triIndex: Int): Unit = {
    val modelInstance = scene.modelInstances(modelIndex)
    val sceneSel = SceneSelection(modelInstance.index, PartId(PartType.MESH, meshIndex) )
    if (selections.contains(sceneSel)) {
      unselectMesh(modelInstance, meshIndex)
    } else {
      selectMesh(modelInstance, meshIndex)
    }
  }
  def selectMesh(modelInstance: ModelInstance[Node], meshIndex: Int) {
    // Check if surface is in selected
    val sceneSel = SceneSelection(modelInstance.index, PartId(PartType.MESH, meshIndex) )
    selections.select(sceneSel)
    debugVisualizer.showMesh(modelInstance, meshIndex)
  }
  def unselectMesh(modelInstance: ModelInstance[Node], meshIndex: Int) {
    val sceneSel = SceneSelection(modelInstance.index, PartId(PartType.MESH, meshIndex) )
    selections.unselect(sceneSel)
    val meshNodeName = "Mesh-" + modelInstance.index + "-" + meshIndex
    val existingMeshNode = debugVisualizer.debugNode.getChild(meshNodeName)
    if (existingMeshNode != null) {
      existingMeshNode.removeFromParent()
    }
  }

  class SceneLoadProgressListener(val distanceScale: Float, onloaded: () => _ = null, onerror: () => _ = null)
    extends LoadProgressListener[GeometricScene[Node]]() {
    override def onProgress(progress: LoadProgress[GeometricScene[Node]]) {
      // Enqueue progress for update loop
      enqueue(new Callable[Unit]() {
        override def call() {
          val percentDone = progress.percentDone
          val i = progress.loaded
          val p = if (progress.partsProgress != null && i < progress.partsProgress.length && progress.partsProgress(i) != null)
            progress.partsProgress(i) else progress
          niftyController.setProgress(percentDone, p.stage + ": " + p.name)
          logger.debug("Loaded " + progress.loaded + " out of " + progress.total + ", percent done " + progress.percentDone)
        }
      })
    }
    override def onDone(result: GeometricScene[Node]) {
      // Enqueue changes for update loop
      enqueue(new Callable[Unit]() {
        override def call() {
          logger.debug("Scene loaded")
          onSceneLoaded(result, distanceScale, onloaded, onerror)
        }
      })
    }
  }

  def onSceneLoaded(scene: GeometricScene[Node], distanceScale: Float, onloaded: () => _ = null, onerror: () => _ = null) {
    if (scene != null && !scene.modelInstances.forall( x => x == null)) {
      onSceneLoadedSuccess(scene, distanceScale, onloaded)
    } else {
      // Error
      if (onerror != null) {
        onerror()
      }
      niftyController.showError("Error loading scene")
    }
    logger.debug("Viewer is now ready")
    state = ViewerState.READY
  }

  def finalizeScene(scene: GeometricScene[Node]): Unit = {
    updateSceneForShadows(config.useShadow)
  }

  def onSceneLoadedSuccess(scene: GeometricScene[Node], distanceScale: Float, onloaded: () => _ = null) {
    this.scene = scene
    finalizeScene(scene)
    if (this.modelInfoAppState != null) {
      this.modelInfoAppState.setScene(scene)
    }
    if (autoAlign) {
      jme.alignScene(scene)
      jme.scaleScene(scene)
    }

    // Clean up old state
    rootNode.detachAllChildren()
    //rootSceneNode.detachAllChildren()
    // Create a new scene node - so we start out fresh
    rootSceneNode = new Node("Root Scene Node")
    shootables = rootSceneNode
    // Create a new debug node - so we start out fresh
    debugVisualizer.debugNode = new Node("Debug Node")
    floor = null
    rootNode.attachChild(rootSceneNode)
    rootSceneNode.attachChild(scene.node)
    rootNode.attachChild(debugVisualizer.debugNode)
    jme.updateScene(scene)

    // Position the camera.
    jme.cameraPositioner.positionCamera(getCamera, Seq(scene.node), distanceScale,
      camHeightRatio = None,
      camAngleFromHorizontal = None, //Option((Math.PI/4).toFloat),
      camPosType = CameraPositioningStrategy.POSITION_TO_FIT)
    // See if there is a camera associated with the scene
    val sceneCamInfo = scene.scene.getCamera()
    // NOTE: Convert from scene to world
    val worldCamInfo = jme.transformCameraInfoFromSceneToWorld(sceneCamInfo, scene.scene)
    if (worldCamInfo != null) {
      logger.debug("Using initial camera to set camera")
      jme.cameraPositioner.positionCamera(getCamera, worldCamInfo)
    }
    if (scene.scene.cameras.isEmpty) {
      // NOTE: convert from world to scene
      logger.debug("Save current camera as initial camera")
      val worldCamInfo = getCurrentCameraInfo(CameraInfo.INITIAL)
      val camInfo = jme.transformCameraInfoFromWorldToScene(worldCamInfo, scene.scene)
      scene.scene.addCamera( camInfo )
    }
    adjustFlyCamSpeed()
    val directionalLight = jme.addDefaultLights(rootSceneNode, scene.node, getCamera, config.lightColor)
    selections.clear()
    saveCameraState()

    toggleHighlightMode(highlightMode)
    // Add floor if not already added
    if (needFloor && floor == null) {
      addFloor(jme.getFastBoundingBox(scene.node))
    }
    // Make sure our debug node also have lights
    for (light <- rootSceneNode.getLocalLightList) {
      debugVisualizer.debugNode.addLight(light)
    }
    // Setup view processors for scene (otherwise, sometimes there is lingering shadows from previous scene)
    setupViewProcessors(directionalLight)

    niftyController.hideMenu()

    if (onloaded != null) onloaded()
  }

  def loadModel(id: String, async: Boolean = asyncLoading, onloaded: () => _ = null, onerror: () => _ = null,  transform: Matrix4f = null) {
    if (async) {
      if (state == ViewerState.READY) {
        val listener = new SceneLoadProgressListener(defaultModelDistanceScale, onloaded, onerror)
        val runnable = new Runnable() {
          override def run() {
            jme.loadModelAsScene(id, transform, listener = listener)
          }
        }
        state = ViewerState.LOAD
        niftyController.setProgress(0, "Loading model: " + id)
        niftyController.showLoadingMenu()
        Threads.execute(runnable, logger, "loadModel")
      } else {
        if (state == ViewerState.LOAD) {
          logger.warn("Loading already in progress....")
        } else {
          logger.warn("Something is happening....")
        }
      }
    } else {
      val scene = jme.loadModelAsScene(id, transform)
      onSceneLoaded(scene, defaultModelDistanceScale, onloaded, onerror)
    }
  }

  def loadScene(s: (String or Scene), async: Boolean = asyncLoading, onloaded: () => _ = null, onerror: () => _ = null) {
    val useSupportHierarchy = true
    if (async) {
      if (state == ViewerState.READY) {
        val listener = new SceneLoadProgressListener(defaultSceneDistanceScale, onloaded, onerror)
        val runnable = new Runnable() {
          override def run() {
            jme.loadScene(s, listener = listener, useSupportHierarchy = useSupportHierarchy)
          }
        }
        state = ViewerState.LOAD
        val name = s match {
          case Left(id) => id
          case Right(scene) => scene.sceneId
        }
        niftyController.setProgress(0, "Loading scene: " + name)
        niftyController.showLoadingMenu()
        Threads.execute(runnable, logger, "loadScene")
      } else {
        if (state == ViewerState.LOAD) {
          logger.warn("Loading already in progress....")
        } else {
          logger.warn("Something is happening....")
        }
      }
    } else {
      val scene = jme.loadScene(s, useSupportHierarchy = useSupportHierarchy)
      onSceneLoaded(scene, defaultSceneDistanceScale, onloaded, onerror)
    }
  }
  def loadSceneState(ss: SceneState, async: Boolean = asyncLoading, onloaded: () => _ = null, onerror: () => _ = null) {
    val onloadFunc = () => {
      setSelected(ss.selections)
      if (onloaded != null)
        onloaded()
    }
    loadScene(ss.scene, async, onloaded = onloadFunc, onerror = onerror)
  }

  def loadModelRandom(source: String = null, category: String = null) = {
    val loadId = dataManager.getRandomModelId(source, category)
    if (loadId != null) {
      logger.info("Loading random model " + loadId)
      loadModel(loadId)
    } else {
      logger.error("Cannot load random model")
      null
    }
  }

  def load(id: String, async: Boolean = asyncLoading, onloaded: () => _ = null, onerror: () => _ = null) = {
    loadModel(id, async, onloaded, onerror)
  }

  def setCamera(c: CameraState) {
    c.setCamera(cam)
  }

  def setCamera(c: CameraInfo) {
    cam.setLocation(c.position)
    if (c.target != null) {
      cam.lookAt(c.target, c.up)
    } else {
      cam.lookAtDirection(c.direction, c.up)
    }
  }

  def setCamera(c: Camera) {
    cam.copyFrom(c)
  }

  def setSceneCamera(c: CameraInfo): Unit = {
    setCamera(jme.transformCameraInfoFromSceneToWorld(c, scene.scene))
  }

  def isReady() = {
    state == ViewerState.READY
  }

  def isSceneLoaded(id: String) = {
    // Check if current scene matches the specified id
    if (id == null || scene == null) false
    else if (scene.scene.sceneId != null) {
      FullId.matches(id, scene.scene.sceneId)
    } else false
  }

  def prepareOffscreen() {
    // Prepare a offscreen view for offscreen computations
    offscreenAnalyzer = new OffscreenAnalyzer(renderManager, cam.getWidth, cam.getHeight, rootSceneNode.getLocalTransform)
  }

  def analyzeScene(printToConsole: Boolean = false) {
    offscreenAnalyzer.enableDisplay(true)
    offscreenAnalyzer.setCamera(cam)
    val selectedNodes = getSelectedNodes
    if (selectedNodes.isEmpty) {
      val fcscene = new FalseColoredScene(scene)(jme)
      getOffScreen.viewScene(fcscene.coloredSceneRoot)
      getSceneStats.analyzeScene("user requested analyze scene", fcscene, () => {
        output(getSceneStats.getStatsString, printToConsole)
      })
    } else {
      val fcscene = new FalseColoredMeshScene(scene, selectedNodes)(jme)
      val node = fcscene.coloredSceneRoot
      getOffScreen.viewScene(node)
    }
  }

  def positionCameraBasic() {
    val selectedNodes = getSelectedNodes
    if (selectedNodes != null)
      optimizeCameraPosition(1, selectedNodes:_*)
    else optimizeCameraPosition(1)
  }

  def optimizeCameraPosition(): Future[CameraOptimizationResult] =  {
    val selectedNodes = getSelectedNodes
    if (selectedNodes != null)
      optimizeCameraPosition(config.nCameraPositionsForOptimize, selectedNodes:_*)
    else optimizeCameraPosition(config.nCameraPositionsForOptimize)
  }

  /**
   * Try to optimize the camera position
   *  by maximizing the number of objects visible
   *  For now, keep camera distance and height
   * select only horizontal positioning
   * @param nCameraPos - number of camera positions to try
   */
  def optimizeCameraPosition(nCameraPos: Int, targetNodes: Spatial*): Future[CameraOptimizationResult] = {
    val listener = new CameraOptimizationPromiseListener()
    optimizeCameraPosition(nCameraPos, listener, targetNodes:_*)
    listener.promise.future
  }

  def optimizeCameraPositionForSelections(nCameraPos: Int, sels: Seq[SceneSelection]): Future[CameraOptimizationResult] = {
    val selectedNodes = sels.filter(x => x.partType == null).map( s => scene.modelInstances(s.objectIndex).node )
    val listener = new CameraOptimizationPromiseListener()
    optimizeCameraPosition(nCameraPos, listener, selectedNodes:_*)
    listener.promise.future
  }

  def optimizeCameraPosition(nCameraPos: Int, listener: CameraOptimizationListener, targetNodes: Spatial*) {
    val options = new CameraPositionOptions( cameraHeightToObjectHeightRatio = Option(1.5f), keepTargetsVisible = true, sceneWithoutRoot = true )
    val camOptimizer = new CameraPositionOptimizer(this, options)
    camOptimizer.optimize( nCameraPos, listener, targetNodes:_* )
  }

  def output(str: String, printToConsole: Boolean = false) {
    if (printToConsole) {
      getConsole.output(str)
    } else {
      println(str)
    }
  }

  def saveModelScreenshots(modelIds: Iterable[String], outputDir: Option[String] = None) {
    def getOuputDirFn(fullId: FullId): String = {
      fullId.source match {
        case "3dw" => {
          if (config.useNestedOutputDir) {
            val (id1, id2) = fullId.id.splitAt(6)
            val prefix = id1.mkString("/") + id2
            fullId.source + File.separator + prefix + File.separator + fullId.id + File.separator
          } else {
            fullId.source + File.separator + fullId.id + File.separator
          }
        }
        case "raw" => ""
        case _ => fullId.source + File.separator + fullId.id + File.separator
      }

    }
    val cameraPositionOptions = new CameraPositionOptions(
      cameraPositioningStrategy = config.cameraPositionStrategy,
      cameraAngleFromHorizontal = Option(config.cameraAngleFromHorizontal),
      startRotation = Option(config.cameraStartOrientation),
      distanceFromObjectRatio = Option(defaultModelDistanceScale)
    )

    val sceneImagesGen = new SceneImagesGenerator(this, randomize = config.randomizeModels, skipExisting = config.skipExisting, getOutputDirFn = getOuputDirFn)
    val cameraPositionGenerator = if (config.includeCanonicalViews) {
      // Create 6 canonical views + 8 views around at height xxx
      val camPosGen1 = CameraPositionGenerator.canonicalViewsToFit(this.getCamera)
      val camPosGen2 = new RotatingCameraPositionGenerator(cam, cameraPositionOptions, nPositions = config.nImagesPerModel)
      new CombinedCameraPositionGenerator(camPosGen1, camPosGen2)
    } else {
      new RotatingCameraPositionGenerator(cam, cameraPositionOptions, nPositions = config.nImagesPerModel)
    }
    sceneImagesGen.configCameraPositions(cameraPositionGenerator)
    sceneImagesGen.process(modelIds, outputDir.getOrElse(config.modelScreenShotDir.getOrElse(screenShotDir + "models" + File.separator)))
  }

  def saveModelStats(modelIds: Iterable[String], filename: String, appendToExisting: Boolean = false): Unit = {
    def toString(value: Any): String = {
      value match {
        case v: Array[_] => v.map( x => toString(x) ).mkString(",")
        case v: Vector3f => "[" + v.getX + "," + v.getY + "," + v.getZ + "]"
        case _ => value.toString
      }
    }
    val append = appendToExisting && IOUtils.isReadableFileWithData(filename)
    var todoModelIds = modelIds
    if (append) {
      // Read existing file and update the remaining modelIds that we still have to do
      val csvreader = new CSVFile(filename, includesHeader = true)
      val iFullId = csvreader.index("id")
      val doneIds = (for (row <- csvreader) yield {
        row(iFullId)
      }).toSet
      todoModelIds = modelIds.filter( x => !doneIds.contains(x) )
      logger.info("Skipping " + doneIds.size + " done ids, processing " + todoModelIds.size)
    }
    val output = new CSVWriter(IOUtils.filePrintWriter(filename, append))
    val statNames = Seq("id", "category", "unit", "up", "front", "nfaces", "nvertices", "nMaterials", "minPoint", "maxPoint",
      "aligned.minPoint", "aligned.maxPoint", "aligned.dims")
    if (!append) {
      output.writeNext(statNames.toArray)
    }
    for (modelId <- todoModelIds) {
      try {
        val stats = getModelStats(modelId)
        val row = statNames.map(x => stats.get(x).map(stat => toString(stat)).getOrElse(""))
        output.writeNext(row.toArray)
      } catch {
        case ex: Exception => {
          logger.warn("Error getting statistics for " + modelId, ex)
        }
      }
    }
    output.close
    logger.info("Saved " + filename)
  }

  def getModelStats(modelId: String): Map[String,_] = {
    val scene = jme.loadModelAsAlignedScene(modelId)
    val model = scene.modelInstances(0).model
    val modelNode = model.node
    val rawBb = jme.computeBoundingBox(modelNode)
    val (rawMin,rawMax) = BoundingBoxUtils.getBBMinMax(rawBb)
    val alignedBb = jme.getComputedBoundingBox(scene.node)
    val (alignedMin,alignedMax) = BoundingBoxUtils.getBBMinMax(alignedBb)
    val alignedDims = BoundingBoxUtils.getBBDims(alignedBb)
    val geoms = jme.getGeometriesUnordered(modelNode)
    val nFaces = geoms.map( g => g.getTriangleCount ).sum
    val nVertices = geoms.map( g => g.getVertexCount ).sum
    val nMaterials = geoms.map( g => g.getMaterial ).distinct.size
    val category = model.modelInfo.category
    val map = Map(
      "id" -> modelId,
      "category" -> category,
      "unit" -> model.modelInfo.unit,
      "up" -> model.up,
      "front" -> model.front,
      "nfaces" -> nFaces,
      "nvertices" -> nVertices,
      "nMaterials" -> nMaterials,
      "minPoint" -> rawMin,
      "maxPoint" -> rawMax,
      "aligned.minPoint" -> alignedMin,
      "aligned.maxPoint" -> alignedMax,
      "aligned.dims" -> alignedDims
    )
    map
  }

  override def destroy() {
    super.destroy()
    // do shutdown stuff here
    if (exitOnStop) {
      sys.exit()
    }
  }

  override def start() {
    if (settings == null) {
      setSettings(new AppSettings(true))
    }
    if (showSettings) {
      if (!JmeSystem.showSettingsDialog(settings, true)) {
        return
      }
    }
    showSettings = false
    setSettings(settings)
    settings.setTitle("ShapeNet Viewer")
    if (config.offscreenMode) {
      super.start(JmeContext.Type.OffscreenSurface)
    } else {
      super.start() // start the game
    }
  }

  override def simpleInitApp() {
    initKeys()

    val shotIndexStart = IOUtils.getMaxIdForRegex(screenShotDir, "Viewer(\\d+).png")
    screenShotState = new ScreenshotAppState(screenShotDir)
    screenShotState.setShotIndex(shotIndexStart)
    generateImagesState = new OffscreenGenerateImagesAppState(this, screenShotDir)

    // Turn off display of stats and fps
    this.setDisplayStatView(false)
    this.setDisplayFps(false)

    this.viewPort.setBackgroundColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 0.0f))

    Jme.initAssetManager(assetManager, useViewerAssets = true, useDataDir = Constants.USE_LOCAL_DATA, useCustomObjLoader = useCustomObjLoader)
    jme = Jme(assetManager, config.modelCacheSize, config.loadFormat)
    Jme.setDefault(jme)
    if (config.shapeNetCoreDir != null) {
      try {
        jme.dataManager.registerShapeNetCore(config.shapeNetCoreDir)
      } catch {
        case ex: Exception => {
          logger.error ("Error initializing ShapeNetCore: " + config.shapeNetCoreDir, ex)
        }
      }
    }
    this.falseBkMaterial = jme.getSimpleFalseColorMaterial(config.neutralColor)

    modelInfoAppState = new ModelInfoAppState()
    modelInfoAppState.setShowModelLabel(config.showModelLabel)
    this.stateManager.attach(modelInfoAppState)

    // FIXME: The flycam rise function assumes 'Y' is up despite being able to set the up direction
    // Initialize flycamera
    this.flyCam.setUpVector(jme.worldUp)
    this.flyCam.setDragToRotate(true)
    //this.getFlyByCamera.setEnabled(false)
    shootables = rootSceneNode

    // Need to have guiViewPort enabled for this to work
    IOUtils.createDirs(Seq(screenShotDir))
    this.stateManager.attach(screenShotState)
    this.stateManager.attach(generateImagesState)

    // Initialize nifty gui
    // (attach nifty state processor after the screenshot ones, so we can hide the loading icon)
    initNifty()

    prepareOffscreen()

    state = ViewerState.READY
    initCustom()
  }

  def initNifty() {
    val niftyDisplay = new NiftyJmeDisplay(assetManager,
      inputManager,
      audioRenderer,
      guiViewPort)
    val nifty = niftyDisplay.getNifty
    niftyController = new ViewerController(this, nifty)
    nifty.fromXml(guiXml, startScreen, niftyController)

    // attach the nifty display to the gui view port as a processor
    guiViewPort.addProcessor(niftyDisplay)
    inputManager.setCursorVisible(true)

    // Attach nifty controller to state manager
    this.stateManager.attach(niftyController)
  }

  def initCustom() {
    if (config.commands != null && config.commands.nonEmpty) {
      // Forces console to be initialized and enqueue commands
      niftyController.showScreen("console")
      //    niftyController.hideMenu()
      // Have empty command so our controller can initialize
      commands.enqueue("")
      commands.enqueue(config.commands:_*)
      if (config.offscreenMode) {
        commands.enqueue("exit")
      }
    } else {
      if (config.defaultModelId.nonEmpty) {
        loadModel(config.defaultModelId)
      }
    }
  }

  private var isProcessing = false
  override def update() {
    isProcessing = !generateImagesState.isEmpty() || !renderTasks.isEmpty()
    super.update()
  }

  override def simpleUpdate(tpf: Float) {
    // Makes sure there is no other activity going on
    // Updates to scene should go in here
    renderTasks.update(tpf)

    if (!isProcessing && state == ViewerState.READY) {
      // TODO: Wait for command to finish
      // Run commands....
      if (commands.nonEmpty) {
        val c = commands.dequeue()
        if (c.nonEmpty) {
          niftyController.commandConsole.run(c)
        }
      }
    }

    if (offscreenAnalyzer != null) offscreenAnalyzer.update(tpf)
  }

  def saveScreenShot(filename: String) {
    logger.info("Save screenshot for file "  + filename)
    screenShotState.takeScreenshot(filename)
  }

  def generateImagesForScenes(scenesFile: String, summaryFile: String) {
    // Read file and generate images
    val sceneIds = IOUtils.getLines(scenesFile).toSeq
    generateImagesState.setSummaryFile(summaryFile)
    generateImagesState.enqueueSceneIds(sceneIds:_*)
    generateImagesState.closeSummaryFile()
  }

  /** Declaring the "Shoot" action and mapping to its triggers. */
  def initKeys() {
    // Remove default binding of exit to ESC
    inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT)

    // Action mapping allows mapping multiple inputs to same action
    inputManager.addMapping(KEY_LCTRL, new KeyTrigger(KeyInput.KEY_LCONTROL))
    inputManager.addMapping(KEY_RCTRL, new KeyTrigger(KeyInput.KEY_RCONTROL))
    inputManager.addMapping(KEY_LSHIFT, new KeyTrigger(KeyInput.KEY_LSHIFT))
    inputManager.addMapping(KEY_RSHIFT, new KeyTrigger(KeyInput.KEY_RSHIFT))
    inputManager.addMapping(KEY_Q, new KeyTrigger(KeyInput.KEY_Q))

    inputManager.addMapping(KEY_LEFT, new KeyTrigger(KeyInput.KEY_LEFT))
    inputManager.addMapping(KEY_RIGHT, new KeyTrigger(KeyInput.KEY_RIGHT))
    inputManager.addMapping(KEY_UP, new KeyTrigger(KeyInput.KEY_UP))
    inputManager.addMapping(KEY_DOWN, new KeyTrigger(KeyInput.KEY_DOWN))
    inputManager.addMapping(KEY_PAGEUP, new KeyTrigger(KeyInput.KEY_PGUP))
    inputManager.addMapping(KEY_PAGEDOWN, new KeyTrigger(KeyInput.KEY_PGDN))

    // Running out of letters, should go to KeyboardInputListener
    // Action mapping allows mapping multiple inputs to same action
    inputManager.addMapping(ACTION_TOGGLE_INSTRUCTIONS, new KeyTrigger(KeyInput.KEY_F1))
    inputManager.addMapping(ACTION_TOGGLE_GUI, new KeyTrigger(KeyInput.KEY_F2))
    inputManager.addMapping(ACTION_TOGGLE_CONSOLE, new KeyTrigger(KeyInput.KEY_F4))
    inputManager.addMapping(ACTION_HIDE_MENU, new KeyTrigger(KeyInput.KEY_ESCAPE))

    inputManager.addMapping(ACTION_TOGGLE_WIREFRAME, new KeyTrigger(KeyInput.KEY_X))
    inputManager.addMapping(ACTION_TOGGLE_HIGHLIGHT, new KeyTrigger(KeyInput.KEY_H))
    inputManager.addMapping(ACTION_RESET_CAMERA, new KeyTrigger(KeyInput.KEY_R))
    inputManager.addMapping(ACTION_SHOW_BOUNDINGBOX, new KeyTrigger(KeyInput.KEY_B))
    inputManager.addMapping(ACTION_CLEAR_SELECTED, new KeyTrigger(KeyInput.KEY_C))
    inputManager.addMapping(ACTION_TAKE_SCREENSHOT, new KeyTrigger(KeyInput.KEY_T))
    inputManager.addMapping(ACTION_ROTATE_AROUND_TARGET_LEFT, new KeyTrigger(KeyInput.KEY_J))
    inputManager.addMapping(ACTION_ROTATE_AROUND_TARGET_RIGHT, new KeyTrigger(KeyInput.KEY_L))
    inputManager.addMapping(ACTION_ROTATE_AROUND_TARGET_UP, new KeyTrigger(KeyInput.KEY_I))
    inputManager.addMapping(ACTION_ROTATE_AROUND_TARGET_DOWN, new KeyTrigger(KeyInput.KEY_K))

    inputManager.addMapping(ACTION_SELECT, new MouseButtonTrigger(MouseInput.BUTTON_RIGHT))

    inputManager.addMapping(ACTION_DELETE_SELECTED, new KeyTrigger(KeyInput.KEY_DELETE))

    inputManager.addListener(niftyGuiActionListener, ACTION_TOGGLE_INSTRUCTIONS)
    inputManager.addListener(niftyGuiActionListener, ACTION_TOGGLE_GUI)
    inputManager.addListener(niftyGuiActionListener, ACTION_TOGGLE_CONSOLE)
    inputManager.addListener(niftyGuiActionListener, ACTION_HIDE_MENU)

    inputManager.addListener(actionListener, ACTION_SELECT)
    inputManager.addListener(actionListener, ACTION_TOGGLE_HIGHLIGHT)
    inputManager.addListener(actionListener, ACTION_TOGGLE_WIREFRAME)
    inputManager.addListener(actionListener, ACTION_RESET_CAMERA)
    inputManager.addListener(actionListener, ACTION_SHOW_BOUNDINGBOX)
    inputManager.addListener(actionListener, ACTION_CLEAR_SELECTED)
    inputManager.addListener(actionListener, ACTION_TAKE_SCREENSHOT)

    inputManager.addListener(analogListener, ACTION_ROTATE_AROUND_TARGET_LEFT)
    inputManager.addListener(analogListener, ACTION_ROTATE_AROUND_TARGET_RIGHT)
    inputManager.addListener(analogListener, ACTION_ROTATE_AROUND_TARGET_UP)
    inputManager.addListener(analogListener, ACTION_ROTATE_AROUND_TARGET_DOWN)

    inputManager.addListener(actionListener, ACTION_DELETE_SELECTED)

    inputManager.addListener(analogListener, KEY_LEFT)
    inputManager.addListener(analogListener, KEY_RIGHT)
    inputManager.addListener(analogListener, KEY_UP)
    inputManager.addListener(analogListener, KEY_DOWN)
    inputManager.addListener(analogListener, KEY_PAGEUP)
    inputManager.addListener(analogListener, KEY_PAGEDOWN)

    inputManager.addListener(actionListener, KEY_LCTRL)
    inputManager.addListener(actionListener, KEY_RCTRL)
    inputManager.addListener(actionListener, KEY_LSHIFT)
    inputManager.addListener(actionListener, KEY_RSHIFT)
    inputManager.addListener(actionListener, KEY_Q)
  }

  lazy val niftyGuiActionListener = new ActionListener() {
    override def onAction(name: String, keyPressed: Boolean, tpf: Float) {
      if (!keyPressed) {
        name match {
          case ACTION_TOGGLE_GUI => niftyController.toggleMenu()
          case ACTION_TOGGLE_INSTRUCTIONS => niftyController.toggleInstructions()
          case ACTION_TOGGLE_CONSOLE => niftyController.toggleConsole()
          case ACTION_HIDE_MENU => niftyController.hideMenu()
          case _ => {}
        }
      }
    }
  }

  /** Defining the actions: Determine what was hit and how to respond. */
  lazy val actionListener = new ActionListener() {
    override def onAction(name: String, keyPressed: Boolean, tpf: Float) {
      if (keyPressed){
        pressedMappings.add(name)
      }else{
        pressedMappings.remove(name)
      }
      if (pressedMappings.contains(KEY_Q) && (pressedMappings.contains(KEY_LCTRL) || pressedMappings.contains(KEY_RCTRL))) {
        stop()
      }
      if (!enableSceneInput) return
      // These action are on keyup (so check if key is no longer pressed)
      if (!keyPressed) {
        // Scene inputs
        try {
          name match {
            case ACTION_SELECT => doPicking()
            case ACTION_TOGGLE_WIREFRAME => debugVisualizer.toggleWireframeMode()
            case ACTION_TOGGLE_HIGHLIGHT => toggleHighlightMode()
            case ACTION_RESET_CAMERA => resetCamera()
            case ACTION_SHOW_BOUNDINGBOX => debugVisualizer.showBoundingBox(getSelectedNodes)
            case ACTION_CLEAR_SELECTED => clearSelectedNodes()
            case ACTION_TAKE_SCREENSHOT => screenShotState.takeScreenshot()
            case _ => {}
          }
        } catch {
          case ex: Exception => {
            ex.printStackTrace()
          }
        }
      }
    }
  }

  lazy val analogListener = new AnalogListener() {
    override def onAnalog(name: String, value: Float, tpf: Float) {
      if (!enableSceneInput) return

      val ctrlPressed = pressedMappings.contains(KEY_LCTRL) || pressedMappings.contains(KEY_RCTRL)
      val shiftPressed = pressedMappings.contains(KEY_LSHIFT) || pressedMappings.contains(KEY_RSHIFT)
      // TODO: Figure out what we actually want as target and axes of rotation
      val target = jme.userData.getOrElse[Vector3f](cam, UserDataConstants.CAMERA_TARGET, null)
      val up = cam.getUp
      name match {
        case ACTION_ROTATE_AROUND_TARGET_LEFT => rotateCameraAroundTarget(-value, up, up, target)
        case ACTION_ROTATE_AROUND_TARGET_RIGHT => rotateCameraAroundTarget(value, up, up, target)
        case ACTION_ROTATE_AROUND_TARGET_UP => rotateCameraAroundTarget(value, cam.getLeft, cam.getUp, target)
        case ACTION_ROTATE_AROUND_TARGET_DOWN => rotateCameraAroundTarget(-value, cam.getLeft, cam.getUp, target)
        case _ => {}
      }
      if (ctrlPressed) {
        val objUp = jme.worldUp
        val objLeft = jme.worldLeft.negate()
        val objDir = jme.worldFront.negate()
        // If ctrl pressed, rotate object
        name match {
          // TODO: Have correct axis of rotation
          case KEY_LEFT => rotateSelected(-value, objUp)
          case KEY_RIGHT => rotateSelected(value, objUp)
          case KEY_UP => rotateSelected(value, objLeft)
          case KEY_DOWN => rotateSelected(-value, objLeft)
          case KEY_PAGEUP => rotateSelected(value, objDir)
          case KEY_PAGEDOWN => rotateSelected(-value, objDir)
          case _ => {}
        }
      } else if (shiftPressed) {
        // If shift pressed scale object
        name match {
          case KEY_LEFT => resizeSelected(-value)
          case KEY_RIGHT => resizeSelected(+value)
          case KEY_UP => resizeSelected(+value)
          case KEY_DOWN => resizeSelected(-value)
          case _ => {}
        }
      } else {
        val objUp = jme.worldUp
        val objLeft = jme.worldLeft.negate()
        val objDir = jme.worldFront.negate()
        // If arrow keys, move object
        // TODO: Improve factor
        val moveFactor = 30.0f
        val moveAmount = value*moveFactor
        name match {
          // TODO: Have correct directions of movement
          case KEY_LEFT => moveSelected(moveAmount, objLeft)
          case KEY_RIGHT => moveSelected(-moveAmount, objLeft)
          case KEY_UP => moveSelected(moveAmount, objDir)
          case KEY_DOWN => moveSelected(-moveAmount, objDir)
          case KEY_PAGEUP => moveSelected(moveAmount, objUp)
          case KEY_PAGEDOWN => moveSelected(-moveAmount, objUp)
          case _ => {}
        }
      }
    }
  }

  def rotateCameraAroundTarget(delta: Float, axis: Vector3f, up: Vector3f, target: Vector3f) {
    val matrix = jme.getRotateAroundAxisMatrix(target, axis, delta)
    val pos = matrix.mult(cam.getLocation)
    cam.setLocation(pos)
    cam.lookAt(target, up)
  }

  def rotateNodes(nodes: Seq[Spatial], delta: Float, worldAxisDir: Vector3f): Unit = {
    if (nodes.nonEmpty) {
      // Get rotation matrix in world space
      val bb = jme.getBoundingBox(nodes:_*)
      val worldCenter = bb.getCenter
      val rotMatrix = jme.getRotateAroundAxisMatrix(worldCenter, worldAxisDir, delta)
      for (node <- nodes) {
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
      // Flag scene as being modified
      scene.geometricSceneModified = true
    }
  }

  def rotateModelInstance(modelInstance: ModelInstance[Node], delta: Float, worldAxisDir: Vector3f) = {
    val nodeSelected = Seq(modelInstance.nodeSelf)
    val selected = jme.filterOutChildModels(getSelectedNodes)
    rotateNodes(nodeSelected, delta, worldAxisDir)
  }

  def rotateSelected(delta: Float, worldAxisDir: Vector3f) {
    val selected = jme.filterOutChildModels(getSelectedNodes)
    rotateNodes(selected, delta, worldAxisDir)
  }

  def resizeSelected(delta: Float) {
    val selected = jme.filterOutChildModels(getSelectedNodes)
    if (selected.nonEmpty) {
      for (node <- selected) {
        // TODO: Keep center stable
        // TODO: make delta absolute instead of relative...
        node.scale(1.0f + delta)
      }
      // Flag scene as being modified
      scene.geometricSceneModified = true
    }
  }

  def moveNodes(nodes: Seq[Spatial], delta: Float, dir: Vector3f) {
    if (nodes.nonEmpty) {
      val worldDelta = dir.mult(delta)
      for (node <- nodes) {
        val p = node.getWorldTranslation.add( worldDelta )
        jme.moveTo(node, p)
      }
      // Flag scene as being modified
      scene.geometricSceneModified = true
    }

  }

  def moveModelInstance(modelInstance: ModelInstance[Node], delta: Float, worldAxisDir: Vector3f) = {
    val nodeSelected = Seq(modelInstance.nodeSelf)
    val selected = jme.filterOutChildModels(getSelectedNodes)
    moveNodes(nodeSelected, delta, worldAxisDir)
  }

  def moveSelected(delta: Float, dir: Vector3f) {
    val selected = jme.filterOutChildModels(getSelectedNodes)
    moveNodes(selected, delta, dir)
  }

  def doPicking() {
    config.selectMode match {
      case SelectMode.Object => doObjectPicking(false)
      case SelectMode.Mesh => doObjectPicking(true)
    }
  }

  def doObjectPicking(selectMesh: Boolean = false) {
    // 1. Reset results list.
    val results = new CollisionResults()
    // 2a. Get current cursor position
    val click2d = inputManager.getCursorPosition
    val click3d = cam.getWorldCoordinates(
      new Vector2f(click2d.x, click2d.y), 0f).clone()
    val dir = cam.getWorldCoordinates(
      new Vector2f(click2d.x, click2d.y), 1f).subtractLocal(click3d).normalizeLocal()
    // 2b. Aim the ray from clicked point to dir.
    val ray = new Ray(click3d, dir)
    // 3. Collect intersections between Ray and Shootables in results list.
    shootables.collideWith(ray, results)
    // 4. Print the results
    //    println("----- Collisions? " + results.size() + "-----")
    //    for (i <- 0 until results.size()) {
    //      // For each hit, we know distance, impact point, name of geometry.
    //      val dist = results.getCollision(i).getDistance()
    //      val pt = results.getCollision(i).getContactPoint()
    //      val hit = results.getCollision(i).getGeometry().getName()
    //      println("* Collision #" + i)
    //      println("  You shot " + hit + " at " + pt + ", " + dist + " wu away.")
    //    }
    // 5. Use the results (we mark the hit object)
    if (results.size() > 0) {
      // The closest collision point is what was truly hit:
      val closest = jme.getClosestCollision(ray, results)
      val clicked = closest.getGeometry
      //      println(" Selected " + clicked.getName())
      // Go up until we find one with a model field
      var s: Spatial = clicked
      var pickedModelInstNode: Spatial = null
      while ((s != null) && pickedModelInstNode == null) {
        if (jme.isModelInstance(s)) {
          pickedModelInstNode = s
        } else {
          s = s.getParent
        }
      }
      if (pickedModelInstNode != null)  {
        //        println(" Selected model " + selectedModelInst.getName())
        if (selectMesh) {
          toggleSelectMesh(
            jme.getModelInstanceIndex(pickedModelInstNode),
            jme.getMeshIndex(clicked),
            closest.getTriangleIndex)
        } else {
          toggleModelInstanceNodeSelection(pickedModelInstNode.asInstanceOf[Node])
        }
      } else {
      }
    }
  }

  def toggleHighlightMode(mode: HighlightMode.Value = null) {
    if (mode != null) {
      config.highlightMode = mode
    } else {
      val v = (this.highlightMode.id + 1) % HighlightMode.maxId
      config.highlightMode = HighlightMode(v)
    }
    for (modelInstance <- scene.modelInstances.filter( m => m != null)) {
      // TODO: si is used for the false material color
      // The logic here is somewhat weird...
      val si = selections.findSelectionIndex(modelInstance.index)
      this.updateModelInstanceNodeMaterial(modelInstance.node, si+1)
    }
  }

  def saveCameraState() {
    savedCam = cam.clone()
  }

  def resetCamera() {
    val sceneCam = scene.scene.getCamera(CameraInfo.INITIAL)
    if (scene != null && sceneCam != null) {
      logger.info("reset camera from scene cam")
      // Convert from scene up/front to world up/front
      val worldCam = jme.transformCameraInfoFromSceneToWorld(sceneCam, scene.scene)
      setCamera(worldCam)
    } else if (savedCam != null) {
      logger.info("reset camera from saved cam")
      cam.copyFrom(savedCam)
    }
  }

  def addFloor(floorCenter: Vector3f, floorExtent: Vector3f, minR: Float = 0.0f) {
    val floor_mat = {
      val material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md")
      material.setName("shaded material")
      material.setBoolean("UseMaterialColors", true)
      material.setBoolean("UseAlpha", false)
      val diffuse = ColorRGBA.White
      val ambient = diffuse.mult(0.1f)
      material.setColor("Ambient", ambient)
      material.setColor("Diffuse", diffuse)
      material
    }

    /** Initialize the floor geometry */
    val floor_mesh = if (config.useRadialFloor) {
      val r0 = math.sqrt(floorExtent.getX*floorExtent.getX + floorExtent.getZ*floorExtent.getZ).toFloat
      val r = math.max(r0, minR)
      new Cylinder(6, 36, r, floorExtent.getY, true)
    } else {
      val x = math.max(floorExtent.getX, minR)
      val z = math.max(floorExtent.getZ, minR)
      new Box(x, floorExtent.getY, z)
    }

    val floor_geo = new Geometry("Floor", floor_mesh)
    floor_geo.setMaterial(floor_mat)
    floor_geo.setLocalTranslation(floorCenter.getX, floorCenter.getY-floorExtent.getY, floorCenter.getZ)
    if (config.useRadialFloor) {
      jme.alignLocalToUpFrontAxes(floor_geo, Vector3f.UNIT_Z, Vector3f.UNIT_X, Vector3f.UNIT_Y, Vector3f.UNIT_X)
    }
    if (config.useShadow) {
      floor_geo.setShadowMode(ShadowMode.Receive)
      //TangentBinormalGenerator.generate(floor_geo)
    }
    this.rootNode.attachChild(floor_geo)
    floor = floor_geo
    // Make sure our floor also have lights
    for (light <- rootSceneNode.getLocalLightList) {
      floor.addLight(light)
    }
  }

  def addFloor(scenebb: BoundingBox): Unit = {
    val scenebbMin = new Vector3f()
    scenebb.getMin(scenebbMin)
    val floorCenter = scenebb.getCenter.clone()
    floorCenter.setY(scenebbMin.getY)
    val floorExtent = new Vector3f()
    scenebb.getExtent(floorExtent)
    floorExtent.setY(1f)
    floorExtent.multLocal(config.floorSizeRatio.toFloat)
    addFloor(floorCenter, floorExtent, config.minFloorSize/2)
  }

  def removeFloor(): Unit = {
    if (floor != null) {
      floor.removeFromParent()
      floor = null
    }
  }

  def updateFloor(): Unit = {
    removeFloor()
    if (config.addFloor && scene != null) {
      addFloor(jme.getBoundingBox(scene.node))
    }
  }

  /** Functions for selecting a model instance node */

  def updateModelInstanceNodeMaterial(node: Node, i: Int) {
    updateModelInstanceNodeMaterial(node, jme.getSimpleFalseColorMaterial(i))
  }

  def updateModelInstanceNodeMaterial(node: Node, material: => Material) {
    var useFalseMaterial = false
    var m = material
    val isSelected = jme.userData.getOrElse(node, UserDataConstants.IS_SELECTED, false)
    this.highlightMode match {
      case HighlightMode.HighlightSelectedFalseBkFalse => {
        useFalseMaterial = true
        if (!isSelected) m = this.falseBkMaterial
      }
      case HighlightMode.HighlightSelectedOrigBkFalse => {
        useFalseMaterial = !isSelected
        if (!isSelected) m = this.falseBkMaterial
      }
      case HighlightMode.HighlightSelectedFalseBkOrig => {
        useFalseMaterial = isSelected
      }
    }
    jme.revertMaterials(node)
    if (useFalseMaterial) {
      jme.setMaterials(node, m, saveOldMaterial = true, blendOld = falseMaterialBlendOld)
    }
  }

  def toggleModelInstanceNodeSelection(i: Int) {
    if (scene != null && i >= 0 && i < scene.modelInstances.size) {
      val node = scene.modelInstances(i).node
      toggleModelInstanceNodeSelection(node)
    }
  }

  def toggleModelInstanceNodeSelection(node: Node) {
    val isSelected = jme.userData.getOrElse(node, UserDataConstants.IS_SELECTED, false)
    jme.userData.set(node, UserDataConstants.IS_SELECTED, !isSelected)
    updateModelInstanceNodeSelection(node)
  }

  def updateModelInstanceNodeSelection(node: Node) {
    val objIndex = jme.getModelInstanceIndex(node)
    var i = selections.findSelectionIndex(objIndex)
    // TODO: Make sure material is not used before...
    val isSelected = jme.userData.getOrElse(node, UserDataConstants.IS_SELECTED, false)
    if (isSelected) {
      // Make sure it is in list of selected nodes
      if (i < 0) {
        i = selections.length
        val objIndex = jme.getModelInstanceIndex(node)
        selections.add(SceneSelection(objIndex))
      }
      updateModelInstanceNodeMaterial(node, i+1)
    } else {
      // Make sure it is removed from list of selected models
      if (i >= 0) {
        selections.remove(i)
      }
      updateModelInstanceNodeMaterial(node, falseBkMaterial)
    }
  }

  def clearSelectedNodes() {
    // Clear our list of selected nodes
    for (modelInstanceNode <- getSelectedNodes) {
      jme.userData.set(modelInstanceNode, UserDataConstants.IS_SELECTED, false)
      updateModelInstanceNodeMaterial(modelInstanceNode, falseBkMaterial)
    }
    if (scene != null) {
      for (modelInstance <- scene.modelInstances) {
        jme.setVisible(modelInstance.node, true)
        jme.setWireframeMode(modelInstance.node, false)
      }
    }
    // Also remove any debugging state
    debugVisualizer.clear()
    selections.clear()
  }

  def setSelected(modelIndex: Int): Unit = {
    setSelected(Seq(SceneSelection(modelIndex)))
  }

  def setSelected(selected: Seq[SceneSelection]) {
    clearSelectedNodes()
    for ((sel,i) <- selected.zipWithIndex) {
      val objIndex = sel.objectIndex
      val modelInstance = scene.modelInstances(objIndex)
      val modelInstanceNode = modelInstance.node
      if (sel.partType == null) {
        jme.userData.set(modelInstanceNode, UserDataConstants.IS_SELECTED, true)
        updateModelInstanceNodeMaterial(modelInstanceNode, i+1)
      } else if (sel.partType == PartType.MESH) {
        showMesh(objIndex, sel.partIndex)
      }
      selections.select(sel)
    }
    toggleHighlightMode(highlightMode)
  }

  def setSelectedNodesFromIndices(modelInstanceIndices: IndexedSeq[Int]) {
    setSelectedNodes(modelInstanceIndices.map( i => scene.modelInstances(i).node ))
  }

  def setSelectedNodes(modelInstanceNodes: IndexedSeq[Node]) {
    clearSelectedNodes()
    for ((modelInstanceNode, i) <- modelInstanceNodes.zipWithIndex) {
      jme.userData.set(modelInstanceNode, UserDataConstants.IS_SELECTED, true)
      val objIndex = jme.getModelInstanceIndex(modelInstanceNode)
      selections.add(SceneSelection(objIndex))
      updateModelInstanceNodeMaterial(modelInstanceNode, i+1)
    }
    toggleHighlightMode(highlightMode)
  }

  def setSelectedNodes(modelInstanceIds: Seq[String]) {
    clearSelectedNodes()
    // Find first models in scene with specified modelIds and add them to our selectedModels
    val modelInstanceIdRegex = new Regex("(.*)#(\\d+)")
    for ((modelInstanceId,i) <- modelInstanceIds.zipWithIndex) {
      // If modelId ends with #i then that indicates it is the ith instance
      modelInstanceId match {
        case modelInstanceIdRegex(modelId, indexStr) => {
          val modelInstanceNodes = findModelInstanceNodes(modelId)
          var index = indexStr.toInt
          if (index >= modelInstanceNodes.length) {
            logger.warn("No instance " + index + " for model " + modelId)
            index = 0
          }
          if (modelInstanceNodes.length > 0) {
            val node = modelInstanceNodes(index)
            jme.userData.set(node, UserDataConstants.IS_SELECTED, true)
            val objIndex = jme.getModelInstanceIndex(node)
            selections.add(SceneSelection(objIndex))
            updateModelInstanceNodeMaterial(node, i+1)
          }
        }
        case _ => {
          logger.warn("Invalid modelInstanceId " + modelInstanceId)
        }
      }
    }
    toggleHighlightMode(highlightMode)
  }

  def findModelInstanceNodes(modelId: String): mutable.IndexedSeq[Node] = {
    val matched = findModelInstances(modelId)
    matched.map( x => x.node )
  }

  def findModelInstances(modelId: String): mutable.IndexedSeq[ModelInstance[Node]] = {
    if (modelId.length == 0) return scene.modelInstances
    // Find model instances in scene matching modelId
    val matchedModelInstances = new mutable.ArrayBuffer[ModelInstance[Node]]()
    for (modelInstance <- scene.modelInstances.filter( m => m != null)) {
      // TODO: Get information from modelInstance directly instead of the userdata...
      val model = jme.userData.getOrElse[Model[Node]](modelInstance.node, UserDataConstants.MODEL, null)
      if (model != null && model.fullId == modelId) {
        matchedModelInstances.add(modelInstance)
      }
    }
    matchedModelInstances
  }

  def enqueueAndWait[V]( f: Unit => V): V = {
    val callable = new Callable[V]() {
      override def call(): V = {
        f()
      }
    }
    val future = enqueue(callable)
    future.get()
  }
}

object ViewerState extends Enumeration {
  type ViewerState = Value
  val INIT, LOAD, BUSY, READY = Value
}

object HighlightMode extends Enumeration {
  type HighlightMode = Value
  val HighlightSelectedFalseBkOrig, HighlightSelectedFalseBkFalse, HighlightSelectedOrigBkFalse = Value
}

object SelectMode extends Enumeration {
  val Object, Mesh = Value
}

object Viewer extends App {
  val config = ConfigHelper.fromOptions(args:_*)
  // Run viewer
  val viewerConfig = ViewerConfig(config)
  WebUtils.useCache = viewerConfig.cacheWebFiles
  val app = new Viewer(viewerConfig)
  app.setShowSettings(viewerConfig.showSettings)
  val appSettings = new AppSettings(true)
  //appSettings.setTitle("ShapeNet Viewer")
  if (viewerConfig.width.isDefined || viewerConfig.height.isDefined) {
    if (viewerConfig.width.isDefined && viewerConfig.height.isDefined) {
      appSettings.setResolution(viewerConfig.width.get, viewerConfig.height.get)
    } else {
      app.logger.warn("Please define both width and height!")
    }
  }
  app.setSettings(appSettings)
  app.setPauseOnLostFocus(false)
  app.initConfig()
  app.start() // start the game
}
