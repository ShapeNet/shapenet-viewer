package jme3dae.utilities;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLocator;
import com.jme3.asset.AssetManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An asset locator that finds assets registered in its cache in byte array form.
 *
 * @author pgi
 */
public class MEMAssetLocator implements AssetLocator {
  private static final Map<String, ByteArrayInputStream> CACHE = new ConcurrentHashMap<String, ByteArrayInputStream>();

  /**
   * Register a key-data pair in the MEMAssetLocator registry
   *
   * @param key  the name of the resource stored. Must begin with "mem://"
   * @param data the data associated with the the key.
   */
  public static void register(final String key, byte[] data) {
    byte[] b = new byte[data.length];
    System.arraycopy(data, 0, b, 0, b.length);
    ByteArrayInputStream bin = new ByteArrayInputStream(b);
    CACHE.put(key, bin);
  }

  public MEMAssetLocator() {
  }

  public void setRootPath(String rootPath) {
  }

  public AssetInfo locate(AssetManager manager, AssetKey key) {
    if (CACHE.containsKey(key.getName())) {
      final ByteArrayInputStream stream = CACHE.get(key.getName());

      return new AssetInfo(manager, key) {

        @Override
        public InputStream openStream() {
          return stream;
        }
      };
    } else {
      return null;
    }
  }
}
