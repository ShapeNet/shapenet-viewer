package edu.stanford.graphics.shapenet.jme3.loaders;

import java.util.Arrays;

/**
 * Code to load/decompress mesh is taken from r100 of this webgl-loader
 * UTF-8 decoder from webgl-loader (r100)
 * http://code.google.com/p/webgl-loader/
 *
 * Model manifest description. Contains objects like:
 * name: {
 *   materials: { 'material_name': { ... } ... },
 *   decodeParams: {
 *     decodeOffsets: [ ... ],
 *     decodeScales: [ ... ],
 *   },
 *   urls: {
 *     'url': [
 *       { material: 'material_name',
 *         attribRange: [#, #],
 *         indexRange: [#, #],
 *         names: [ 'object names' ... ],
 *         lengths: [#, #, # ... ]
 *       }
 *     ],
 *     ...
 *   }
 * }
 *
 * @author Angel Chang
 */
public class UTF8Decoder {

  public Mesh decode(String str, MeshParams meshParams, DecodeParams decodeParams, String name, int idx) {
    int[] indexRange = meshParams.indexRange;
    if ( indexRange != null ) {
      int meshEnd = indexRange[ 0 ] + 3 * indexRange[ 1 ];
      if ( str.length() < meshEnd ) return null;
      return decompressMesh( str, meshParams, decodeParams, name, idx );
    } else {
      int[] codeRange = meshParams.codeRange;
      int meshEnd = codeRange[ 0 ] + codeRange[ 1 ];
      if ( str.length() < meshEnd ) return null;
      return decompressMesh2( str, meshParams, decodeParams, name, idx );
    }
  }

  public static class DecodeParams {
    int[] decodeOffsets;
    float[] decodeScales;

    public DecodeParams(int[] decodeOffsets, float[] decodeScales) {
      this.decodeOffsets = decodeOffsets;
      this.decodeScales = decodeScales;
    }

    public DecodeParams(int[] decodeOffsets, double[] decodeScales) {
      this.decodeOffsets = decodeOffsets;
      this.decodeScales = new float[decodeScales.length];
      for (int i = 0; i < decodeScales.length; i++) {
        this.decodeScales[i] = (float) decodeScales[i];
      }
    }
  }

  public static class MeshParams {
    String material;
    int[] attribRange;
    int[] indexRange;
    int bboxes;
    String[] names;
    int[] codeRange;

    public MeshParams(String material, int[] attribRange, int[] indexRange) {
      this.material = material;
      this.attribRange = attribRange;
      this.indexRange = indexRange;
    }

    public MeshParams(String material, int[] attribRange, int[] indexRange, int bboxes, String[] names, int[] codeRange) {
      this.material = material;
      this.attribRange = attribRange;
      this.indexRange = indexRange;
      this.bboxes = bboxes;
      this.names = names;
      this.codeRange = codeRange;
    }
  }

  public static class Mesh {
    MeshParams meshParams;
    String name;
    int idx;
    float[] attribs;
    int[] indices;
    float[] bboxen;

    // Extracted from attribs
    float[] positions;
    float[] normals;
    float[] uvs;

