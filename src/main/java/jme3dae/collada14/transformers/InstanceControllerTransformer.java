package jme3dae.collada14.transformers;

import com.jme3.animation.*;
import com.jme3.math.Matrix4f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;
import com.jme3.util.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jme3dae.DAENode;
import jme3dae.collada14.ChannelTarget;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.collada14.ColladaSpec141.Semantic;
import jme3dae.utilities.Bindings;
import jme3dae.utilities.Conditions;
import jme3dae.utilities.Matrix4fTransformer;
import jme3dae.utilities.Tuple2;
import jme3dae.utilities.VertexSkinningData;

/**
 * Transforms an instance_controller node in ... some kind of jme node.
 *
 * @author pgi
 */
public class InstanceControllerTransformer extends GeometryTransformer<DAENode, Node> {

  public static InstanceControllerTransformer create() {
    return new InstanceControllerTransformer();
  }

  private InstanceControllerTransformer() {
  }

  public TransformedValue<Node> transform(DAENode instanceController) {
    Conditions.checkTrue(instanceController.isDefined());
    Conditions.checkTrue(instanceController.hasName(Names.INSTANCE_CONTROLLER));

    Node result = null;
    DAENode controller = instanceController.getLinkedURL(); //this points to the instantiated controller
    DAENode skin = controller.getChild(Names.SKIN); //and this is the skin of that controller
    DAENode mesh = skin.getLinkedSource().getChild(Names.MESH);
    DAENode bindMaterial = instanceController.getChild(Names.BIND_MATERIAL);
    Bindings bindings = Bindings.create();
    bindings.put(Names.BIND_MATERIAL, bindMaterial);
    TransformedValue<Node> geom = MeshTransformer.create().transform(Tuple2.create(mesh, bindings));
    if (geom.isDefined()) {
      //get the geometric elements
      List<DAENode> geomElementList = mesh.getChildren(Names.TRIANGLES, Names.POLYGONS, Names.POLYLIST);
      DAENode vertexWeights = skin.getChild(Names.VERTEX_WEIGHTS);
      //parse the inputs shared in vertex_weights. They contain the vertex-bone indices and weights.
      DAENode joint = vertexWeights.getChild(Names.INPUT, Names.SEMANTIC, Semantic.JOINT.name());
      DAENode weight = vertexWeights.getChild(Names.INPUT, Names.SEMANTIC, Semantic.WEIGHT.name());

      // get the data
      String[] jointNames = joint.getLinkedSource().getChild(Names.IDREF_ARRAY).getContent(NAME_LIST).get();
      if (jointNames == null) {
        // Changed by larynx - try to read the skin joints from name_array
        jointNames = joint.getLinkedSource().getChild(Names.NAME_ARRAY).getContent(NAME_LIST).get();
      }

      DAENode[] boneNodes = new DAENode[jointNames.length];
      Bone[] jmeBones = new Bone[jointNames.length];
      Matrix4f[] matrices = getSkinJointsMatrices(skin);
      //create the bones and set the bind matrices
      for (int i = 0; i < jointNames.length; i++) {
        String sid = jointNames[i];
        boneNodes[i] = instanceController.getLinkedNode(sid);
        jmeBones[i] = new Bone(sid);
        Quaternion bindRot = matrices[i].toRotationQuat();
        Vector3f bindTrans = matrices[i].toTranslationVector();
        jmeBones[i].setBindTransforms(bindTrans, bindRot, Vector3f.UNIT_XYZ);
        boneNodes[i].setParsedData(jmeBones[i]);
      }
      //generate the parent-child relationships among bones, following the parent-child relationships
      //of the collada nodes
      mapBoneTree(boneNodes, jmeBones);

      float[] weights = weight.getLinkedSource().getChild(Names.FLOAT_ARRAY).getContent(FLOAT_LIST).get();
      int[] vcount = vertexWeights.getChild(Names.VCOUNT).getContent(INTEGER_LIST).get();
      IntBuffer v = IntBuffer.wrap(vertexWeights.getChild(Names.V).getContent(INTEGER_LIST).get());
      List<VertexSkinningData> vsdList = new LinkedList<VertexSkinningData>();
      for (int i = 0; i < vcount.length; i++) { //i is also the index of the vertex in the vertex buffer of the geometry. Maybe.
        int size = vcount[i];
        for (int j = 0; j < size; j++) {
          VertexSkinningData vsd = VertexSkinningData.create(i, v.get(), weights[v.get()]);
          vsdList.add(vsd);
        }
      }

      //generate the skeleton data
      Skeleton skeleton = new Skeleton(jmeBones);
      skeleton.setBindingPose();

      //apply skin data and bind pose
      for (DAENode geomNode : geomElementList) {
        Mesh jmeMesh = applySkinData(geomNode, vsdList);
        createBindPose(jmeMesh);
      }

      //create the node that will hold the skinned geometry
      result = new Node(instanceController.getParent().getAttribute(Names.NAME, TEXT).get());
      result.attachChild(geom.get());

      //create AnimControl
      DAENode libraryAnimations = instanceController.getRootNode().findDescendant(Names.LIBRARY_ANIMATIONS);
      AnimControl control = createAnimControl(geom.get(), libraryAnimations, geomElementList, skeleton);
      geom.get().setName(instanceController.getAttribute(Names.URL, TEXT).get());
      geom.get().addControl(control);
      control.setEnabled(false);

    }
    return TransformedValue.create(result);
  }

