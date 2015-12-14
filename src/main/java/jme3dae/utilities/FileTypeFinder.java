package jme3dae.utilities;

/**
 * Scans an array of bytes searching for a known header sequence. Used in the
 * (nasty) case where COLLADA stores an image file in a hex sequence without
 * declaring the image format.
 *
 * @author pgi
 */
public class FileTypeFinder {

  /**
   * Instance creator.
   *
   * @return a new FileTypeFinder instance.
   */
  public static FileTypeFinder create() {
    return new FileTypeFinder();
  }

  private FileTypeFinder() {
  }

  /**
   * Returns the file type for the given byte header.
   *
   * @param header the bytes to check for a known file header match.
   * @return the file type denoted by the given header (maybe FileType.UNKNONW).
   */
  public FileType getFileType(byte[] header) {
    FileType[] types = FileType.values();
    for (int i = 0; i < types.length; i++) {
      if (types[i].matches(header)) {
        return types[i];
      }
    }
    return FileType.UNKNOWNW;
  }
}