    public Mesh(MeshParams meshParams, String name, int idx, float[] attribs, int[] indices, float[] bboxen) {
      this.meshParams = meshParams;
      this.name = name;
      this.idx = idx;
      this.attribs = attribs;
      this.indices = indices;
      this.bboxen = bboxen;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Mesh mesh = (Mesh) o;

      if (idx != mesh.idx) return false;
      if (!Arrays.equals(attribs, mesh.attribs)) return false;
      if (!Arrays.equals(bboxen, mesh.bboxen)) return false;
      if (!Arrays.equals(indices, mesh.indices)) return false;
      if (meshParams != null ? !meshParams.equals(mesh.meshParams) : mesh.meshParams != null) return false;
      if (name != null ? !name.equals(mesh.name) : mesh.name != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result = meshParams != null ? meshParams.hashCode() : 0;
      result = 31 * result + (name != null ? name.hashCode() : 0);
      result = 31 * result + idx;
      result = 31 * result + (attribs != null ? Arrays.hashCode(attribs) : 0);
      result = 31 * result + (indices != null ? Arrays.hashCode(indices) : 0);
      result = 31 * result + (bboxen != null ? Arrays.hashCode(bboxen) : 0);
      return result;
    }

    public String getName() {
      return name;
    }

    public float[] getUVs() {
      if (uvs == null) {
        extract();
      }
      return uvs;
    }

    public float[] getVertices() {
      if (positions == null) {
        extract();
      }
      return positions;
    }

    public float[] getNormals() {
      if (normals == null) {
        extract();
      }
      return normals;
    }

    public float[] getAttribs() {
      return attribs;
    }

    public int[] getIndices() {
      return indices;
    }

    private void extract() {
      int[] indexArray = this.indices;
      float[] attribArray = this.attribs;

      int ntris = indexArray.length / 3;

      float[] positionArray = new float[3*3*ntris];
      float[] normalArray = new float[3*3*ntris];
      float[] uvArray = new float[2*3*ntris];

      int i, j, offset;
      float x, y, z;
      float u, v;

      int end = attribArray.length;
      int stride = 8;

      // extract positions
      j = 0;
      offset = 0;

      for( i = offset; i < end; i += stride ) {
        x = attribArray[ i ];
        y = attribArray[ i + 1 ];
        z = attribArray[ i + 2 ];

        positionArray[ j++ ] = x;
        positionArray[ j++ ] = y;
        positionArray[ j++ ] = z;
      }

      // extract uvs
      j = 0;
      offset = 3;
      for( i = offset; i < end; i += stride ) {
        u = attribArray[ i ];
        v = attribArray[ i + 1 ];

        uvArray[ j++ ] = u;
        uvArray[ j++ ] = v;
      }

      // extract normals
      j = 0;
      offset = 5;
      for( i = offset; i < end; i += stride ) {
        x = attribArray[ i ];
        y = attribArray[ i + 1 ];
        z = attribArray[ i + 2 ];

        normalArray[ j++ ] = x;
        normalArray[ j++ ] = y;
        normalArray[ j++ ] = z;
      }

      this.positions = positionArray;
      this.normals = normalArray;
      this.uvs = uvArray;
    }
  }

  private static final int[] defaultDecodeOffsets = new int[]{-4095, -4095, -4095, 0, 0, -511, -511, -511};
  private static final double[] defaultDecodeScales = new double[]{1/8191.0, 1/8191.0, 1/8191.0, 1/1023.0, 1/1023.0, 1/1023.0, 1/1023.0, 1/1023.0};
  public static  final DecodeParams DEFAULT_DECODE_PARAMS = new DecodeParams(defaultDecodeOffsets,defaultDecodeScales) {


    // TODO: normal decoding? (see walt.js)
    // needs to know: input, output (from vertex format!)
    //
    // Should split attrib/index.
    // 1) Decode position and non-normal attributes.
    // 2) Decode indices, computing normals
    // 3) Maybe normalize normals? Only necessary for refinement, or fixed?
    // 4) Maybe refine normals? Should this be part of regular refinement?
    // 5) Morphing

  };

// Triangle strips!

// TODO: will it be an optimization to specialize this method at
// runtime for different combinations of stride, decodeOffset and
// decodeScale?

  private void decompressAttribsInner_(String str, int inputStart, int inputEnd,
                                       float[] output, int outputStart, int stride,
      int decodeOffset, float decodeScale )
  {
    int prev = 0;
    for ( int j = inputStart; j < inputEnd; j ++ ) {
      int code = str.codePointAt(j);
      prev += ( code >> 1 ) ^ ( -( code & 1 ) );
      output[ outputStart ] = decodeScale * ( prev + decodeOffset );
      outputStart += stride;
     }
  }

