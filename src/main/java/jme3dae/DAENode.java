package jme3dae;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import jme3dae.transformers.ValueTransformer;
import jme3dae.transformers.ValueTransformer.TransformedValue;
import jme3dae.utilities.PlainTextTransformer;
import org.w3c.dom.Element;

/**
 * Wraps a xml node and offers a few utility functions to operate on collada elements.
 *
 * @author pgi
 */
public class DAENode {

  /**
   * This is where collada elements with a unique id are published
  */
  private static final Map<Object, DAENode> DOCUMENT_REGISTRY = new HashMap<Object, DAENode>();
  /**
   * Represents the null value for DAENode objects.
   */
  public static final DAENode NONE = new DAENode();

  public static void clearRegistry() {
    DOCUMENT_REGISTRY.clear();
  }

  /**
   * Returns a new DAENode instance
   *
   * @param parent the DAENode parent of the new node
   * @param node   the xml node to wrap
   * @return a new DAENode child of parent, wrapping node
   */
  public static DAENode create(DAENode parent, Node node) {
    ProgressListener listener = ColladaDocumentFactory.getProgressListener();
    if (listener != null) {
      listener.onProgress(1);
    }
    DAENode n = new DAENode(parent, node);
    if (node instanceof Element) {
      String id = ((Element) node).getAttribute("id");
      if (id.length() > 0) {
        DOCUMENT_REGISTRY.put(id, n);
      }
    }
    return n;
  }

  private final DAENode parent;
  private final Node node;
  private final List<DAENode> children = new LinkedList<DAENode>();
  private final List<Object> parsedData = new LinkedList<Object>();

  /**
   * Used for the instantiation of NONE
   */
  private DAENode() {
    parent = null;
    node = null;
  }

  /**
   * Access the root node of the collada document
   *
   * @return the root node of the document or DAENode.NONE if this node is
   * undefined (aka DAENode.NONE)
   */
  public DAENode getRootNode() {
    DAENode n = this;
    while (n.getParent().isDefined()) {
      n = n.getParent();
    }
    return n;
  }

  /**
   * Stores parsed data in this node. The data is stored in a typep registry and
   * can be retrieved asking for its class type.
   *
   * @param data the parsed data to store.
   */
  public void setParsedData(Object data) {
    if (data != null) {
      parsedData.add(data);
    }
  }

  /**
   * Returns the parsed data stored in this DAENode.
   *
   * @param <T>  the required type
   * @param type the type of the required data
   * @return the data in this nore or null if the node has no parsed data
   * or the type of the data is not = type
   */
  public <T> T getParsedData(Class<T> type) {
    for (Object object : parsedData) {
      if (type.isAssignableFrom(object.getClass())) {
        return type.cast(object);
      }
    }
    return null;
  }

  /**
   * Checks if this node holds some parsed data.
   *
   * @param <T>  the type of the data
   * @param type the type of the data to check
   * @return true if the node has some parsed data with a type T <: type.
   * False otherwise.
   */
  public <T> boolean hasParsedData(Class<T> type) {
    return parsedData == null
        ? null
        : type.isAssignableFrom(parsedData.getClass());
  }

  /**
   * Initializes a new DAENode
   *
   * @param parent the parent of this node, maybe null (the node will be the
   *               root of a tree)
   * @param node   the wrapped xml node, cannot be null
   * @throws IllegalArgumentException if node is null
   */
  private DAENode(DAENode parent, Node node) {
    if (node == null) {
      throw new IllegalArgumentException("DAENode xml node cannot be null.");
    }
    this.parent = parent;
    this.node = node;
  }

  /**
   * Checks if this DAENode is NONE
   *
   * @return true if this DANENode is not DAENode.NONE, false otherwise
   */
  public boolean isDefined() {
    return this != NONE;
  }

  /**
   * Returns the node linked pointed by this node with the value of the "source"
   * parameter
   *
   * @return the linked node or DAENode.NONE
   */
  public DAENode getLinkedSource() {
    DAENode r = NONE;
    if (node instanceof Element) {
      Element e = (Element) node;
      String att = e.getAttribute("source");
      if (att.length() > 0) {
        return getLinkedNode(att);
      }
    }
    return r;
  }

  /**
   * Returns the node linked by the attribute url of this node.
   *
   * @return the node linked via the url attribute of this node or DAENode.NONE
   * if no such node is found.
   */
  public DAENode getLinkedURL() {
    DAENode r = NONE;
    if (node instanceof Element) {
      Element e = (Element) node;
      String att = e.getAttribute("url");
      if (att.length() > 0) {
        return getLinkedNode(att);
      }
    }
    return r;
  }

  /**
   * Parses a named attribute of this node (same as attribute of a org.w3c.dom.Element).
   *
   * @param <R>    the type of the parsed value. Same as <code>parseAttribute(name, parser).get()</code>
   * @param name   the name of the attribute to parse
   * @param parser the transformer that will be applied to the attribute to generate
   *               the value
   * @return the result of the application of parser to the named attribute
   */
  public <R> TransformedValue<R> getAttribute(String name, ValueTransformer<String, R> parser) {
    String value = "";
    if (node instanceof Element) {
      value = ((Element) node).getAttribute(name);
    }
    return parser.transform(value);
  }

  /**
   * Returns the text content of this node as transformed by the given parser.
   * Same as <code>parseContent(parser).get();</code>
   *
   * @param <R>    the type of the parsed value
   * @param parser the parser that will transform the text content of this
   *               DAENode into a R
   * @return the transformed value of the text content of this node
   */
  public <R> TransformedValue<R> getContent(ValueTransformer<String, R> parser) {
    return node == null ? TransformedValue.<R>create(null) : parser.transform(node.getTextContent());
  }

