package jme3dae;

/**
 * Experimenti: this class wraps some values used by the plugin to autogenerate
 * some effects (like normal mapping).
 *
 * @author pgi
 */
public class FXEnhancerInfo {

  public boolean getIgnoreLights() {
    return ignoreLights;
  }

  public static enum IgnoreLights {
    ON, OFF
  }

  public static enum NormalMapGenerator {
    ON, OFF
  }

  public static enum TwoSidedMaterial {
    ON, OFF
  }

  public static enum IgnoreMeasuringUnit {
    ON, OFF
  }

  public static enum UseJME3Materials {
    ON, OFF
  }

  public static FXEnhancerInfo create(IgnoreMeasuringUnit igm) {
    return new FXEnhancerInfo(false, false, igm == IgnoreMeasuringUnit.ON, false, true);
  }

  public static FXEnhancerInfo create(NormalMapGenerator ngen, TwoSidedMaterial tsmat, IgnoreMeasuringUnit igm,
                                      IgnoreLights il) {
    return new FXEnhancerInfo(
        ngen == NormalMapGenerator.ON,
        tsmat == TwoSidedMaterial.ON,
        igm == IgnoreMeasuringUnit.ON,
        il == IgnoreLights.ON,
        true);
  }

  public static FXEnhancerInfo create(NormalMapGenerator ngen, TwoSidedMaterial tsmat, IgnoreMeasuringUnit igm) {
    return new FXEnhancerInfo(
        ngen == NormalMapGenerator.ON,
        tsmat == TwoSidedMaterial.ON,
        igm == IgnoreMeasuringUnit.ON,
        false,
        true);
  }

  public static FXEnhancerInfo create(NormalMapGenerator ngen, TwoSidedMaterial tsmat, IgnoreMeasuringUnit igm, IgnoreLights il, UseJME3Materials ujm) {
    return new FXEnhancerInfo(
        ngen == NormalMapGenerator.ON,
        tsmat == TwoSidedMaterial.ON,
        igm == IgnoreMeasuringUnit.ON,
        il == IgnoreLights.ON,
        ujm == UseJME3Materials.ON);
  }

  public static FXEnhancerInfo create(NormalMapGenerator ngen, TwoSidedMaterial tsmat) {
    return new FXEnhancerInfo(
        ngen == NormalMapGenerator.ON,
        tsmat == TwoSidedMaterial.ON,
        false,
        false,
        true);
  }

  public static FXEnhancerInfo create(NormalMapGenerator ngen) {
    return new FXEnhancerInfo(ngen == NormalMapGenerator.ON, false, false, false, true);
  }

  private final boolean autoBump;
  private final boolean twoSided;
  private final boolean ignoreMeasuringUnit;
  private final boolean ignoreLights;
  private final boolean useJME3Materials;

  FXEnhancerInfo(boolean autoBump, boolean twoSidedMaterials, boolean ignoreMeasuringUnit, boolean ignoreLights, boolean useJME3Materials) {
    this.autoBump = autoBump;
    twoSided = twoSidedMaterials;
    this.ignoreMeasuringUnit = ignoreMeasuringUnit;
    this.ignoreLights = ignoreLights;
    //this.useJME3Materials = autoBump || useJME3Materials;
    this.useJME3Materials = true;

  }

  public boolean getUseJME3Materials() {
    return useJME3Materials;
  }

  public boolean getIgnoreMeasuringUnit() {
    return ignoreMeasuringUnit;
  }

  public boolean getAutoBump() {
    return autoBump;
  }

  public boolean getTwoSided() {
    return twoSided;
  }
}
