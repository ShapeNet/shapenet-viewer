package edu.stanford.graphics.shapenet.jme3.viewer

import com.jme3.system._
import com.jme3.renderer.{ViewPort, Renderer, Camera, RenderManager}
import com.jme3.scene.{Geometry, Node, Spatial}
import com.jme3.math.{Vector3f, ColorRGBA}
import edu.stanford.graphics.shapenet.jme3.Jme
import com.jme3.scene.shape.Box
import com.jme3.material.Material

/**
 * Simple API to render a scene
 * @author Angel Chang
 */
class SimpleRenderer(val width: Int, val height: Int, val isOffscreen: Boolean = false) extends SystemListener {
  val settings: AppSettings = new AppSettings(true)
  settings.setWidth(width)
  settings.setHeight(height)

  val contextType = if (isOffscreen) JmeContext.Type.OffscreenSurface else JmeContext.Type.Display
  val context = JmeSystem.newContext(settings, contextType)
  context.setSystemListener(this)
  context.create(true)

  var timer: Timer = null
  var renderer: Renderer = null
  var renderManager: RenderManager = null

  var speed = 1f
  var camera: Camera = null
  var viewport: ViewPort = null
  var rootNode: Node = null
  private var scene: Spatial = null

  def getCamera = camera
  def setCamera(cam: Camera) {
    this.camera.copyFrom(cam)
  }

  def renderScene(scene: Spatial) {
    this.scene = scene
    //renderManager.renderScene()
  }

  def destroy() {}

  def handleError(errorMsg: String, t: Throwable) {}

  def loseFocus() {}

  def gainFocus() {}

  def requestClose(esc: Boolean) {}

  def update() {
    val tpf: Float = timer.getTimePerFrame * speed
    if (scene != null) {
      rootNode.detachAllChildren()
      rootNode.attachChild(scene)
    }
    rootNode.updateLogicalState(tpf)
    rootNode.updateGeometricState()
    renderManager.render(tpf, context.isRenderable)
  }

  def reshape(width: Int, height: Int) {}

  def initialize() {
    timer = context.getTimer
    renderer = context.getRenderer
    renderManager = new RenderManager(renderer)
    renderManager.setTimer(timer)

    camera = new Camera(width,height)
    camera.setFrustumPerspective(45f, camera.getWidth().toFloat / camera.getHeight(), 1f, 1000f)
    camera.setLocation(new Vector3f(0f, 0f, 10f))
    camera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y)

    // Attach viewport to renderer
    //  val viewport = renderManager.createPreView("Offscreen View", camera)
    viewport = renderManager.createMainView("Default View", camera)
    viewport.setBackgroundColor(ColorRGBA.White)
    viewport.setClearFlags(true, true, true)

    rootNode = new Node()
    viewport.attachScene(rootNode)
  }
}

class SimpleOffscreenRenderer(width: Int, height: Int) extends SimpleRenderer(width, height, true) {
  private var offscreenAnalyzer: OffscreenAnalyzer = null
  def getSceneStats = offscreenAnalyzer.getSceneStats
  def getOffScreen = offscreenAnalyzer.getOffScreen
  def getOffScreenDisplay = offscreenAnalyzer.getOffScreenDisplay
  def getOffScreenAnalyzer = offscreenAnalyzer
  val renderTasks = new RenderTaskQueue()

  private def prepareOffscreen() {
    // Prepare a offscreen view for offscreen computations
    offscreenAnalyzer = new OffscreenAnalyzer(renderManager, camera.getWidth, camera.getHeight/*, rootSceneNode.getLocalTransform*/)
  }

  override def initialize() {
    super.initialize()
    prepareOffscreen()
  }

  override def update() {
    super.update()
    val tpf: Float = timer.getTimePerFrame * speed
    renderTasks.update(tpf)
    if (offscreenAnalyzer != null) offscreenAnalyzer.update(tpf)
  }
}


object SimpleRendererTest extends App {
  val jme = Jme()
  val b = new Box(1, 1, 1) // create cube shape
  val geom = new Geometry("Box", b)  // create cube geometry from the shape
  val mat = new Material(jme.assetManager,
    "Common/MatDefs/Misc/Unshaded.j3md")  // create a simple material
  mat.setColor("Color", ColorRGBA.Blue)   // set color of material to blue
  geom.setMaterial(mat);                   // set the cube's material

  val simpleRenderer = new SimpleRenderer(800,600,isOffscreen=false)
  simpleRenderer.renderScene(geom)
}
