package jme3dae.collada14.transformers;

import com.jme3.animation.Bone;
import com.jme3.asset.AssetKey;
import com.jme3.asset.cache.AssetCache;
import com.jme3.asset.cache.WeakRefAssetCache;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.MatParam;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.LinkedList;
import java.util.List;

import jme3dae.DAENode;
import jme3dae.FXEnhancerInfo;
import jme3dae.collada14.ColladaSpec141.DefaultValues;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.collada14.ColladaSpec141.NodeType;
import jme3dae.collada14.ColladaSpec141.Semantic;
import jme3dae.utilities.Bindings;

import static jme3dae.utilities.Conditions.*;

import jme3dae.utilities.Todo;
import jme3dae.utilities.TransformerPack;
import jme3dae.utilities.Tuple2;

/**
 * Transforms a collada <scene> element. The results of the transformation are
 * attached to a JME3 node.
 *
 * @author pgi
 */
public class SceneTransformer implements TransformerPack<Tuple2<DAENode, Node>, Void> {

  private AssetCache assetCache = new WeakRefAssetCache();

  /**
   * Instance creator.
   *
   * @return a new SceneTransformer instance.
   */
  public static SceneTransformer create() {
    return new SceneTransformer();
  }

  private SceneTransformer() {
  }

  /**
   * Apply this transform
   *
   * @param value a Tuple2 where the DAENode is a collada "scene" node, the Node
   *              is a jme3 node where to add the parsed scene.
   * @return an undefined TransformedValue
   */
  public TransformedValue<Void> transform(Tuple2<DAENode, Node> value) {
    DAENode scene = value.getA();
    Node root = value.getB();
    checkTrue(scene.hasName(Names.SCENE), "Expected 'scene' got " + scene);
    for (DAENode instancePhysicsScene : scene.getChildren(Names.INSTANCE_PHYSICS_SCENE)) {
      Todo.parse(instancePhysicsScene);
    }
    DAENode instanceVisualScene = scene.getChild(Names.INSTANCE_VISUAL_SCENE);
    if (instanceVisualScene.isDefined()) {
      parseInstanceVisualScene(instanceVisualScene, root);
    }
    return TransformedValue.create(null);
  }

  private void parseInstanceVisualScene(DAENode instanceVisualScene, Node jmeRoot) {
    checkTrue(instanceVisualScene.hasName(Names.INSTANCE_VISUAL_SCENE));
    TransformedValue<String> url = instanceVisualScene.getAttribute(Names.URL, TEXT);
    checkTrue(url.isDefined(), "Collada spec 1.4.1 requires url attribute for instance_visual_scene element");
    DAENode visualScene = instanceVisualScene.getLinkedNode(url.get());
    checkTrue(visualScene.hasName(Names.VISUAL_SCENE), "Expected 'visual_scene' got " + visualScene);
    DAENode extra = visualScene.getChild(Names.EXTRA);
    List<DAENode> evaluateSceneList = visualScene.getChildren(Names.EVALUATE_SCENE);
    DAENode asset = visualScene.getChild(Names.ASSET);
    if (extra.isDefined()) {
      Todo.parse(extra);
    }
    if (!evaluateSceneList.isEmpty()) {
      Todo.parse(evaluateSceneList.get(0));
    }
    if (asset.isDefined()) {
      Todo.parse(asset);
    }
    parseVisualScene(visualScene, jmeRoot);
  }

  /*
   * Instance controller required because the joints refers to sids in the context of the
   * instance controller node.
   */
  private void parseSkin(DAENode instanceController, DAENode skin, DAENode bindMaterial, Node sceneLink) {
    DAENode geometry = skin.getLinkedSource();
    //1 get the polygons
    DAENode mesh = geometry.getChild(Names.MESH);
    List<Geometry> jmeGeometries = new LinkedList<Geometry>();
    for (DAENode dAENode : mesh.getChildren(Names.TRIANGLES)) {

    }

    Todo.implementThis();
  }

  private DAENode findSemanticInput(DAENode parent, Semantic s) {
    for (DAENode input : parent.getChildren(Names.INPUT)) {
      TransformedValue<Semantic> sem = input.getAttribute(Names.SEMANTIC, SemanticTransformer.create());
      if (sem.isDefined() && sem.get() == s) {
        return input;
      }
    }
    return DAENode.NONE;
  }

