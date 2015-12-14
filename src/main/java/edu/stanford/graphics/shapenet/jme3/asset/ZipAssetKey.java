package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetKey;

/**
 * Asset key in Zip file
 *
 * @author Angel Chang
 */
public class ZipAssetKey<T> extends AssetKey<T> {
  AssetKey<T> baseAssetKey;

  public ZipAssetKey(AssetKey<T> baseAssetKey, String name){
    this.baseAssetKey = baseAssetKey;
    this.name = reducePath(name);
    this.extension = getExtension(this.name);
  }

}
