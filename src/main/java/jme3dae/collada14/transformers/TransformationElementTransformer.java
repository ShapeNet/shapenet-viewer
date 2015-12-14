package jme3dae.collada14.transformers;

import com.jme3.math.*;

import java.util.List;

import jme3dae.DAENode;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.transformers.ValueTransformer.TransformedValue;

import static jme3dae.utilities.Conditions.*;

import jme3dae.utilities.*;

/**
 * Transforms a list of collada transform elements (lookat, matrix, rotate ...) in
 * a jme3 transform instance. This transformer uses the MeasuringUnit stored in the
 * collada root node to scale the translation components.
 *
 * @author pgi
 */
public class TransformationElementTransformer implements TransformerPack<List<DAENode>, Transform> {

  /**
   * Instance initializer.
   *
   * @return a new TransformationElementTransformer
   */
  public static TransformationElementTransformer create() {
    return new TransformationElementTransformer();
  }

  private final Vector3fTransformer VECTOR = Vector3fTransformer.create();

  private TransformationElementTransformer() {
  }

  /**
   * Takes a collada node and returns a jme3 transform that holds the result of
   * the accumulation of the transform child of the given node.
   *
   * @param value the DAENode to transform
   * @return a jme3 transform.
   */
  public TransformedValue<Transform> transform(List<DAENode> value) {
    Transform transform = Transform.IDENTITY.clone();
    if (!value.isEmpty()) {
      DAENode collada = value.get(0).getRootNode();
      MeasuringUnit unit = collada.getParsedData(MeasuringUnit.class);
      //check values
      for (DAENode node : value) {
        if (node.hasName(Names.LOOKAT)) {
          pushLookat(transform, node, unit);
        } else if (node.hasName(Names.MATRIX)) {
          pushMatrix(transform, node, unit);
        } else if (node.hasName(Names.ROTATE)) {
          pushRotate(transform, node, unit);
        } else if (node.hasName(Names.SCALE)) {
          pushScale(transform, node, unit);
        } else if (node.hasName(Names.SKEW)) {
          pushSkew(transform, node, unit);
        } else if (node.hasName(Names.TRANSLATE)) {
          pushTranslate(transform, node, unit);
        } else {
          throw new IllegalArgumentException(node + " is not a lookat, matrix, rotate, scale, skew or translate element.");
        }
      }
    }
    return TransformedValue.create(transform);
  }

  private void pushLookat(Transform transform, DAENode node, MeasuringUnit unit) {
    TransformedValue<float[]> floatList = node.getContent(FLOAT_LIST);
    checkTrue(floatList.isDefined());
    checkValue(floatList.get().length, 9);
    float[] data = floatList.get();
    Vector3f eye = new Vector3f(data[0], data[1], data[2]);
    Vector3f dest = new Vector3f(data[3], data[4], data[5]);

    Vector3f up = new Vector3f(data[6], data[7], data[8]);
    Vector3f dir = eye.subtract(dest).normalize();
    Quaternion rot = new Quaternion();
    rot.lookAt(dir, up);

    Transform t = newIdentity();
    t.setTranslation(eye);
    t.setRotation(rot);
    t = t.combineWithParent(transform);
    transform.set(t);
  }

  private void pushMatrix(Transform transform, DAENode node, MeasuringUnit unit) {
    TransformedValue<Matrix4f> matrix = node.getContent(MatrixTransformer.create());
    if (matrix.isDefined()) {
      Transform t = MatrixUtils.matrix4fToTransform(matrix.get());
      t = t.combineWithParent(transform);
      transform.set(t);
    } else {
      Todo.task("where is my matrix?");
    }
  }

  private void pushRotate(Transform transform, DAENode node, MeasuringUnit unit) {
    TransformedValue<float[]> floatList = node.getContent(FLOAT_LIST);
    checkTrue(floatList.isDefined());
    checkValue(floatList.get().length, 4);
    float ax = floatList.get()[0];
    float ay = floatList.get()[1];
    float az = floatList.get()[2];
    float deg = floatList.get()[3];

    Vector3f axis = new Vector3f(ax, ay, az);
    float angle = FastMath.DEG_TO_RAD * deg;
    Quaternion rot = new Quaternion();
    rot = rot.fromAngleAxis(angle, axis);
    Transform t = newIdentity();
    t.setRotation(rot);
    t = t.combineWithParent(transform);
    transform.set(t);
  }

  private void pushScale(Transform transform, DAENode node, MeasuringUnit unit) {
    TransformedValue<Vector3f> vector = node.getContent(VECTOR);
    checkTrue(vector.isDefined());
    Transform t = newIdentity();
    t.setScale(vector.get());
    t.combineWithParent(transform);
    transform.set(t);
  }

  private void pushSkew(Transform transform, DAENode node, MeasuringUnit unit) {
    Todo.implementThis();
  }

  private void pushTranslate(Transform transform, DAENode node, MeasuringUnit unit) {
    TransformedValue<Vector3f> data = node.getContent(VECTOR);
    checkTrue(data.isDefined(), node + " is not a three float container?");
    Vector3f vec = data.get().mult(unit.getMeter());
    Transform t = newIdentity();
    t.setTranslation(vec);
    t.combineWithParent(transform);
    transform.set(t);
  }

  private Transform newIdentity() {
    return Transform.IDENTITY.clone();
  }
}
