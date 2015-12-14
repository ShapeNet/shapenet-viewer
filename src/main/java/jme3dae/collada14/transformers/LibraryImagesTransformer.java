package jme3dae.collada14.transformers;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Texture;

import java.io.File;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jme3dae.ColladaInfo;
import jme3dae.DAENode;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.utilities.FileType;
import jme3dae.utilities.FileTypeFinder;
import jme3dae.utilities.HexSequenceTransformer;
import jme3dae.utilities.MEMAssetLocator;
import jme3dae.utilities.TextureBaseList;
import jme3dae.utilities.Todo;
import jme3dae.utilities.TransformerPack;
import jme3dae.utilities.Tuple2;

/**
 * Parses a library_images DAENode. The parser stores the result in the parsed DAE nodes as JME3
 * Texture objects. Maybe.
 *
 * @author pgi
 */
public class LibraryImagesTransformer implements TransformerPack<Tuple2<DAENode, AssetManager>, Void> {

  /**
   * Instance creator.
   *
   * @return a new LibraryImagesTransformer instance.
   */
  public static LibraryImagesTransformer create() {
    return new LibraryImagesTransformer();
  }

  private final HexSequenceTransformer HEX_SEQUENCE = HexSequenceTransformer.create();

  private LibraryImagesTransformer() {
  }

  /**
   * Given a DAENode-AssetManager pair this transformer creates and stores textures into
   * each image child of the supplied DAENode.
   *
   * @param value a collada library_images node and a JME3 AssetManager. The asset manager
   *              is used to create jme3 texture. The library_images node will be scanned searching of image
   *              nodes. For each node the transformer will try to load an image and, if successful, will store
   *              back into that node (via setParsedData) the resulting texture.
   * @return an undefined TransformedValue.
   */
  public TransformedValue<Void> transform(Tuple2<DAENode, AssetManager> value) {
    DAENode libraryImagesNode = value.getA();
    AssetManager assetManager = value.getB();
    if (libraryImagesNode != null && libraryImagesNode.isDefined() && assetManager != null) {
      transform(libraryImagesNode, assetManager);
    }
    return TransformedValue.<Void>create(null);
  }

  private void transform(DAENode libraryImagesNode, AssetManager assetManager) {
    DAENode assetNode = libraryImagesNode.getChild(Names.ASSET);
    DAENode extraNode = libraryImagesNode.getChild(Names.EXTRA);
    if (assetNode.isDefined()) {
      Todo.parse(assetNode);
    }
    if (extraNode.isDefined()) {
      Todo.parse(extraNode);
    }
    List<DAENode> effectList = libraryImagesNode.getChildren(Names.EFFECT);
    if (!effectList.isEmpty()) {
      Todo.task("implement parsing of effect list");
    }
    List<DAENode> imageList = libraryImagesNode.getChildren(Names.IMAGE);
    for (DAENode imageNode : imageList) {
      parseImageNode(imageNode, assetManager);
    }
  }

  private void parseImageNode(DAENode imageNode, AssetManager assetManager) {
    TransformedValue<byte[]> imageData = imageNode.getChild(Names.DATA).getContent(HEX_SEQUENCE);
    TransformedValue<String> initFrom = imageNode.getChild(Names.INIT_FROM).getContent(TEXT);
    String id = imageNode.getAttribute(Names.ID, TEXT).get();
    Texture texture = null;
    if (imageData.isDefined()) {

      TransformedValue<String> format = imageNode.getAttribute(Names.FORMAT, TEXT);
      FileType fileType = FileTypeFinder.create().getFileType(imageData.get());
      if (format.isDefined()) {
        id = "mem://" + id + "." + format.get();
      } else if (fileType != FileType.UNKNOWNW) {
        id = "mem://" + id + "." + fileType.getExtension();
      } else {
        id = null;
      }
      if (id != null) {
        assetManager.registerLocator("mem://", MEMAssetLocator.class);
        texture = assetManager.loadTexture(id);
      }
    }
    if (initFrom.isDefined()) {
      DAENode collada = imageNode.getRootNode();
      ColladaInfo colladaInfo = collada.getParsedData(ColladaInfo.class);
      String name = colladaInfo.getInfo().getKey().getName();
      String folder = colladaInfo.getInfo().getKey().getFolder();
      String location = initFrom.get();
      File f = new File(location);


      /**
       *  Added by larynx 2011.03.19
       *  Try to load the texture in this order:
       *  original image <init_from> from the collada file
       *  path of collada file + "/" + original
       *  path of collada file + "/" + file name
       *  file name alone
       */

      TextureKey texkey = new TextureKey(location);
      AssetInfo texinfo = assetManager.locateAsset(texkey);
      if (texinfo == null) {
        location = combinePaths(folder, location);
        texkey = new TextureKey(location);
        texinfo = assetManager.locateAsset(texkey);
        if (texinfo == null) {
          f = new File(location);
          location = combinePaths(folder, f.getName());
          texkey = new TextureKey(location);
          texinfo = assetManager.locateAsset(texkey);

          if (texinfo == null) {
            location = f.getName();
            texkey = new TextureKey(location);
            texinfo = assetManager.locateAsset(texkey);
          }
        }
      }

      if (texinfo != null) {
        try {
          texture = assetManager.loadTexture(location);
        } catch (Exception ex) {
          Logger.getLogger(getClass().getName()).log(Level.INFO, "Exception while loading texture " + location);
        }
      }


      if (texture == null) {
        texture = scanTextureBase(assetManager, imageNode.getRootNode(), f.getName());
      }

      if (texture != null) {
        texture.setWrap(Texture.WrapMode.Repeat);
        texture.setMagFilter(Texture.MagFilter.Bilinear);
        texture.setMinFilter(Texture.MinFilter.Trilinear);
      }
    }

    if (texture == null) {
      Todo.task("Cannot create texture for node " + imageNode);
    } else {
      imageNode.setParsedData(texture);
    }
  }

  private Texture scanTextureBase(AssetManager assetManager, DAENode rootNode, String name) {
    TextureBaseList base = rootNode.getParsedData(TextureBaseList.class);
    if (base != null) {
      for (String b : base) {
        try {
          return assetManager.loadTexture(b + name);
        } catch (Exception ex) {
          Logger.getLogger(getClass().getName()).log(Level.INFO, "Texture " + name + " not in " + b);
        }
      }
    }
    return null;
  }

  public static String combinePaths(String... paths) {
    if (paths.length == 0) {
      return "";
    }

    File combined = new File(paths[0]);

    int i = 1;
    while (i < paths.length) {
      combined = new File(combined, paths[i]);
      ++i;
    }

    // Safer to use '/' as path for zip files and AssetKey.reducePath
    return combined.getPath().replace('\\', '/');
  }
}
