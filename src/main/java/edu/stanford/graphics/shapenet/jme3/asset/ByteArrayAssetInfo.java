package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Byte array asset info
 *
 * @author Angel Chang
 */
public class ByteArrayAssetInfo extends AssetInfo {
  private final byte[] bytes;

  public ByteArrayAssetInfo(AssetManager manager, AssetKey key, byte[] bytes) {
    super(manager, key);
    this.bytes = bytes;
  }

  public InputStream openStream() {
    return new ByteArrayInputStream(bytes);
  }
}
