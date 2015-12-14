package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class AssetInfoFile extends AssetInfo {
  public final File file;

  public AssetInfoFile(AssetManager manager, AssetKey key, File file){
    super(manager, key);
    this.file = file;
  }

  @Override
  public InputStream openStream() {
    try{
      return new FileInputStream(file);
    } catch (FileNotFoundException ex){
      // NOTE: Can still happen even if file.exists() is true, e.g.
      // permissions issue and similar
      throw new AssetLoadException("Failed to open file: " + file, ex);
    }
  }
}