  /**
   * Checks if this node has the given name
   *
   * @param value the value to check
   * @return true if the name of this node equals the supplied value. Case
   * sensitive.
   */
  public boolean hasName(String value) {
    if (value == null || (value.length() == 0) || !isDefined()) {
      return false;
    } else {
      return node.getNodeName().equals(value);
    }
  }

  /**
   * Returns the list of children of this node that have a name matching one
   * of the element in the given list. If the name list is empty (size 0) returns all the
   * children of this node. If the name is null returns an empty list.
   *
   * @param names alist of names. Can be null or empty
   * @return a list of DAENode, children of this node, whose tag matches (equality)
   * one of the components of the given array of names. Returned value can be empty,
   * never null.
   */
  public List<DAENode> getChildren(String... names) {
    if (node == null || names == null) {
      return Collections.<DAENode>emptyList();
    }

    if (names.length == 0) {
      return children;
    }

    List<String> nameList = Arrays.asList(names);
    List<DAENode> result = new LinkedList<DAENode>();
    for (DAENode e : children) {
      if (nameList.contains(e.getNodeName())) {
        result.add(e);
      }
    }
    return result;
  }

  /**
   * Returns the name of the wrapped node or null if the node is null.
   *
   * @return the name of the wrapped node or null.
   */
  private String getNodeName() {
    return node != null ? node.getNodeName() : null;
  }

  /**
   * Returns the first child of this node with the given name or DAENode.NONE
   * if no such child exists
   *
   * @param name the name to check
   * @return the first child with the given name or DAENode.NONE
   */
  public DAENode getChild(String name) {
    for (DAENode e : children) {
      if (e.hasName(name)) {
        return e;
      }
    }
    return NONE;
  }

  /**
   * Find and returns the first child of this node with the given name and the
   * attribute with the given value
   *
   * @param name
   * @param attribute
   * @param value
   * @return DAENode.NONE if no child is found.
   */
  public DAENode getChild(String name, String attribute, String value) {
    for (DAENode e : children) {
      if (e.hasName(name) && e.node instanceof Element) {
        Element m = (Element) e.node;
        if (value.equals(m.getAttribute(attribute))) {
          return e;
        }
      }
    }
    return DAENode.NONE;
  }

  /**
   * Returns the result of parsing the child with the given name with the
   * give transformer o an empty value if no child exists. This is the same as
   * calling transformer.transform(getChild(name));
   *
   * @param <T>         the type of the value returned by the transformer
   * @param name        the name of child to be transformed. If there are more children
   *                    with the given name, just the first one will be transformed.
   * @param transformer the value transformer that trasforms the child in a T
   * @return the result of the transformation of getChild(name) by transformer. Can
   * be an undefined TransformedValue, never null.
   */
  public <T> TransformedValue<T> getChildValue(String name, ValueTransformer<DAENode, T> transformer) {
    DAENode n = getChild(name);
    return n.isDefined() ? transformer.transform(n) : TransformedValue.<T>create(null);
  }

  /**
   * Returns the parent of this node.
   *
   * @return the parent of this node or DAENode.NONE if no parent is found (ie
   * this is the root node or DAENode.NONE).
   */
  public DAENode getParent() {
    return parent == null ? DAENode.NONE : parent;
  }

  /**
   * Add a child to this node. Used during the wrapping of a xml tree.
   *
   * @param node the node to add as a child of this node.
   */
  void addChild(DAENode node) {
    children.add(node);
  }

  /**
   * Returns the DAENode pointed by the given url.
   *
   * @param url the link to a DAENode. If url starts with a # the # character
   *            is removed before to search.
   * @return the DAENode denoted by the given url or DAENode.NONE if no such
   * node is found. The returned DAENode will be the first node in the
   * encosing context with a sid matching the given url (minus # if present). If
   * no such node exists, the returned DAENode will be the node with a global
   * id matching the given url (minus #).
   */
  public DAENode getLinkedNode(String url) {
    if (url == null || (url.length() == 0)) {
      return DAENode.NONE;
    }

    if (url.startsWith("#")) {
      url = url.substring(1);
    }

    PlainTextTransformer parser = PlainTextTransformer.create();
    DAENode e = getParent();

    while (e.isDefined()) {
      //parent has sid = url
      if (e.getAttribute("sid", parser).contains(url)) {
        return e;
      }

      //a child of parent has sid = url
      for (DAENode child : e.getChildren()) {
        if (child.getAttribute("sid", parser).contains(url)) {
          return child;
        }
      }

      e = e.getParent();
    }
    if ((!e.isDefined()) && DOCUMENT_REGISTRY.containsKey(url)) {
      e = DOCUMENT_REGISTRY.get(url);
    }
    return e;
  }

  /**
   * Returns a human readable representation of this node.
   *
   * @return a string representing this dae node.
   */
  @Override
  public String toString() {
    if (node == null) return "NONE";
    StringBuilder sb = new StringBuilder();
    // Create pretty string version of the node with just the attributes
    sb.append("<").append(node.getNodeName());
    NamedNodeMap attributes = node.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Node attr = attributes.item(i);
        sb.append(" ").append(attr.getNodeName()).append("=\"").append(attr.getNodeValue()).append("\"");
      }
      sb.append("/>");
    }
    return sb.toString();
  }

  public DAENode findDescendant(String tag) {
    if (this == DAENode.NONE) {
      return DAENode.NONE;
    }
    LinkedList<DAENode> list = new LinkedList<DAENode>();
    list.add(this);
    while (!list.isEmpty()) {
      DAENode n = list.removeFirst();
      if (n.hasName(tag)) return n;
      list.addAll(n.getChildren());
    }
    return DAENode.NONE;
  }
}
