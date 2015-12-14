package jme3dae;

import jme3dae.utilities.MeasuringUnit;
import jme3dae.utilities.Tuple2;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.math.FastMath;
import com.jme3.scene.Node;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3dae.transformers.ValueTransformer.TransformedValue;
import jme3dae.utilities.TextureBaseList;
import jme3dae.utilities.UpAxis;

/**
 * A JME3 Asset loader that loads a collada file.
 *
 * @author pgi
 */
public class ColladaLoader implements AssetLoader {
  public static class ModelDirectoryUrl {
    final String url;

    public ModelDirectoryUrl(String url) {
      this.url = url;
    }
  }

  /**
   * Registers the loader to load elements from the given url base
   *
   * @param manager the asset manager where to register the loader
   * @param url     the url path of the directory that contains the models to load
   */
  public static void register(AssetManager manager, ModelDirectoryUrl url) {
    manager.registerLoader(ColladaLoader.class, "dae");
    manager.registerLocator(url.url, UrlLocator.class);
  }

  /**
   * Used to set the texture base directory
   */
  private static final TextureBaseList TEXTURE_BASE = new TextureBaseList();

  /**
   * Add a path to instruct the loader where to find textures. The path will be
   * used to form an assetmanager path along with the name of the texture if that
   * name cannot be resolved per se. Eg. if a texture named "x/y/z/abc.png" cannot be
   * resolved by the asset manager, the collada loader will try to load the file
   * "abc.png" using the base paths setted with this method. If the path is
   * "/a/b/c/" then the loader will pass to the assetmanager the path "/a/b/c/abc.png".
   *
   * @param base an asset loader base path where textures might be stored.
   */
  public static void addTextureBase(String base) {
    TEXTURE_BASE.add(base);
  }

  public static void removeTextureBase(String base) {
    TEXTURE_BASE.remove(base);
  }

  /**
   * Default no-arg constructor.
   */
  public ColladaLoader() {
  }

  /**
   * Load a collada document.
   *
   * @param assetInfo a pointer to a collada (dae) document.
   * @return a Node element wrapping the contents of the loaded document or null
   * if some (logged) exception happens during the process.
   */
  public Node load(AssetInfo assetInfo) {
    InputStream in = assetInfo.openStream();
    Node node = null;
    try {
      // Clear the DAENode registry so we get our memory back
      DAENode.clearRegistry();
      DAENode root = DAELoader.create().load(in); //generate the collada-xml root node
      root.setParsedData(ColladaInfo.create(assetInfo)); //stores the collada info into the root node
      root.setParsedData(TEXTURE_BASE);
      Tuple2<DAENode, AssetManager> data = Tuple2.create(root, assetInfo.getManager());

      //Creates and applies the transformed that maps the entire collada document (via its root)
      //to a jme node
      TransformedValue<Node> jmeRoot = ColladaDocumentFactory.newColladaDocumentParser(root).transform(data);
      node = jmeRoot.get(); //this may return null
      if (node != null) {
        UpAxis upAxis = root.getParsedData(UpAxis.class);
        if (upAxis != null) {
          node.setUserData("up", upAxis.toString());
        }
        MeasuringUnit measuringUnit = root.getParsedData(MeasuringUnit.class);
        if (measuringUnit != null) {
          node.setUserData("unit", measuringUnit.getMeter());
        }
      }
      // Clear the DAENode registry so we get our memory back
      DAENode.clearRegistry();
    } catch (Exception ex) {
      Logger.getLogger(ColladaLoader.class.getName()).log(Level.SEVERE, null, ex);
    } finally {
      try {
        in.close();
      } catch (IOException ex) {
        Logger.getLogger(ColladaLoader.class.getName()).log(Level.SEVERE, null, ex);
      }
    }
    return node;
  }

}
