package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetKey;

/**
 * Enhanced asset key that stored some more information
 *
 * @author Angel Chang
 */
public class EnhancedAssetKey<T> extends AssetKey<T> {
  final String geometryPath;
  final String materialsPath;

  public EnhancedAssetKey(String name, String geometryPath, String materialsPath) {
    super(name);
    this.geometryPath = geometryPath;
    this.materialsPath = materialsPath;
  }

  public String getGeometryPath() {
    return geometryPath;
  }

  public String getMaterialsPath() {
    return materialsPath;
  }
}