  private Mesh applySkinData(DAENode geomNode, List<VertexSkinningData> vsdList) {
    PolygonData[] polygons = geomNode.getParsedData(PolygonData[].class); //triangulated
    Mesh jmeMesh = geomNode.getParsedData(Mesh.class);
    int meshVCount = jmeMesh.getVertexCount();
    VertexBuffer wb = new VertexBuffer(Type.BoneWeight);
    VertexBuffer bi = new VertexBuffer(Type.BoneIndex);
    FloatBuffer wbuffer = FloatBuffer.allocate(meshVCount * 4);
    ByteBuffer ibuffer = ByteBuffer.allocate(meshVCount * 4);
    for (int i = 0; i < polygons.length; i++) {
      PolygonData polygonData = polygons[i];
      polygonData.pushVertexSkinningData(vsdList); //this will map old vertices to new vertices
      polygonData.popVertexSkinningData(ibuffer, wbuffer); //fill the weight and index buffer
    }
    wb.setupData(Usage.CpuOnly, 4, Format.Float, wbuffer);
    bi.setupData(Usage.CpuOnly, 4, Format.UnsignedByte, ibuffer);
    jmeMesh.setBuffer(wb);
    jmeMesh.setBuffer(bi);
    return jmeMesh;
  }

  private void createBindPose(Mesh jmeMesh) {
    //create bind pose. I have no idea of what/when/how this should be called...
    VertexBuffer pos = jmeMesh.getBuffer(Type.Position);
    VertexBuffer bindPos = new VertexBuffer(Type.BindPosePosition);
    bindPos.setupData(Usage.CpuOnly, 3, Format.Float, BufferUtils.clone(pos.getData()));
    jmeMesh.setBuffer(bindPos);
    pos.setUsage(Usage.Stream);
    VertexBuffer norm = jmeMesh.getBuffer(Type.Normal);
    if (norm != null) {
      VertexBuffer bindNorm = new VertexBuffer(Type.BindPoseNormal);
      bindNorm.setupData(Usage.CpuOnly, 3, Format.Float, BufferUtils.clone(norm.getData()));
      jmeMesh.setBuffer(bindNorm);
      norm.setUsage(Usage.Stream);
    }
  }

  /**
   * sid of bone nodes is the name of the jme bones.
   *
   * @param boneNodes
   * @param jmeBones
   */
  private void mapBoneTree(DAENode[] boneNodes, Bone[] jmeBones) {
    for (int i = 0; i < boneNodes.length; i++) {
      DAENode daeNode = boneNodes[i];
      Bone bone = jmeBones[i];
      for (DAENode c : daeNode.getChildren()) {
        if (c.hasParsedData(Bone.class)) {
          bone.addChild(c.getParsedData(Bone.class));
        }
      }
    }
  }

  private Matrix4f[] getSkinJointsMatrices(DAENode skin) {
    DAENode joints = skin.getChild(Names.JOINTS);
    DAENode ibmInput = joints.getChild(Names.INPUT, Names.SEMANTIC, Semantic.INV_BIND_MATRIX.name());
    DAENode ibmSource = ibmInput.getLinkedSource();
    DAENode floatArray = ibmSource.getChild(Names.FLOAT_ARRAY);
    TransformedValue<Matrix4f[]> matrices = floatArray.getContent(Matrix4fTransformer.create());

    Conditions.checkTrue(joints.isDefined());
    Conditions.checkTrue(ibmInput.isDefined());
    Conditions.checkTrue(ibmSource.isDefined());
    Conditions.checkTrue(floatArray.isDefined());
    Conditions.checkTrue(matrices.isDefined());

    return matrices.get();
  }

