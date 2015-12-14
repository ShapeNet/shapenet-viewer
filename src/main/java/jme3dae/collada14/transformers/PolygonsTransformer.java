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
 * Transformed a collada polygons element in a JME3 Geometry.
 *
 * @author pgi
 */
public class PolygonsTransformer extends GeometryTransformer<Tuple2<DAENode, Bindings>, Geometry> {

  /**
   * Instance creator.
   *
   * @return a new PolygonsTransformer instance.
   */
  public static PolygonsTransformer create() {
    return new PolygonsTransformer();
  }

  private PolygonsTransformer() {
  }

  /**
   * Transforms a DAENode-Bindings pair in a JME3 geometry. The DAENode wraps a
   * collada polygons element, the bindings hold a collada bind_material element
   * under the BIND_MATERIAL string key.
   *
   * @param value a polygons-bind_material pair
   * @return a JME3 Geometry or an undefined value if the transformation fails.
   */
  public TransformedValue<Geometry> transform(Tuple2<DAENode, Bindings> value) {
    Geometry geom = null;
    DAENode polys = value.getA();
    Bindings bindings = value.getB();
    Tuple2<Integer, List<InputShared>> inputData = super.getInputs(polys);
    int chunkSize = inputData.getA();
    List<InputShared> inputs = inputData.getB();
    PolygonData[] polygons = new PolygonData[polys.getAttribute(Names.COUNT, INTEGER).get()];
    int polyIndex = 0;
    int[] pvalues = new int[0];
    for (DAENode pNode : polys.getChildren(Names.P)) {
      int[] indices = pNode.getContent(INTEGER_LIST).get();
      int vertexCount = indices.length / chunkSize;
      polygons[polyIndex] = PolygonData.create(vertexCount);
      polyIndex += 1;
      pvalues = super.merge(pvalues, indices);
    }
    for (InputShared inputShared : inputs) {
      inputShared.transferData(chunkSize, pvalues, polygons);
    }
    MeasuringUnit unit = polys.getRootNode().getParsedData(MeasuringUnit.class);
    TransformedValue<Tuple2<Mesh, PolygonData[]>> mesh = PolygonArrayTransformer.create().transform(Tuple2.create(unit, polygons));
    if (mesh.isDefined()) {
      polys.setParsedData(mesh.get().getB());
      polys.setParsedData(mesh.get().getA());
      geom = new Geometry("model");
      geom.setMesh(mesh.get().getA());
      geom.setModelBound(new BoundingBox());
      geom.updateModelBound();
      applyMaterial(geom, polys, bindings);
    } else {
      Todo.task("cannot create mesh, check this out.");
    }
    Todo.implementThis();
    return TransformedValue.create(geom);
  }
}