  private void decompressIndices_(String str, int inputStart, int numIndices,
                                  int[] output, int outputStart)
  {
    int highest = 0;
    for ( int i = 0; i < numIndices; i ++ ) {
      int code = str.codePointAt( inputStart ++ );
      output[ outputStart ++ ] = highest - code;
      if ( code == 0 ) {
        highest ++;
      }
    }
  }

  private float[] decompressAABBs_(String str, int inputStart, int numBBoxen,
                                   int[] decodeOffsets, float[] decodeScales) {
    int numFloats = 6 * numBBoxen;
    int inputEnd = inputStart + numFloats;
    int outputStart = 0;
    float[] bboxen = new float[numFloats];  // originally float32
    for (int i = inputStart; i < inputEnd; i += 6 ) {
      int minX = str.codePointAt(i + 0) + decodeOffsets[0];
      int minY = str.codePointAt(i + 1) + decodeOffsets[1];
      int minZ = str.codePointAt(i + 2) + decodeOffsets[2];

      int radiusX = (str.codePointAt(i + 3) + 1) >> 1;
      int radiusY = (str.codePointAt(i + 4) + 1) >> 1;
      int radiusZ = (str.codePointAt(i + 5) + 1) >> 1;

      bboxen[ outputStart++ ] = decodeScales[0] * (minX + radiusX);
      bboxen[ outputStart++ ] = decodeScales[1] * (minY + radiusY);
      bboxen[ outputStart++ ] = decodeScales[2] * (minZ + radiusZ);

      bboxen[ outputStart++ ] = decodeScales[0] * radiusX;
      bboxen[ outputStart++ ] = decodeScales[1] * radiusY;
      bboxen[ outputStart++ ] = decodeScales[2] * radiusZ;
    }

    return bboxen;
  }

  private Mesh decompressMesh(String str, MeshParams meshParams, DecodeParams decodeParams,
                              String name, int idx) {
    // Extract conversion parameters from attribArrays.
    int stride = decodeParams.decodeScales.length;

    int[] decodeOffsets = decodeParams.decodeOffsets;
    float[] decodeScales = decodeParams.decodeScales;

    int attribStart = meshParams.attribRange[0];
    int numVerts = meshParams.attribRange[1];

    // Decode attributes.
    int inputOffset = attribStart;
    float[] attribsOut = new float[ stride * numVerts ];  // originally float32

    for (int j = 0; j < stride; j ++ ) {
      int end = inputOffset + numVerts;
      float decodeScale = decodeScales[j];
      if ( decodeScale != 0) {
        // Assume if decodeScale is never set, simply ignore the
        // attribute.
        this.decompressAttribsInner_( str, inputOffset, end,
          attribsOut, j, stride,
          decodeOffsets[j], decodeScale );
      }
      inputOffset = end;
    }

    int indexStart = meshParams.indexRange[ 0 ];
    int numIndices = 3 * meshParams.indexRange[ 1 ];

    int[] indicesOut = new int[ numIndices ];   // originally uint16
    this.decompressIndices_( str, inputOffset, numIndices, indicesOut, 0 );

    // Decode bboxen.
    float[] bboxen = null;
    int bboxOffset = meshParams.bboxes;

    if ( bboxOffset > 0) {
      bboxen = this.decompressAABBs_( str, bboxOffset, meshParams.names.length, decodeOffsets, decodeScales );
    }

    return new Mesh(meshParams, name, idx, attribsOut, indicesOut, bboxen);
  }

  private void copyAttrib(int stride, int[] attribsOutFixed, int[] lastAttrib, int index) {
    for ( int j = 0; j < stride; j ++ ) {
      lastAttrib[ j ] = attribsOutFixed[ stride * index + j ];
    }
  }

