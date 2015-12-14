package edu.stanford.graphics.shapenet

import java.awt.Color

/**
 * Color utilities
 * @author Angel Chang
 */
package object colors {
  def getColor(id: Int, alpha: Float): Color = DefaultColorPalette.getColor(id, alpha)
  def getColor(id: Int): Color = DefaultColorPalette.getColor(id)
  def getColor(c: Color, alpha: Float): Color = {
    new Color(c.getRed, c.getGreen, c.getBlue, (alpha*255).toInt)
  }
  def toHex(c: Color): String = {
    val hexColor = "#%06X%02X".format((0xFFFFFF & c.getRGB), c.getAlpha)
    hexColor
  }
}
