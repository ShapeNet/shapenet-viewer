package edu.stanford.graphics.shapenet.jme3.viewer

import de.lessvoid.nifty.controls.Controller
import de.lessvoid.nifty.elements.Element
import de.lessvoid.nifty.Nifty
import de.lessvoid.nifty.screen.Screen
import de.lessvoid.nifty.input.NiftyInputEvent
import de.lessvoid.xml.xpp3.Attributes
import java.util.Properties

/**
 * Progress bar controller
 * @author Angel Chang
 */
class ProgressBarControl extends Controller {
  var progressBarElement: Element = null
  override def bind(nifty: Nifty, screen: Screen, elem: Element, props: Properties, attrs: Attributes) {
    progressBarElement = elem.findElementByName("progressbar")
  }

  override def inputEvent(inputEvent: NiftyInputEvent) = true

  override def init(props: Properties, attrs: Attributes) {}

  override def onFocus(getFocus: Boolean) {}

  override def onStartScreen() {}
}
