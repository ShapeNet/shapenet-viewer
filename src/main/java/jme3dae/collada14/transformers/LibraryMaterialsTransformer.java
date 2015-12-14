package jme3dae.collada14.transformers;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.texture.Texture;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3dae.DAENode;
import jme3dae.FXEnhancerInfo;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.materials.BlinnMaterialGenerator;
import jme3dae.materials.ConstantMaterialGenerator;
import jme3dae.materials.FXBumpMaterialGenerator;
import jme3dae.utilities.Conditions;
import jme3dae.materials.LambertMaterialGenerator;
import jme3dae.materials.PhongMaterialGenerator;
import jme3dae.utilities.Todo;
import jme3dae.utilities.TransformerPack;
import jme3dae.utilities.Tuple2;

/**
 * Parses library_material elements. This transformer stores the transformed materials in the DAENode.
 *
 * @author pgi
 */
public class LibraryMaterialsTransformer implements TransformerPack<Tuple2<DAENode, AssetManager>, Void> {

  /**
   * Instance creator
   *
   * @return a new instance of LibraryMaterialsTransformer
   */
  public static LibraryMaterialsTransformer create() {
    return new LibraryMaterialsTransformer();
  }

  private final ColorRGBATransformer COLOR = ColorRGBATransformer.create();
  private final TextureElementTransformer TEXTURE = TextureElementTransformer.create();

  private LibraryMaterialsTransformer() {
  }

  /**
   * Transforms a library_materials DAENode / AssetManager pair. The results
   * are stored in the material nodes as JME3 Material instances.
   *
   * @param value the library_materials collada node and the JME3 asset manager.
   * @return nothing.
   */
  public TransformedValue<Void> transform(Tuple2<DAENode, AssetManager> value) {
    DAENode libraryMaterials = value.getA();
    AssetManager assetManager = value.getB();

    if (assetManager != null) {
      Conditions.checkTrue(libraryMaterials.hasName(Names.LIBRARY_MATERIALS), "expected library_materials, got " + libraryMaterials);

      DAENode optionalAsset = libraryMaterials.getChild(Names.ASSET);
      List<DAENode> optionalExtras = libraryMaterials.getChildren(Names.EXTRA);
      List<DAENode> materials = libraryMaterials.getChildren(Names.MATERIAL);

      if (optionalAsset.isDefined()) {
        Todo.task("implement parsing of asset element");
      }
      if (!optionalExtras.isEmpty()) {
        Todo.task("implements parsing of extra elements");
      }

      Conditions.checkFalse(materials.isEmpty(), "Collada 1.4.1 requires at least one material element in library_materials");

      for (DAENode material : materials) {
        parseMaterial(material, assetManager);
      }
    } else {
      System.err.println("AssetManager is null, testing?");
    }
    return TransformedValue.<Void>create(null);
  }

  private void parseMaterial(DAENode material, AssetManager assetManager) {
    DAENode optionalAsset = material.getChild(Names.ASSET);
    if (optionalAsset.isDefined()) {
      Todo.task("parse material asset");
    }
    DAENode instanceEffect = material.getChild(Names.INSTANCE_EFFECT);
    Conditions.checkTrue(instanceEffect.isDefined(), "Collada 1.4.1 requires an instance_effect child for material");
    List<DAENode> optionalExtras = material.getChildren(Names.EXTRA);
    if (!optionalExtras.isEmpty()) {
      Todo.task("parse material extra");
    }
    parseInstanceEffect(material, instanceEffect, assetManager);
  }

  private void parseInstanceEffect(DAENode material, DAENode instanceEffect, AssetManager assetManager) {
    TransformedValue<String> url = instanceEffect.getAttribute(Names.URL, TEXT);
    Conditions.checkTrue(url.isDefined(), "Collada 1.4.1 requires an url attribute for instance_effect element");
    instanceEffect.getChildren(Names.TECHNIQUE_HINT);
    DAENode effect = instanceEffect.getLinkedNode(url.get());
    if (effect.isDefined()) {
      parseEffect(material, instanceEffect, effect, assetManager);
    } else {
      Todo.task("effect not found for link " + url.get() + ": external reference or a bug?");
    }
  }