  private AnimControl createAnimControl(Node parent, DAENode animationsNode, List<DAENode> geomElementList, Skeleton skeleton) {
    Mesh[] meshes = new Mesh[geomElementList.size()];
    int index = 0;
    for (DAENode geomNode : geomElementList) {
      meshes[index] = geomNode.getParsedData(Mesh.class);
      index++;
    }
    Animation[] animations = createBoneAnimations(animationsNode, skeleton);
    AnimControl animation = new AnimControl(skeleton);
    animation.setAnimations(new HashMap<String, Animation>());
    for (Animation boneAnimation : animations) {
      animation.addAnim(boneAnimation);
    }
    animation.setEnabled(false);
    return animation;
  }

  private Animation[] createBoneAnimations(DAENode animationsNode, Skeleton skeleton) {
    List<Animation> list = new ArrayList<Animation>();
    for (int i = 0; i < skeleton.getBoneCount(); i++) {
      Bone bone = skeleton.getBone(i);
      List<Animation> animations = createBoneAnimationList(animationsNode, bone, skeleton);
      list.addAll(animations);
    }
    return list.toArray(new Animation[list.size()]);
  }

  private List<Animation> createBoneAnimationList(DAENode animationsNode, Bone bone, Skeleton skeleton) {
    List<Animation> animations = new LinkedList<Animation>();
    List<DAENode> animationNodes = getAnimationsNodeForBone(animationsNode, bone);
    for (DAENode node : animationNodes) {
      Animation anim = createBoneAnimation(node, bone, skeleton);
      animations.add(anim);
    }
    return animations;
  }

  private List<DAENode> getAnimationsNodeForBone(DAENode animationLibraryNodes, Bone bone) {
    List<DAENode> animationNodes = animationLibraryNodes.getChildren(Names.ANIMATION);
    List<DAENode> boneAnimationNodes = new LinkedList<DAENode>();
    for (DAENode node : animationNodes) {
      TransformedValue<String> targetValue = node.getChild(Names.CHANNEL).getAttribute(Names.TARGET, TEXT);
      if (targetValue.isDefined()) {
        String targetBone = targetValue.get().substring(0, targetValue.get().indexOf('/'));
        if (bone.getName().equals(targetBone)) {
          boneAnimationNodes.add(node);
        }
      }
    }
    return boneAnimationNodes;
  }

  private Animation createBoneAnimation(DAENode animationNode, Bone bone, Skeleton skeleton) {
    DAENode channel = animationNode.getChild(Names.CHANNEL);
    DAENode sampler = animationNode.getChild(Names.SAMPLER);
    DAENode times = sampler.getChild(Names.INPUT, Names.SEMANTIC, Semantic.INPUT.name()).getLinkedSource();
    DAENode values = sampler.getChild(Names.INPUT, Names.SEMANTIC, Semantic.OUTPUT.name()).getLinkedSource();

    float[] timeValues = times.getChild(Names.FLOAT_ARRAY).getContent(FLOAT_LIST).get();
    float[] outputValues = values.getChild(Names.FLOAT_ARRAY).getContent(FLOAT_LIST).get();
    ChannelTarget channelTarget = ChannelTarget.forName(channel.getAttribute(Names.TARGET, TEXT).get());

    Conditions.checkTrue(sampler.isDefined());
    Conditions.checkTrue(times.isDefined());
    Conditions.checkTrue(values.isDefined());
    Conditions.checkTrue(channel.isDefined());
    Conditions.checkNotNull(channelTarget);
    Conditions.checkNotNull(timeValues);
    Conditions.checkNotNull(outputValues);

    Transform[] transformList = channelTarget.transform(outputValues).get();

    Conditions.checkNotNull(transformList);
    Conditions.checkValue(transformList.length, timeValues.length);

    int boneIndex = skeleton.getBoneIndex(bone);
    float animationLength = max(timeValues);

    Quaternion[] rotations = getRotations(transformList);
    Vector3f[] translations = getTranslations(transformList);
    BoneTrack track = new BoneTrack(boneIndex, timeValues, translations, rotations);
    Animation anim = new Animation(animationNode.getAttribute(Names.ID, TEXT).get(), animationLength);
    anim.setTracks(new BoneTrack[]{track});
    return anim;
  }

  private float max(float[] timeValues) {
    float m = 0;
    for (int i = 0; i < timeValues.length; i++) {
      m = Math.max(m, timeValues[i]);
    }
    return m;
  }

  private Quaternion[] getRotations(Transform[] transformList) {
    Quaternion[] result = new Quaternion[transformList.length];
    for (int i = 0; i < transformList.length; i++) {
      Transform transform = transformList[i];
      result[i] = transform.getRotation();
    }
    return result;
  }

  private Vector3f[] getTranslations(Transform[] transformList) {
    Vector3f[] result = new Vector3f[transformList.length];
    for (int i = 0; i < transformList.length; i++) {
      Transform transform = transformList[i];
      result[i] = transform.getTranslation();
    }
    return result;
  }
}
