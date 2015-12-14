package edu.stanford.graphics.shapenet.jme3.viewer

import com.jme3.renderer.{Camera, RenderManager}
import com.jme3.math.Transform
import edu.stanford.graphics.shapenet.common.CameraState

/**
 * Wrapper for offscreen analysis
 * @author Angel Chang
 */
class OffscreenAnalyzer(val renderManager: RenderManager,
                        val width: Int, val height: Int,
                        val transform: Transform = null) {
  // Member variables for offscreen analysis
  private var offscreen: OffscreenView = null
  private var offscreenDisplay: ImageDisplaySceneProcessor = null
  private var sceneStats: SceneStatsSceneProcessor = null
  prepareOffscreen()

  def getSceneStats = sceneStats
  def getOffScreen = offscreen
  def getOffScreenDisplay = offscreenDisplay

  private def prepareOffscreen() {
    // Prepare a offscreen view for offscreen computations
    offscreen = new OffscreenView(renderManager, width, height, transform)
    // Add processor for debugging off screen image by displaying
    offscreenDisplay = new ImageDisplaySceneProcessor(false)
    offscreen.addProcessor(offscreenDisplay)

    // Add processor for getting scene stats
    sceneStats = new SceneStatsSceneProcessor()
    offscreen.addProcessor(sceneStats)
  }

  def enableDisplay(flag: Boolean) {
    offscreenDisplay.setEnabled(flag)
  }

  def update(tpf: Float) {
    offscreen.update(tpf)
  }

  def setCamera(cam: Camera) {
    offscreen.setCamera(cam)
  }

  def setCameraState(cam: CameraState) {
    offscreen.setCamera(cam)
  }

  def setCameraFrame(cam: Camera) {
    offscreen.setCameraFrame(cam)
  }

}
