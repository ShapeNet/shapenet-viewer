package jme3dae.utilities;

public class VertexSkinningData {

  public static VertexSkinningData create(int vertexIndex, int boneIndex, float weight) {
    return new VertexSkinningData(vertexIndex, boneIndex, weight);
  }

  private final int boneIndex;
  private final float weight;
  private final int vertexIndex;

  private VertexSkinningData(int vertexIndex, int boneIndex, float weight) {
    this.vertexIndex = vertexIndex;
    this.boneIndex = boneIndex;
    this.weight = weight;
  }

  public int getVertexIndex() {
    return vertexIndex;
  }

  public int getBoneIndex() {
    return boneIndex;
  }

  public float getWeight() {
    return weight;
  }
}
