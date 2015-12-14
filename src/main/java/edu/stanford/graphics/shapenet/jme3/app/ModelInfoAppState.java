package edu.stanford.graphics.shapenet.jme3.app;


import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bounding.BoundingBox;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import edu.stanford.graphics.shapenet.common.GeometricScene;
import edu.stanford.graphics.shapenet.common.ModelInstance;
import edu.stanford.graphics.shapenet.jme3.JmeUtils$;
import edu.stanford.graphics.shapenet.jme3.geom.BoundingBoxUtils$;
import scala.Option;

import static edu.stanford.graphics.shapenet.jme3.app.ModelInfoAppState.BitmapLabel.Alignment.*;

/**
 * Displays information about objects in the scene
 *
 * @author Angel Chang
 */
public class ModelInfoAppState extends AbstractAppState {
  protected Application app;
  protected Node guiNode;
  protected BitmapFont guiFont;
  protected Camera camera;
  protected GeometricScene<Node> scene;

  protected boolean showModelLabel;
  protected Node labelsNode;
  protected BitmapLabel[] modelLabels;

  public static class BitmapLabel extends Node {
    public BitmapText text;
    public Geometry background;
    public Alignment alignment = Alignment.CENTER;

    public enum Mode {
      BLACK_ON_WHITE,
      WHITE_ON_BLACK,
      DEFAULT
    }

    public enum Alignment {
      CENTER,
      TOP,
      BOTTOM,
      LEFT,
      RIGHT,
    }

    public BitmapLabel(Application app, BitmapFont font, String label, boolean withBackground) {
      this.text = new BitmapText(font);
      this.text.setText(label);
      this.attachChild(this.text);
      if (withBackground) {
        createBackground(app, new ColorRGBA(0.0F, 0.0F, 0.0F, 0.5F));
        this.setMode(Mode.BLACK_ON_WHITE);
      }
    }

    public void setMode(Mode mode) {
      ColorRGBA color;
      switch (mode) {
        case BLACK_ON_WHITE:
          this.text.setColor(ColorRGBA.Black);
          color = new ColorRGBA(1.0F, 1.0F, 1.0F, 1.0F);
          if (background != null) this.background.getMaterial().setColor("Color", color);
          break;
        case WHITE_ON_BLACK:
          this.text.setColor(ColorRGBA.White);
          color = new ColorRGBA(0.0F, 0.0F, 0.0F, 1.0F);
          if (background != null) this.background.getMaterial().setColor("Color", color);
          break;
        default:
          this.text.setColor(ColorRGBA.White);
          color = new ColorRGBA(0.0F, 0.0F, 0.0F, 0.5F);
          if (background != null) this.background.getMaterial().setColor("Color", color);
          break;
      }
    }

    private void createBackground(Application app, ColorRGBA color) {
      Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
      mat.setColor("Color", color);
      mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
      this.background = new Geometry("labelBackground", new Quad(text.getLineWidth(), text.getHeight()));
      this.background.setLocalTranslation(0.0F, -text.getHeight(), -1.0F);
      this.background.setMaterial(mat);
      this.attachChild(this.background);
    }

    public void setPosition(Vector3f pos) {
      float offsetX = text.getLineWidth()/2.0f;
      float offsetY = text.getHeight()/2.0f;
      float posX = pos.x - offsetX;
      float posY = pos.y + offsetX;
      switch (alignment) {
        case BOTTOM:
          posY += offsetY;
          break;
        case TOP:
          posY -= offsetY;
          break;
        case LEFT:
          posX += offsetX;
          break;
        case RIGHT:
          posX -= offsetX;
          break;
        default:
      }
      this.setLocalTranslation(posX, posY, pos.z);
    }

  }

  public ModelInfoAppState() {
  }

  public ModelInfoAppState(Node guiNode, BitmapFont guiFont) {
    this.guiNode = guiNode;
    this.guiFont = guiFont;
  }