  private void parseEffect(DAENode material, DAENode instanceEffect, DAENode effect, AssetManager assetManager) {
    DAENode asset = effect.getChild(Names.ASSET);
    List<DAENode> annotateList = effect.getChildren(Names.ANNOTATE);
    List<DAENode> imageList = effect.getChildren(Names.IMAGE);
    List<DAENode> newparamList = effect.getChildren(Names.NEWPARAM);
    List<DAENode> profileCGList = effect.getChildren(Names.PROFILE_CG);
    List<DAENode> profileGLESList = effect.getChildren(Names.PROFILE_GLES);
    List<DAENode> profileGLSLList = effect.getChildren(Names.PROFILE_GLSL);
    List<DAENode> profileCOMMONList = effect.getChildren(Names.PROFILE_COMMON);
    List<DAENode> extraList = effect.getChildren(Names.EXTRA);

    if (asset.isDefined()) {
      Todo.task("implement asset parsing for effect");
    }
    if (!annotateList.isEmpty()) {
      Todo.task("implement annotate list parsing");
    }
    if (!imageList.isEmpty()) {
      Todo.task("implement image list parsing");
    }
    if (!newparamList.isEmpty()) {
      Todo.task("implement image list parsing");
    }
    if (!profileCGList.isEmpty()) {
      Todo.task("implement profileCG list parsing");
    }
    if (!profileGLESList.isEmpty()) {
      Todo.task("implement profileGLES list parsing");
    }
    if (!profileGLSLList.isEmpty()) {
      Todo.task("implements profileGLSL list parsing");
    }
    if (!extraList.isEmpty()) {
      Todo.task("implement extra list parsing");
    }
    if (profileCOMMONList.size() > 1) {
      Todo.task("implement multiple profile_COMMON parsing");
    }

    if (!profileCOMMONList.isEmpty()) {
      DAENode profileCommon = profileCOMMONList.get(0);
      DAENode technique = profileCommon.getChild(Names.TECHNIQUE);
      Conditions.checkTrue(technique.isDefined(), "Collada 1.4.1 requires one technique child for profile_COMMON");
      DAENode phong = technique.getChild(Names.PHONG);
      DAENode lambert = technique.getChild(Names.LAMBERT);
      DAENode blinn = technique.getChild(Names.BLINN);
      DAENode constant = technique.getChild(Names.CONSTANT);
      if (phong.isDefined()) {
        parsePhong(material, instanceEffect, effect, phong, assetManager);
      } else if (lambert.isDefined()) {
        parseLambert(material, instanceEffect, effect, lambert, assetManager);
      } else if (blinn.isDefined()) {
        parseBlinn(material, instanceEffect, effect, blinn, assetManager);
      } else if (constant.isDefined()) {
        parseConstant(material, instanceEffect, effect, constant, assetManager);
      } else {
        Todo.task("parse material (no phong, blinn or lambert found)");
      }
      Material mat = material.getParsedData(Material.class);
      if (mat != null) {
        TransformedValue<String> name = material.getAttribute(Names.NAME, TEXT);
        if (name.isDefined()) { mat.setName(name.get()); }
        FXEnhancerInfo fx = material.getRootNode().getParsedData(FXEnhancerInfo.class);
        boolean twoSided = fx.getTwoSided();
        if (twoSided) {
          mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        }
      }

      List<DAENode> profileExtraList = profileCommon.getChildren(Names.EXTRA);
      if (!profileExtraList.isEmpty()) {
        for (DAENode extra : profileExtraList) {
          parseExtra(extra, mat);
        }
      }
    }

  }

  private float getTransparencyLevel(TransformedValue<Float> transparencyFloat, TransformedValue<ColorRGBA> transparentColor) {
    float transparencyLevel = 0.0f;
    if (transparencyFloat.isDefined() && transparentColor.isDefined()) {
      // Convert transparent color RGB to average value
      ColorRGBA color = transparentColor.get();
      transparencyLevel = (color.r + color.g + color.b)/3 * transparencyFloat.get();
    }
    return transparencyLevel;
  }

