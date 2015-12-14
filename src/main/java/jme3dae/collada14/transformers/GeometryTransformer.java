package jme3dae.collada14.transformers;

import com.jme3.material.Material;
import com.jme3.scene.Geometry;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jme3dae.DAENode;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.collada14.ColladaSpec141.Semantic;
import jme3dae.utilities.Bindings;
import jme3dae.utilities.Todo;
import jme3dae.utilities.TransformerPack;
import jme3dae.utilities.Tuple2;

/**
 * Base class for the transformers of geometric elements. Defines few methods
 * used by the "real" transformers.
 *
 * @param <A> The value transformed by this transformer
 * @param <B> the result of the transformation.
 * @author pgi
 * @see jme3dae.collada14.transformers.TrianglesTransformer
 * @see jme3dae.collada14.transformers.PolygonsTransformer
 * @see jme3dae.collada14.transformers.PolylistTransformer
 */
public abstract class GeometryTransformer<A, B> implements TransformerPack<A, B> {

  private static final InputSharedTransformer INPUT_SHARED = InputSharedTransformer.create();

  /**
   * Given a node that holds a list of collada input-shared elements, returns the size
   * of the primitive data chunk required to hold one index for each input and a the
   * list of InputShared elements that maps each input-shared element.
   *
   * @param inputParent the DAENode holding a set of input-shared elements (eg
   *                    triangles, polygons, polylist).
   * @return an integer-list pair where the integer is the size of a chunk holding
   * one index of each input element and the list is a list of the InputShared instances
   * mapping each input-shared element. This method is used during geometry construction to
   * transform the offset-index list structure of collada into a set of triangles suitable
   * to fill a JME3 Mesh object.
   */
  protected Tuple2<Integer, List<InputShared>> getInputs(DAENode inputParent) {
    List<InputShared> inputs = new LinkedList<InputShared>();
    int chunkSize = 0;
    for (DAENode input : inputParent.getChildren(Names.INPUT)) {
      // Changed by larynx to get the normals also (to make it work with Sketchup 8)

      // Get POSITION
      TransformedValue<InputShared> buffer = INPUT_SHARED.transform(input);
      if (buffer.isDefined()) {
        inputs.add(buffer.get());

        if (buffer.get().getSemantic() == Semantic.VERTEX) {
          // Fetch normals also
          // Get NORMAL
          TransformedValue<InputShared> bufferN = INPUT_SHARED.transformNormal(input);
          if (bufferN.isDefined()) {
            inputs.add(bufferN.get());
            chunkSize = Math.max(chunkSize, bufferN.get().getOffset());
          }
        }

        chunkSize = Math.max(chunkSize, buffer.get().getOffset());
      }
    }
    chunkSize += 1;
    return Tuple2.create(chunkSize, inputs);
  }

  /**
   * This utility functions searches and applies a material to a geometry.
   *
   * @param geom         the jme3 geometry that will receive the material
   * @param geomDataNode the collada node holding the material name attribute. This attribute
   *                     identifies an instance material in a visual scene. The instance material is the instantiation
   *                     of a material library element. This method also resolves (well, it will in the future) the
   *                     parameter bindings between a geometric element and an instance material.
   * @param bindings     a table with at least a DAENode element under the Names.BIND_MATERIAL tag.
   *                     The DAENode is the node wrapping a collada bind_material element.
   */
  protected void applyMaterial(Geometry geom, DAENode geomDataNode, Bindings bindings) {
    TransformedValue<String> linkedSymbol = geomDataNode.getAttribute(Names.MATERIAL, TEXT);
    if (linkedSymbol.isDefined()) {
      DAENode bindMaterial = bindings.get(Names.BIND_MATERIAL, DAENode.class);
      List<DAENode> instanceMaterials = bindMaterial.getChild(Names.TECHNIQUE_COMMON).getChildren(Names.INSTANCE_MATERIAL);
      Map<String, DAENode> instanceMaterialsForSymbol = new HashMap<String, DAENode>();
      for (DAENode mat : instanceMaterials) {
        TransformedValue<String> symbol = mat.getAttribute(Names.SYMBOL, TEXT);
        if (symbol.isDefined()) {
          instanceMaterialsForSymbol.put(symbol.get(), mat);
        }
      }
      DAENode instanceMaterial = instanceMaterialsForSymbol.get(linkedSymbol.get());
      if (instanceMaterial == null) {
        instanceMaterial = DAENode.NONE;
        Todo.task("Material symbol not found, trying to guess it...");
        for (String string : instanceMaterialsForSymbol.keySet()) {
          if (string.toLowerCase().startsWith(linkedSymbol.get())) {
            instanceMaterial = instanceMaterialsForSymbol.get(string);
            Todo.task("Guessed " + string);
            break;
          }
        }
      }
      TransformedValue<String> target = instanceMaterial.getAttribute(Names.TARGET, TEXT);
      if (target.isDefined()) {
        DAENode material = bindMaterial.getLinkedNode(target.get());
        if (material.isDefined()) {
          Material parsedData = material.getParsedData(Material.class);
          Material clone = parsedData.clone();
          geom.setMaterial(clone);
        }
      }
    }
  }

  /**
   * Merges two arrays.
   *
   * @param head the first part of the new array
   * @param tail the second part of the new array
   * @return a new array composed by the elements of head followed by the elements
   * of tail.
   */
  protected int[] merge(int[] head, int[] tail) {
    int[] buffer = new int[head.length + tail.length];
    System.arraycopy(head, 0, buffer, 0, head.length);
    System.arraycopy(tail, 0, buffer, head.length, tail.length);
    return buffer;
  }
}
