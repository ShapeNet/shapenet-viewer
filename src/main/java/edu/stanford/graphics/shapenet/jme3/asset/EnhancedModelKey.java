package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.ModelKey;

/**
 * Model key with more properties
 *
 * @author Angel Chang
 */
public class EnhancedModelKey extends ModelKey {
  final String geometryPath;
  final String materialsPath;

  public EnhancedModelKey(String name, String geometryPath, String materialsPath) {
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
