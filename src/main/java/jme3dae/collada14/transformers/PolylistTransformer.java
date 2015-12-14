package jme3dae.collada14.transformers;

import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;

import java.util.List;

import jme3dae.DAENode;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.transformers.ValueTransformer.TransformedValue;
import jme3dae.utilities.Bindings;
import jme3dae.utilities.MeasuringUnit;
import jme3dae.utilities.PolygonArrayTransformer;
import jme3dae.utilities.Todo;
import jme3dae.utilities.Tuple2;

/**
 * Transforms a collada polylist element in a JME3 geometry.
 *
 * @author pgi
 */
public class PolylistTransformer extends GeometryTransformer<Tuple2<DAENode, Bindings>, Geometry> {

  /**
   * Instance creator.
   *
   * @return a new PolylistTransformer
   */
  public static PolylistTransformer create() {
    return new PolylistTransformer();
  }

  private PolylistTransformer() {
  }

  /**
   * Transforms a polylist-bind_material pair into a JME3 geometry.
   *
   * @param value a pair of values where the DAENode is a polylist collada node and
   *              the bindings holds a bind_material DAENode instance under the key Names.BIND_MATERIAL.
   * @return a JME3 geometry or an undefined value if the transformation fails.
   */
  public TransformedValue<Geometry> transform(Tuple2<DAENode, Bindings> value) {
    Geometry geom = null;
    DAENode poly = value.getA();
    Bindings bindings = value.getB();
    Tuple2<Integer, List<InputShared>> inputData = getInputs(poly);
    List<InputShared> inputs = inputData.getB();
    int chunkSize = inputData.getA();
    PolygonData[] polygons = new PolygonData[poly.getAttribute(Names.COUNT, INTEGER).get()];
    TransformedValue<int[]> vcount = poly.getChild(Names.VCOUNT).getContent(INTEGER_LIST);
    if (vcount.isDefined()) {
      int[] vc = vcount.get();
      for (int i = 0; i < vc.length; i++) {
        polygons[i] = PolygonData.create(vc[i]);
      }
      TransformedValue<int[]> pvalues = poly.getChild(Names.P).getContent(INTEGER_LIST);
      if (pvalues.isDefined()) {
        int[] p = pvalues.get();
        for (InputShared inputShared : inputs) {
          inputShared.transferData(chunkSize, p, polygons);
        }
        MeasuringUnit unit = poly.getRootNode().getParsedData(MeasuringUnit.class);
        TransformedValue<Tuple2<Mesh, PolygonData[]>> mesh = PolygonArrayTransformer.create().transform(Tuple2.create(unit, polygons));
        if (mesh.isDefined()) {
          poly.setParsedData(mesh.get().getB());
          poly.setParsedData(mesh.get().getA());
          geom = new Geometry("model");
          geom.setMesh(mesh.get().getA());
          geom.setModelBound(new BoundingBox());
          geom.updateModelBound();
          applyMaterial(geom, poly, bindings);
        } else {
          Todo.task("cannot generate mesh... why");
        }
      } else {
        Todo.task("polylist has no p data");
      }
    } else {
      Todo.task("polylist element has no vcount data.");
    }
    return TransformedValue.create(geom);
  }
}
