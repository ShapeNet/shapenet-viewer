package jme3dae.utilities;

import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;

/**
 * Transforms a string-boolean list into a boolean array
 *
 * @author pgi
 */
public class BooleanListTransformer implements ValueTransformer<String, boolean[]> {

  /**
   * Instance creator.
   *
   * @return a new BooleanListTransformer
   */
  public static BooleanListTransformer create() {
    return new BooleanListTransformer();
  }

  private BooleanListTransformer() {
  }

  /**
   * Transforms a string in a list of booleans
   *
   * @param value a string value, can be null.
   * @return an array of boolean or an undefined value if parsing is not possible.
   */
  public TransformedValue<boolean[]> transform(String value) {
    boolean[] result = null;
    if (value != null) {
      value = value.trim();
      String[] data = value.split(" ");
      result = new boolean[data.length];
      for (int i = 0; i < data.length; i++) {
        String string = data[i];
        if ("true".equals(string)) {
          result[i] = true;
        } else if ("false".equals(string)) {
          result[i] = false;
        } else {
          result = null;
          break;
        }
      }
    }
    return TransformedValue.create(result);
  }
}
