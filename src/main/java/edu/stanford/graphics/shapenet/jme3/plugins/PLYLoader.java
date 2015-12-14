package edu.stanford.graphics.shapenet.jme3.plugins;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.mesh.IndexBuffer;
import com.jme3.scene.mesh.IndexIntBuffer;
import com.jme3.scene.mesh.IndexShortBuffer;
import com.jme3.util.BufferUtils;
import org.smurn.jply.*;
import org.smurn.jply.util.NormalMode;
import org.smurn.jply.util.NormalizingPlyReader;
import org.smurn.jply.util.TesselationMode;
import org.smurn.jply.util.TextureMode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PLY loader using http://jply.smurn.org/manual.html
 *
 * @author Angel Chang
 */
public final class PLYLoader implements AssetLoader {
  private static final Logger logger = Logger.getLogger(PLYLoader.class.getName());

  public Object load(AssetInfo info) throws IOException {
    Object obj = null;
    InputStream in = null;
    try {
      in = info.openStream();
      Geometry geom = parse(in, info.getKey().getName(), info.getManager());
      obj = geom;
    } finally {
      if (in != null) {
        in.close();
      }
    }
    return obj;
  }

  public Geometry parse(InputStream in, String name, AssetManager assetManager) throws IOException {
    PlyReader ply = new PlyReaderFile(in);
    // Normalize so we have triangles and normals
    ply = new NormalizingPlyReader(ply,
      TesselationMode.TRIANGLES,
      NormalMode.ADD_NORMALS_CCW,
      TextureMode.XY
    );
    PlyInfo plyinfo = new PlyInfo();
    Mesh mesh = createMesh(ply, plyinfo);
    ply.close();

    Geometry geom = new Geometry(name, mesh);
    Material material = null;
    if (material == null){
      // create default material
      if (plyinfo.hasColor) {
        material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setBoolean("VertexColor", true);
      } else {
        material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setFloat("Shininess", 64);
      }
    }
    geom.setMaterial(material);
    if (material.isTransparent())
      geom.setQueueBucket(RenderQueue.Bucket.Transparent);
    else
      geom.setQueueBucket(RenderQueue.Bucket.Opaque);

    if (material.getMaterialDef().getName().contains("Lighting")
      && mesh.getFloatBuffer(VertexBuffer.Type.Normal) == null){
      logger.log(Level.WARNING, "OBJ mesh {0} doesn't contain normals! "
        + "It might not display correctly", geom.getName());
    }

    return geom;
  }

  private static class PlyInfo {
    int numberOfVertices;
    int numberOfFaces;
    boolean hasTexCoord;
    boolean hasNormals;
    boolean hasColor;
  }

  private Mesh createMesh(PlyReader ply, PlyInfo plyinfo) throws IOException {
    // get basic ply info
    int numberOfVertices = ply.getElementCount("vertex");
    int numberOfFaces = ply.getElementCount("face");

    Mesh m = new Mesh();
    m.setMode(Mesh.Mode.Triangles);

    boolean hasTexCoord = false;
    boolean hasNormals = false;
    boolean hasColor = false;
    boolean hasAlpha = false;

    for (ElementType elementType : ply.getElementTypes()) {
      if (elementType.getName().equals("vertex")) {
        List<Property> props = elementType.getProperties();
        Map<String, Property> propMap = new HashMap<>();
        for (Property prop : props) {
          propMap.put(prop.getName(), prop);
        }
        if (propMap.containsKey("red") && propMap.containsKey("green") && propMap.containsKey("blue")) {
          hasColor = true;
        }
        if (propMap.containsKey("nx") && propMap.containsKey("ny") && propMap.containsKey("nz")) {
          hasNormals = true;
        }
        if (propMap.containsKey("alpha")) {
          hasAlpha = true;
        }
      }
    }

    if (plyinfo != null) {
      plyinfo.hasColor = hasColor;
      plyinfo.hasNormals = hasNormals;
      plyinfo.hasTexCoord = hasTexCoord;
      plyinfo.numberOfFaces = numberOfFaces;
      plyinfo.numberOfVertices = numberOfVertices;
    }

    // Create mesh
    FloatBuffer posBuf = BufferUtils.createFloatBuffer(numberOfVertices * 3);
    FloatBuffer normBuf = null;
    FloatBuffer tcBuf = null;
    FloatBuffer colorBuf = null;
    IndexBuffer indexBuf = null;

    if (hasNormals) {
      normBuf = BufferUtils.createFloatBuffer(numberOfVertices * 3);
      m.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
    }
    if (hasTexCoord) {
      tcBuf = BufferUtils.createFloatBuffer(numberOfVertices * 2);
      m.setBuffer(VertexBuffer.Type.TexCoord, 2, tcBuf);
    }
    if (hasColor) {
      colorBuf = BufferUtils.createFloatBuffer(numberOfVertices * 4);
      m.setBuffer(VertexBuffer.Type.Color, 4, colorBuf);
    }

    if (numberOfVertices >= 65536) {
      // too many vertices: use intbuffer instead of shortbuffer
      IntBuffer ib = BufferUtils.createIntBuffer(numberOfFaces * 3);
      m.setBuffer(VertexBuffer.Type.Index, 3, ib);
      indexBuf = new IndexIntBuffer(ib);
    } else {
      ShortBuffer sb = BufferUtils.createShortBuffer(numberOfFaces * 3);
      m.setBuffer(VertexBuffer.Type.Index, 3, sb);
      indexBuf = new IndexShortBuffer(sb);
    }

    // The elements are stored in order of their types. For each
    // type we get a reader that reads the elements of this type.
    ElementReader reader = ply.nextElementReader();
    while (reader != null) {
      ElementType type = reader.getElementType();
      // In PLY files vertices always have a type named "vertex".
      if (type.getName().equals("vertex")) {
        Element element = reader.readElement();
        while (element != null) {
          double x = element.getDouble("x");
          double y = element.getDouble("y");
          double z = element.getDouble("z");
          posBuf.put((float) x).put((float) y).put((float) z);

          if (hasNormals) {
            double nx = element.getDouble("nx");
            double ny = element.getDouble("ny");
            double nz = element.getDouble("nz");
            normBuf.put((float) nx).put((float) ny).put((float) nz);
          }
          if (hasColor) {
            double r = element.getDouble("red") / 255.0;
            double g = element.getDouble("green") / 255.0;
            double b = element.getDouble("blue") / 255.0;
            double a = hasAlpha ? element.getDouble("alpha") / 255.0 : 1.0;
            colorBuf.put((float) r).put((float) g).put((float) b).put((float) a);
          }
          if (hasTexCoord) {

          }
          element = reader.readElement();
        }
      } else if (type.getName().equals("face")) {
        Element element = reader.readElement();
        int i = 0;
        while (element != null) {
          int[] indices = element.getIntList("vertex_index");
          for (int index : indices) {
            indexBuf.put(i, index);
            i++;
          }
          element = reader.readElement();
        }
      }

      // Close the reader for the current type before getting the next one.
      reader.close();
      reader = ply.nextElementReader();
    }

    m.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
    // index buffer and others were set on creation

    m.setStatic();
    m.updateBound();
    m.updateCounts();

    return m;
  }
}
