package edu.stanford.graphics.shapenet.jme3.asset;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetLoadException;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * AssetInfo that handles compressions
 * @author Angel Chang
 */
public class UncompressedAssetInfo extends AssetInfo {
  public enum CompressionFormat {
    GZIP, BZIP2
  }

  static final Map<String, CompressionFormat> extToCompressionFormatMapping =
      new HashMap<String, CompressionFormat>();
  static {
    extToCompressionFormatMapping.put("bz2", CompressionFormat.BZIP2);
    extToCompressionFormatMapping.put("bzip2", CompressionFormat.BZIP2);
    extToCompressionFormatMapping.put("gzip", CompressionFormat.GZIP);
    extToCompressionFormatMapping.put("gz", CompressionFormat.GZIP);
  }
  static public Set<String> compressedExtensions() {
    return extToCompressionFormatMapping.keySet();
  }

  final CompressionFormat compressionFormat;
  final AssetInfo baseAssetInfo;

  public UncompressedAssetInfo(AssetInfo baseAssetInfo, AssetKey key, CompressionFormat compressionFormat) {
    super(baseAssetInfo.getManager(), key);
    this.baseAssetInfo = baseAssetInfo;
    this.compressionFormat = compressionFormat;
  }

  public static AssetInfo create(AssetInfo baseAssetInfo) {
    if (baseAssetInfo.getKey() instanceof CompressedAssetKey) {
      String ext = ((CompressedAssetKey) baseAssetInfo.getKey()).getCompressionExt();
      CompressionFormat compressionFormat = extToCompressionFormatMapping.get(ext.toLowerCase());
      AssetKey key = ((CompressedAssetKey) baseAssetInfo.getKey()).getBaseAssetKey();
      return new UncompressedAssetInfo(baseAssetInfo, key, compressionFormat);
    } else {
      String ext = baseAssetInfo.getKey().getExtension();
      CompressionFormat compressionFormat = extToCompressionFormatMapping.get(ext.toLowerCase());
      if (compressionFormat != null) {
        String name = baseAssetInfo.getKey().getName();
        String strippedName = name.substring(0, name.length() - ext.length() - 1);
        AssetKey key = new AssetKey( strippedName );
        return new UncompressedAssetInfo(baseAssetInfo, key, compressionFormat);
      } else {
        return baseAssetInfo;
      }
    }
  }

  @Override
  public InputStream openStream() {
    try {
      switch (compressionFormat) {
        case BZIP2:
          return new BZip2CompressorInputStream(baseAssetInfo.openStream());
        case GZIP:
          return new GZIPInputStream(baseAssetInfo.openStream());
        default:
          throw new UnsupportedOperationException("Unsupported compression format: " + compressionFormat);
      }
    } catch (IOException ex) {
      throw new AssetLoadException("Error loading asset: " + baseAssetInfo.getKey(), ex);
    }
  }
}
