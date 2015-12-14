package jme3dae.collada14.transformers;

import jme3dae.collada14.ColladaSpec141.Semantic;
import jme3dae.utilities.IndexExtractor;
import jme3dae.utilities.Todo;

/**
 * Maps a collada input_shared element into a plugin object than knows how to transfer its
 * data into a set of polygons. This is used during geometry construction to parse the
 * various elements that define a collada geometry. Eg for a collade node:
 * <pre>&lt;polylist count="X"&gt;
 *  &lt;input semantic="VERTEX" source=...&gt;
 *  &lt;input semantic="NORMAL" source=...&gt;
 *  &lt;p&gt;1 2 3 4 5...&lt;/p&gt;
 * &lt;/polylist&gt;</pre>
 * the plugin will create 2 InputShared element, one for vertex, one for normal and a list of
 * X PolygonData components. The input
 * shared elements will then be used to parse the list of indices in the <code>p</code> node and
 * fill the PolygonData list. A vertex input will transfer data to the vertex buffer of each
 * polygon, a normal input will transfer data to the normal buffer of the polygon and so on.
 *
 * @author pgi
 */
public class InputShared {

  /**
   * Instance creator.
   *
   * @param semantic the collada semantic of the input data. The semantic is used by
   *                 the InputShared instance to decide what to do with its data when asked to transfer
   *                 it to a polygon set
   * @param offset   the offset of the input. This is the value of the offset attribute of
   *                 the collada input_shared element. Can be null, if so defaults to zero.
   * @param stride   how many components in the buffer used by this InputShared make one
   *                 data element. Eg a 3D vertex will have a stride of 3, an index buffer will have a stride
   *                 of 1. Can be null, defaults to 1.
   * @param set      the set defined by this input_shared element. Eg a geometry could have multiple
   *                 inputs for the texcoord semantic where each input defines a texcoord set in the geometry. Can
   *                 be null, defaults to zero.
   * @param idref    the idref buffer that will be used as the source of this input, can be null.
   * @param name     the name buffer that will be used as the source of this input, can be null
   * @param bool     the boolean buffer that will be used as the source of this input, can be null
   * @param floats   the float buffer that will be used as the source of this input, can be null
   * @param ints     the int buffer that will be used as the source of this input, can be null
   * @return a new InputShared instance
   */
  public static InputShared create(Semantic semantic, Integer offset, Integer stride, Integer set, String[] idref, String[] name, boolean[] bool, float[] floats, int[] ints) {
    InputShared instance = new InputShared(
        semantic,
        offset == null ? 0 : offset,
        stride == null ? 1 : stride,
        set == null ? 0 : set,
        idref, name, bool, floats, ints);
    return instance;
  }

  private final int stride;
  private final String[] idrefBuffer;
  private final String[] nameBuffer;
  private final boolean[] boolBuffer;
  private final float[] floatBuffer;
  private final int[] intBuffer;
  private final Semantic semantic;
  private final int offset;
  private final int set;

  private InputShared(Semantic s, int offset, int stride, int set, String[] idref, String[] name, boolean[] bool, float[] floats, int[] ints) {
    this.set = set;
    this.offset = offset;
    this.semantic = s;
    this.stride = stride;
    this.idrefBuffer = idref;
    this.nameBuffer = name;
    this.boolBuffer = bool;
    this.floatBuffer = floats;
    this.intBuffer = ints;
  }

  /**
   * Returns the offset of this InputShared. The offset is used to extract the indices
   * of the values defined by this input shared from a list of collada's primitive indices.
   *
   * @return the offset of this InputShared.
   */
  public int getOffset() {
    return offset;
  }

  public Semantic getSemantic() {
    return semantic;
  }

  /**
   * Transfers the data pointed by this input shared into the given set of polygons.
   *
   * @param chunkSize the size of a chunk of indices in the chunks list
   * @param chunks    the list of indices
   * @param polygons  the list of polygons to fill.
   */
  public void transferData(int chunkSize, int[] chunks, PolygonData[] polygons) {
    int[] indices = IndexExtractor.create(chunkSize, chunks).get(offset);
    switch (semantic) {
      case VERTEX:
      case POSITION:
        if (floatBuffer != null) transferVertices(indices, polygons, floatBuffer);
        break;
      case NORMAL:
        if (floatBuffer != null) transferNormals(indices, polygons, floatBuffer);
        break;
      case BINORMAL:
        if (floatBuffer != null) transferBinormals(indices, polygons, floatBuffer);
        break;
      case TEXCOORD:
        if (floatBuffer != null) transferTexCoords(indices, polygons, floatBuffer);
        break;
      case TANGENT:
        if (floatBuffer != null) transferTangents(indices, polygons, floatBuffer);
        break;
      default:
        Todo.task("implement transfer of input type " + semantic);
    }
  }


  private void transferVertices(int[] indices, PolygonData[] polygons, float[] floatBuffer) {
    int index = 0;
    int polygon = 0;
    while (index < indices.length && polygon < polygons.length) {
      for (int i = 0; i < polygons[polygon].getVertexCount() & index < indices.length; i++) {
        float[] vdata = getFloatSet(floatBuffer, stride, indices[index]);
        polygons[polygon].pushVertex(set, indices[index], vdata);
        index++;
      }
      polygon++;
    }
  }

  private void transferNormals(int[] indices, PolygonData[] polygons, float[] floatBuffer) {
    int index = 0;
    int polygon = 0;
    while (index < indices.length && polygon < polygons.length) {
      for (int i = 0; i < polygons[polygon].getVertexCount() & index < indices.length; i++) {
        float[] vdata = getFloatSet(floatBuffer, stride, indices[index]);
        polygons[polygon].pushNormal(set, vdata);
        index++;
      }
      polygon++;
    }
  }

  private void transferBinormals(int[] indices, PolygonData[] polygons, float[] floatBuffer) {
    int index = 0;
    int polygon = 0;
    while (index < indices.length && polygon < polygons.length) {
      for (int i = 0; i < polygons[polygon].getVertexCount() & index < indices.length; i++) {
        float[] vdata = getFloatSet(floatBuffer, stride, indices[index]);
        polygons[polygon].pushBinormal(set, vdata);
        index++;
      }
      polygon++;
    }
  }

  private void transferTexCoords(int[] indices, PolygonData[] polygons, float[] floatBuffer) {
    int index = 0;
    int polygon = 0;
    while (index < indices.length && polygon < polygons.length) {
      for (int i = 0; i < polygons[polygon].getVertexCount() & index < indices.length; i++) {
        float[] vdata = getFloatSet(floatBuffer, stride, indices[index]);
        polygons[polygon].pushTexcoord(set, vdata);
        index++;
      }
      polygon++;
    }
  }

  private void transferTangents(int[] indices, PolygonData[] polygons, float[] floatBuffer) {
    int index = 0;
    int polygon = 0;
    while (index < indices.length && polygon < polygons.length) {
      for (int i = 0; i < polygons[polygon].getVertexCount() & index < indices.length; i++) {
        float[] vdata = getFloatSet(floatBuffer, stride, indices[index]);
        polygons[polygon].pushTangent(set, vdata);
        index++;
      }
      polygon++;
    }
  }

  private float[] getFloatSet(float[] floatBuffer, int stride, int i) {
    float[] buffer = new float[stride];
    System.arraycopy(floatBuffer, i * stride, buffer, 0, stride);
    return buffer;
  }
}
