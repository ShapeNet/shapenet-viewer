package jme3dae.collada14.transformers;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import jme3dae.utilities.Todo;
import jme3dae.utilities.Tuple2;
import jme3dae.utilities.Vertex;
import jme3dae.utilities.VertexSkinningData;

/**
 * A progressive polygon. The polygon defines a number of vertices and waits to be
 * "filled" with data. The polygon supports multiple data sets and a variety of
 * values. It also supports a very cheap triangulation form (fanning).
 *
 * @author pgi
 */
public class PolygonData {

  /**
   * Instance initializer.
   *
   * @param vertexCount the number of vertices in this polygon. The same number is
   *                    also used for normals, texcoords, tangents and so on.
   * @return a new PolygonData instance.
   */
  public static PolygonData create(int vertexCount) {
    PolygonData instance = new PolygonData(vertexCount);
    return instance;
  }

  private final int vertexCount;
  private Map<Integer, List<Vertex>> vsets = new HashMap<Integer, List<Vertex>>();
  private Map<Integer, List<float[]>> nsets = new HashMap<Integer, List<float[]>>();
  private Map<Integer, List<float[]>> tsets = new HashMap<Integer, List<float[]>>();
  private Map<Integer, List<float[]>> bnsets = new HashMap<Integer, List<float[]>>();
  private Map<Integer, List<float[]>> tansets = new HashMap<Integer, List<float[]>>();
  private Map<Vertex, List<Tuple2<Integer, Float>>> influences = new HashMap<Vertex, List<Tuple2<Integer, Float>>>();//3 beacause this will most likely be called for triangle

  private PolygonData(int vertexCount) {
    this.vertexCount = vertexCount;
  }

  /**
   * Returns the indices of the vertex sets stored in this polygon in ascendint order.
   *
   * @return the indices of the vertex sets of this polygon. Never null,
   * can be empty.
   */
  public Set<Integer> getVertexSets() {
    return new TreeSet<Integer>(vsets.keySet());
  }

  /**
   * Returns the indices of the normal sets of this polygon in ascending order.
   *
   * @return the indices of the normal sets of this polygon. Never null, can
   * be empty.
   */
  public Set<Integer> getNormalSets() {
    return new TreeSet<Integer>(nsets.keySet());
  }

  /**
   * Returns the indices of the texcoord sets of this polygon in ascending order.
   *
   * @return the indices of the texcoord sets of this polygon. Never null, can be
   * empty.
   */
  public Set<Integer> getTexcoordSets() {
    return new TreeSet<Integer>(tsets.keySet());
  }

  /*
   * Returns the indices of the binormal sets of this polygon in ascending order.
   * @return the indices of the binormal sets of this polygon. Never null, can be
   * empty.
   */
  public Set<Integer> getBinormalSets() {
    return new TreeSet<Integer>(bnsets.keySet());
  }

  /*
   * Returns the indices of the tangent sets of this polygon in ascending order.
   * @return the indices of the tangent sets of this polygon. Never null, can be
   * empty.
   */
  public Set<Integer> getTangentSets() {
    return new TreeSet<Integer>(tansets.keySet());
  }


