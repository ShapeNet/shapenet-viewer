package edu.stanford.graphics.shapenet.colors

import java.awt.Color
import javax.imageio.ImageIO
import java.io.File

import edu.stanford.graphics.shapenet.Constants

/**
 * A color palette
 * @author Angel Chang
 */
trait ColorPalette {
  def getColor(id: Int): Color
  def getColorCount(): Int = -1

  def getColor(id: Int, alpha: Float): Color = {
    val c = getColor(id)
    edu.stanford.graphics.shapenet.colors.getColor(c, alpha)
  }
}

class ColorBar(rgbColors: Array[Color]) extends ColorPalette {
  val nColors = rgbColors.length
  def getColor(r: Double): Color = getColor((r*(nColors-1)).toInt)
  def getColor(id: Int): Color = rgbColors(id % nColors)
  override def getColorCount() = nColors
}

object ColorBar {
  val texturesDir = Constants.ASSETS_DIR + "Textures" + File.separator
  lazy val coolwarmBar = ColorBar(texturesDir + "Cool2WarmBar.png")
  lazy val warmBar = ColorBar(texturesDir + "heatmap.png")
  def apply(filename: String): ColorBar = {
    val img = ImageIO.read(new File(filename))
    val rgb = Array.ofDim[Color](img.getWidth)
    for (x <- 0 until rgb.length) {
      rgb(x) = new Color(img.getRGB(x, 0))
    }
    new ColorBar(rgb)
  }
}

object PhiColorPalette extends ColorPalette {
  def getColor(id: Int): Color = {
    val startColor = new Color(0x4FD067)
    val hsb = Color.RGBtoHSB(startColor.getRed, startColor.getGreen, startColor.getBlue, null)
    val invPhi = 1.0/Constants.phi
    var hue = hsb(0) + id*invPhi
    hue = hue - math.floor(hue)
    val c = Color.getHSBColor(hue.toFloat, 0.5f, 0.95f)
    // Switch blue and green for nice pretty colors
    new Color(c.getRed, c.getBlue, c.getGreen)
  }
}

object DefaultColorPalette extends ColorPalette {
  def getColor(id: Int): Color = {
    var h = (-3.88 * id) % (2*Math.PI)
    if (h<0) h += 2*Math.PI
    h /= 2*Math.PI
    val c = Color.getHSBColor(h.toFloat, (0.4 + 0.2 * Math.sin(0.42 * id)).toFloat, 0.5f)
    c
  }
}

