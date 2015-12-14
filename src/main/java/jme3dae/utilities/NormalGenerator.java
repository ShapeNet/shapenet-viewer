package jme3dae.utilities;

import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;

public class NormalGenerator {

  public static NormalGenerator create() {
    return new NormalGenerator();
  }

  public void generateNormals(Mesh mesh) {
    VertexBuffer positionBuffer = mesh.getBuffer(Type.Position);
    if (positionBuffer != null && positionBuffer.getData() instanceof FloatBuffer) {
      FloatBuffer buffer = (FloatBuffer) positionBuffer.getData();
      FloatBuffer normals = generateNormals(buffer, mesh);
      if (normals != null) {
        mesh.setBuffer(Type.Normal, 3, normals);
      }
    }
  }

  private FloatBuffer generateNormals(FloatBuffer positions, Mesh mesh) {
    FloatBuffer normals = BufferUtils.createFloatBuffer(positions.capacity());
    for (int i = 0; i < positions.capacity(); i += 3) {
      float x = positions.get(i);
      float y = positions.get(i + 1);
      float z = positions.get(i + 2);
      Vector3f n = new Vector3f();
      List<Triangle> triangles = getTrianglesSharingIndex(i / 3, mesh);

      for (Triangle t : triangles) {
        t.calculateNormal();
        n.addLocal(t.getNormal());
      }
      n.normalizeLocal();
      normals.put(n.x).put(n.y).put(n.z);
//	    normals.put(0).put(0).put(0);
    }
    normals.flip();
    return normals;
  }

  private List<Triangle> getTrianglesSharingIndex(int index, Mesh mesh) {
    List<Triangle> result = new LinkedList<Triangle>();
    int[] buffer = new int[3];
    for (int i = 0; i < mesh.getTriangleCount(); i++) {
      mesh.getTriangle(i, buffer);
      if (buffer[0] == index || buffer[1] == index || buffer[2] == index) {
        Triangle t = new Triangle(new Vector3f(), new Vector3f(), new Vector3f());
        mesh.getTriangle(i, t);
        result.add(t);
      }
    }
    return result;
  }
}
