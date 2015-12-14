package jme3dae;

import com.jme3.asset.AssetInfo;

/**
 * Informations about the collada document, stored in the DAENode root as
 * parsed data. This is used to access external resources (currently just the AssetInfo
 * to find texture images).
 *
 * @author pgi
 */
public class ColladaInfo {

  /**
   * Creator for ColladaInfo instances.
   *
   * @param info the AssetInfo pointing to the dae document. The location of
   *             the file wrapped in the asset info is used to search images.
   * @return a new ColladaInfo instance.
   */
  public static ColladaInfo create(AssetInfo info) {
    return new ColladaInfo(info);
  }


  private final AssetInfo INFO;

  /**
   * Initialize this instance.
   *
   * @param info the asset info wrapped by this ColladInfo.
   */
  private ColladaInfo(AssetInfo info) {
    INFO = info;
  }

  /**
   * Returns the asset info that points to the collada document file.
   *
   * @return the asset info used to load the collada model.
   */
  public AssetInfo getInfo() {
    return INFO;
  }
}
