package jme3dae.utilities;

import com.jme3.bounding.BoundingBox;
import com.jme3.export.JmeExporter;
import com.jme3.export.JmeImporter;
import com.jme3.material.Material;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExplicitAnimationNode extends Node {
  private static final Pattern INT_PATTERN = Pattern.compile("\\d+");

  private static String getNamePart(String name) {
    return name.split("\\d+")[0];
  }

  private static Integer getIndexPart(String name) {
    Matcher matcher = INT_PATTERN.matcher(name);
    matcher.find();
    return new Integer(name.substring(matcher.start(), matcher.end()));
  }

  private static boolean isFrameGeometryName(String name) {
    return INT_PATTERN.matcher(name).find();
  }

  private static Mesh[] generateFrameSequence(List<Geometry> frames) {
    Map<Integer, Mesh> map = new HashMap<Integer, Mesh>();
    for (Geometry geometry : frames) {
      Integer index = getIndexPart(geometry.getName());
      map.put(index, geometry.getMesh());
    }
    ArrayList<Mesh> list = new ArrayList<Mesh>();
    Set<Integer> keys = new TreeSet<Integer>(map.keySet());
    for (Integer integer : keys) {
      list.add(map.get(integer));
    }
    return list.toArray(new Mesh[list.size()]);
  }

  private static Map<String, Mesh[]> generateFrames(Map<String, List<Geometry>> packedGeometries) {
    Map<String, Mesh[]> res = new HashMap<String, Mesh[]>();
    for (String string : packedGeometries.keySet()) {
      res.put(string, generateFrameSequence(packedGeometries.get(string)));
    }
    return res;
  }

  private static Map<String, List<Geometry>> packByName(List<Geometry> geometries) {
    //geom name is NAME+INDEX, this group geometries by name
    Map<String, List<Geometry>> res = new HashMap<String, List<Geometry>>();
    for (Geometry geometry : geometries) {
      String name = getNamePart(geometry.getName());
      List<Geometry> list = res.get(name);
      if (list == null) res.put(name, list = new LinkedList<Geometry>());
      list.add(geometry);
    }
    return res;
  }

  private static List<Geometry> extractAllFrames(Node scene) {
    List<Geometry> frames = new LinkedList<Geometry>();
    for (Spatial s : new SpatialTreeIterable(scene)) {
      if (s instanceof Geometry && isFrameGeometryName(s.getName())) {
        frames.add((Geometry) s);
      }
    }
    return frames;
  }

  private static Map<String, Mesh[]> extractAnimations(Node scene, ExplicitAnimationNode ex) {
    List<Geometry> geometries = extractAllFrames(scene);
    ex.setMaterial(geometries.get(0).getMaterial());
    Map<String, List<Geometry>> namedPacks = packByName(geometries);
    return generateFrames(namedPacks);
  }

  private static String createAnimationName(String name) {
    if (name.startsWith("#") && name.length() > 1) name = name.substring(1);
    return name;
  }

  public static ExplicitAnimationNode createFrom(Node animationPack) {
    ExplicitAnimationNode node = new ExplicitAnimationNode("");
    Map<String, Mesh[]> animations = extractAnimations(animationPack, node);
    for (String string : animations.keySet()) {
      String animName = createAnimationName(string);
      node.addSequence(animName, animations.get(string));
    }
    return node;
  }

  private final Control animator = new Control() {
    public Control cloneForSpatial(Spatial spatial) {
      return this;
    }

    public void setSpatial(Spatial spatial) {
    }

    public void setEnabled(boolean enabled) {
      ExplicitAnimationNode.this.setEnabled(enabled);
    }

    public boolean isEnabled() {
      return ExplicitAnimationNode.this.isEnabled();
    }

    public void update(float tpf) {
      ExplicitAnimationNode.this.update(tpf);
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void write(JmeExporter ex) throws IOException {
    }

    public void read(JmeImporter im) throws IOException {
    }
  };
  private final Map<String, Mesh[]> sequences = new HashMap<String, Mesh[]>();
  private final Geometry geometry;
  private int currentIndex;
  private Mesh[] currentSequence;
  private boolean enabled;
  private float animationTimeline;
  private float animationFrameTime = 0.5f;
  private float totalAnimationTime;
  private Runnable sequenceEndTask;

  public ExplicitAnimationNode(String name) {
    super(name);
    geometry = new Geometry(name);
    attachChild(geometry);
    addControl(animator);
  }

  public void addSequence(String name, Mesh[] meshes) {
    sequences.put(name, meshes);
    if (currentSequence == null) {
      setCurrentSequence(name);
    }
  }

  public Collection<String> getAvailableSequenceNames() {
    return sequences.keySet();
  }

  @Override
  public void setMaterial(Material mat) {
    super.setMaterial(mat);
    geometry.setMaterial(mat);
  }

  private void nextFrame() {
    currentIndex++;
    if (currentIndex >= currentSequence.length) {
      currentIndex = 0;
      if (sequenceEndTask != null) {
        sequenceEndTask.run();
      }
    }
    geometry.setMesh(currentSequence[currentIndex]);
  }

  public void setCurrentSequence(String name) {
    currentSequence = findSequence(name);
    currentIndex = 0;
    if (geometry.getMesh() == null) {
      geometry.setMesh(currentSequence[currentIndex]);
      geometry.setModelBound(new BoundingBox());
      geometry.updateModelBound();
      setModelBound(new BoundingBox());
      updateModelBound();
    } else if (currentSequence != null) {
      geometry.setMesh(currentSequence[currentIndex]);
    } else {
      System.err.println("Can't play animation " + name);
      return;
    }
    if (totalAnimationTime != 0) {
      animationFrameTime = totalAnimationTime / currentSequence.length;
    }
  }

  public void setAnimationFrameTime(float t) {
    animationFrameTime = t;
    totalAnimationTime = 0;
  }

  public void setAnimationLength(float t) {
    totalAnimationTime = t;
    if (currentSequence != null) {
      animationFrameTime = totalAnimationTime / currentSequence.length;
    }
  }

  public void setLoopEndNotifiable(Runnable task) {
    sequenceEndTask = task;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  private void update(float tpf) {
    if (enabled && currentSequence != null) {
      animationTimeline += tpf;
      while (animationTimeline >= animationFrameTime) {
        animationTimeline -= animationFrameTime;
        nextFrame();
      }
    }
  }

  private Mesh[] findSequence(String name) {
    Mesh[] result = sequences.get(name);
    if (result == null) {
      Logger.getLogger(getClass().getName()).log(Level.INFO, "Sequence name " + name + " has no match, trying to guess...");
      result = guessSequence(name);
    }
    return result;
  }

  private Mesh[] guessSequence(String name) {
    for (String string : sequences.keySet()) {
      if (string.toLowerCase().contains(name.toLowerCase())) return sequences.get(string);
    }
    return null;
  }
}
