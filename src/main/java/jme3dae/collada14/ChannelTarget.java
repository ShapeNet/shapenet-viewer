package jme3dae.collada14;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import jme3dae.transformers.ValueTransformer;

public abstract class ChannelTarget implements ValueTransformer<float[], Transform[]> {
  public static final ChannelTarget ROT_X_ANGLE = new ChannelTarget() {

    public TransformedValue<Transform[]> transform(float[] value) {
      Transform[] result = null;
      if (value != null) {
        result = new Transform[value.length];
        for (int i = 0; i < value.length; i++) {
          float angle = value[i];
          result[i] = ChannelTarget.newTransform(0, 0, 0, FastMath.DEG_TO_RAD * angle, 0, 0, 1, 1, 1);
        }
      }
      return TransformedValue.create(result);
    }
  };
  public static final ChannelTarget ROT_Y_ANGLE = new ChannelTarget() {

    public TransformedValue<Transform[]> transform(float[] value) {
      Transform[] result = null;
      if (value != null) {
        result = new Transform[value.length];
        for (int i = 0; i < value.length; i++) {
          float angle = value[i];
          result[i] = ChannelTarget.newTransform(0, 0, 0, 0, FastMath.DEG_TO_RAD * angle, 0, 1, 1, 1);
        }
      }
      return TransformedValue.create(result);
    }
  };
  public static final ChannelTarget ROT_Z_ANGLE = new ChannelTarget() {

    public TransformedValue<Transform[]> transform(float[] value) {
      Transform[] result = null;
      if (value != null) {
        result = new Transform[value.length];
        for (int i = 0; i < value.length; i++) {
          float angle = value[i];
          result[i] = ChannelTarget.newTransform(0, 0, 0, 0, 0, FastMath.DEG_TO_RAD * angle, 1, 1, 1);
        }
      }
      return TransformedValue.create(result);
    }
  };
  public static final ChannelTarget TRANSLATE = new ChannelTarget() {

    public TransformedValue<Transform[]> transform(float[] value) {
      Transform[] result = null;
      if (value != null && value.length % 3 == 0) {
        result = new Transform[value.length / 3];
        int index = 0;
        for (int i = 0; i < value.length; i += 3) {
          float x = value[i];
          float y = value[i + 1];
          float z = value[i + 2];
          result[index] = ChannelTarget.newTransform(x, y, z, 0, 0, 0, 1, 1, 1);
          index++;
        }
      }
      return TransformedValue.create(result);
    }
  };

  public static ChannelTarget forName(String targetName) {
    if (targetName.contains("/")) {
      targetName = targetName.substring(targetName.indexOf('/') + 1, targetName.length());
    }
    if (targetName.equals("rotateX.ANGLE")) {
      return ROT_X_ANGLE;
    } else if (targetName.equals("rotateY.ANGLE")) {
      return ROT_Y_ANGLE;
    } else if (targetName.equals("rotateZ.ANGLE")) {
      return ROT_Z_ANGLE;
    } else if (targetName.equals("translate")) {
      return TRANSLATE;
    } else {
      return null;
    }
  }

  private static Transform newTransform(float tx, float ty, float tz, float rx, float ry, float rz, float sx, float sy, float sz) {
    Transform t = new Transform();
    t.loadIdentity();
    t.setRotation(new Quaternion().fromAngles(rx, ry, rz));
    t.setScale(sx, sy, sz);
    t.setTranslation(tx, ty, tz);
    return t;
  }
}
