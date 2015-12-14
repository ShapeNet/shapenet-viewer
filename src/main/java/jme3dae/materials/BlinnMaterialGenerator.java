package jme3dae.materials;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

/**
 * Utility class to fill a JME3 material with the values defined in a collada blinn material node.
 *
 * @author pgi
 */
public class BlinnMaterialGenerator extends FXBumpMaterialGenerator {

  /**
   * Instance creator.
   *
   * @param am the AssetManager used to generate the jme3 material.
   * @return a new BlinnMaterialGenerator instance.
   */
  public static BlinnMaterialGenerator create(AssetManager am) {
    return new BlinnMaterialGenerator(am);
  }

  private BlinnMaterialGenerator(AssetManager am) {
    //super(am, new Material(am, "jme3dae/materials/ColladaBlinn.j3md"));
    super(am, "blinn");
  }

  public void setSpecular(ColorRGBA color) {
    if (color != null) {
      MATERIAL.setBoolean("UseMaterialColors", true);
      MATERIAL.setColor("Specular", color);
    }
  }

  public void setSpecular(Texture texture) {
    if (texture != null) {
      MATERIAL.setTexture("SpecularMap", texture);
      MATERIAL.setBoolean("UseMaterialColors", false);
    }
  }
}
