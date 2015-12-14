/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jme3dae;

import com.jme3.animation.*;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.plugins.AWTLoader;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A test application to check the loading of a collada node.
 *
 * @author pgi
 */
public class Main extends SimpleApplication {

  private static volatile String model;
  private static volatile String modelParent;

  /**
   * Starts the program.
   *
   * @param args the application's arguments. Requires a string with the path
   *             of a collada file.
   * @throws MalformedURLException
   */
  public static void main(String[] args) throws MalformedURLException, URISyntaxException {
    if (args != null && args.length > 0) {
      if (args[0].startsWith("file:")) {
        model = new File(new URL(args[0]).toURI()).toString();
      } else {
        model = new File(args[0]).toURI().toURL().toString();
      }
      modelParent = new File(args[0]).getParentFile().toURI().toURL().toString();
      Main main = new Main();
      main.setShowSettings(false);
      main.start();
    } else {
      System.out.println("Usage: java -jar jme3dae.jar modelFileToLoad");
    }
  }

  private AnimControl controlledAnimation;
  private Spatial debugMesh;

  /**
   * Default no-arg constructor.
   */
  public Main() {
  }

  /**
   * Initializes the JME3 application.
   */
  @Override
  public void simpleInitApp() {
    inputManager.setCursorVisible(true);
    renderer.setBackgroundColor(ColorRGBA.Gray);
    inputManager.addMapping("animate", new KeyTrigger(KeyInput.KEY_1));
    inputManager.addListener(new ActionListener() {

      public void onAction(String string, boolean bln, float f) {
        if (controlledAnimation != null) {
          AnimChannel channel = controlledAnimation.createChannel();
          channel.setSpeed(0.001f);
          channel.setAnim("l_hip_rotateY");
          controlledAnimation.setEnabled(!controlledAnimation.isEnabled());
        }
      }
    }, "animate");
    //todo add fx enhancer shadow mode settings
    ColladaDocumentFactory.setFXEnhance(FXEnhancerInfo.create(
        FXEnhancerInfo.NormalMapGenerator.ON,
        FXEnhancerInfo.TwoSidedMaterial.ON,
        FXEnhancerInfo.IgnoreMeasuringUnit.ON));

    assetManager.registerLoader(AWTLoader.class, "tif");
    assetManager.registerLoader(ColladaLoader.class, "dae");
    assetManager.registerLocator(modelParent, UrlLocator.class);
    Spatial scene = assetManager.loadModel(model);
    scene.setLocalTranslation(0, 0, -15);
    scene.updateModelBound();
    rootNode.attachChild(scene);

    DirectionalLight dl = new DirectionalLight();
    dl.setColor(ColorRGBA.White.clone());
    dl.setDirection(new Vector3f(1, 1, 1).normalizeLocal().negateLocal());
    rootNode.addLight(dl);
    PointLight pl = new PointLight();
    pl.setColor(ColorRGBA.Gray.clone());
    pl.setPosition(new Vector3f(0, 100, -100));
    pl.setRadius(1000);
    rootNode.addLight(pl);

    AmbientLight al = new AmbientLight();
    rootNode.addLight(al);

    cam.lookAt(scene.getWorldTranslation(), Vector3f.UNIT_Y);
    flyCam.setMoveSpeed(100);

    //cam.setFrustumFar(25);
    //node.setShadowMode(ShadowMode.CastAndReceive);
    try {
      //inspect(node);
    } catch (Exception ex) {
      Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void inspect(Node node) throws Exception {
    LinkedList<Spatial> list = new LinkedList<Spatial>();
    list.add(node);
    while (!list.isEmpty()) {
      Spatial s = list.pop();
//	    System.out.println("SPATIAL TYPE " + s.getClass().getSimpleName() + " NAME " + s.getName());
      if (s instanceof Node) {
        list.addAll(((Node) s).getChildren());
      }
      AnimControl animation = s.getControl(AnimControl.class);
      if (animation != null) {
        debugAnimation(animation);
        controlledAnimation = animation;
        debugMesh(s);
      }
    }
  }

  private void debugMesh(Spatial s) {
    debugMesh = s;
    Spatial g = s;
    int index = 0;
    while (!(g instanceof Geometry) & index++ < 100) {
      g = ((Node) s).getChild(0);
    }
    Mesh mesh = ((Geometry) g).getMesh();
    VertexBuffer poseweight = mesh.getBuffer(Type.BoneWeight);
    VertexBuffer boneindex = mesh.getBuffer(Type.BoneIndex);
    FloatBuffer vbuffer = (FloatBuffer) mesh.getBuffer(Type.Position).getData();
    FloatBuffer wbuffer = (FloatBuffer) poseweight.getData();
    ByteBuffer bbuffer = (ByteBuffer) boneindex.getData();
    wbuffer.rewind();
    bbuffer.rewind();
    vbuffer.rewind();
    int vindex = 0;
    while (wbuffer.hasRemaining()) {
      byte[] bones = new byte[4];
      float[] weights = new float[4];
      float[] vertex = new float[3];
      vbuffer.get(vertex);
      wbuffer.get(weights);
      bbuffer.get(bones);
      System.out.println("Vertex " + vindex + " {" + toString(vertex) + "} : Bones {" + toString(bones) + "}" + " Weights {" + toString(weights) + "}");
      vindex++;
    }
    wbuffer.rewind();
    bbuffer.rewind();
  }

  private String toString(byte[] data) {
    String t = "";
    for (byte b : data) t += b + "...";
    return t.substring(0, t.length() - 3);
  }

  private String toString(float[] data) {
    String t = "";
    for (float f : data) {
      t += f + "...";
    }
    return t.substring(0, t.length() - 3);
  }

  private void debugAnimation(AnimControl animation) throws Exception {
    System.out.println("ANIMATION DATA");
    Skeleton skeleton = animation.getSkeleton();
    debugSkeleton(skeleton);
  /*
	Mesh[] targets = animation.getSkeleton().
	for (int i = 0; i < targets.length; i++) {
	    Mesh mesh = targets[i];
	    System.out.println("TARGET MESH: " + mesh);
	}
	*/
    System.out.println("Animations bound to this control: ");
    Collection<String> animationNames = animation.getAnimationNames();
    for (String string : animationNames) {
      Animation anim = animation.getAnim(string);
      debugAnimation(anim);
    }
  }

  private void debugAnimation(Animation anim) throws Exception {
    System.out.println("ANIMATION: " + anim.getName());
    Track[] tracks = anim.getTracks();
    for (int i = 0; i < tracks.length; i++) {
      Track boneTrack = tracks[i];
      // TODO: do something????
    }
  }

  private void debugSkeleton(Skeleton skeleton) throws Exception {
    System.out.println("SKELETON");
    int boneCount = skeleton.getBoneCount();
    System.out.println("bone count " + boneCount);
    for (int i = 0; i < boneCount; i++) {
      Bone bone = skeleton.getBone(i);
      System.out.println("Bone: " + bone.getName());
      System.out.println("Parent: " + bone.getParent());
      System.out.print("Children: ");
      for (Bone bone1 : bone.getChildren()) {
        System.out.print("[" + bone1 + "]");
      }
      System.out.println("");
    }
  }
}
