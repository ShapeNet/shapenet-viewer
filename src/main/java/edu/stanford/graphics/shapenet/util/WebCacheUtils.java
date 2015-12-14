package edu.stanford.graphics.shapenet.util;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Utility functions for fetching files from the web and caching them on disk
 *
 * @author Angel Chang
 */
public class WebCacheUtils {
  private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WebCacheUtils.class);

  public static long DEFAULT_DELAY_BETWEEN_RETRIES = 1000; // In milliseconds
  private static int BUFFER_SIZE = 65536;

  public static URL checkedFetchAndSave(String urlPath, String destinationFile, long expires) throws IOException {
    return checkedFetchAndSave(urlPath, destinationFile, 1, expires);
  }

  public static URL checkedFetchAndSave(String urlPath, String destinationFile, int retries, long expires) throws IOException {
    return checkedFetchAndSave(urlPath, destinationFile, retries, DEFAULT_DELAY_BETWEEN_RETRIES, expires);
  }

  public static URL checkedFetchAndSave(String urlPath, String destinationFile, int retries, long delay, long expires) throws IOException {
    File file = new File(destinationFile);
    // File already exists, don't fetch
    if (file.canRead()) {
      boolean expired = expires <= System.currentTimeMillis() && file.lastModified() <= expires;
      if (!expired) return null;
    }
    return fetchAndSave(urlPath, file, retries, delay);
  }

  public static URL fetchAndSave(String urlPath, String destinationFile) throws IOException {
    File file = new File(destinationFile);
    return fetchAndSave(urlPath, file);
  }

  public static URL fetchAndSave(String urlPath, String destinationFile, int retries) throws IOException {
    return fetchAndSave(urlPath, destinationFile, retries, DEFAULT_DELAY_BETWEEN_RETRIES);
  }

  public static URL fetchAndSave(String urlPath, String destinationFile, int retries, long delay) throws IOException {
    return fetchAndSave(urlPath, new File(destinationFile), retries, delay);
  }

  public static long fetchAndSave(InputStream is, File file) throws IOException {
    // Save to temporary file
    boolean useTempFile = false;
    File tmpFile = null;
    if (useTempFile) {
      tmpFile = File.createTempFile("webcacheutils", "cache");
      tmpFile.deleteOnExit();
    }
    OutputStream os = new BufferedOutputStream(new FileOutputStream(
      (tmpFile != null)? tmpFile: file), BUFFER_SIZE);

    byte[] b = new byte[BUFFER_SIZE];
    int length;
    long total = 0;

    while ((length = is.read(b)) != -1) {
      os.write(b, 0, length);
      total += length;
    }

    is.close();
    os.close();
    if (tmpFile != null) {
      // move temporary file
      // For some reason this rename doesn't work
      boolean ok = tmpFile.renameTo(file);
      if (!ok) {
        logger.warn("Cannot rename " + tmpFile + " to " + file);
      }
    }
    return total;
  }

  public static URL fetchAndSave(String urlPath, File file) throws IOException {
    logger.debug("Fetching from URL: " + urlPath);
    File parent = file.getParentFile();
    parent.mkdirs();

    URL url = new URL(urlPath);
    URLConnection connection = url.openConnection();
    // Actual URL (maybe redirected)
    URL redirectedURL = connection.getURL();
    InputStream is = new BufferedInputStream(connection.getInputStream(), BUFFER_SIZE);
    fetchAndSave(is, file);
    return redirectedURL;
  }

  public static URL fetchAndSave(String urlPath, File file, int retries) throws IOException {
    return fetchAndSave(urlPath, file, retries, DEFAULT_DELAY_BETWEEN_RETRIES);
  }

  public static URL fetchAndSave(String urlPath, File file, int retries, long delay) throws IOException {
    for (int n = 0; n < retries; n++) {
      try {
        return fetchAndSave(urlPath, file);
      } catch (IOException ex) {
        if (n+1 < retries) {
          //okay
          try{
            logger.debug("Sleeping for " + delay + " ms");
            Thread.currentThread().sleep(delay);
          }
          catch(InterruptedException ie){}
        } else {
          throw new IOException("Error fetching from " + urlPath, ex);
        }
      }
    }
    return null;
  }
}
