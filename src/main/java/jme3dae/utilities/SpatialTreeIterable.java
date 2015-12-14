package jme3dae.utilities;

import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import java.util.Iterator;
import java.util.LinkedList;

public class SpatialTreeIterable implements Iterable<Spatial> {
  private final Spatial root;

  public SpatialTreeIterable(Spatial root) {
    this.root = root;
  }

  public Iterator<Spatial> iterator() {
    final LinkedList<Spatial> list = new LinkedList<Spatial>();
    list.add(root);
    return new Iterator<Spatial>() {

      public boolean hasNext() {
        return !list.isEmpty();
      }

      public Spatial next() {
        Spatial s = list.pop();
        if (s instanceof Node) list.addAll(((Node) s).getChildren());
        return s;
      }

      public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
      }
    };
  }

}
