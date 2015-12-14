package jme3dae.utilities;

import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;

/**
 * Transforms a string in a sequence of bytes. The string is read as a hexadecimal
 * encoded byte sequence.
 *
 * @author pgi
 */
public class HexSequenceTransformer implements ValueTransformer<String, byte[]> {

  /**
   * Instance creator
   *
   * @return a new HexSequenceTransformer instance.
   */
  public static HexSequenceTransformer create() {
    return new HexSequenceTransformer();
  }

  private HexSequenceTransformer() {
  }

  /**
   * Transforms a string carrying a sequence of hexadecimal bytes in a byte array
   *
   * @param value a sequence of hexadecimal characters
   * @return a byte array or an undefined value if the parsing fails.
   */
  public TransformedValue<byte[]> transform(String value) {
    byte[] data = null;
    if (value != null && (value.length() != 0)) {
      data = parse(value);
    }
    return TransformedValue.create(data);
  }

  private byte[] parse(String value) {
    byte[] data = new byte[value.length() / 2];
    for (int i = 0; i < value.length(); i += 2) {
      String s = "0x" + value.substring(i, i + 2);
      data[i / 2] = (byte) (0x000000FF & Integer.decode(s));
    }
    return data;
  }
}
