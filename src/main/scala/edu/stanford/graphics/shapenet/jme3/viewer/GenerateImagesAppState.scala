package edu.stanford.graphics.shapenet.jme3.viewer

import au.com.bytecode.opencsv.CSVWriter
import com.jme3.app.Application
import com.jme3.app.state.AppStateManager
import com.jme3.scene.Node
import com.jme3.math.Transform
import edu.stanford.graphics.shapenet.common._
import edu.stanford.graphics.shapenet.jme3.geom.BoundingBoxUtils
import edu.stanford.graphics.shapenet.util.ConversionUtils._
import edu.stanford.graphics.shapenet.util.IOUtils

import scala.concurrent.{Promise,Future}

/**
 * App state for generating and saving away images
 */
class GenerateImagesAppState(val viewer: Viewer,
                             private var screenShotDir: String,
                             private var nObjects: Int = 0) extends ScreenshotAppState()  {

  val SET_SUMMARYFILE = "SetSummaryFile"
  val CLOSE_SUMMARYFILE = "CloseSummaryFile"
  val GEN_IMAGES_FOR_SCENE = "GenImagesForScene"
  val SET_SCREENSHOT_DIR = "SetScreenshotDir"
  val DELIVER_PROMISE = "DeliverPromise"

  case class Action(name: String, args: Any*) {
    // State indicates action processing state
    var state: String = ActionStates.EMPTY

    def withState(s: String) = {
      state = s
      this
    }
  }

  var imageFilenameUseFullId = true
  var revertTransformationForBbdims = false

  // Already determined screen shots
  private val screenshotQueue = new scala.collection.mutable.Queue[ScreenShotInfo]()

  // Sequences of actions that we want to take
  private val actionQueue = new scala.collection.mutable.Queue[Action]()

  private var summaryFile: CSVWriter = null
  private var summaryFilename: String = null
  override def initialize(stateManager: AppStateManager, app: Application)
  {
    if (!super.isInitialized) {
      // TODO: initialization
    }
    super.initialize(stateManager, app)
  }

  def setSummaryFile(summaryFilename: String, append: Boolean = false) = {
    actionQueue.synchronized {
      actionQueue += Action(SET_SUMMARYFILE, summaryFilename, append)
    }
  }

  def closeSummaryFile() {
    actionQueue.synchronized {
      actionQueue += Action(CLOSE_SUMMARYFILE)
    }
  }

  def setScreenShotDir(screenShotDir: String) {
    actionQueue.synchronized {
      actionQueue += Action(SET_SCREENSHOT_DIR, screenShotDir)
    }
  }

  private def _setScreenShotDir(screenShotDir: String) {
    IOUtils.createDirs(screenShotDir)
    this.screenShotDir = screenShotDir
  }

  private def _closeSummaryFile() {
    if (summaryFile != null) {
      println("Closing " + summaryFilename)
      summaryFile.close()
      summaryFile = null
      summaryFilename = null
    }
  }

  private def _setSummaryFile(summaryFilename: String, append: Boolean = false) = {
    // Finish old file
    _closeSummaryFile()
    this.summaryFilename = summaryFilename
    if (summaryFilename != null) {
      println("Opening " + summaryFilename)
      val hasData = IOUtils.isReadableFileWithData(summaryFilename)
      summaryFile = new CSVWriter(IOUtils.filePrintWriter(summaryFilename, append))
      if (!append || !hasData) {
        // Only output header if empty
        summaryFile.writeNext(Array("scene","image","bbmin","bbmax","camera.position","camera.up","camera.target","camera.direction")
          ++ (1 to nObjects).toArray.map( i => "obj" + i ))
      }
    }
  }

  def enqueue(items: ScreenShotInfo*) = screenshotQueue.synchronized{
    screenshotQueue ++= items
    enqueuePromise()
  }

  def enqueueSceneIds(ids: String*) = actionQueue.synchronized{
    actionQueue ++= ids.map( id => Action(GEN_IMAGES_FOR_SCENE, id) )
    enqueuePromise()
  }

  def enqueueSceneIds(genFn: GenerateImagesFn, actionState: String, ids: String*) = actionQueue.synchronized{
    actionQueue ++= ids.map( id => Action(GEN_IMAGES_FOR_SCENE, id, genFn).withState(actionState) )
    enqueuePromise()
  }

  def enqueueScenes(scenes: Scene*) = actionQueue.synchronized{
    actionQueue ++= scenes.map( scene => Action(GEN_IMAGES_FOR_SCENE, scene) )
    enqueuePromise()
  }

  def enqueueScenes(genFn: GenerateImagesFn, actionState: String, scenes: Scene*) = actionQueue.synchronized{
    actionQueue ++= scenes.map( scene => Action(GEN_IMAGES_FOR_SCENE, scene, genFn).withState(actionState) )
    enqueuePromise()
  }

  def enqueueScenesWithFilenames(scenes: (Scene,String)*) = actionQueue.synchronized{
    actionQueue ++= scenes.map( scene => Action(GEN_IMAGES_FOR_SCENE, scene) )
    enqueuePromise()
  }

  def enqueueScenesWithFilenames(genFn: GenerateImagesFn, actionState: String, scenes: (Scene,String)*) = actionQueue.synchronized{
    actionQueue ++= scenes.map( scene => Action(GEN_IMAGES_FOR_SCENE, scene, genFn).withState(actionState) )
    enqueuePromise()
  }

  def enqueueSceneStates(scenes: SceneState*) = actionQueue.synchronized{
    actionQueue ++= scenes.map( scene => Action(GEN_IMAGES_FOR_SCENE, scene) )
    enqueuePromise()
  }

  def enqueueSceneStates(genFn: GenerateImagesFn, actionState: String, scenes: SceneState*) = actionQueue.synchronized{
    actionQueue ++= scenes.map( scene => Action(GEN_IMAGES_FOR_SCENE, scene, genFn).withState(actionState) )
    enqueuePromise()
  }

  def enqueueSceneStatesWithFilenames(scenes: (SceneState,String)*) = actionQueue.synchronized{
    actionQueue ++= scenes.map( scene => Action(GEN_IMAGES_FOR_SCENE, scene) )
    enqueuePromise()
  }

  def enqueueSceneStatesWithFilenames(genFn: GenerateImagesFn, actionState: String, scenes: (SceneState,String)*) = actionQueue.synchronized{
    actionQueue ++= scenes.map( scene => Action(GEN_IMAGES_FOR_SCENE, scene, genFn).withState(actionState) )
    enqueuePromise()
  }

  private def enqueuePromise(value: String = "ok"): Future[String] = {
    val promise = Promise[String]
    actionQueue.synchronized{
      actionQueue += Action(DELIVER_PROMISE, promise, value)
    }
    promise.future
  }

  def processScenes(outputDir: String, scenes: Scene*): Future[String] = {
    val oldOutputDIr = screenShotDir
    setScreenShotDir(outputDir)
    setSummaryFile(outputDir + "summary.csv")
    enqueueScenes(scenes:_*)
    closeSummaryFile()
    setScreenShotDir(oldOutputDIr)
    enqueuePromise()
  }

  def processScenes(outputDir: String, actionState: String, genFn: GenerateImagesFn, scenes: Scene*): Future[String] = {
    val oldOutputDIr = screenShotDir
    setScreenShotDir(outputDir)
    setSummaryFile(outputDir + "summary.csv")
    enqueueScenes(genFn, actionState, scenes:_*)
    closeSummaryFile()
    setScreenShotDir(oldOutputDIr)
    enqueuePromise()
  }

  def processScenesWithFilenames(outputDir: String, scenes: (Scene,String)*): Future[String] = {
    val oldOutputDIr = screenShotDir
    setScreenShotDir(outputDir)
    setSummaryFile(outputDir + "summary.csv")
    enqueueScenesWithFilenames(scenes:_*)
    closeSummaryFile()
    setScreenShotDir(oldOutputDIr)
    enqueuePromise()
  }

  def processScenesWithFilenames(outputDir: String, actionState: String, genFn: GenerateImagesFn, scenes: (Scene,String)*): Future[String] = {
    val oldOutputDIr = screenShotDir
    setScreenShotDir(outputDir)
    setSummaryFile(outputDir + "summary.csv")
    enqueueScenesWithFilenames(genFn, actionState, scenes:_*)
    closeSummaryFile()
    setScreenShotDir(oldOutputDIr)
    enqueuePromise()
  }

  def processSceneStatesWithFilenames(outputDir: String, actionState: String, genFn: GenerateImagesFn, scenes: (SceneState,String)*): Future[String] = {
    val oldOutputDIr = screenShotDir
    setScreenShotDir(outputDir)
    setSummaryFile(outputDir + "summary.csv")
    enqueueSceneStatesWithFilenames(genFn, actionState, scenes:_*)
    closeSummaryFile()
    setScreenShotDir(oldOutputDIr)
    enqueuePromise()
  }

  def processCurrent(filenameBase: String): Future[String] = {
    val id = viewer.scene.scene.sceneId
    generateImagesForScene(viewer.scene, filenameBase)
    enqueuePromise()
  }

  def processCurrentAllCameras(filenameBase: String): Future[String] = {
    val scene = viewer.scene.scene
    val id = scene.sceneId
    if (scene.cameras nonEmpty) {
      val camStates = scene.cameras.map( camInfo =>
        CameraState(viewer.jme.transformCameraInfoFromSceneToWorld(camInfo, scene))
      )
      generateImagesForScene(viewer.scene, filenameBase, cameras = camStates)
    } else {
      generateImagesForScene(viewer.scene, filenameBase)
    }
    enqueuePromise()

  }

  // Is there anything still left on the queue to do...
  def isEmpty() = {
   actionQueue.synchronized {
      actionQueue.isEmpty
   } && screenshotQueue.synchronized {
     screenshotQueue.isEmpty
   }
  }

  override def update(tpf: Float) {
    if (!viewer.isReady()) return

    var processed = false
    screenshotQueue.synchronized {
      if (screenshotQueue.nonEmpty) {
        val item = screenshotQueue.head
        item.state match {
          case ScreenShotState.INIT => {
            println("Loading scene for " + item.filename)
            if (!viewer.isSceneLoaded(item.sceneId)) {
              if (item.scene != null) {
                // Constructed scene... TODO: need to make sure we have unique scene Ids
                viewer.loadScene(item.scene, async = true)
              } else {
                viewer.loadScene(item.sceneId, async = true)
              }
            }
            item.state = ScreenShotState.LOAD_SCENE
          }
          case ScreenShotState.LOAD_SCENE => {
            if (item.highlightMode != viewer.highlightMode)
              viewer.toggleHighlightMode(item.highlightMode)
            viewer.setSelectedNodesFromIndices(item.selectedModelIndices)
            if (item.camera == null && item.optimizeView) {
              println("Optimize camera position for " + item.filename)
              val listener = new CameraOptimizationListener() {
                override def optimized(result: CameraOptimizationResult) {
                  item.camera = result.cameraState
                  item.state = ScreenShotState.VIEW_READY
                }
              }
              viewer.optimizeCameraPosition(viewer.config.nCameraPositionsForOptimize, listener)
              item.state = ScreenShotState.OPTIMIZE_VIEW
            } else {
              item.state = ScreenShotState.VIEW_READY
            }
          }
          case ScreenShotState.OPTIMIZE_VIEW => {
            // optimizing view, waiting
          }
          case ScreenShotState.VIEW_READY => {
            // view ready, wait one screen update before taking screenshot
            // Make sure the camera state is good
            if (item.camera != null) {
              viewer.setCamera(item.camera)
            }
            onViewReady();
            item.state = ScreenShotState.TAKE_SHOT
          }
          case ScreenShotState.TAKE_SHOT => {
            def toString[T >: Null](o:T) = if (o == null) "" else o.toString()
            println("Taking screen shot for " + item.filename)
            if (summaryFile != null) {
              // Code to revert any transformation
              var oldTransform: Transform = null
              if (revertTransformationForBbdims) {
                oldTransform = viewer.scene.node.getLocalTransform.clone()
                viewer.scene.node.setLocalTransform(Transform.IDENTITY)
              }
              val bb = viewer.jme.getBoundingBox(viewer.scene.node)
              val (bbmin,bbmax) = BoundingBoxUtils.getBBMinMax(bb)
              // Put transformations back
              if (oldTransform != null) {
                viewer.scene.node.setLocalTransform(oldTransform)
              }

              var imageName = if (screenShotDir.nonEmpty) item.filename.replace(screenShotDir,"") else item.filename
              imageName = imageName.replaceAll("\\\\","/")
              val row =
                Array(item.id, imageName,
                  toString(bbmin), toString(bbmax),
                  toString(item.camera.position),
                  toString(item.camera.up),
                  toString(item.camera.target),
                  toString(item.camera.direction)) ++
                  item.selectedModelIndices.map( x => x.toString )
              summaryFile.writeNext(row)
              summaryFile.flush()
            }
            takeScreenshot(item.filename)
            item.state = ScreenShotState.DONE
            screenshotQueue.dequeue()
            if (screenshotQueue.isEmpty) {
              println("Finished processing screen shots")
            }
          }
        }
        processed = true
      }
    }

    if (!processed) {
      actionQueue.synchronized {
        if (actionQueue.nonEmpty) {
          val action = actionQueue.head
          var dequeue = true
          println("Process action " + action)
          action.name match {
            case GEN_IMAGES_FOR_SCENE => {
              val (sceneId,sceneState,filenameBase): (String, SceneState, String) = action.args(0) match {
                case sceneId: String => (sceneId, null, null)
                case scene: Scene => (scene.sceneId, SceneState(scene), null)
                case (scene: Scene, filenameBase: String) => (scene.sceneId, SceneState(scene), filenameBase)
                case sceneState: SceneState => (sceneState.scene.sceneId, sceneState, null)
                case (sceneState: SceneState, filenameBase: String) => (sceneState.scene.sceneId, sceneState, filenameBase)
              }
              if (action.state == ActionStates.FORCE_LOAD || !viewer.isSceneLoaded(sceneId)) {
                println("Need to load scene " + sceneId)
                dequeue = false // Keep on queue
                if (action.state == ActionStates.FORCE_LOAD) {
                  action.state = ActionStates.EMPTY
                }
                if (sceneState != null) {
                  // Constructed scene... TODO: need to make sure we have unique scene Ids
                  viewer.loadSceneState(sceneState, async = true, onerror = () => {
                    // need to move on error
                    actionQueue.synchronized {
                      actionQueue.dequeueFirst( a => a == action )
                    }
                  })
                } else {
                  viewer.load(sceneId, async = true, onerror = () => {
                    // need to move on error
                    actionQueue.synchronized {
                      actionQueue.dequeueFirst( a => a == action )
                    }
                  })
                }
              } else if (action.state.length < 2) {
                // Make sure there is a frame where the scene is loaded before proceeding
                if (action.state.length % 50 == 0) {
                  println("Loaded scene " + sceneId + " waited " +  action.state.length)
                }
                dequeue = false // Keep on queue
                action.state += "x"
              } else {
                // Make sure menu is hidden
                viewer.hideMenu()
                println("Generating images for scene " + sceneId)
                // Images are queued into the screenshotQueue for the next update
                if (action.args.length > 1) {
                  val genFn = action.args(1).asInstanceOf[GenerateImagesFn]
                  generateImagesForScene(viewer.scene, filenameBase, genFn)
                } else {
                  generateImagesForScene(viewer.scene)
                }
              }
            }
            case DELIVER_PROMISE => {
              val promise = action.args(0).asInstanceOf[Promise[Any]]
              val result = action.args(1)
              promise.success(result)
            }
            case SET_SCREENSHOT_DIR => {
              _setScreenShotDir(action.args(0).asInstanceOf[String])
            }
            case SET_SUMMARYFILE => {
              _setSummaryFile(action.args(0).asInstanceOf[String], action.args(1).asInstanceOf[Boolean])
            }
            case CLOSE_SUMMARYFILE => {
              _closeSummaryFile()
            }
          }
          if (dequeue) {
            actionQueue.dequeue()
          }
        }
      }
    }
  }

  protected def generateImagesForScene(scene: GeometricScene[Node], filenameBase: String = null,
                                       genFn: GenerateImagesFn = null, cameras: Seq[CameraState] = Seq()) {
    // Pass in genFn to generate other images
    // We just add the scene as is
    val id = scene.scene.sceneId
    val myFilenameBase = if (filenameBase != null) {
      filenameBase
    } else {
      val filename = if (imageFilenameUseFullId) FullId(id).fullid else {
        IOUtils.getFilename(FullId(id).id)
      }
      screenShotDir + filename
    }
    if (genFn == null) {
      val screenshot = new ScreenShotInfo(id, myFilenameBase + "." + imageFormat, id, state = ScreenShotState.VIEW_READY)
      screenshotQueue.synchronized {
        screenshotQueue.enqueue(screenshot)
      }
      if (cameras.nonEmpty) {
        for ((cam,index) <- cameras.zipWithIndex) {
          val camScreenshot = new ScreenShotInfo(id + "-" + index, myFilenameBase + "-" + index + "." + imageFormat, id,
            camera = cam, state = ScreenShotState.VIEW_READY)
          screenshotQueue.synchronized {
            screenshotQueue.enqueue(camScreenshot)
          }
        }
      }
    } else {
      val options = GenerateImagesFnOptions(sceneId = id, outputDir = screenShotDir,
        filenameBase = myFilenameBase, imageFormat = imageFormat)
      val screenshots = genFn(options, scene)
      screenshotQueue.synchronized {
        screenshotQueue.enqueue(screenshots:_*)
      }
    }
  }

  override def cleanup() {
    _closeSummaryFile()
  }

  // For OffscreenGenerateImagesAppState to override
  def onViewReady() {}
}

