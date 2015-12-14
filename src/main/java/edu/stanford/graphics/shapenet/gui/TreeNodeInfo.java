package edu.stanford.graphics.shapenet.gui;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class including information about a tree node
 */
public class TreeNodeInfo<T> {
  public String name;
  public T value;
  public Map<String,Object> data;

  public TreeNodeInfo(String name, T value) {
    this.name = name;
    this.value = value;
  }

  public String toString() {
    return name;
  }

  public <V> V get(String key) {
    if (data != null) {
      return (V) data.get(key);
    } else return null;
  }

  public <V> void put(String key, V v) {
    if (data == null) {
      data = new HashMap<String,Object>();
    }
    data.put(key,v);
  }
}
