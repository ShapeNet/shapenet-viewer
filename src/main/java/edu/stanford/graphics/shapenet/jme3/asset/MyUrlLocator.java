package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.UrlAssetInfo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * My version of the <code>com.jme3.asset.plugins.UrlLocator</code> that checks
 * check that the url start with the rootPath and treats it as a full URL
 * (otherwise, incurs cost of fetch bad content)
 *
 * @author Angel Chang
 */
public class MyUrlLocator implements AssetLocator {

  private static final Logger logger = Logger.getLogger(MyUrlLocator.class.getName());
  private URL root;

  public void setRootPath(String rootPath) {
    if (rootPath == null) {
      throw new IllegalArgumentException("rootPath is required: use '\' to register any url");
    } else if (rootPath == "/") {
      // any url is okay
      root = null;
      return;
    }

    try {
      this.root = new URL(rootPath);
    } catch (MalformedURLException ex) {
      throw new IllegalArgumentException("Invalid rootUrl specified", ex);
    }
  }

  public AssetInfo locate(AssetManager manager, AssetKey key) {
    String name = key.getName();
    if (root == null) {
      try {
        URL url = new URL(name);
        AssetInfo urlAssetInfo = CachedUrlAssetInfo.create(manager, key, url);
        return UncompressedAssetInfo.create(urlAssetInfo);
      } catch (MalformedURLException ex) {
        return null;
      } catch (IOException ex){
        logger.log(Level.WARNING, "Error while locating " + name, ex);
        return null;
      }
    }

    else if (!name.startsWith(root.toExternalForm())) return null;
    try{
      // See if we already have a complete URL
      URL url;
      try {
        url = new URL(name);
      } catch (MalformedURLException ex) {
        if (name.startsWith("/")){
          name = name.substring(1);
        }
        url = new URL(root.toExternalForm() + name);
      }
      AssetInfo urlAssetInfo = CachedUrlAssetInfo.create(manager, key, url);
      return UncompressedAssetInfo.create(urlAssetInfo);
    }catch (FileNotFoundException e){
      return null;
    }catch (IOException ex){
      logger.log(Level.WARNING, "Error while locating " + name, ex);
      return null;
    }
  }


}
