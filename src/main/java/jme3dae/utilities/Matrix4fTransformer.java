package jme3dae.utilities;

import com.jme3.math.Matrix4f;
import jme3dae.transformers.ValueTransformer.TransformedValue;

public class Matrix4fTransformer implements TransformerPack<String, Matrix4f[]> {

  public static Matrix4fTransformer create() {
    return new Matrix4fTransformer();
  }

  protected Matrix4fTransformer() {
  }

  public TransformedValue<Matrix4f[]> transform(String value) {
    Matrix4f[] result = null;
    if (value != null) {
      float[] data = FLOAT_LIST.transform(value).get();
      result = new Matrix4f[data.length / 16];
      for (int i = 0; i < data.length; i += 16) {
        Matrix4f m = new Matrix4f();
        m.m00 = data[i + 0];
        m.m01 = data[i + 1];
        m.m02 = data[i + 2];
        m.m03 = data[i + 3];
        m.m10 = data[i + 4];
        m.m11 = data[i + 5];
        m.m12 = data[i + 6];
        m.m13 = data[i + 7];
        m.m20 = data[i + 8];
        m.m21 = data[i + 9];
        m.m22 = data[i + 10];
        m.m23 = data[i + 11];
        m.m30 = data[i + 12];
        m.m31 = data[i + 13];
        m.m32 = data[i + 14];
        m.m33 = data[i + 15];
        result[i / 16] = m;
      }
    }
    return TransformedValue.create(result);
  }
}
