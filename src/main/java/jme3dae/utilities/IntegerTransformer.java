package jme3dae.utilities;

import java.util.logging.Level;
import java.util.logging.Logger;

import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;

/**
 * Transforms a string in an integer value.
 *
 * @author pgi
 */
public class IntegerTransformer implements ValueTransformer<String, Integer> {

  /**
   * Instance creator.
   *
   * @return a new IntegerTransformer
   */
  public static IntegerTransformer create() {
    return new IntegerTransformer();
  }

  private IntegerTransformer() {
  }

  /**
   * Transforms a string in a Integer
   *
   * @param value the string to transform
   * @return a Integer value of an undefined value if parsing fails.
   */
  public TransformedValue<Integer> transform(String value) {
    Integer v = null;
    value = value.trim();
    if (value.length() != 0) {
      try {
        v = new Integer(value.trim());
      } catch (NumberFormatException ex) {
        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", ex);
      }
    }
    return TransformedValue.create(v);
  }
}
