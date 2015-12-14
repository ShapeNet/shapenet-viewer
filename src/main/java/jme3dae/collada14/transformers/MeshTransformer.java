package jme3dae.collada14.transformers;

import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.TangentBinormalGenerator;

import java.util.LinkedList;
import java.util.List;

import jme3dae.DAENode;
import jme3dae.FXEnhancerInfo;
import jme3dae.collada14.ColladaSpec141.Names;
import jme3dae.utilities.Bindings;
import jme3dae.utilities.Conditions;
import jme3dae.utilities.Todo;
import jme3dae.utilities.TransformerPack;
import jme3dae.utilities.Tuple2;

/**
 * Transforms a collada mesh element into a jme3 node. The mesh element is transformed into a Node
 * (and not a Mesh or Geometry) because a collada mesh cannot always be mapped to a single JME3 mesh (eg
 * when per face materials are involved). Well, maybe a single jme3 mesh could be used implementing per-face
 * materials with a glsl shader but that's far far over my glsl skill.
 *
 * @author pgi
 */
public class MeshTransformer implements TransformerPack<Tuple2<DAENode, Bindings>, Node> {

  /**
   * Instance creator
   *
   * @return a new MeshTransformer instance.
   */
  public static MeshTransformer create() {
    return new MeshTransformer();
  }

  private final TrianglesTransformer TRIANGLES = TrianglesTransformer.create();
  private final PolylistTransformer POLYLIST = PolylistTransformer.create();
  private final PolygonsTransformer POLYGONS = PolygonsTransformer.create();

  private MeshTransformer() {
  }

  /**
   * Transforms a mesh element - bindings pair into a jme3 node object
   *
   * @param value the mesh - bindings pair. The bindings holds a reference
   *              to the DAENode bind_material referenced by the mesh triangles.
   * @return a jme3 Node holding the result of the Collada mesh element transformation.
   */
  public TransformedValue<Node> transform(Tuple2<DAENode, Bindings> value) {
    DAENode mesh = value.getA();
    Bindings bindings = value.getB();
    Conditions.checkTrue(mesh.hasName(Names.MESH));
    Conditions.checkNotNull(bindings);
    Node node = null;
    List<Geometry> geometries = new LinkedList<Geometry>();

    for (DAENode lines : mesh.getChildren(Names.LINES)) {
      Todo.task("parse lines");
    }

    for (DAENode linestrips : mesh.getChildren(Names.LINESTRIPS)) {
      Todo.task("parse linestrips");
    }

    for (DAENode polygons : mesh.getChildren(Names.POLYGONS)) {
      TransformedValue<Geometry> geom = POLYGONS.transform(Tuple2.create(polygons, bindings));
      if (geom.isDefined()) {
        geometries.add(geom.get());
      }
    }

    for (DAENode polylist : mesh.getChildren(Names.POLYLIST)) {
      TransformedValue<Geometry> geom = POLYLIST.transform(Tuple2.create(polylist, bindings));
      if (geom.isDefined()) {
        geometries.add(geom.get());
      }
    }

    for (DAENode triangles : mesh.getChildren(Names.TRIANGLES)) {
      TransformedValue<Geometry> geom = TRIANGLES.transform(Tuple2.create(triangles, bindings));
      if (geom.isDefined()) {
        geometries.add(geom.get());
      }
    }

    for (DAENode trifans : mesh.getChildren(Names.TRIFANS)) {
      Todo.task("parse trifans");
    }

    for (DAENode tristrips : mesh.getChildren(Names.TRISTRIPS)) {
      Todo.task("parse tristrips");
    }

    for (DAENode extra : mesh.getChildren(Names.EXTRA)) {
      Todo.task("parse extra");
    }

    if (!geometries.isEmpty()) {
      node = new Node();
      FXEnhancerInfo fx = mesh.getRootNode().getParsedData(FXEnhancerInfo.class);
      for (Geometry geometry : geometries) {
        if (fx.getAutoBump()) {
          generateTangentBinormals(geometry);
        }
        node.attachChild(geometry);
        if (geometry.getModelBound() == null) {
          geometry.setModelBound(new BoundingBox());
        }
        geometry.updateModelBound();
      }
    }

    return TransformedValue.create(node);
  }

  private void generateTangentBinormals(Geometry geometry) {
    Mesh mesh = geometry.getMesh();
    VertexBuffer bibuffer = mesh.getBuffer(Type.Binormal);
    VertexBuffer tanbuffer = mesh.getBuffer(Type.Tangent);
    VertexBuffer texCoordBuffer = mesh.getBuffer(Type.TexCoord);
    VertexBuffer coordBuffer = mesh.getBuffer(Type.Position);
    if (mesh != null && bibuffer == null || tanbuffer == null && texCoordBuffer != null && coordBuffer != null) {
      try {
        TangentBinormalGenerator.generate(mesh);
      } catch (NullPointerException ex) {
        Todo.task("Check why this generates a npe");
      } catch (IllegalArgumentException e) {
        Todo.task("Check why this generates a iae");
      }
    }
  }
}