class OffscreenGenerateImagesAppState(viewer: Viewer,
                                      screenShotDir: String,
                                      nObjects: Int = 0) extends GenerateImagesAppState(viewer, screenShotDir, nObjects)  {

  private var offscreenView: OffscreenView = null

  override def onViewReady(): Unit = {
    offscreenView = viewer.getOffScreen
    offscreenView.viewScene(viewer.rootSceneNode.clone())
    offscreenView.setCamera(viewer.getCamera)
  }

  override def takeScreenshot(filename: String): Unit = {
    offscreenView.saveImage(filename)
  }

}

trait GenerateImagesFn {
  def toScreenShot(options: GenerateImagesFnOptions, cam: CameraState, index: Int, scene: Scene = null): ScreenShotInfo = {
    val screenshot = new ScreenShotInfo(
      id = options.sceneId + "-" + index,
      filename = options.filenameBase + "-" + index + "." + options.imageFormat,
      sceneId = options.sceneId,
      state = ScreenShotState.VIEW_READY,
      camera = cam,
      scene = scene
    )
    screenshot
  }
  def toScreenShotWithOptimizedView(options: GenerateImagesFnOptions, index: Int, scene: Scene = null): ScreenShotInfo = {
    val screenshot = new ScreenShotInfo(
      id = options.sceneId + "-" + index,
      filename = options.filenameBase + "-" + index + "." + options.imageFormat,
      sceneId = options.sceneId,
      state = ScreenShotState.LOAD_SCENE,
      camera = null,
      scene = scene,
      optimizeView = true
    )
    screenshot
  }
  def apply(options: GenerateImagesFnOptions, scene: GeometricScene[Node]): Seq[ScreenShotInfo]
}

case class GenerateImagesFnOptions(
  sceneId: String,
  outputDir: String,
  filenameBase: String,
  imageFormat: String
)

object ActionStates {
  val EMPTY = ""
  val FORCE_LOAD = "FORCE_LOAD"
}

object ScreenShotState extends Enumeration {
  type ScreenShotState = Value
  val INIT, LOAD_SCENE, OPTIMIZE_VIEW, VIEW_READY, TAKE_SHOT, DONE = Value
}

class ScreenShotInfo(val id: String, // Unique id for this screen shot
                     val filename: String, // File name to save this screen shot
                     val sceneId: String, // Scene Id
                     val selectedModelIndices: IndexedSeq[Int] = IndexedSeq(),
                     val highlightMode: HighlightMode.Value = HighlightMode.HighlightSelectedFalseBkOrig,
                     var state: ScreenShotState.Value = ScreenShotState.INIT,
                     var camera: CameraState = null,
                     var scene: Scene = null, // Scene associated with the screen shot info
                     val optimizeView: Boolean = false // Should the view be automatically optimized?
                      )



