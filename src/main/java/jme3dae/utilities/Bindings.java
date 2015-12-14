package jme3dae.utilities;

import java.util.HashMap;
import java.util.Map;

/**
 * A value holder. Currently it is used to bind material symbols to
 * material values.
 *
 * @author pgi
 */
public class Bindings {

  /**
   * Instance creator.
   *
   * @return a new Bindings.
   */
  public static Bindings create() {
    return new Bindings();
  }

  private final Map<String, Object> MAP = new HashMap<String, Object>();

  private Bindings() {
  }

  /**
   * Put a key-value pair in the bindings
   *
   * @param key   the key
   * @param value the value
   */
  public void put(String key, Object value) {
    MAP.put(key, value);
  }

  /**
   * Returns a value given a key
   *
   * @param <T>  the type of the value to get
   * @param key  the key of the value
   * @param type the type of the requested value
   * @return the typed value associated to key or null if no such value
   * exists or the value type is not <: T
   */
  public <T> T get(String key, Class<T> type) {
    T result = null;
    Object value = MAP.get(key);
    if (value != null && type.isAssignableFrom(value.getClass())) {
      result = type.cast(value);
    }
    return result;
  }

  /**
   * Checks if this bindings contains a value associated with the given key
   * of type V <: T
   *
   * @param <T>  the type of the value to check
   * @param key  the key
   * @param type the type of the value
   * @return true if there is a value under the given key of type V <: T
   */
  public <T> boolean contains(String key, Class<T> type) {
    return get(key, type) != null;
  }
}
