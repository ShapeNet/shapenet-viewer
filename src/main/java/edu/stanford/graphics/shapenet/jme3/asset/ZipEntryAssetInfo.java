package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Zip entry asset info
 */
public class ZipEntryAssetInfo extends AssetInfo {
  private final ZipFile zipFile;
  private final ZipEntry entry;

  public ZipEntryAssetInfo(AssetManager manager, AssetKey key, ZipFile zipFile, ZipEntry entry) {
    super(manager, key);
    this.zipFile = zipFile;
    this.entry = entry;
  }

  public InputStream openStream() {
    try {
      return this.zipFile.getInputStream(this.entry);
    } catch (IOException ex) {
      throw new AssetLoadException("Failed to load zip entry: " + this.entry, ex);
    }
  }
}
