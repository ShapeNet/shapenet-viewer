package jme3dae.materials;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

/**
 * Utility class that generates a JME3 material to map a phong material
 *
 * @author pgi
 */
public class PhongMaterialGenerator extends FXBumpMaterialGenerator {

  /**
   * Instance creator
   *
   * @param am the asset manager for the jme3 material
   * @return a new PhongMaterialGenerator
   */
  public static PhongMaterialGenerator create(AssetManager am) {
    return new PhongMaterialGenerator(am);
  }

  /**
   * Instance initializer
   *
   * @param am the asset manager for the generated material
   */
  private PhongMaterialGenerator(AssetManager am) {
    //super(am, new Material(am, "jme3dae/materials/ColladaPhong.j3md"));
    super(am, "phong");
  }


}