  public void setScene(GeometricScene<Node> scene) {
    this.scene = scene;
    updateLabels();
  }

  public void initialize(AppStateManager stateManager, Application app) {
    super.initialize(stateManager, app);
    this.app = app;
    this.camera = app.getCamera();
    if(app instanceof SimpleApplication) {
      SimpleApplication simpleApp = (SimpleApplication)app;
      if(this.guiNode == null) {
        this.guiNode = simpleApp.getGuiNode();
      }

//      if(this.guiFont == null) {
//        this.guiFont = simpleApp.guiFont;
//      }
    }

    if(this.guiNode == null) {
      throw new RuntimeException("No guiNode specific and cannot be automatically determined.");
    } else {
      if(this.guiFont == null) {
        this.guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
      }
    }

    initLabels();
  }

  protected void initLabels() {
    this.labelsNode = new Node();
    this.labelsNode.setCullHint(this.isEnabled() && this.showModelLabel? Spatial.CullHint.Never: Spatial.CullHint.Always);
    updateLabels();
    this.guiNode.attachChild(this.labelsNode);
  }

  protected void clearLabels() {
    this.modelLabels = null;
    labelsNode.detachAllChildren();
  }

  protected void updateLabels() {
    clearLabels();
    if (scene == null || !isEnabled() || !showModelLabel) {
      return;
    }
    int nObjects = scene.getNumberOfObjects();
    modelLabels = new BitmapLabel[nObjects];
    for (int i = 0; i < nObjects; i++) {
      ModelInstance<Node> modelInstance = scene.getModelInstance(i);
      if (modelInstance != null) {
        String label = modelInstance.getLabel();
        modelLabels[i] = new BitmapLabel(this.app, this.guiFont, label, true);
        modelLabels[i].alignment = BitmapLabel.Alignment.BOTTOM;
        labelsNode.attachChild(modelLabels[i]);

        BoundingBox bb = JmeUtils$.MODULE$.getScreenBoundingBox(camera, modelInstance.nodeSelf());
        Vector3f pos = BoundingBoxUtils$.MODULE$.getPointFromCenter(bb, new Vector3f(0, 1, 0));
//        Vector3f pos = camera.getScreenCoordinates(modelInstance.nodeSelf().getWorldTranslation());
        modelLabels[i].setPosition(pos);
      }
    }
  }

  public boolean getShowModelLabel() {
    return showModelLabel;
  }

  public void setShowModelLabel(boolean showModelLabel) {
    this.showModelLabel = showModelLabel;
    if (this.labelsNode != null) {
      this.labelsNode.setCullHint(this.isEnabled() && this.showModelLabel ? Spatial.CullHint.Never : Spatial.CullHint.Always);
    }
  }

  public void toggleDisplay() {
    this.setShowModelLabel(!this.showModelLabel);
  }

  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    this.labelsNode.setCullHint(enabled && this.showModelLabel ? Spatial.CullHint.Never : Spatial.CullHint.Always);
  }

  public void update(float tpf) {
    if (isEnabled() && this.showModelLabel) {
      if (this.modelLabels == null) {
        updateLabels();
      }
      if (this.modelLabels != null) {
        for (int i = 0; i < this.modelLabels.length; i++) {
          ModelInstance<Node> modelInstance = scene.getModelInstance(i);
          BoundingBox bb = JmeUtils$.MODULE$.getScreenBoundingBox(camera, modelInstance.nodeSelf());
          Vector3f pos = BoundingBoxUtils$.MODULE$.getPointFromCenter(bb, new Vector3f(0, 1, 0));
          //        Vector3f pos = camera.getScreenCoordinates(modelInstance.nodeSelf().getWorldTranslation());
          modelLabels[i].setPosition(pos);
        }
      }
    }
  }

  public void cleanup() {
    super.cleanup();
    this.guiNode.detachChild(this.labelsNode);
  }

}
