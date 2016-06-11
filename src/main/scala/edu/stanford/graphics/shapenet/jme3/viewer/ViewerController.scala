package edu.stanford.graphics.shapenet.jme3.viewer

import com.jme3.app.state.AbstractAppState
import de.lessvoid.nifty.{NiftyEventSubscriber, Nifty}
import de.lessvoid.nifty.screen.{Screen, ScreenController}
import de.lessvoid.nifty.elements.Element
import de.lessvoid.nifty.controls.{TextField, Console}
import de.lessvoid.nifty.elements.render.TextRenderer
import de.lessvoid.nifty.tools.SizeValue
import de.lessvoid.nifty.input.{NiftyStandardInputEvent, NiftyInputEvent}
/**
 * Nifty GUI controller for viewer
 * @author Angel Chang
 */
class ViewerController(val viewer: Viewer, val nifty: Nifty)
  extends AbstractAppState with ScreenController {
  var progressBarElement: Element = null
  var commandConsole: CommandConsole = null
  val queuedScreens = scala.collection.mutable.Stack[String]()

  // Implement NIFTY GUI ScreenController
  override def bind(nifty: Nifty, screen: Screen) {
    //   System.out.println("bind( " + screen.getScreenId() + ")")
    progressBarElement = nifty.getScreen("loading").findElementByName("progressbar")
    // get the console control (this assumes that there is a console in the current screen with the id="console"
    if (screen.getScreenId == "console") {
      // Initialize our console
      val console = screen.findNiftyControl("consoleControl", classOf[Console])
      commandConsole = new CommandConsole(this, console)
      commandConsole.init()
    } else if (screen.getScreenId == "start") {
      val element = screen.findElementByName("startText")
      println(viewer.startText)
      val textRenderer = element.getRenderer(classOf[TextRenderer])
      textRenderer.setText(viewer.startText)
    }
  }

  override def onStartScreen() {
    //System.out.println("onStartScreen " + nifty.getCurrentScreen.getScreenId)
  }

  override def onEndScreen() {
    //System.out.println("onEndScreen " + nifty.getCurrentScreen.getScreenId)
  }

  // Our functions
  def quit(){
    showScreen("end")
  }

  def setProgress(progress: Float, loadingText: String) {
    val MIN_WIDTH = 32
    val pixelWidth = (MIN_WIDTH + (progressBarElement.getParent.getWidth - MIN_WIDTH) * progress).toInt
    progressBarElement.setConstraintWidth(new SizeValue(pixelWidth + "px"))
    progressBarElement.getParent.layoutElements()

    val element = nifty.getScreen("loading").findElementByName("loadingtext")
    val textRenderer = element.getRenderer(classOf[TextRenderer])
    textRenderer.setText(loadingText)
  }

  def showError(errorText: String) {
    val element = nifty.getScreen("error").findElementByName("errorText")
    val textRenderer = element.getRenderer(classOf[TextRenderer])
    textRenderer.setText(errorText)
    showScreen("error")
  }

  @NiftyEventSubscriber(id = "loadTarget")
  def onLoadTargetTextFieldInputEvent(id: String, event: NiftyInputEvent) {
    if (NiftyStandardInputEvent.SubmitText.equals(event)) {
      loadPressed()
    }
  }

  def showLoadingMenu() {
    showScreen("loading")
    //load = true
  }

  def toggleMenu() {
    toggleScreen("menu")
  }

  def toggleInstructions() {
    toggleScreen("instructions")
  }

  def toggleConsole() {
    toggleScreen("console")
  }

  def toggleScreen(name: String) {
    if (nifty.getCurrentScreen.getScreenId == name) {
      hideMenu()
    } else {
      viewer.enableSceneInput = false
      showScreen(name)
    }
  }

  override def update(tpf: Float) {
    // Process queued screens
    // The nifty.gotoScreen will discard other requests if
    //   gotoScreen is already in progress
    // We work around this by queuing up the screens and
    //   and only displaying the last requested screen
    //   on every update
    queuedScreens.synchronized {
      if (!queuedScreens.isEmpty) {
        val last = queuedScreens.pop
        //println("Going to screen " + last + ", queued was " + queuedScreens.mkString(","))
        queuedScreens.clear()
        nifty.gotoScreen(last)
      }
    }
  }

  def showScreen(name: String) {
    // Queue screen for display on the next update
    queuedScreens.synchronized {
      //println("show screen " + name)
      queuedScreens.push(name)
    }
  }

  def hideMenu() {
    showScreen("blank")
    viewer.enableSceneInput = true
  }

  def loadPressed() {
    val textField = nifty.getCurrentScreen.findNiftyControl("loadTarget", classOf[TextField])
    val loadId = textField.getRealText
    println("Loading requested scene " + loadId)
    viewer.load(loadId)
  }

}

