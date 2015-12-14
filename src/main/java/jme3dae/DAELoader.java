package jme3dae;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Loads a collada xml document. This loader wraps the xml nodes of the collada
 * document in DAENode elements. A DAENode is an extended xml node that provides some
 * utility methods related to the structure of collada xml nodes.
 *
 * @author pgi
 */
public class DAELoader {

  /**
   * Instance creator. Creates a new DAELoader. The loader is stateless.
   *
   * @return a new DAELoader instance.
   */
  public static DAELoader create() {
    return new DAELoader();
  }

  private DAELoader() {
  }

  /**
   * Load the collada document from the given input stream. Closes the stream
   * after loading.
   *
   * @param in the input stream of the collada document
   * @return a DAENode wrapping the COLLADA element of the dae document. Returns
   * null if parsing fails for any (logged) reason.
   */
  public DAENode load(InputStream in) {
    DAENode root = null;
    try {
      DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
      DocumentBuilder bui = fac.newDocumentBuilder();
      Document doc = bui.parse(in);
      root = wrap(null, doc.getDocumentElement());
    } catch (Exception ex) {
      Logger.getLogger(getClass().getName()).log(Level.SEVERE, "", ex);
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ex) {
          Logger.getLogger(DAELoader.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }
    return root;
  }

  /**
   * Wraps the a tree of xml nodes into a tree of DAENode nodes. This method is
   * called recursively.
   *
   * @param parent the DAENode parent of the wrapping node produced by this
   *               method
   * @param node   the xml node to wrap
   * @return the root of the tree of DAENodes
   */
  private DAENode wrap(DAENode parent, Node node) {
    DAENode dae = DAENode.create(parent, node);
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      dae.addChild(wrap(dae, child));
    }
    return dae;
  }
}
