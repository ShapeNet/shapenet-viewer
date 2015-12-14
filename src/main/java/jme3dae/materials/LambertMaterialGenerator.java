package jme3dae.materials;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;

/**
 * Utility class to fill a JME3 material with the values defined in a collada lambert material node.
 *
 * @author pgi
 */
public class LambertMaterialGenerator extends FXBumpMaterialGenerator {

  /**
   * Instance creator
   *
   * @param am the asset manager used to create the jme3 material
   * @return a new LamberMaterialGenerator
   */
  public static LambertMaterialGenerator create(AssetManager am) {
    return new LambertMaterialGenerator(am);
  }

  private LambertMaterialGenerator(AssetManager am) {
    //super(am, new Material(am, "jme3dae/materials/ColladaLambert.j3md"));
    super(am, "lambert");
  }
}
