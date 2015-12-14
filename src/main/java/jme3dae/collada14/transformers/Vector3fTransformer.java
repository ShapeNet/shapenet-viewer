package jme3dae.collada14.transformers;

import com.jme3.math.Vector3f;
import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;

/**
 * Transfors a list of three space separated float strings in a Vector3f
 *
 * @author pgi
 */
public class Vector3fTransformer implements ValueTransformer<String, Vector3f> {

  /**
   * Instance creator.
   *
   * @return a new Vector3f transformer.
   */
  public static Vector3fTransformer create() {
    return new Vector3fTransformer();
  }

  private Vector3fTransformer() {
  }

  /**
   * Transforms a string (eg 0 1.34 3) in a Vector3f.
   *
   * @param value the string to transform
   * @return a new vector3f or an undefined value if the parsing fails.
   */
  public TransformedValue<Vector3f> transform(String value) {
    Vector3f v = null;
    if (value != null && (value.length() != 0)) {
      String[] c = value.split(" ");
      if (c.length == 3) {
        try {
          float x = Float.parseFloat(c[0]);
          float y = Float.parseFloat(c[1]);
          float z = Float.parseFloat(c[2]);
          v = new Vector3f(x, y, z);
        } catch (NumberFormatException ex) {
          System.err.println("Vector3fTransformer: not a float string");
        }
      }
    }
    return TransformedValue.create(v);
  }

}
