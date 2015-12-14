package jme3dae;

import jme3dae.utilities.Tuple2;
import com.jme3.asset.AssetManager;
import com.jme3.scene.Node;
import jme3dae.FXEnhancerInfo.IgnoreLights;
import jme3dae.FXEnhancerInfo.IgnoreMeasuringUnit;
import jme3dae.FXEnhancerInfo.NormalMapGenerator;
import jme3dae.FXEnhancerInfo.TwoSidedMaterial;
import jme3dae.FXEnhancerInfo.UseJME3Materials;
import jme3dae.collada14.ColladaDocumentV14;
import jme3dae.transformers.ValueTransformer;

/**
 * A factory for collada document parsers.
 *
 * @author pgi
 */
public class ColladaDocumentFactory {
  private static ProgressListener progressListener = null;

  public static void setProgressListener(ProgressListener listener) {
    progressListener = listener;
  }

  public static ProgressListener getProgressListener() {
    return progressListener;
  }

  public static class FXSettingsGenerator {
    private FXEnhancerInfo fx = FXEnhancerInfo.create(FXEnhancerInfo.IgnoreMeasuringUnit.OFF);

    public FXSettingsGenerator setIgnoreMeasuringUnit(IgnoreMeasuringUnit v) {
      fx = new FXEnhancerInfo(fx.getAutoBump(), fx.getTwoSided(), v == IgnoreMeasuringUnit.ON, fx.getIgnoreLights(),
          fx.getUseJME3Materials());
      return this;
    }

    public FXSettingsGenerator setNormalMapGeneration(FXEnhancerInfo.NormalMapGenerator v) {
      fx = new FXEnhancerInfo(v == NormalMapGenerator.ON, fx.getTwoSided(), fx.getIgnoreMeasuringUnit(), fx.getIgnoreLights(),
          fx.getUseJME3Materials());
      return this;
    }

    public FXSettingsGenerator setIgnoreLights(IgnoreLights v) {
      fx = new FXEnhancerInfo(fx.getAutoBump(), fx.getTwoSided(), fx.getIgnoreMeasuringUnit(), v == IgnoreLights.ON,
          fx.getUseJME3Materials());
      return this;
    }

    public FXSettingsGenerator setTwoSidedMaterial(TwoSidedMaterial m) {
      fx = new FXEnhancerInfo(fx.getAutoBump(), m == TwoSidedMaterial.ON, fx.getIgnoreMeasuringUnit(), fx.getIgnoreLights(),
          fx.getUseJME3Materials());
      return this;
    }

    public FXSettingsGenerator setUseJME3Materials(UseJME3Materials m) {
      fx = new FXEnhancerInfo(fx.getAutoBump(), fx.getTwoSided(), fx.getIgnoreMeasuringUnit(), fx.getIgnoreLights(),
          m == UseJME3Materials.ON);
      return this;
    }

    public FXEnhancerInfo get() {
      return fx;
    }

  }

  private static FXEnhancerInfo fxInfo = FXEnhancerInfo.create(
      FXEnhancerInfo.NormalMapGenerator.OFF,
      FXEnhancerInfo.TwoSidedMaterial.OFF);

  public static void setFXEnhance(FXEnhancerInfo info) {
    fxInfo = info;
  }

  private ColladaDocumentFactory() {
  }

  /**
   * Parses a DAENode wrapping the root of a collada document (COLLADA element). Returns a parser
   * for that document. The root node is used to choose the parser version (if collada version is 1.4 the
   * factory will return a 1.4 parser, if the version is 1.5 the factory will return a ... 1.4 parser
   * because it's the only one available).
   *
   * @param root the DAENode wrapping the COLLADA element of a collada document.
   * @return a parser for the collada document wrapped by the given DAENode.
   */
  public static ValueTransformer<Tuple2<DAENode, AssetManager>, Node> newColladaDocumentParser(DAENode root) {
    return ColladaDocumentV14.create(fxInfo);
  }
}
