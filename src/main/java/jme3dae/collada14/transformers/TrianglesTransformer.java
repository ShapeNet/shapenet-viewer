package jme3dae.collada14.transformers;

import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Mesh;
import jme3dae.utilities.Bindings;
import jme3dae.utilities.Tuple2;
import com.jme3.scene.Geometry;

import java.util.List;

import jme3dae.DAENode;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.utilities.Conditions;
import jme3dae.utilities.MeasuringUnit;
import jme3dae.utilities.PolygonArrayTransformer;
import jme3dae.utilities.Todo;

/**
 * Transforms a collada 1.4.1 triangles element into a JME3 Mesh.
 *
 * @author pgi
 */
public class TrianglesTransformer extends GeometryTransformer<Tuple2<DAENode, Bindings>, Geometry> {

  /**
   * Instance creator
   *
   * @return a new TrianglesTransformer instance
   */
  public static TrianglesTransformer create() {
    return new TrianglesTransformer();
  }

  private TrianglesTransformer() {
  }

  /**
   * Transforms a triangles element in a jme3 geometry.
   *
   * @param value a daenode-bindings pair where the daenode wraps a triangles element
   *              and the bindings contains informations required to parse the parameters that the
   *              triangles element defines for the instance material bound to the geometry.
   * @return a jme3 geometry or an undefined value if the parsing fails.
   */
  public TransformedValue<Geometry> transform(Tuple2<DAENode, Bindings> value) {
    Geometry geom = null;
    DAENode triangles = value.getA();
    Bindings bindings = value.getB();
    Tuple2<Integer, List<InputShared>> inputData = getInputs(triangles);
    List<InputShared> inputs = inputData.getB();
    int chunkSize = inputData.getA();
    TransformedValue<Integer> triangleCount = triangles.getAttribute(Names.COUNT, INTEGER);
    Conditions.checkTrue(triangleCount.isDefined(), "Collada 1.4.1 requires triangle count attribute for triangle element");

    TransformedValue<int[]> primitives = triangles.getChild(Names.P).getContent(INTEGER_LIST);
    if (primitives.isDefined()) {
      int[] indices = primitives.get();
      PolygonData[] polygons = new PolygonData[triangleCount.get()];
      for (int i = 0; i < polygons.length; i++) {
        polygons[i] = PolygonData.create(3);
      }
      for (InputShared inputShared : inputs) {
        inputShared.transferData(chunkSize, indices, polygons);
      }
      MeasuringUnit unit = triangles.getRootNode().getParsedData(MeasuringUnit.class);
      TransformedValue<Tuple2<Mesh, PolygonData[]>> mesh = PolygonArrayTransformer.create().transform(Tuple2.create(unit, polygons));
      if (mesh.isDefined()) {
        triangles.setParsedData(mesh.get().getB());
        triangles.setParsedData(mesh.get().getA());
        geom = new Geometry("model");
        geom.setMesh(mesh.get().getA());
        geom.setModelBound(new BoundingBox());
        geom.updateModelBound();
        applyMaterial(geom, triangles, bindings);
      } else {
        Todo.task("unable to generate mesh, AHHHH!");
      }
    } else {
      Todo.task("triangles element has no p child. Maybe extra element contains data?");
    }
    return TransformedValue.create(geom);
  }
}