  /**
   * Triangulates this polygon. The current triangulation is fanning, which works
   * for simple convex polygons but goes nowhere near proper triangulation for
   * everything else.
   *
   * @return an array of triangular polygons.
   */
  public PolygonData[] triangulate() {
    if (vertexCount <= 3) {
      return new PolygonData[]{this};
    } else {
      int tricount = vertexCount - 2;
      PolygonData[] tris = new PolygonData[tricount];
      int tri = 0;
      int vindex = 1;
      for (int i = 0; i < tricount; i++, vindex++) {
        PolygonData p = PolygonData.create(3);
        for (Map.Entry<Integer, List<Vertex>> entry : vsets.entrySet()) {
          Vertex v0 = entry.getValue().get(0);
          Vertex v1 = entry.getValue().get(vindex);
          Vertex v2 = entry.getValue().get(vindex + 1);
          p.pushVertex(entry.getKey(), v0.getIndex(), v0.getData());
          p.pushVertex(entry.getKey(), v1.getIndex(), v1.getData());
          p.pushVertex(entry.getKey(), v2.getIndex(), v2.getData());
        }
        for (Map.Entry<Integer, List<float[]>> entry : nsets.entrySet()) {
          p.pushNormal(entry.getKey(), entry.getValue().get(0));
          p.pushNormal(entry.getKey(), entry.getValue().get(vindex));
          p.pushNormal(entry.getKey(), entry.getValue().get(vindex + 1));
        }
        for (Map.Entry<Integer, List<float[]>> entry : tsets.entrySet()) {
          p.pushTexcoord(entry.getKey(), entry.getValue().get(0));
          p.pushTexcoord(entry.getKey(), entry.getValue().get(vindex));
          p.pushTexcoord(entry.getKey(), entry.getValue().get(vindex + 1));
        }
        for (Map.Entry<Integer, List<float[]>> entry : bnsets.entrySet()) {
          p.pushBinormal(entry.getKey(), entry.getValue().get(0));
          p.pushBinormal(entry.getKey(), entry.getValue().get(vindex));
          p.pushBinormal(entry.getKey(), entry.getValue().get(vindex + 1));
        }
        for (Map.Entry<Integer, List<float[]>> entry : tansets.entrySet()) {
          p.pushTangent(entry.getKey(), entry.getValue().get(0));
          p.pushTangent(entry.getKey(), entry.getValue().get(vindex));
          p.pushTangent(entry.getKey(), entry.getValue().get(vindex + 1));
        }
        tris[i] = p;
      }
      return tris;
    }
  }

  /**
   * Returns the number of vertices of this polygon. Eg if 3 then this is a
   * triangle.
   *
   * @return the number of vertices of this polygon.
   */
  public int getVertexCount() {
    return vertexCount;
  }

  /**
   * Add a tangent to the given tangent set.
   *
   * @param set     the tangent set to fill
   * @param tandata the new tangent
   * @throws IllegalStateException if the given set has already <code>getVertexCount()</code> values stored.
   */
  public void pushTangent(int set, float[] tandata) throws IllegalStateException {
    List<float[]> list = getTangentSet(set);
    checkSize(list);
    list.add(tandata);
  }

  /**
   * Add a texcoord to the given texcoord set.
   *
   * @param set   the texcoord set to fill
   * @param tdata the texcoord to add
   * @throws IllegalStateException if the given set has already <code>getVertexCount()</code> values stored.
   */
  public void pushTexcoord(int set, float[] tdata) throws IllegalStateException {
    List<float[]> tset = getTexcoordSet(set);
    checkSize(tset);
    tset.add(tdata);
  }

  /**
   * Add a normal to the given normal set
   *
   * @param set   the normal set to fill
   * @param vdata the normal to add
   * @throws IllegalStateException if the given set has already <code>getVertexCount()</code> values stored.
   */
  public void pushNormal(int set, float[] vdata) throws IllegalStateException {
    List<float[]> nset = getNormalSet(set);
    checkSize(nset);
    nset.add(vdata);
  }

  /**
   * Add a binormal to the given binormal set
   *
   * @param set    the binormal set to fill
   * @param bndata the binormal to add
   * @throws IllegalStateException if the given set has already <code>getVertexCount()</code> values stored.
   */
  public void pushBinormal(int set, float[] bndata) throws IllegalStateException {
    List<float[]> list = getBinormalSet(set);
    checkSize(list);
    list.add(bndata);
  }

  /**
   * Add a vertex to the given vertex set
   *
   * @param set           the vertex set to fill
   * @param originalIndex the index of the vertex in the collada vertex buffer. This is needed because the
   *                      vertex buffer created when transforming polygons in a jme3 mesh can change but other part of the collada
   *                      document (namely animation) can refer to the original index of a vertex.
   * @param vdata         the vertex data
   * @throws IllegalStateException if the given set has already <code>getVertexCount()</code> values stored.
   */
  public void pushVertex(int set, int originalIndex, float[] vdata) throws IllegalStateException {
    List<Vertex> vset = getVertexSet(set);
    checkSize(vset);
    Vertex v = Vertex.create(originalIndex, vdata);
    vset.add(v);
  }

