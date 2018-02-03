package edu.stanford.graphics.shapenet.jme3.viewer

import edu.stanford.graphics.shapenet.common.CameraState
import edu.stanford.graphics.shapenet.util.{IOUtils, Loggable}
import com.jme3.math.{Vector3f, Transform, ColorRGBA}
import com.jme3.post.SceneProcessor
import com.jme3.renderer.{RenderManager, Camera}
import com.jme3.scene.{Node, Spatial}
import com.jme3.texture.FrameBuffer
import com.jme3.texture.Image.Format
import com.jme3.system.JmeSystem
import com.jme3.util.BufferUtils
import java.io.{File, IOException, OutputStream}

/**
 * Offscreen view
 * @author Angel Chang
 */
class OffscreenView(val renderManager: RenderManager,
                    val width: Int, val height: Int, val transform: Transform = null) extends Loggable {
  val camera: Camera = new Camera(width,height)
  camera.setFrustumPerspective(30f, camera.getWidth().toFloat / camera.getHeight(), 1f, 1000f)
  camera.setLocation(new Vector3f(0f, 0f, 10f))
  camera.lookAt(new Vector3f(0f, 0f, 0f), Vector3f.UNIT_Y)

  // create offscreen framebuffer
  val framebuffer = new FrameBuffer(width, height, 1)
  //setup framebuffer to use renderbuffer
  // this is faster for gpu -> cpu copies
  framebuffer.setDepthBuffer(Format.Depth)
  framebuffer.setColorBuffer(Format.RGBA8)
  //        offBuffer.setColorTexture(offTex);

  // create a pre-view. a view that is rendered before the main view
  val viewport = renderManager.createPreView("Offscreen View", camera)
  viewport.setBackgroundColor(new ColorRGBA(1.0f, 1.0f, 1.0f, 0.0f))
  viewport.setClearFlags(true, true, true)

  //set viewport to render to offscreen framebuffer
  viewport.setOutputFrameBuffer(framebuffer)

  val rootNode = new Node("Offscreen rootnode")
  if (transform != null) rootNode.setLocalTransform(transform)
  // attach the scene to the viewport to be rendered
  viewport.attachScene(rootNode)

  // Offscreen view functions

  def addProcessor(sceneProcessor: SceneProcessor) {
    // this will let us know when the scene has been rendered to the
    // frame buffer
    if (!sceneProcessor.isInitialized) {
      sceneProcessor.initialize(renderManager, viewport)
    }
    viewport.addProcessor(sceneProcessor)
  }

  def viewScene(scene: Spatial){
    rootNode.detachAllChildren()
    rootNode.attachChild(scene)
  }

  def setCamera(c: CameraState) {
    c.setCamera(camera)
  }

  def setCamera(c: Camera) {
    camera.copyFrom(c)
  }

  // Don't copy width/height...
  def setCameraFrame(cam: Camera) {
    camera.setRotation(cam.getRotation)
    camera.setLocation(cam.getLocation)
  }

  def update(tpf: Float) {
    // These need to be call before the scene can be rendered
    // (should be done as part of the AppState update)
    rootNode.updateLogicalState(tpf)
    rootNode.updateGeometricState()
  }

  def saveImage(filename: String, imageFormat: String = "png") {
    val outBuf = BufferUtils.createByteBuffer(width * height * 4)
    renderManager.getRenderer.readFrameBuffer(framebuffer, outBuf)
    val file = new File(filename)
    var outStream: OutputStream = null
    logger.info("Saving offscreen view to: {0}", file.getAbsolutePath())
    try {
      outStream = IOUtils.fileOutputStream(filename)
      JmeSystemMod.writeImageFile(outStream, imageFormat, outBuf, width, height)
    } catch {
      case ex: IOException => {
        logger.error("Error while saving offscreen view", ex)
      }
    } finally {
      if (outStream != null) {
        try {
          outStream.close
        }
        catch {
          case ex: IOException => {
            logger.error("Error while saving offscreen view", ex)
          }
        }
      }
    }
  }

  def clearProcessors() {
    viewport.clearProcessors()
  }
}