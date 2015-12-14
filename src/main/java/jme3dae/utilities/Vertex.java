package jme3dae.utilities;

/**
 * A class used to define the vertex of a polygon. The vertex carries an index
 * value. The index is used to bind this vertex to the original vertex indexing of
 * the collada document.
 *
 * @author pgi
 */
public class Vertex {

  /**
   * Instance creator
   *
   * @param index the index of the vertex in the collada document
   * @param data  the components of the vertex (x, y, z)
   * @return a new Vertex
   */
  public static Vertex create(int index, float[] data) {
    return new Vertex(index, data);
  }

  private final int index;
  private final float[] data;

  private Vertex(int index, float[] data) {
    this.index = index;
    this.data = data;
  }

  /**
   * Returns the index of this vertex in the collada document vertex set.
   *
   * @return the index of this vertex in the collada document vertex set.
   */
  public int getIndex() {
    return index;
  }

  /**
   * Returns the components of this vertex. The returned array is the same array
   * held by this instance: changes to the returned array may have effects on other
   * objects.
   *
   * @return the components of this vertex.
   */
  public float[] getData() {
    return data;
  }

  /**
   * A string representing this vertex, for debug purposes.
   *
   * @return a string representation of this vertex.
   */
  @Override
  public String toString() {
    return "(" + data[0] + "," + data[1] + "," + data[2] + ")";
  }
}