  private void decodeAttrib2(String str, int stride,
                             int[] decodeOffsets, float[] decodeScales,
                             int deltaStart, int numVerts,
                             float[] attribsOut, int[] attribsOutFixed, int[] lastAttrib, int index) {
    for ( int j = 0; j < 5; j ++ ) {
      int code = str.codePointAt(deltaStart + numVerts * j + index);
      int delta = ( code >> 1) ^ (-(code & 1));

      lastAttrib[ j ] += delta;
      attribsOutFixed[ stride * index + j ] = lastAttrib[ j ];
      attribsOut[ stride * index + j ] = decodeScales[ j ] * ( lastAttrib[ j ] + decodeOffsets[ j ] );
    }
  }

  private void accumulateNormal(int i0, int i1, int i2, int[] attribsOutFixed,  int[] crosses) {
    int p0x = attribsOutFixed[ 8*i0 ];
    int p0y = attribsOutFixed[ 8*i0 + 1 ];
    int p0z = attribsOutFixed[ 8*i0 + 2 ];

    int p1x = attribsOutFixed[ 8*i1 ];
    int p1y = attribsOutFixed[ 8*i1 + 1 ];
    int p1z = attribsOutFixed[ 8*i1 + 2 ];

    int p2x = attribsOutFixed[ 8*i2 ];
    int p2y = attribsOutFixed[ 8*i2 + 1 ];
    int p2z = attribsOutFixed[ 8*i2 + 2 ];

    p1x -= p0x;
    p1y -= p0y;
    p1z -= p0z;

    p2x -= p0x;
    p2y -= p0y;
    p2z -= p0z;

    p0x = p1y*p2z - p1z*p2y;
    p0y = p1z*p2x - p1x*p2z;
    p0z = p1x*p2y - p1y*p2x;

    crosses[ 3*i0 ]     += p0x;
    crosses[ 3*i0 + 1 ] += p0y;
    crosses[ 3*i0 + 2 ] += p0z;

    crosses[ 3*i1 ]     += p0x;
    crosses[ 3*i1 + 1 ] += p0y;
    crosses[ 3*i1 + 2 ] += p0z;

    crosses[ 3*i2 ]     += p0x;
    crosses[ 3*i2 + 1 ] += p0y;
    crosses[ 3*i2 + 2 ] += p0z;
  }

