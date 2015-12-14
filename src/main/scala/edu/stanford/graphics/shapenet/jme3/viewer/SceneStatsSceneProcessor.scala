package edu.stanford.graphics.shapenet.jme3.viewer

import edu.stanford.graphics.shapenet.util.Loggable
import com.jme3.post.SceneProcessor
import com.jme3.renderer.RenderManager
import com.jme3.renderer.Renderer
import com.jme3.renderer.ViewPort
import com.jme3.renderer.queue.RenderQueue
import com.jme3.texture.FrameBuffer
import com.jme3.util.BufferUtils

import java.nio.ByteBuffer

/**
 * Screen Processor for getting stats on what is being displayed
 *
 * @author Angel Chang
 */
class SceneStatsSceneProcessor() extends SceneProcessor with Loggable {
  private var renderer: Renderer = null

  private var width: Int = 0
  private var height: Int = 0
  private var cpuBuf: ByteBuffer = null

  private var analyzeMessage: String = null
  private var analyze: Boolean = false
  private var fcscene: FalseColoredScene = null
  private var callback: () => Unit = null
  private var savePixels: Boolean = false
  private var modelIndexCounts: Map[Int,Int] = null
  private var modelIndexPixels: FalseColoredScene.IndexToPixelsMap = null

  def getModelIndexCounts() = modelIndexCounts.filter( x => x._1 != fcscene.backgroundIndex )
  def getModelIndexCountsWithPercent(thresholdPerc: Double = 0.0005) =
    modelIndexCounts.filter( x => x._1 != fcscene.backgroundIndex && x._2 >= thresholdPerc*getTotalPixels() )

  def getTotalPixels() = width*height

  def getVisibleModelIndices(): Set[Int] = {
    if (modelIndexCounts != null)
      modelIndexCounts.keySet.filter( x => x != fcscene.backgroundIndex )
    else Set()
  }

  def getModelIndexPixels(): FalseColoredScene.IndexToPixelsMap = {
    if (modelIndexPixels != null) modelIndexPixels
    else if (cpuBuf != null) {
      // Some analysis was done...
      modelIndexPixels = fcscene.getIndexPixels(cpuBuf, width, height)
      modelIndexPixels
    } else null
  }

  def analyzeScene(msg: String, scene: FalseColoredScene, callback: () => Unit = null, savePixels: Boolean = false) {
    if (analyze) {
      logger.warn("Scene analyzer already registered: " + this.analyzeMessage)
    } else {
      this.analyzeMessage = msg
      this.fcscene = scene
      this.callback = callback
      this.modelIndexCounts = null
      this.modelIndexPixels = null
      this.savePixels = savePixels
      analyze = true
    }
  }

  override def initialize(rm: RenderManager, vp: ViewPort) {
    reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight())
    renderer = rm.getRenderer()
  }

  override def reshape(vp: ViewPort, w: Int, h: Int) {
    width = w
    height = h
    cpuBuf = BufferUtils.createByteBuffer(width * height * 4)
  }

  override def isInitialized() = {
    renderer != null
  }

  override def preFrame(tpf: Float) {
  }

  override def postQueue(rq: RenderQueue) {
  }

  /**
   * Update the CPU image's contents after the scene has
   * been rendered to the framebuffer.
   */
  override def postFrame(out: FrameBuffer) {
    if (analyze) {
      cpuBuf.clear()
      renderer.readFrameBuffer(out, cpuBuf)
      modelIndexCounts = fcscene.getIndexCounts(cpuBuf, width, height)
      if (savePixels) {
        modelIndexPixels = fcscene.getIndexPixels(cpuBuf, width, height)
      }
      //println("Visible models\n" + modelIndexCounts.mkString("\n"))
      if (callback != null) {
        callback()
      }
      analyze = false
    }
  }

  def printStats() {
    println(getStatsString)
  }

  def getStatsString: String = {
    val sb = new StringBuilder()
    val s = fcscene.inputScene.scene
    if (s != null) {
      sb.append("Scene id: " + s.sceneId + "\n")
      sb.append("Scene name: " + s.sceneName + "\n")
      if (s.category != null && s.category.nonEmpty)
        sb.append("Scene category: " + s.category.mkString(",") + "\n")
      sb.append("Scene scale: " + s.unit + "\n")
      sb.append("Scene up: " + s.up + "\n")
      sb.append("Scene front: " + s.front + "\n")
    }
    sb.append("Visible models\n" + modelIndexCounts.mkString("\n"))
    sb.toString
  }

  override def cleanup() {
  }


}