  private void parseVisualScene(DAENode visualScene, Node jmeRoot) {
    checkTrue(visualScene.hasName(Names.VISUAL_SCENE));
    DAENode asset = visualScene.getChild(Names.ASSET);
    List<DAENode> nodeList = visualScene.getChildren(Names.NODE);
    List<DAENode> evaluateSceneList = visualScene.getChildren(Names.EVALUATE_SCENE);
    List<DAENode> extraList = visualScene.getChildren(Names.EXTRA);

    if (asset.isDefined()) {
      Todo.parse(asset);
    }
    if (!evaluateSceneList.isEmpty()) {
      Todo.parse(evaluateSceneList.get(0));
    }
    if (!extraList.isEmpty()) {
      Todo.parse(extraList.get(0));
    }

    for (DAENode node : nodeList) {
      parseNode(node, jmeRoot);
    }
  }

  private void parseNode(DAENode node, Node jmeRoot) {
    final FXEnhancerInfo FX_INFO = node.getRootNode().getParsedData(FXEnhancerInfo.class);

    checkTrue(node.hasName(Names.NODE));
    TransformedValue<String> optionalType = node.getAttribute(Names.TYPE, TEXT);
    TransformedValue<String[]> optionalLayers = node.getAttribute(Names.LAYER, NAME_LIST);
    TransformedValue<String> optionalName = node.getAttribute(Names.NAME, TEXT);
    TransformedValue<String> optionalId = node.getAttribute(Names.ID, TEXT);
    DAENode optionalAsset = node.getChild(Names.ASSET);
    List<DAENode> optionalExtras = node.getChildren(Names.EXTRA);

    //node transform functions
    List<DAENode> transformElements = node.getChildren(
        Names.LOOKAT, Names.MATRIX, Names.ROTATE,
        Names.SCALE, Names.SKEW, Names.TRANSLATE);

    //scene elements
    List<DAENode> optionalInstanceCameras = node.getChildren(Names.INSTANCE_CAMERA);
    List<DAENode> optionalInstanceControllers = node.getChildren(Names.INSTANCE_CONTROLLER);
    List<DAENode> optionalInstanceGeometries = node.getChildren(Names.INSTANCE_GEOMETRY);
    List<DAENode> optionalInstanceLights = node.getChildren(Names.INSTANCE_LIGHT);
    List<DAENode> optionalInstanceNodes = node.getChildren(Names.INSTANCE_NODE);
    List<DAENode> optionalNodes = node.getChildren(Names.NODE);

    //create jme node
    Node jmeNode = new Node(optionalName.isDefined() ? optionalName.get() : DefaultValues.NODE_NAME);
    if (optionalId.isDefined()) {
      jmeNode.setUserData("colladaId", optionalId.get());
      jmeNode.setUserData("id", optionalId.get());
    }
    node.setParsedData(jmeNode);

    //apply transform
    TransformedValue<Transform> optionalTransform = TransformationElementTransformer.create().transform(transformElements);
    if (optionalTransform.isDefined()) {
      jmeNode.setLocalTransform(optionalTransform.get());
    }
    jmeRoot.attachChild(jmeNode);

    for (DAENode instanceGeometry : optionalInstanceGeometries) {
      parseInstanceGeometry(instanceGeometry, jmeNode);
    }

    if (!FX_INFO.getIgnoreLights()) {
      for (DAENode instanceLight : optionalInstanceLights) {
        parseInstanceLight(instanceLight, jmeNode);
      }
    }

    for (DAENode childNode : optionalNodes) {
      parseNode(childNode, jmeNode);
    }

    for (DAENode instanceCamera : optionalInstanceCameras) {
      parseInstanceCamera(instanceCamera, jmeNode);
    }

    for (DAENode instanceController : optionalInstanceControllers) {
      parseInstanceController(instanceController, jmeNode);
    }

    for (DAENode instanceNode : optionalInstanceNodes) {
      parseInstanceNode(instanceNode, jmeNode);
    }

    //todo...
    if (optionalType.isDefined()) {
      if (optionalType.get().equals(NodeType.JOINT.name())) {
        Bone bone = new Bone(node.getAttribute(Names.SID, TEXT).get());
        node.setParsedData(bone);
        Bone parent = node.getParent().getParsedData(Bone.class);
        if (parent != null) {
          parent.addChild(bone);
        }
      } else {
        Todo.task("parse node type " + optionalType.get());
      }
    }
    if (optionalLayers.isDefined()) {
      Todo.task("implement parsing of node layer list");
    }
    if (optionalAsset.isDefined()) {
      Todo.task("parse " + optionalAsset);
    }
    if (!optionalExtras.isEmpty()) {
      Todo.task("implement parsing of optional extras");
    }


  }

