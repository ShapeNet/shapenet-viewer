package jme3dae.materials;

import com.jme3.asset.AssetManager;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3dae.utilities.NormalMapFilter;
import jme3tools.converters.ImageToAwt;

public class FXBumpMaterialGenerator {
  private static final Map<Texture, Texture> NORMAL_MAPS = new ConcurrentHashMap<Texture, Texture>();

  public static FXBumpMaterialGenerator create(AssetManager am, String baseTextureName) {
    return new FXBumpMaterialGenerator(am, baseTextureName);
  }

  private final AssetManager ASSET_MANAGER;
  protected Material MATERIAL = null;

  protected final boolean useLighting = true;
  private boolean hasTexture = false;

  protected FXBumpMaterialGenerator(AssetManager am, String baseTextureName) {
    Logger.getLogger(getClass().getName()).log(Level.INFO, "Generating Material " + baseTextureName);
    ASSET_MANAGER = am;

    if (useLighting) {
      MATERIAL = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
      setDiffuse(ColorRGBA.White);
    } else {
      MATERIAL = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
    }
  }

  public void setTexture(Texture texture) {
    if (texture != null) {
      if (useLighting) {
        MATERIAL.setTexture("DiffuseMap", texture);

      } else {
        MATERIAL.setTexture("ColorMap", texture);
      }
      hasTexture = true;
    }
  }

  public void setAmbient(ColorRGBA color) {
    if (color != null) {
      if (useLighting) {
        MATERIAL.setBoolean("UseMaterialColors", true);
        MATERIAL.setColor("Ambient", color);
      } else {
        MATERIAL.setColor("Color", color);
      }
    }
  }

  public void setAmbient(Texture texture) {
    if (useLighting) {
      if (texture != null) {
        MATERIAL.setTexture("DiffuseMap", texture);
      }
    } else {
      if (texture != null) {
        MATERIAL.setTexture("ColorMap", texture);
      }

    }
  }

  public void setDiffuse(ColorRGBA color) {
    if (color != null) {
      if (useLighting) {
        MATERIAL.setBoolean("UseMaterialColors", true);
        MATERIAL.setColor("Diffuse", color);
      } else {
        MATERIAL.setColor("Color", color);
      }
    }
  }

  public void setDiffuse(Texture texture) {
    if (useLighting) {
      if (texture != null) {
        if (MATERIAL.getParam("DiffuseMap") == null)
          MATERIAL.setTexture("DiffuseMap", texture);
      }
    } else {
      if (texture != null) {
        MATERIAL.setTexture("LightMap", texture);
      }
    }
  }

  public void setSpecular(ColorRGBA color) {
    if (color != null) {
      if (useLighting) {
        MATERIAL.setBoolean("UseMaterialColors", true);
        MATERIAL.setColor("Specular", color);
      } else {
        MATERIAL.setColor("GlowColor", color);
      }
    }
  }

  public void setSpecular(Texture texture) {
    if (texture != null) {
      if (useLighting) {
        if (MATERIAL.getParam("SpecularMap") == null)
          MATERIAL.setTexture("SpecularMap", texture);
      } else {
        MATERIAL.setTexture("GlowMap", texture);
      }
    }
  }

  public void setShininess(Float value) {
    if (value != null) {
      MATERIAL.setFloat("Shininess", value);
    }
  }

  public void setTransparency(Float value) {
    boolean isTransparent = (value != null && value > 0.0f);
    MATERIAL.setTransparent(isTransparent);
    if (isTransparent) {
      float opacity = 1 - value;
      if (MATERIAL.getParam("Diffuse") != null) {
        MatParam mp = MATERIAL.getParam("Diffuse");
        ColorRGBA diffuseColor = (ColorRGBA) mp.getValue();
        diffuseColor.a = opacity;
        setAmbient((ColorRGBA) mp.getValue());
        //setSpecular((ColorRGBA) mp.getValue());
      }

      //MATERIAL.setFloat("AlphaDiscardThreshold", 0.3f);
      MATERIAL.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
      //MATERIAL.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off); // show back side too
      MATERIAL.getAdditionalRenderState().setAlphaTest(true); // alpha on each face
      MATERIAL.getAdditionalRenderState().setAlphaFallOff(0.01f);
    }
  }


  public Material get() {
    return MATERIAL.clone();
  }

  private Texture getNormalMap(Texture texture) {
    Texture nmap = NORMAL_MAPS.get(texture);
    if (nmap == null) {
      NORMAL_MAPS.put(texture, nmap = generateNormalMap(texture));
    }
    return nmap;
  }

  private Texture generateNormalMap(Texture texture) {
    BufferedImage image = ImageToAwt.convert(texture.getImage(), false, false, 0);
    BufferedImage normal = NormalMapFilter.create().filter(image, 0.01f);
    Image jme = new AWTLoader().load(normal, false);
    Texture2D jmeTexture = new Texture2D(jme);
    jmeTexture.setWrap(Texture.WrapAxis.S, texture.getWrap(Texture.WrapAxis.S));
    jmeTexture.setWrap(Texture.WrapAxis.T, texture.getWrap(Texture.WrapAxis.T));
    jmeTexture.setMagFilter(texture.getMagFilter());
    jmeTexture.setMinFilter(texture.getMinFilter());
    return jmeTexture;
  }
}
