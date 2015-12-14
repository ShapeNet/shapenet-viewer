package jme3dae.utilities;

/**
 * A couple of file types, used in case the COLLADA document stores image data
 * in line (as hex byte sequence).
 *
 * @author pgi
 */
public enum FileType {

  /**
   * Unknown type
   */
  UNKNOWNW("unknown"),

  /**
   * BMP image
   */
  BMP("bmp", 0x42, 0x4D),

  /**
   * GIF89A image
   */
  GIF89A("gif", 0x47, 0x49, 0x46, 0x38, 0x39, 0x61),

  /**
   * GIF87A image
   */
  GIF87A("gif", 0x47, 0x49, 0x46, 0x38, 0x37, 0x61),

  /**
   * JPEG image
   */
  JPEG("jpg", 0xFF, 0xD8),

  /**
   * JFIF image
   */
  JFIF("jpg", 0x4A, 0x46, 0x49, 0x46),

  /**
   * MIDI file
   */
  MIDI("midi", 0x4D, 0x54, 0x68, 0x64),

  /**
   * TIFF image
   */
  TIFF1("tiff", 0x49, 0x49, 0x2A, 0x00),

  /**
   * TIFF image
   */
  TIFF2("tiff", 0x4D, 0x4D, 0x00, 0x2A),

  /**
   * PNG image
   */
  PNG("png", 137, 80, 78, 71, 13, 10, 26, 10);

  private int[] header;
  private String ext;

  FileType(String ext, int... bytes) {
    header = bytes;
  }

  /**
   * Returns the typical file extension of this file format
   *
   * @return the file extension of this format (no dots).
   */
  public String getExtension() {
    return ext;
  }

  /**
   * True if the given byte array starts with the header of this file type
   *
   * @param values the byte array holding the data of the header of a file
   * @return true if the array matches the header of this file type.
   */
  public boolean matches(byte[] values) {
    if (values.length == header.length) {
      for (int i = 0; i < values.length; i++) {
        if (values[i] == header[i]) {
          return true;
        }
      }
      return false;
    } else {
      return false;
    }
  }
}