  private void parseInstanceGeometry(DAENode instanceGeometry, Node jmeNode) {
    checkTrue(instanceGeometry.hasName(Names.INSTANCE_GEOMETRY));
    DAENode optionalBindMaterial = instanceGeometry.getChild(Names.BIND_MATERIAL);
    Bindings materialBindings = Bindings.create();
    parseBindMaterial(optionalBindMaterial, materialBindings);

    List<DAENode> optionalExtras = instanceGeometry.getChildren(Names.EXTRA);
    TransformedValue<String> url = instanceGeometry.getAttribute(Names.URL, TEXT);
    checkTrue(url.isDefined(), "Collada spec 1.4.1 requires url attribute for instance_geometry element");
    TransformedValue<String> optionalName = instanceGeometry.getAttribute(Names.NAME, TEXT);
    DAENode geometry = instanceGeometry.getLinkedNode(url.get());
    checkTrue(geometry.isDefined(), "Cannot find linked node " + url.get());
    checkTrue(geometry.hasName(Names.GEOMETRY), "Collada spec 1.4.1 requires geometry here");

    //parse geometry
    DAENode geometryOptionalAsset = geometry.getChild(Names.ASSET);
    List<DAENode> geometricElement = geometry.getChildren(Names.CONVEX_MESH, Names.MESH, Names.SPLINE);
    checkValue(geometricElement.size(), 1); //1 geometric element required
    DAENode geomElement = geometricElement.get(0);
    TransformedValue<Node> jmeMeshNode = TransformedValue.<Node>create(null);
    if (geomElement.hasName(Names.CONVEX_MESH)) {
      Todo.task("implement parsing of convex_mesh geometry");
    } else if (geomElement.hasName(Names.MESH)) {
      jmeMeshNode = MeshTransformer.create().transform(Tuple2.create(geomElement, materialBindings));
    } else if (geomElement.hasName(Names.SPLINE)) {
      Todo.task("implement parsing of spline geometry");
    }

    if (jmeMeshNode.isDefined()) {
      TransformedValue<String> optionalId = geometry.getAttribute(Names.ID, TEXT);
      if (optionalId.isDefined()) {
        jmeMeshNode.get().setUserData("colladaId", optionalId.get());
        jmeMeshNode.get().setUserData("id", optionalId.get());
      }
      jmeMeshNode.get().setName(optionalName.isDefined() ? optionalName.get() : DefaultValues.NODE_NAME);
      for (Spatial spatial : jmeMeshNode.get().getChildren()) {
        String name = url.get();
        if (spatial instanceof Geometry) {
          Geometry g = (Geometry) spatial;
          if (g.getMaterial() != null && g.getMaterial().getName() != null) {
            name = name + ":" + g.getMaterial().getName();
          }
        }
        spatial.setName(name);

// AXC: Note this attempt to reuse cached geometry is just wrong
//   (causes models like 3dw.1d4eaafea3ae8303ce94fde2d6224f79 to be broken)
//        if ((spatial instanceof Geometry) && (spatial.getName().length() != 0)) {
//          Geometry g = (Geometry) spatial;
//          AssetKey assetKey = new AssetKey(spatial.getName());
//          Geometry cachedGeom = (Geometry) assetCache.getFromCache(assetKey);
//
//          if (cachedGeom != null) {
//            g.setMesh(cachedGeom.getMesh());
//          } else {
//            assetCache.addToCache(assetKey, g);
//          }
//        }

      }
      jmeNode.attachChild(jmeMeshNode.get());
    }
  }

