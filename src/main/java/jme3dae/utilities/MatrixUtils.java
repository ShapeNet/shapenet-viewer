package jme3dae.utilities;

import com.jme3.math.*;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utilities for working with matrices
 *
 * @author Angel Chang
 */
public class MatrixUtils {
  private static final Logger logger = Logger.getLogger(MatrixUtils.class.getName());

  private static final float TOLERANCE = 0.0001f;

  public static final RealMatrix matrix3fToRealMatrix(Matrix3f matrix3f) {
    RealMatrix m = org.apache.commons.math3.linear.MatrixUtils.createRealMatrix(3, 3);
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        m.setEntry(row, col, matrix3f.get(row, col));
      }
    }
    return m;
  }

  public static final Matrix3f realMatrixToMatrix3f(RealMatrix m) {
    if (m.getRowDimension() != 3 || m.getColumnDimension() != 3) {
      throw new IllegalArgumentException("3x3 matrix expected, got: " + m);
    }
    Matrix3f matrix3f = new Matrix3f();
    for (int row = 0; row < 3; row++) {
      for (int col = 0; col < 3; col++) {
        matrix3f.set(row, col, (float) m.getEntry(row, col));
      }
    }
    return matrix3f;
  }

  public static final Pair<Matrix3f, Vector3f> polarDecomposition(Matrix3f matrix3f) {
    RealMatrix M = matrix3fToRealMatrix(matrix3f);
    SingularValueDecomposition svd = new SingularValueDecomposition(M);
    RealMatrix U = svd.getU().multiply(svd.getVT()); // Rotation (with possible reflection)
    RealMatrix P = svd.getV().multiply(svd.getS()).multiply(svd.getVT()); // Scale
    P = P.add(P.transpose()).scalarMultiply(0.5);
    Vector3f scale = new Vector3f((float) P.getEntry(0,0), (float) P.getEntry(1,1), (float) P.getEntry(2,2));
    Matrix3f rotation = realMatrixToMatrix3f(U);
    float det = matrix3f.determinant();
    if (det < 0) {
      // Flip one of our scales
      scale.x = -scale.x;
      int col = 0;
      for (int row = 0; row < 3; row++) {
        float val = rotation.get(row,col);
        rotation.set(row, col, -val);
      }
    }
    return new Pair<>(rotation, scale);
  }

  public static final Pair<Matrix3f, Vector3f> decompose(Matrix3f matrix3f) {
    float scaleX = (float) matrix3f.getColumn(0).length();
    float scaleY = (float) matrix3f.getColumn(1).length();
    float scaleZ = (float) matrix3f.getColumn(2).length();
    Vector3f scale = new Vector3f(scaleX, scaleY, scaleZ);
    float det = matrix3f.determinant();
    if (det < 0) {
      logger.fine("Determinant is negative: " + matrix3f + ", det=" + det + ", flipping x-scale");
      // Flip one of our scales
      scale.x = -scale.x;
    }

    // Figure out rotation
    Matrix3f rotMat = new Matrix3f();
    for (int col = 0; col < 3; col++) {
      double invScale = 1.0/scale.get(col);
      for (int row = 0; row < 3; row++) {
        double val = matrix3f.get(row,col)*invScale;
        rotMat.set(row,col, (float) val);
      }
    }
    float detRot = rotMat.determinant();
    if (Math.abs( Math.abs(detRot) - 1.0 ) > TOLERANCE) {
      logger.warning("Determinant is not good: " + rotMat + ", det=" + detRot);
      if (Math.abs(detRot) > TOLERANCE) {
        // Non zero - try matrix polar decomposition
        Pair<Matrix3f,Vector3f> pair = polarDecomposition(matrix3f);
        rotMat = pair.getFirst();
        scale = pair.getSecond();
      }
    } else if (detRot < 0) {
      logger.warning("Determinant is still negative: " + rotMat + ", det=" + detRot);
    }

    return new Pair<>(rotMat, scale);
  }

  public static final Transform matrix4fToTransform(Matrix4f matrix4f) {
    Matrix3f matrix3f = matrix4f.toRotationMatrix();
    Pair<Matrix3f, Vector3f> rotScale = decompose(matrix3f);
    Matrix3f rotMat = rotScale.getFirst();
    Vector3f scale = rotScale.getSecond();

    Quaternion rotQuat = new Quaternion();
    rotQuat.fromRotationMatrix(rotMat);

    // Figure out translation
    Vector3f position = matrix4f.toTranslationVector();
    return new Transform(position, rotQuat, scale);
  }

  public static final Matrix4f transformToMatrix4f(Transform transform) {
    Matrix4f matrix4f = new Matrix4f();
    Matrix3f matrix3f = transform.getRotation().toRotationMatrix();
    matrix4f.setTransform(transform.getTranslation(), transform.getScale(), matrix3f);
    return matrix4f;
  }

  public static class MatrixComparison {
    public final Matrix4f mat1;
    public final Matrix4f mat2;
    public final float tolerance;
    public final Matrix4f diff;
    public final List<Pair<Integer,Integer>> diffPositions;

    public MatrixComparison(Matrix4f mat1, Matrix4f mat2, float tolerance) {
      this.mat1 = mat1;
      this.mat2 = mat2;
      this.tolerance = tolerance;
      this.diff = mat1.add(mat2.mult(-1));
      this.diffPositions = new ArrayList<Pair<Integer,Integer>>();
      for (int i = 0; i < 4; i++) {
        for (int j = 0; j < 4; j++) {
          if (Math.abs(diff.get(i,j)) > tolerance) {
            diffPositions.add(new Pair(i,j));
          }
        }
      }
    }
    public boolean isOkay() {
      return diffPositions.isEmpty();
    }
  }

  public static final MatrixComparison compareMatrices(Matrix4f mat1, Matrix4f mat2, float tolerance) {
    return new MatrixComparison(mat1, mat2, tolerance);
  }
}