  private Mesh decompressMesh2(String str, MeshParams meshParams, DecodeParams decodeParams, String name, int idx) {
    int MAX_BACKREF = 96;

    // Extract conversion parameters from attribArrays.
    int stride = decodeParams.decodeScales.length;
    int[] decodeOffsets = decodeParams.decodeOffsets;
    float[] decodeScales = decodeParams.decodeScales;

    int deltaStart = meshParams.attribRange[ 0 ];
    int numVerts = meshParams.attribRange[ 1 ];

    int codeStart = meshParams.codeRange[ 0 ];
    int codeLength = meshParams.codeRange[ 1 ];

    int numIndices = 3 * meshParams.codeRange[ 2 ];

    int[] indicesOut = new int[ numIndices ];   // originally uint16
    int[] crosses = new int[ 3 * numVerts ]; // originally int32
    int[] lastAttrib = new int[ stride ];  // originally uint16
    int[] attribsOutFixed = new int[ stride * numVerts ];  // originally uint16
    float[] attribsOut = new float[ stride * numVerts ];       // originally float32

    int highest = 0;
    int outputStart = 0;
    for ( int i = 0; i < numIndices; i += 3 ) {
      int code = str.codePointAt(codeStart++);
      int max_backref = Math.min( i, MAX_BACKREF );

      if ( code < max_backref ) {

        // Parallelogram
        int winding = code % 3;
        int backref = i - ( code - winding );
        int i0 = 0, i1 = 0, i2 = 0;

        switch ( winding ) {
          case 0:
            i0 = indicesOut[ backref + 2 ];
            i1 = indicesOut[ backref + 1 ];
            i2 = indicesOut[ backref + 0 ];
            break;

          case 1:
            i0 = indicesOut[ backref + 0 ];
            i1 = indicesOut[ backref + 2 ];
            i2 = indicesOut[ backref + 1 ];
            break;

          case 2:
            i0 = indicesOut[ backref + 1 ];
            i1 = indicesOut[ backref + 0 ];
            i2 = indicesOut[ backref + 2 ];
            break;
        }

        indicesOut[ outputStart ++ ] = i0;
        indicesOut[ outputStart ++ ] = i1;

        code = str.codePointAt(codeStart++);

        int index = highest - code;
        indicesOut[ outputStart ++ ] = index;

        if ( code == 0 ) {
          for (int j = 0; j < 5; j ++ ) {
            int deltaCode = str.codePointAt(deltaStart + numVerts * j + highest);
            int prediction = ((deltaCode >> 1) ^ (-(deltaCode & 1))) +
              attribsOutFixed[stride*i0 + j] +
              attribsOutFixed[stride*i1 + j] -
              attribsOutFixed[stride*i2 + j];

            lastAttrib[j] = prediction;

            attribsOutFixed[ stride * highest + j ] = prediction;
            attribsOut[ stride * highest + j ] = decodeScales[ j ] * ( prediction + decodeOffsets[ j ] );
          }
          highest ++;
        } else {
          this.copyAttrib( stride, attribsOutFixed, lastAttrib, index );
        }
        this.accumulateNormal( i0, i1, index, attribsOutFixed, crosses );
      } else {
        // Simple
        int index0 = highest - ( code - max_backref );
        indicesOut[ outputStart ++ ] = index0;
        if ( code == max_backref ) {
          this.decodeAttrib2( str, stride, decodeOffsets, decodeScales, deltaStart,
            numVerts, attribsOut, attribsOutFixed, lastAttrib,
            highest ++ );
        } else {
          this.copyAttrib(stride, attribsOutFixed, lastAttrib, index0);
        }
        code = str.codePointAt(codeStart++);

        int index1 = highest - code;
        indicesOut[ outputStart ++ ] = index1;

        if ( code == 0 ) {
          this.decodeAttrib2( str, stride, decodeOffsets, decodeScales, deltaStart,
          numVerts, attribsOut, attribsOutFixed, lastAttrib,
          highest ++ );
        } else {
          this.copyAttrib( stride, attribsOutFixed, lastAttrib, index1 );
        }

        code = str.codePointAt(codeStart++);

        int index2 = highest - code;
        indicesOut[ outputStart ++ ] = index2;

        if ( code == 0 ) {
          for ( int j = 0; j < 5; j ++ ) {
            lastAttrib[ j ] = ( attribsOutFixed[ stride * index0 + j ] + attribsOutFixed[ stride * index1 + j ] ) / 2;
          }
          this.decodeAttrib2( str, stride, decodeOffsets, decodeScales, deltaStart,
          numVerts, attribsOut, attribsOutFixed, lastAttrib,
          highest ++ );
        } else {
          this.copyAttrib( stride, attribsOutFixed, lastAttrib, index2 );
        }

        this.accumulateNormal( index0, index1, index2, attribsOutFixed, crosses );
      }
    }

    for ( int i = 0; i < numVerts; i ++ ) {
      int nx = crosses[ 3*i ];
      int ny = crosses[ 3*i + 1 ];
      int nz = crosses[ 3*i + 2 ];

      float norm = (float) (511.0 / Math.sqrt( nx*nx + ny*ny + nz*nz ));

      int cx = str.codePointAt(deltaStart + 5 * numVerts + i);
      int cy = str.codePointAt(deltaStart + 6 * numVerts + i);
      int cz = str.codePointAt(deltaStart + 7 * numVerts + i);

      attribsOut[ stride*i + 5 ] = norm*nx + ((cx >> 1) ^ (-(cx & 1)));
      attribsOut[ stride*i + 6 ] = norm*ny + ((cy >> 1) ^ (-(cy & 1)));
      attribsOut[ stride*i + 7 ] = norm*nz + ((cz >> 1) ^ (-(cz & 1)));
    }

    return new Mesh( meshParams, name, idx, attribsOut, indicesOut, null );
  }
}
