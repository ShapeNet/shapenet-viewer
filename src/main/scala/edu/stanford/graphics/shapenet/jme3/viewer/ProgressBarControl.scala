package edu.stanford.graphics.shapenet.jme3.viewer

import de.lessvoid.nifty.controls.{Parameters, Controller}
import de.lessvoid.nifty.elements.Element
import de.lessvoid.nifty.Nifty
import de.lessvoid.nifty.screen.Screen
import de.lessvoid.nifty.input.NiftyInputEvent

/**
 * Progress bar controller
 * @author Angel Chang
 */
class ProgressBarControl extends Controller {
  var progressBarElement: Element = null
  override def bind(nifty: Nifty, screen: Screen, elem: Element, params: Parameters) {
    progressBarElement = elem.findElementById("progressbar")
  }

  override def inputEvent(inputEvent: NiftyInputEvent) = true

  override def init(params: Parameters) {}

  override def onFocus(getFocus: Boolean) {}

  override def onStartScreen() {}
}
