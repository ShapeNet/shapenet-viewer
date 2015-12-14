package jme3dae.utilities;

import com.jme3.math.Matrix4f;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;


/**
 * Transforms a string in a Matrix4f
 *
 * @author pgi
 */
public class MatrixTransformer implements ValueTransformer<String, Matrix4f> {

  /**
   * Instance initializer
   *
   * @return a new MatrixTransformer
   */
  public static MatrixTransformer create() {
    return new MatrixTransformer();
  }

  private MatrixTransformer() {
  }

  /**
   * Transforms a string in a Matrix3f. The string is read as a sequence of
   * space separated float values. The first float is the m00 component, the
   * second one the m01, then m02, m03, m10, m11 and so on.
   *
   * @param value the string to transform
   * @return a  matrix4f or an undefined value if the parsing fails.
   */
  public TransformedValue<Matrix4f> transform(String value) {
    Matrix4f m = null;
    if (value != null && (value.length() != 0)) {
      m = new Matrix4f();
      //float[] v = getMatrixElements(value);
      float[] v = FloatListTransformer.create().transform(value).get();
      m.m00 = v[0];
      m.m01 = v[1];
      m.m02 = v[2];
      m.m03 = v[3];
      m.m10 = v[4];
      m.m11 = v[5];
      m.m12 = v[6];
      m.m13 = v[7];
      m.m20 = v[8];
      m.m21 = v[9];
      m.m22 = v[10];
      m.m23 = v[11];
      m.m30 = v[12];
      m.m31 = v[13];
      m.m32 = v[14];
      m.m33 = v[15];
    }
    return TransformedValue.create(m);
  }

  private float[] getMatrixElements(String value) {
    float[] values = new float[16];
    Scanner in = new Scanner(value);
    int i = 0;
    while (in.hasNextFloat()) {
      if (i == 16) {
        Todo.task("This matrix has more than 16 elements...");
        break;
      }
      values[i] = in.nextFloat();
      i++;
    }
    return values;
  }
}
