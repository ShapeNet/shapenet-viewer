package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetProcessor;
import com.jme3.asset.cache.AssetCache;

/**
 * Compressed asset
 *
 * @author Angel Chang
 */
public class CompressedAssetKey<T> extends AssetKey<T> {
  AssetKey<T> baseAssetKey;
  String compressionExt;
  public CompressedAssetKey(AssetKey<T> baseAssetKey, String compressionExt) {
    super(baseAssetKey.getName() + "." + compressionExt);
    this.baseAssetKey = baseAssetKey;
    this.compressionExt = compressionExt;
  }

  public String getCompressionExt() {
    return compressionExt;
  }

  public AssetKey<T> getBaseAssetKey() {
    return baseAssetKey;
  }

  public String getExtension() {
    return baseAssetKey.getExtension();
  }

  @Override
  public Class<? extends AssetCache> getCacheType() {
    return baseAssetKey.getCacheType();
  }

  @Override
  public Class<? extends AssetProcessor> getProcessorType() {
    return baseAssetKey.getProcessorType();
  }
}
