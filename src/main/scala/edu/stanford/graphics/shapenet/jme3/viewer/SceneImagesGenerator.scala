package edu.stanford.graphics.shapenet.jme3.viewer

import edu.stanford.graphics.shapenet.common.FullId
import edu.stanford.graphics.shapenet.jme3.Jme
import edu.stanford.graphics.shapenet.common.GeometricScene
import edu.stanford.graphics.shapenet.util.{Loggable, IOUtils}
import com.jme3.scene.Node
import java.io.File
import scala.util.Random

/**
 * Generates and saves away images for scenes
 * @author Angel Chang
 */
class SceneImagesGenerator(val viewer: Viewer,
                           val sceneProcessor: (GenerateImagesFnOptions,GeometricScene[Node]) => _ = null,
                           val randomize: Boolean = false,
                           val skipExisting: Boolean = false,
                           val getOutputDirFn: FullId => String = null) extends Loggable {
  val generateImagesState: GenerateImagesAppState = viewer.generateImagesState

  protected var camPositionGenerator: CameraPositionGenerator = null

  def configCameraPositions(cameraPositionOptions: CameraPositionOptions = CameraPositionGenerator.defaultCameraPositionOptions,
                            nViews: Int = 1,
                            useDebugPosition: Boolean = viewer.config.useDebugPositionsForSceneImages) = {
    this.camPositionGenerator = CameraPositionGenerator(viewer, cameraPositionOptions, nViews, useDebugPosition)
  }

  def configCameraPositions(cameraPositionGenerator: CameraPositionGenerator): Unit = {
    this.camPositionGenerator = cameraPositionGenerator
  }

  def process(inputSceneIds: Iterable[String], outputDirName: String, appendMode: Boolean = false)(implicit jme: Jme) {
    val outputDir = IOUtils.ensureDirname(outputDirName)
    if (camPositionGenerator == null) {
      configCameraPositions()
    }
    val sceneIds = if (randomize) Random.shuffle(inputSceneIds.toSeq) else inputSceneIds
    var nProcessed = 0
    val genFn = new GenerateImagesFn {
      def apply(options: GenerateImagesFnOptions, scene: GeometricScene[Node]): Seq[ScreenShotInfo] = {
        // take scene and get extract surfaces
        nProcessed += 1
        logger.info("Processing " + nProcessed)
        if (sceneProcessor != null) {
          sceneProcessor.apply(options, scene)
        }
        // produce screen shot infos
        val cameraPositions = camPositionGenerator.generatePositions(scene.node)
        val screenshots = cameraPositions.zipWithIndex.map(x =>
          toScreenShot(options, x._1, x._2)
        )
        logger.info("Processed: " + scene.scene.sceneId)
        screenshots.toSeq
      }
    }

    // Make sure that the menu is hidden
    viewer.hideMenu()
    generateImagesState.imageFilenameUseFullId = false

    generateImagesState.setScreenShotDir(outputDir)
    generateImagesState.setSummaryFile(outputDir + "summary.csv", appendMode)
    // Have different output dir per model
    var skipped = 0
    var enqueued = 0
    for (sceneId <- sceneIds) {
      // Skip if we already have a directory with images and stuff
      val fullId = FullId(sceneId)
      val dir = if (getOutputDirFn != null) {
        outputDir + File.separator + getOutputDirFn(fullId)
      } else {
        outputDir + File.separator + fullId.source + File.separator + fullId.id + File.separator
      }
      val files = IOUtils.listFiles(dir)
      val doScene = if (skipExisting) {
        val pngs = files.filter( x => x.getName.endsWith(".png")).length
        pngs < camPositionGenerator.nViews
      } else true
      if (doScene) {
        generateImagesState.setScreenShotDir(dir)
        generateImagesState.enqueueSceneIds(genFn, ActionStates.EMPTY, sceneId)
        enqueued += 1
      } else {
        skipped += 1
      }
    }
    logger.info("Enqueued: " + enqueued + ", Skipped: " + skipped)

    generateImagesState.closeSummaryFile()
    generateImagesState.setScreenShotDir(viewer.screenShotDir)
  }
}