  private void parsePhong(DAENode material, DAENode instanceEffect, DAENode effect, DAENode phong, AssetManager assetManager) {
    DAENode emission = phong.getChild(Names.EMISSION);
    DAENode ambient = phong.getChild(Names.AMBIENT);
    DAENode diffuse = phong.getChild(Names.DIFFUSE);
    DAENode specular = phong.getChild(Names.SPECULAR);
    DAENode shininess = phong.getChild(Names.SHININESS);
    DAENode reflective = phong.getChild(Names.REFLECTIVE);
    DAENode reflectivity = phong.getChild(Names.REFLECTIVITY);
    DAENode transparent = phong.getChild(Names.TRANSPARENT);
    DAENode transparency = phong.getChild(Names.TRANSPARENCY);
    DAENode indexOfRefraction = phong.getChild(Names.INDEX_OF_REFRACTION);

    TransformedValue<ColorRGBA> emissionColor = emission.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> ambientColor = ambient.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> diffuseColor = diffuse.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> specularColor = specular.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> shininessFloat = shininess.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<ColorRGBA> reflectiveColor = reflective.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> reflectivityFloat = reflectivity.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<ColorRGBA> transparentColor = transparent.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> transparencyFloat = transparency.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<Float> indexOfRefractionFloat = indexOfRefraction.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<Texture> emissionTexture = emission.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> ambientTexture = ambient.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> diffuseTexture = diffuse.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> specularTexture = specular.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> reflectiveTexture = reflective.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> transparentTexture = transparent.getChildValue(Names.TEXTURE, TEXTURE);
    float transparencyLevel = getTransparencyLevel(transparencyFloat, transparentColor);

    FXEnhancerInfo fx = material.getRootNode().getParsedData(FXEnhancerInfo.class);
    if (fx.getUseJME3Materials()) {
      String diffuseTextureName = "";
      if (diffuseTexture.isDefined()) {
        diffuseTextureName = diffuseTexture.get().getKey().getName();
      }
      FXBumpMaterialGenerator bump = FXBumpMaterialGenerator.create(assetManager, diffuseTextureName);
      bump.setAmbient(ambientColor.get());
      bump.setDiffuse(diffuseColor.get());
      bump.setSpecular(specularColor.get());
      bump.setShininess(shininessFloat.get());
      bump.setAmbient(ambientTexture.get());
      bump.setDiffuse(diffuseTexture.get());
      bump.setSpecular(specularTexture.get());
      bump.setTransparency(transparencyLevel);
      material.setParsedData(bump.get());
    } else {
      PhongMaterialGenerator m = PhongMaterialGenerator.create(assetManager);
      m.setAmbient(ambientColor.get());
      m.setDiffuse(diffuseColor.get());
      m.setSpecular(specularColor.get());
      m.setShininess(shininessFloat.get());
      m.setAmbient(ambientTexture.get());
      m.setDiffuse(diffuseTexture.get());
      m.setSpecular(specularTexture.get());
      m.setTransparency(transparencyLevel);
      material.setParsedData(m.get());
    }
  }

  private void parseLambert(DAENode material, DAENode instanceEffect, DAENode effect, DAENode lambert, AssetManager assetManager) {
    DAENode emissionNode = lambert.getChild(Names.EMISSION);
    DAENode ambientNode = lambert.getChild(Names.AMBIENT);
    DAENode diffuseNode = lambert.getChild(Names.DIFFUSE);
    DAENode reflective = lambert.getChild(Names.REFLECTIVE);
    DAENode reflectivity = lambert.getChild(Names.REFLECTIVITY);
    DAENode transparent = lambert.getChild(Names.TRANSPARENT);
    DAENode transparency = lambert.getChild(Names.TRANSPARENCY);
    DAENode indexOfRefraction = lambert.getChild(Names.INDEX_OF_REFRACTION);

    TransformedValue<ColorRGBA> emissionColor = emissionNode.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> ambientColor = ambientNode.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> diffuseColor = diffuseNode.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> reflectiveColor = reflective.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> reflectivityFloat = reflectivity.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<ColorRGBA> transparentColor = transparent.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> transparencyFloat = transparency.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<Float> indexOfRefractionFloat = indexOfRefraction.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<Texture> emissionTexture = emissionNode.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> ambientTexture = ambientNode.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> diffuseTexture = diffuseNode.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> reflectiveTexture = reflective.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> transparentTexture = transparent.getChildValue(Names.TEXTURE, TEXTURE);
    float transparencyLevel = getTransparencyLevel(transparencyFloat, transparentColor);

    FXEnhancerInfo fx = material.getRootNode().getParsedData(FXEnhancerInfo.class);
    if (fx.getUseJME3Materials()) {
      String diffuseTextureName = "";
      if (diffuseTexture.isDefined()) {
        diffuseTextureName = diffuseTexture.get().getKey().getName();
      }
      FXBumpMaterialGenerator bump = FXBumpMaterialGenerator.create(assetManager, diffuseTextureName);
      bump.setAmbient(ambientColor.get());
      bump.setDiffuse(diffuseColor.get());
      bump.setDiffuse(diffuseTexture.get());
      bump.setTransparency(transparencyLevel);
      material.setParsedData(bump.get());
    } else {
      LambertMaterialGenerator m = LambertMaterialGenerator.create(assetManager);
      m.setAmbient(ambientColor.get());
      m.setDiffuse(diffuseColor.get());
      m.setDiffuse(diffuseTexture.get());
      m.setTransparency(transparencyLevel);
      material.setParsedData(m.get());
    }
  }