  /**
   * Transfers a texcoord set to the given buffer
   *
   * @param set    the texcoord set to extract.
   * @param buffer the destination buffer. The values are inserted starting from the current
   *               buffer position.
   */
  public void popTexcoordSet(int set, FloatBuffer buffer) {
    for (float[] fs : tsets.get(set)) {
      buffer.put(fs);
    }
  }

  /**
   * Transfers a normal set to the given buffer
   *
   * @param set    the normal set to transfer
   * @param buffer the destination buffer. The values are inserted starting from the current
   *               buffer position.
   */
  public void popNormalSet(int set, FloatBuffer buffer) {
    for (float[] fs : nsets.get(set)) {
      buffer.put(fs);
    }
  }

  /**
   * Transfers a binormal set to the given buffer
   *
   * @param set    the binormal set to transfer
   * @param buffer the destination buffer. The values are inserted starting from the current
   *               buffer position.
   */
  public void popBinormalSet(int set, FloatBuffer buffer) {
    for (float[] fs : bnsets.get(set)) {
      buffer.put(fs);
    }
  }

  /**
   * Transfers a vertex set to the given buffer
   *
   * @param set    the vertex set to transfer
   * @param buffer the destination buffer. The values are inserted starting from the current
   *               buffer position.
   */
  public void popVertexSet(int set, FloatBuffer buffer) {
    for (Vertex vertex : vsets.get(set)) {
      buffer.put(vertex.getData());
    }
  }

  /**
   * Transfers a tangent set to the given buffer
   *
   * @param set    the tangent set to transfer
   * @param buffer the destination buffer. The values are inserted starting from the current
   *               buffer position
   */
  public void popTangentSet(int set, FloatBuffer buffer) {
    for (float[] fs : tansets.get(set)) {
      buffer.put(fs);
    }
  }

  /**
   * Returns the number of components of a texture coordinate element in the
   * given set (eg 2 for a tex2d element, 3 for a tex3d 4 for a tex4d)
   *
   * @param set the texcoord set to check
   * @return the number of components of a texture coordinate element
   */
  public int getTexcoordStride(int set) {
    return tsets.get(set).get(0).length;
  }

  /**
   * Returns the number of components of a normal element in the given set (eg
   * 2 for a 2D normal, 3 for a 3D normal)
   *
   * @param set the normal set to check
   * @return the number of components of a normal value in the given set
   */
  public int getNormalStride(int set) {
    return nsets.get(set).get(0).length;
  }

  /**
   * Returns the number of components of binormal element in the given set.
   *
   * @param set the binormal set to check
   * @return the number of components of a binormal value in the given set
   */
  public int getBinormalStride(int set) {
    return bnsets.get(set).get(0).length;
  }

  /**
   * Returns the number of components of a vertex element in the given set
   *
   * @param set the vertex set to check
   * @return the number of components of a vertex value in the given set (eg
   * 2 for a 2D vertex, 3 for a 3D one).
   */
  public int getVertexStride(int set) {
    return 3;
  }

  /**
   * Returns the number of components of a tangent element in the given set.
   *
   * @param set the tangent set to check.
   * @return the number of components of a tangent value if the given set.
   */
  public int getTangentStride(int set) {
    return tansets.get(set).get(0).length;
  }


  private void checkSize(List<?> list) {
    if (list.size() == vertexCount) throw new IllegalStateException("Polygon is full");
  }

  private List<float[]> getTangentSet(int set) {
    List<float[]> list = tansets.get(set);
    if (list == null) tansets.put(set, new ArrayList<float[]>(vertexCount));
    return list;
  }

  private List<float[]> getBinormalSet(int set) {
    List<float[]> list = bnsets.get(set);
    if (list == null) bnsets.put(set, list = new ArrayList<float[]>(vertexCount));
    return list;
  }

