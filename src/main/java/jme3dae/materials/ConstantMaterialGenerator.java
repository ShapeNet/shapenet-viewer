package jme3dae.materials;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;

public class ConstantMaterialGenerator extends FXBumpMaterialGenerator {

  public static ConstantMaterialGenerator create(AssetManager am) {
    return new ConstantMaterialGenerator(am);
  }

  private ConstantMaterialGenerator(AssetManager am) {
    //super(am, new Material(am, "jme3dae/materials/ColladaConstant.j3md"));
    super(am, "const");
  }

}
