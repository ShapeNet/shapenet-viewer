package jme3dae.utilities;

import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;

/**
 * Transforms a String into an array of strings.
 *
 * @author pgi
 */
public class NameListTransformer implements ValueTransformer<String, String[]> {

  /**
   * Instance initializer.
   *
   * @return a new NameListTransformer
   */
  public static NameListTransformer create() {
    return new NameListTransformer();
  }

  private NameListTransformer() {
  }

  /**
   * Transforms a string in an array of strings. The array has one component
   * for each space-separated name in the given string
   *
   * @param value the string to transform
   * @return an array of strings or an undefined value if parsing fails.
   */
  public TransformedValue<String[]> transform(String value) {
    String[] names = null;
    if (value != null && (value.length() != 0)) {
      names = value.trim().split(" ");
    }
    return TransformedValue.create(names);
  }
}
