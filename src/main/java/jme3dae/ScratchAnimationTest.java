package jme3dae;

import com.jme3.animation.*;
import com.jme3.app.SimpleApplication;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Format;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.VertexBuffer.Usage;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.HashMap;

public class ScratchAnimationTest extends SimpleApplication {
  public static void main(String[] args) {
    new ScratchAnimationTest().start();
  }

  private Mesh debmesh;
  private int loop;

  public static void printFloatBuffer(String label, VertexBuffer buffer) {
    FloatBuffer data = (FloatBuffer) buffer.getData();
    data.rewind();
    System.out.print(label);
    while (data.hasRemaining()) {
      System.out.print(data.get() + " ");
    }
    System.out.println("");
    data.rewind();
  }

  @Override
  public void simpleInitApp() {
    renderer.setBackgroundColor(ColorRGBA.Gray);
    float[] normals = {0, 0, 1, 0, 0, 1, 0, 0, 1};
    float[] positions = {-1, 0, 0, 1, 0, 0, 0, 2, 0};
    int[] indices = {0, 1, 2};
    float[] weights = {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0}; //just v2 affected by bone 0
    byte[] bones = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; //all v bound to bone 0

    Mesh mesh = new Mesh();
    mesh.setBuffer(Type.Position, 3, positions.clone());
    mesh.setBuffer(Type.Normal, 3, normals.clone());
    mesh.setBuffer(Type.Index, 1, indices.clone());
    mesh.setBuffer(Type.BindPosePosition, 3, positions.clone());
    mesh.setBuffer(Type.BindPoseNormal, 3, normals.clone());

    VertexBuffer vertexWeightBuffer = new VertexBuffer(Type.BoneWeight);
    VertexBuffer boneIndexBuffer = new VertexBuffer(Type.BoneIndex);
    vertexWeightBuffer.setupData(Usage.CpuOnly, 4, Format.Float, FloatBuffer.wrap(weights));
    boneIndexBuffer.setupData(Usage.CpuOnly, 4, Format.UnsignedByte, ByteBuffer.wrap(bones));
    mesh.setBuffer(vertexWeightBuffer);
    mesh.setBuffer(boneIndexBuffer);

    Geometry geom = new Geometry("triangle", mesh);
    geom.setMaterial(assetManager.loadMaterial("/Common/Materials/RedColor.j3m"));

    Node node = new Node("geom owner");
    node.attachChild(geom);

    Bone bone = new Bone("bone 0");
    bone.setBindTransforms(new Vector3f(0, 0, 0), new Quaternion(Quaternion.IDENTITY), Vector3f.UNIT_XYZ);

    Skeleton skeleton = new Skeleton(new Bone[]{bone});
    skeleton.setBindingPose();
    skeleton.updateWorldVectors();
    skeleton.resetAndUpdate();
    skeleton.computeSkinningMatrices();

    float[] times = {0, 10};
    Vector3f[] translations = {new Vector3f(0, 0, 0), new Vector3f(0, 2, 0)};
    Quaternion[] rotations = {new Quaternion(0, 0, 0, 1), new Quaternion(0, 0, 0, 1)};
    BoneTrack boneTrack = new BoneTrack(0, times, translations, rotations);

    Animation boneAnim = new Animation("test animation", 3);
    boneAnim.setTracks(new BoneTrack[]{boneTrack});

    final AnimControl anim = new AnimControl(skeleton);
    anim.setAnimations(new HashMap<String, Animation>());
    anim.addAnim(boneAnim);
    AnimChannel channel = anim.createChannel();
    channel.setAnim("test animation");
    channel.setLoopMode(LoopMode.Loop);
    channel.setSpeed(0.5f);

    node.addControl(anim);

    rootNode.attachChild(node);

    debmesh = geom.getMesh();
  }

  @Override
  public void simpleUpdate(float tpf) {
    super.simpleUpdate(tpf);
    if (loop++ % 1000 == 0) {
      printFloatBuffer("Position:     ", debmesh.getBuffer(Type.Position));
      printFloatBuffer("BindPosition: ", debmesh.getBuffer(Type.BindPosePosition));
    }
  }

}
