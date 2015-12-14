package edu.stanford.graphics.shapenet.jme3.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.ZipLocator;
import jme3dae.ColladaLoader;
import edu.stanford.graphics.shapenet.jme3.asset.*;
import edu.stanford.graphics.shapenet.util.IOUtils;
import edu.stanford.graphics.shapenet.util.WebCacheUtils;
import edu.stanford.graphics.shapenet.util.WebUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Load a KMZ
 * @author Angel Chang
 */
public class KMZLoader implements AssetLoader {
  ColladaLoader colladaLoader = new ColladaLoader();
  private static final Logger logger = Logger.getLogger(MTLLoader.class.getName());
  private static boolean useTempFile = true;

  @Override
  public Object load(AssetInfo assetInfo) throws IOException {
    if (assetInfo instanceof AssetInfoFile) {
      File file = ((AssetInfoFile) assetInfo).file;
      return loadFromFile(assetInfo, file);
    } else if (assetInfo instanceof CachedUrlAssetInfo) {
      File file = ((CachedUrlAssetInfo) assetInfo).getFile();
      if (file != null) {
        return loadFromFile(assetInfo, file);
      }
    }
    if (useTempFile) {
      File file = null;
      try {
        file = saveToFile(assetInfo);
        Object obj = loadFromFile(assetInfo, file);
        return obj;
      } finally {
        if (file != null) {
          file.delete();
        }
      }
    }
    return loadFromStream(assetInfo);
  }

  public File saveToFile(AssetInfo assetInfo) throws IOException {
    InputStream inputStream = null;
    try {
      File tmpFile = File.createTempFile(assetInfo.getKey().getName(), "kmz");
      tmpFile.deleteOnExit();
      inputStream = assetInfo.openStream();
      WebCacheUtils.fetchAndSave(inputStream, tmpFile);
      return tmpFile;
    } finally {
      if (inputStream != null) {
        inputStream.close();
      }
    }
  }

  public Object loadFromFile(AssetInfo assetInfo, File file) throws IOException {
    String filename = file.getAbsolutePath();
    ZipEntry zipEntry = null;
    Object obj = null;
    AssetManager assetManager = assetInfo.getManager();
    ZipFile zipFile = new ZipFile(file);
    try {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        zipEntry = entries.nextElement();
        String name = zipEntry.getName();
        if (name.endsWith(".dae")) {
          // Ah, found our collada file!!!
          AssetInfo colladaAssetInfo = new ZipEntryAssetInfo(assetInfo.getManager(),
            new ZipAssetKey(assetInfo.getKey(), name), zipFile, zipEntry);
          assetManager.registerLocator(filename, ZipLocator.class);
          obj = colladaLoader.load(colladaAssetInfo);
          assetManager.unregisterLocator(filename, ZipLocator.class);
          break;
        }
      }
    } finally {
      zipFile.close();
    }
    return obj;
  }

  public Object loadFromStream(AssetInfo assetInfo) throws IOException {
    Map<String, byte[]> byteEntries = new HashMap<String, byte[]>();
    ZipEntry zipEntry = null;
    Object obj = null;
    String daeEntryName = null;
    ZipInputStream zipInputSteam = new ZipInputStream(assetInfo.openStream());
    byte[] bytes = new byte[65536];
    try {
      while ((zipEntry = zipInputSteam.getNextEntry()) != null) {
        String name = zipEntry.getName();
        int size = (int) zipEntry.getSize();
        if (size < 0) size = bytes.length;
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream(size);
        int read = 0;
        while ((read = zipInputSteam.read(bytes)) > 0) {
          outputBytes.write(bytes, 0, read);
        }
        byteEntries.put(name, outputBytes.toByteArray());

        if (name.endsWith(".dae")) {
          // Ah, found our collada file!!!
          daeEntryName = name;
        }
        zipInputSteam.closeEntry();
      }
    } finally {
      zipInputSteam.close();
    }

    AssetManager assetManager = assetInfo.getManager();
    if (daeEntryName != null) {
      String filename = assetInfo.getKey().getName();
      AssetInfo colladaAssetInfo = new ByteArrayAssetInfo(assetInfo.getManager(),
          new ZipAssetKey(assetInfo.getKey(), daeEntryName), byteEntries.get(daeEntryName));
      MemBytesLocator.register(filename, byteEntries);
      assetManager.registerLocator(filename, MemBytesLocator.class);
      obj = colladaLoader.load(colladaAssetInfo);
      assetManager.unregisterLocator(filename, MemBytesLocator.class);
      MemBytesLocator.unregister(filename);
    }
    return obj;
  }
}

