package jme3dae;

import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.scene.Spatial;

public class TestNoApp {

  public static void main(String[] args) {
    DesktopAssetManager dam = new DesktopAssetManager();
    String base = "file:///home/pgi/3d models/";
    dam.registerLoader(ColladaLoader.class, "dae");
    dam.registerLocator(base, UrlLocator.class.getName());
    Spatial loadModel = dam.loadModel(base + "triangle_anim.dae");
  }
}