  private void parseInstanceLight(DAENode instanceLight, Node jmeNode) {
    checkTrue(instanceLight.hasName(Names.INSTANCE_LIGHT), "expected instance light, got " + instanceLight);
    TransformedValue<String> url = instanceLight.getAttribute(Names.URL, TEXT);
    checkTrue(url.isDefined(), "Collada 1.4.1 requires instance_light url to be defined");
    DAENode light = instanceLight.getLinkedNode(url.get());
    checkTrue(light.isDefined(), "Cannot resolve link " + url.get());
    checkTrue(light.hasName(Names.LIGHT), "Collada 1.4.1 requires instance_light url to point to a light element");
    DAENode techniqueCommon = light.getChild(Names.TECHNIQUE_COMMON);
    checkTrue(techniqueCommon.isDefined(), "Collada 1.4.1 requires a technique_common child for light element");
    List<DAENode> lightElement = techniqueCommon.getChildren(Names.AMBIENT, Names.DIRECTIONAL, Names.POINT, Names.SPOT);
    checkTrue(lightElement.size() == 1, "Collada 1.4.1 requires one ambient or directional or point or spot element per light");
    DAENode lightNode = lightElement.get(0);
    //position, direction and so on are based on the parent node transform
    //all lights have colors
    //position, direction and so on are based on the parent node transform
    ColorRGBA color = lightNode.getChild(Names.COLOR).getContent(ColorRGBATransformer.create()).get();
    Transform worldTransform = jmeNode.getWorldTransform();
    jmeNode.updateGeometricState();
    if (lightNode.hasName(Names.DIRECTIONAL)) {
      DirectionalLight dl = new DirectionalLight();
      Vector3f dir = new Vector3f(0, 0, -1); //as per spec 1.4.1 2nd ed, 5-30
      dir = worldTransform.transformVector(dir, dir);
      dl.setDirection(dir.normalizeLocal());
      dl.setColor(color);
      Node p = jmeNode;
      while (p.getParent() != null) {
        p = p.getParent();
      } //search for root node...
      p.addLight(dl);
    } else if (lightNode.hasName(Names.POINT)) {
      PointLight pl = new PointLight();
      Vector3f pos = worldTransform.transformVector(new Vector3f(), new Vector3f());
      pl.setColor(color);
      pl.setPosition(pos);
      pl.setRadius(100);
      Node p = jmeNode;
      while (p.getParent() != null) {
        p = p.getParent();
      }
      p.addLight(pl);
      Todo.task("set arbitrary radius 100 for point light at " + jmeNode.getWorldTranslation());
    } else {
      Todo.task("Parse light of type " + lightNode);
    }
  }

  private void parseInstanceCamera(DAENode instanceCamera, Node jmeNode) {
    Todo.implementThis();
  }

  private void parseInstanceController(DAENode instanceController, Node jmeNode) {
    TransformedValue<Node> r = InstanceControllerTransformer.create().transform(instanceController);
    if (r.isDefined()) {
      jmeNode.attachChild(r.get());
    }
  }

  private void parseInstanceNode(DAENode instanceNode, Node jmeNode) {
    checkTrue(instanceNode.hasName(Names.INSTANCE_NODE));
    List<DAENode> optionalExtras = instanceNode.getChildren(Names.EXTRA);
    TransformedValue<String> url = instanceNode.getAttribute(Names.URL, TEXT);
    checkTrue(url.isDefined());
    DAENode linkedNode = instanceNode.getLinkedNode(url.get());
    if (!optionalExtras.isEmpty()) {
      Todo.task("parse extra elements of linked node element");
    }
    if (linkedNode.isDefined()) {
      checkTrue(linkedNode.hasName(Names.NODE), "Collada 1.4.1 requires node element here");
      parseNode(linkedNode, jmeNode);
    } else {
      Todo.task("linked node " + url.get() + "not found, maybe external resource?");
    }
  }

  private void parseBindMaterial(DAENode bindMaterial, Bindings materialBindings) {
    materialBindings.put(Names.BIND_MATERIAL, bindMaterial);
  }

  private void dumpGeometry(Geometry geometry) {
    System.out.println("Geometry " + geometry.getName());
    Material mat = geometry.getMaterial();
    if (mat != null) {
      for (MatParam matParam : mat.getParams()) {
        System.out.println(matParam.getName() + " = " + matParam.getValue());
      }
    }
  }
}