  private void parseBlinn(DAENode material, DAENode instanceEffect, DAENode effect, DAENode blinn, AssetManager assetManager) {
    DAENode emission = blinn.getChild(Names.EMISSION);
    DAENode ambient = blinn.getChild(Names.AMBIENT);
    DAENode diffuse = blinn.getChild(Names.DIFFUSE);
    DAENode specular = blinn.getChild(Names.SPECULAR);
    DAENode shininess = blinn.getChild(Names.SHININESS);
    DAENode reflective = blinn.getChild(Names.REFLECTIVE);
    DAENode reflectivity = blinn.getChild(Names.REFLECTIVITY);
    DAENode transparent = blinn.getChild(Names.TRANSPARENT);
    DAENode transparency = blinn.getChild(Names.TRANSPARENCY);
    DAENode indexOfRefraction = blinn.getChild(Names.INDEX_OF_REFRACTION);

    TransformedValue<ColorRGBA> emissionColor = emission.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> ambientColor = ambient.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> diffuseColor = diffuse.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> specularColor = specular.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> shininessFloat = shininess.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<ColorRGBA> reflectiveColor = reflective.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> reflectivityFloat = reflectivity.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<ColorRGBA> transparentColor = transparent.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> transparencyFloat = transparency.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<Float> indexOfRefractionFloat = indexOfRefraction.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<Texture> emissionTexture = emission.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> ambientTexture = ambient.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> diffuseTexture = diffuse.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> specularTexture = specular.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> reflectiveTexture = reflective.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> transparentTexture = transparent.getChildValue(Names.TEXTURE, TEXTURE);
    float transparencyLevel = getTransparencyLevel(transparencyFloat, transparentColor);

    FXEnhancerInfo fx = material.getRootNode().getParsedData(FXEnhancerInfo.class);
    if (fx.getUseJME3Materials()) {
      String diffuseTextureName = "";
      if (diffuseTexture.isDefined()) {
        diffuseTextureName = diffuseTexture.get().getKey().getName();
      }
      FXBumpMaterialGenerator bump = FXBumpMaterialGenerator.create(assetManager, diffuseTextureName);
      bump.setAmbient(ambientColor.get());
      bump.setDiffuse(diffuseColor.get());
      bump.setSpecular(specularColor.get());
      bump.setShininess(shininessFloat.get());
      bump.setAmbient(ambientTexture.get());
      bump.setDiffuse(diffuseTexture.get());
      bump.setSpecular(specularTexture.get());
      bump.setTransparency(transparencyLevel);
      material.setParsedData(bump.get());
    } else {
      BlinnMaterialGenerator m = BlinnMaterialGenerator.create(assetManager);
      m.setAmbient(ambientColor.get());
      m.setDiffuse(diffuseColor.get());
      m.setSpecular(specularColor.get());
      m.setShininess(shininessFloat.get());
      m.setAmbient(ambientTexture.get());
      m.setDiffuse(diffuseTexture.get());
      m.setSpecular(specularTexture.get());
      m.setTransparency(transparencyLevel);
      material.setParsedData(m.get());
    }
  }

  private void parseConstant(DAENode material, DAENode instanceEffect, DAENode effect, DAENode constant, AssetManager assetManager) {
    DAENode emission = constant.getChild(Names.EMISSION);
    DAENode reflective = constant.getChild(Names.REFLECTIVE);
    DAENode reflectivity = constant.getChild(Names.REFLECTIVITY);
    DAENode transparent = constant.getChild(Names.TRANSPARENT);
    DAENode transparency = constant.getChild(Names.TRANSPARENCY);
    DAENode indexOfRefraction = constant.getChild(Names.INDEX_OF_REFRACTION);
    TransformedValue<ColorRGBA> emissionColor = emission.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<ColorRGBA> reflectiveColor = reflective.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> reflectivityFloat = reflectivity.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<ColorRGBA> transparentColor = transparent.getChild(Names.COLOR).getContent(COLOR);
    TransformedValue<Float> transparencyFloat = transparency.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<Float> indexOfRefractionFloat = indexOfRefraction.getChild(Names.FLOAT).getContent(FLOAT);
    TransformedValue<Texture> emissionTexture = emission.getChildValue(Names.TEXTURE, TEXTURE);
    TransformedValue<Texture> transparentTexture = transparent.getChildValue(Names.TEXTURE, TEXTURE);
    float transparencyLevel = getTransparencyLevel(transparencyFloat, transparentColor);

    ConstantMaterialGenerator m = ConstantMaterialGenerator.create(assetManager);
    m.setAmbient(emissionColor.get());
    m.setDiffuse(emissionColor.get());
    m.setSpecular(emissionColor.get());
    m.setTransparency(transparencyLevel);

    material.setParsedData(m.get());
  }

  private void parseExtra(DAENode extra, Material material) {
    if (material != null) {
      List<DAENode> techniques = extra.getChildren(Names.TECHNIQUE);
      for (DAENode tech : techniques) {
        TransformedValue<String> profileValue = tech.getAttribute(Names.PROFILE, TEXT);
        if (profileValue.contains("GOOGLEEARTH")) {
          DAENode ds = tech.getChild("double_sided");
          TransformedValue<Integer> content = ds.getContent(INTEGER);
          if (content.contains(1)) {
            Logger.getLogger(getClass().getName()).log(Level.INFO, "GOOGLEEARTH double sided attribute detected");
            material.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
          }
        }
      }
    }
  }
}
