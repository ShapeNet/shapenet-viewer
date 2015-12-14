package jme3dae.utilities;

import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;

/**
 * Transforms a string in a float value.
 *
 * @author pgi
 */
public class FloatTransformer implements ValueTransformer<String, Float> {

  /**
   * Instance creator
   *
   * @return a new FloatTransformer instance.
   */
  public static FloatTransformer create() {
    return new FloatTransformer();
  }

  private FloatTransformer() {
  }

  /**
   * Tranforms a string in a float value.
   *
   * @param value the string to transform
   * @return a float or an undefined value is parsing fails.
   */
  public TransformedValue<Float> transform(String value) {
    Float r = null;
    if (value != null) {
      value = value.trim();
      try {
        r = new Float(value);
      } catch (NumberFormatException ex) {
      }
    }
    return TransformedValue.create(r);
  }
}