  private List<float[]> getTexcoordSet(int set) {
    List<float[]> list = tsets.get(set);
    if (list == null) tsets.put(set, list = new ArrayList<float[]>(vertexCount));
    return list;
  }

  private List<float[]> getNormalSet(int set) {
    List<float[]> list = nsets.get(set);
    if (list == null) nsets.put(set, list = new ArrayList<float[]>(vertexCount));
    return list;
  }

  private List<Vertex> getVertexSet(int set) {
    List<Vertex> list = vsets.get(set);
    if (list == null) {
      vsets.put(set, list = new ArrayList<Vertex>(vertexCount));
    }
    return list;
  }

  /**
   * Returns a string representing this polygon (debug purposes)
   *
   * @return a string representing this polygon.
   */
  @Override
  public String toString() {
    StringBuilder buffer = new StringBuilder();
    buffer.append("Polygon(").append(vertexCount).append(")");
    for (Map.Entry<Integer, List<Vertex>> entry : vsets.entrySet()) {
      buffer.append("VSET(").append(entry.getKey()).append(")");
      for (Vertex vertex : entry.getValue()) {
        buffer.append(vertex);
      }
    }
    return buffer.toString();
  }

  /**
   * Sets the skinning data.
   *
   * @param vsdList the list of indices/weight relative to the "original" vertex indexing (ie
   *                the indexing defined by the collada document). This method will map the skin data to
   *                the new vertex.
   */
  public void pushVertexSkinningData(List<VertexSkinningData> vsdList) {
    List<Vertex> defaultVertexSet = vsets.get(0);
    for (Vertex vertex : defaultVertexSet) {
      List<Tuple2<Integer, Float>> inf = getInfluenceData(vertex);
      VertexSkinningData skin = getSkinData(vertex, vsdList);
      if (skin != null) {
        Tuple2<Integer, Float> t = Tuple2.create(skin.getBoneIndex(), skin.getWeight());
        inf.add(t);
      }
    }
  }

  /**
   * Extract the vertex skinning data.
   *
   * @param boneIndicesData the bone indices buffer (4 elements per vertex)
   * @param boneWeightData  the bone weight buffer (4 elements per vertex)
   */
  public void popVertexSkinningData(ByteBuffer boneIndicesData, FloatBuffer boneWeightData) {
    for (Vertex v : this.vsets.get(0)) {
      List<Tuple2<Integer, Float>> data = getInfluenceData(v);
      byte[] bones = new byte[4];
      float[] weights = new float[4];
      popInfluenceData(data, bones, weights);
      boneIndicesData.put(bones);
      boneWeightData.put(weights);
    }
  }

  private VertexSkinningData getSkinData(Vertex vertex, List<VertexSkinningData> vsdList) {
    for (VertexSkinningData vertexSkinningData : vsdList) {
      if (vertexSkinningData.getVertexIndex() == vertex.getIndex()) {
        return vertexSkinningData;
      }
    }
    return null;
  }

  private List<Tuple2<Integer, Float>> getInfluenceData(Vertex vertex) {
    List<Tuple2<Integer, Float>> list = influences.get(vertex);
    if (list == null) {
      influences.put(vertex, list = new ArrayList<Tuple2<Integer, Float>>(4));
    }
    return list;
  }

  private void popInfluenceData(List<Tuple2<Integer, Float>> data, byte[] bones, float[] weights) {
    if (bones.length != 4 || weights.length != 4) {
      throw new IllegalArgumentException("buffers size must be 4");
    }
    for (int i = 0; i < 4; i++) {
      if (i < data.size()) {
        Tuple2<Integer, Float> inf = data.get(i);
        bones[i] = inf.getA().byteValue();
        weights[i] = inf.getB().floatValue();
      }
    }
    normalize(weights);
  }

  private void normalize(float[] weights) {
    float n = 0;
    for (int i = 0; i < weights.length; i++) {
      n += weights[i] * weights[i];
    }
    if (n != 0) {
      n = (float) Math.sqrt(n);
      for (int i = 0; i < weights.length; i++) {
        weights[i] /= n;
      }
    }
  }
}
