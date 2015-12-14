package jme3dae.utilities;

import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Logger;

import jme3dae.collada14.transformers.PolygonData;
import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;

/**
 * Transforms an array of polygons in a JME3 mesh.
 *
 * @author pgi
 */
public class PolygonArrayTransformer implements ValueTransformer<Tuple2<MeasuringUnit, PolygonData[]>, Tuple2<Mesh, PolygonData[]>> {
  Logger logger = Logger.getLogger(PolygonArrayTransformer.class.getName());

  /**
   * Instance creator.
   *
   * @return a new PolygonArrayTransformer
   */
  public static PolygonArrayTransformer create() {
    return new PolygonArrayTransformer();
  }

  private PolygonArrayTransformer() {
  }

  /**
   * Transforms a polygons array in a JME3 mesh. Requires a measuring unit to
   * scale the coordinates to match the unit conversion.
   *
   * @param data the measuring unit and the polygon array to be transformed
   * @return a jme3 mesh and the polygons that defines it or an undefined value if parsing fails.
   */
  public TransformedValue<Tuple2<Mesh, PolygonData[]>> transform(Tuple2<MeasuringUnit, PolygonData[]> data) {
    MeasuringUnit unit = data.getA();
    PolygonData[] polygons = data.getB();
    Tuple2<Mesh, PolygonData[]> result = null;
    if (polygons != null && polygons.length > 0) {
      Mesh mesh = null;
      //check if triangles
      boolean triangles = true;
      for (int i = 0; i < polygons.length & triangles; i++) {
        PolygonData polygonData = polygons[i];
        if (polygonData.getVertexCount() != 3) {
          triangles = false;
        }
      }
      if (!triangles) {
        polygons = triangulate(polygons);
      }
      int vcount = polygons.length * 3;
      PolygonData tag = polygons[0];
      if (!tag.getVertexSets().isEmpty()) {
        Integer vset = tag.getVertexSets().iterator().next();
        mesh = new Mesh();
        generatePositionsAndIndices(vcount, tag, vset, polygons, mesh, unit);
        generateBinormals(tag, vcount, polygons, mesh);
        generateNormals(tag, vcount, polygons, mesh);
        generateTangents(tag, vcount, polygons, mesh);
        generateTexcoords(tag, vcount, polygons, mesh);
        result = Tuple2.create(mesh, polygons);
      } else {
        logger.warning("No vertices for polygons!!!!");
      }
    }
    return TransformedValue.create(result);
  }

  private void generateTexcoords(PolygonData tag, int vcount, PolygonData[] polygons, Mesh mesh) {
    Set<Integer> texcoordSets = tag.getTexcoordSets();
    if (!texcoordSets.isEmpty()) {
      Integer set = texcoordSets.iterator().next();
      FloatBuffer buffer = BufferUtils.createFloatBuffer(tag.getTexcoordStride(set) * vcount);
      for (int i = 0; i < polygons.length; i++) {
        PolygonData polygonData = polygons[i];
        polygonData.popTexcoordSet(set, buffer);
      }
      buffer.flip();
      mesh.setBuffer(Type.TexCoord, tag.getTexcoordStride(set), buffer);
    }
  }

  private void generateTangents(PolygonData tag, int vcount, PolygonData[] polygons, Mesh mesh) {
    Set<Integer> tangentSets = tag.getTangentSets();
    if (!tangentSets.isEmpty()) {
      Integer set = tangentSets.iterator().next();
      FloatBuffer buffer = BufferUtils.createFloatBuffer(tag.getTangentStride(set) * vcount);
      for (int i = 0; i < polygons.length; i++) {
        PolygonData polygonData = polygons[i];
        polygonData.popTangentSet(set, buffer);
      }
      buffer.flip();
      mesh.setBuffer(Type.Tangent, tag.getTangentStride(set), buffer);
    }
  }

  private void generateNormals(PolygonData tag, int vcount, PolygonData[] polygons, Mesh mesh) {
    Set<Integer> normalSets = tag.getNormalSets();
    if (!normalSets.isEmpty()) {
      Integer set = normalSets.iterator().next();
      FloatBuffer buffer = BufferUtils.createFloatBuffer(tag.getNormalStride(set) * vcount);
      for (int i = 0; i < polygons.length; i++) {
        PolygonData polygonData = polygons[i];
        polygonData.popNormalSet(set, buffer);
      }
      buffer.flip();
      mesh.setBuffer(Type.Normal, tag.getNormalStride(set), buffer);
    }
  }

  private void generateBinormals(PolygonData tag, int vcount, PolygonData[] polygons, Mesh mesh) {
    Set<Integer> binormalSets = tag.getBinormalSets();
    if (!binormalSets.isEmpty()) {
      Integer set = binormalSets.iterator().next();
      FloatBuffer buffer = BufferUtils.createFloatBuffer(tag.getBinormalStride(set) * vcount);
      for (int i = 0; i < polygons.length; i++) {
        PolygonData polygonData = polygons[i];
        polygonData.popBinormalSet(set, buffer);
      }
      buffer.flip();
      mesh.setBuffer(Type.Binormal, tag.getBinormalStride(set), buffer);
    }
  }

  private void generatePositionsAndIndices(int vcount, PolygonData tag, Integer vset, PolygonData[] polygons, Mesh mesh, MeasuringUnit unit) {
    FloatBuffer vbuffer = BufferUtils.createFloatBuffer(vcount * tag.getVertexStride(vset));
    for (int i = 0; i < polygons.length; i++) {
      polygons[i].popVertexSet(vset, vbuffer);
    }
    vbuffer.flip();
    for (int i = 0; i < vbuffer.capacity(); i++) {
      vbuffer.put(i, vbuffer.get(i));
    }
    mesh.setBuffer(Type.Position, tag.getVertexStride(vset), vbuffer);

    IntBuffer indices = BufferUtils.createIntBuffer(vcount);
    for (int i = 0; i < vcount; i++) {
      indices.put(i);
    }
    indices.flip();
    mesh.setBuffer(Type.Index, 1, indices);
  }

  private PolygonData[] triangulate(PolygonData[] polygons) {
    ArrayList<PolygonData> data = new ArrayList<PolygonData>();
    for (int i = 0; i < polygons.length; i++) {
      PolygonData polygonData = polygons[i];
      data.addAll(Arrays.asList(polygonData.triangulate()));
    }
    polygons = data.toArray(new PolygonData[data.size()]);
    return polygons;
  }
}
