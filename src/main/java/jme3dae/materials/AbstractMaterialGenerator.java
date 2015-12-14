package jme3dae.materials;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

public class AbstractMaterialGenerator {
  protected final AssetManager ASSET_MANAGER;
  protected final Material MATERIAL;

  protected AbstractMaterialGenerator(AssetManager am, Material mat) {
    ASSET_MANAGER = am;
    MATERIAL = mat;
  }

  public Material get() {
    return MATERIAL;
  }

  public void setAmbient(ColorRGBA color) {
    //if(color != null) {
    //    MATERIAL.setColor("ambientColor", color);
    //    MATERIAL.setBoolean("useAmbientColor", true);
    //}
    if (color != null) {
      MATERIAL.setBoolean("UseMaterialColors", true);
      MATERIAL.setColor("Ambient", color);
    }
  }

  public void setAmbient(Texture texture) {
    if (texture != null) {
      MATERIAL.setTexture("ambientTexture", texture);
      MATERIAL.setBoolean("useAmbientTexture", true);
    }
  }

  public void setDiffuse(ColorRGBA color) {
    if (color != null) {
      MATERIAL.setColor("diffuseColor", color);
      MATERIAL.setBoolean("useDiffuseColor", true);
    }
  }

  public void setDiffuse(Texture texture) {
    if (texture != null) {
      MATERIAL.setTexture("diffuseTexture", texture);
      MATERIAL.setBoolean("useDiffuseTexture", true);
    }
  }

  public void setSpecular(ColorRGBA color) {
    if (color != null) {
      MATERIAL.setColor("specularColor", color);
      MATERIAL.setBoolean("useSpecularColor", true);
    }
  }

  public void setSpecular(Texture texture) {
    if (texture != null) {
      MATERIAL.setTexture("specularTexture", texture);
      MATERIAL.setBoolean("useSpecularTexture", true);
    }
  }
}
