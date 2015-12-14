package jme3dae.utilities;

/**
 * Extract an array of indices from an array of integers read as a sequence of
 * chunks.
 *
 * @author pgi
 */
public class IndexExtractor {

  /**
   * Instance creator
   *
   * @param chunkSize the number of integers that form a chunk in the data
   *                  array.
   * @param data      the array of indices to read
   * @return a new IndexExtractor.
   */
  public static IndexExtractor create(int chunkSize, int[] data) {
    return new IndexExtractor(chunkSize, data);
  }

  private final int[] DATA;
  private final int CHUNK_SIZE;

  private IndexExtractor(int chunkSize, int[] data) {
    CHUNK_SIZE = chunkSize;
    DATA = data;
  }

  /**
   * Returns the indices in the data set that can be mapped to the given offset.
   * For an data set {1, 2, 3, 4, 5, 6} where the chunkSize is set to 3, the offset
   * 0 returns the values {1, 4} the offset 1 the values {2, 5} and so on
   *
   * @param offset the offset of the indices to extract
   * @return an array of indices.
   */
  public int[] get(int offset) {
    int[] buffer = new int[DATA.length / CHUNK_SIZE];
    int index = 0;
    for (int i = 0; i < DATA.length; i += CHUNK_SIZE) {
      buffer[index] = DATA[i + offset];
      index++;
    }
    return buffer;
  }
}
