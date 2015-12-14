package jme3dae.collada14.transformers;

import com.jme3.math.ColorRGBA;
import jme3dae.transformers.ValueTransformer;

/**
 * Transforms a 3 or 4 float color component list (values between 0 and 1) into a
 * JME3 color instance. Eg.
 * <pre> String color = "0 0 0";
 * TransformedValue&lt;ColorRGBA&gt; value = ColorRGBATransformer.create().transform(color);
 * ColorRGBA black = value.get()</pre>
 * An invalid string results in an undefined transformed value.
 * <pre> String color = "hello world";
 * TransformedValue&lt;ColorRGBA&gt; value = ColorRGBATransformer.create().transform(color);
 * value.isDefined() &lt;- returns false</pre>
 *
 * @author pgi
 */
public class ColorRGBATransformer implements ValueTransformer<String, ColorRGBA> {

  /**
   * Instance creator.
   *
   * @return a new ColorRGBATransformer instance.
   */
  public static ColorRGBATransformer create() {
    return new ColorRGBATransformer();
  }

  private ColorRGBATransformer() {
  }

  /**
   * Transforms a string into a ColorRGBA if possible. The string must have
   * 3 or 4 float values, each clamped to 0-1.
   *
   * @param value the string to transform
   * @return a TransformedValue holding a ColorRGBA if the transformation
   * succeded, undefined (isDefined() <- false) otherwise.
   */
  public TransformedValue<ColorRGBA> transform(String value) {
    ColorRGBA color = null;
    value = value.trim();
    if (value.length() != 0) {
      String[] rgba = value.split(" ");
      if (rgba.length == 3 || rgba.length == 4) {
        try {
          float red = Float.parseFloat(rgba[0]);
          float green = Float.parseFloat(rgba[1]);
          float blue = Float.parseFloat(rgba[2]);
          float alpha = rgba.length == 3 ? 1 : Float.parseFloat(rgba[3]);
          color = new ColorRGBA(red, green, blue, alpha);
        } catch (Exception ex) {
          System.err.println("COLORGBATransformer: not a number");
        }
      }
    }
    return TransformedValue.create(color);
  }
}
