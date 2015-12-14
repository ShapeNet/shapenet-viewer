package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import com.jme3.asset.AssetManager;
import edu.stanford.graphics.shapenet.util.WebUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

// URL asset info that is cached
public class CachedUrlAssetInfo extends AssetInfo {
  private URL url;
  private File cachedFile;

  public static CachedUrlAssetInfo create(AssetManager assetManager, AssetKey key, URL url) throws IOException {
    return new CachedUrlAssetInfo(assetManager, key, url);
  }

  public File getFile() {
    if (cachedFile == null) {
      cachedFile = WebUtils.cachedFile(url);
    }
    return cachedFile;
  }

  private CachedUrlAssetInfo(AssetManager assetManager, AssetKey key, URL url) throws IOException {
    super(assetManager, key);
    this.url = url;
  }

  private static Pair<InputStream,File> getInputStream(URL url) {
    scala.Tuple2<InputStream,File> tuple = WebUtils.inputStreamWithCachedFile(url).getOrElse(null);
    if (tuple != null) {
      return Pair.of(tuple._1(), tuple._2());
    } else return null;
  }

  public InputStream openStream() {
    Pair<InputStream,File> pair = getInputStream(url);
    if (pair == null) {
      throw new AssetLoadException("Failed to read URL " + this.url);
    }
    this.cachedFile = pair.getRight();
    return pair.getLeft();
  }
}
