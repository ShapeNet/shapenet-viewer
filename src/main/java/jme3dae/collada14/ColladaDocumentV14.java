package jme3dae.collada14;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3dae.DAENode;
import jme3dae.FXEnhancerInfo;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.collada14.transformers.LibraryImagesTransformer;
import jme3dae.collada14.transformers.LibraryMaterialsTransformer;
import jme3dae.collada14.transformers.SceneTransformer;
import jme3dae.utilities.Tuple2;
import jme3dae.transformers.ValueTransformer;
import jme3dae.utilities.Conditions;
import jme3dae.utilities.FloatTransformer;
import jme3dae.utilities.MeasuringUnit;
import jme3dae.utilities.PlainTextTransformer;
import jme3dae.utilities.UpAxis;

/**
 * Parser of the root node of a COLLADA element for a collada document v. 1.4.
 *
 * @author pgi
 */
public class ColladaDocumentV14 implements ValueTransformer<Tuple2<DAENode, AssetManager>, Node> {

  public static ColladaDocumentV14 create(FXEnhancerInfo fx) {
    return new ColladaDocumentV14(fx);
  }

  private final FXEnhancerInfo fxInfo;

  private ColladaDocumentV14(FXEnhancerInfo fx) {
    this.fxInfo = fx;
  }

  /**
   * Transforms a collada root node into a JME3 node.
   *
   * @param value a tuple wrapping the root of a collada document (COLLADA element) and
   *              a JME3 AssetManager. The asset manager is used to gain access to textures, shaders and
   *              other JME3 assets.
   * @return a JME3 node containing the transformation of the collada document into a JME3 scene. The
   * returned node is never null but can be empty is the parser fails or the collada document has no
   * geometry attached to a scene element.
   */
  public TransformedValue<Node> transform(Tuple2<DAENode, AssetManager> value) {
    Logger.getLogger(getClass().getName()).log(Level.INFO, "ColladaParser v. 0.1");
    DAENode colladaNode = value.getA();
    Conditions.checkTrue(value.getA().hasName(Names.COLLADA), "Expected COLLADA, got " + value.getA());
    parseColladaNode(colladaNode);

    LibraryImagesTransformer libraryImagesTransformer = LibraryImagesTransformer.create();
    for (DAENode libraryImages : value.getA().getChildren(Names.LIBRARY_IMAGES)) {
      libraryImagesTransformer.transform(Tuple2.create(libraryImages, value.getB()));
    }

    LibraryMaterialsTransformer libraryMaterialsTransformer = LibraryMaterialsTransformer.create();
    for (DAENode libraryMaterial : value.getA().getChildren(Names.LIBRARY_MATERIALS)) {
      libraryMaterialsTransformer.transform(Tuple2.create(libraryMaterial, value.getB()));
    }

    Node root = new Node("COLLADA SCENE");
    DAENode scene = value.getA().getChild(Names.SCENE);
    if (scene.isDefined()) {
      SceneTransformer.create().transform(Tuple2.create(scene, root));
    }
    if (value.getB() != null) {
      applyDefaultMaterial(value.getB(), root);
    } else {
      System.err.println("AssetManager is null, testing aren't we?");
    }
    return TransformedValue.create(root);
  }

  /*
   * jme3 doesn't really like geometries with no material. This method check if
   * geometry elements have a material. If not, a default material is set for them.
   */
  private void applyDefaultMaterial(AssetManager am, Node root) {
    Material defaultMaterial = am.loadMaterial("Common/Materials/RedColor.j3m");
    LinkedList<Spatial> stack = new LinkedList<Spatial>();
    stack.addFirst(root);
    while (!stack.isEmpty()) {
      Spatial s = stack.removeFirst();
      if (s instanceof Geometry) {
        Geometry g = (Geometry) s;
        if (g.getMaterial() == null) {
          g.setMaterial(defaultMaterial);
        }
      } else if (s instanceof Node) {
        Node n = (Node) s;
        if (n.getQuantity() > 0) {
          stack.addAll(n.getChildren());
        }
      }
    }
  }

  /**
   * Parses the root generating a MeasuringUnit element.
   *
   * @param colladaNode the node to parse.
   */
  private void parseColladaNode(DAENode colladaNode) {
    MeasuringUnit unit = MeasuringUnit.create(1);
    UpAxis upAxis = UpAxis.Y_UP;
    DAENode asset = colladaNode.getChild(Names.ASSET);
    if (asset.isDefined()) {
      DAENode u = asset.getChild(Names.UNIT);
      if (u.isDefined()) {
        unit = MeasuringUnit.create(u.getAttribute(Names.METER, FloatTransformer.create()).get());
      }
      DAENode ua = asset.getChild(Names.UP_AXIS);
      if (ua.isDefined()) {
        TransformedValue<String> content = ua.getContent(PlainTextTransformer.create());
        if (content.contains("Z_UP")) {
          upAxis = UpAxis.Z_UP;
        } else if (content.contains("Y_UP")) {
          upAxis = UpAxis.Y_UP;
        } else if (content.contains("X_UP")) {
          upAxis = UpAxis.X_UP;
        }
      }
    }
    if (fxInfo.getIgnoreMeasuringUnit()) {
      unit = MeasuringUnit.create(1);
    }
    colladaNode.setParsedData(upAxis);
    colladaNode.setParsedData(unit);
    colladaNode.setParsedData(fxInfo);
  }
}
