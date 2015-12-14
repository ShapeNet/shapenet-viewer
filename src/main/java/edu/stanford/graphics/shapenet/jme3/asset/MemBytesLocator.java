package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create AssetInfo from to some in memory bytes
 */
public class MemBytesLocator implements AssetLocator {
  private static final Map<String, Map<String, byte[]>> cache = new ConcurrentHashMap<String, Map<String, byte[]>>();

  private String rootPath;

  public static final void register(String prefix, Map<String, byte[]> map) {
    cache.put(prefix, map);
  }

  public static final void unregister(String prefix) {
    cache.remove(prefix);
  }

  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  public AssetInfo locate(AssetManager manager, AssetKey key) {
    final Map<String, byte[]> map = cache.get(rootPath);
    if (map != null) {
      final byte[] bytes = map.get(key.getName());
      if (bytes != null) {
        return new ByteArrayAssetInfo(manager, key, bytes);
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

}
