package edu.stanford.graphics.shapenet.gui;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.IOException;
import java.net.URL;

/**
 * Shows a tree panel
 *
 * @author Angel Chang
 */
public class TreePanel extends JPanel implements TreeSelectionListener {
  protected JTextPane htmlPane;
  protected JTree tree;
  protected URL helpURL;
  protected static boolean DEBUG = false;

  //Optionally set the look and feel.
  private static boolean useSystemLookAndFeel = false;

  public TreePanel(DefaultMutableTreeNode top) {
    super(new GridLayout(1, 0));

    //Create a tree that allows one selection at a time.
    tree = new JTree(top);
    tree.getSelectionModel().setSelectionMode
        (TreeSelectionModel.SINGLE_TREE_SELECTION);

    //Listen for when the selection changes.
    tree.addTreeSelectionListener(this);

    //Create the scroll pane and add the tree to it.
    JScrollPane treeView = new JScrollPane(tree);

    //Create the HTML viewing pane.
    htmlPane = new JTextPane();
    htmlPane.setEditable(false);
    JScrollPane htmlView = new JScrollPane(htmlPane);

    //Add the scroll panes to a split pane.
    JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPane.setTopComponent(treeView);
    splitPane.setBottomComponent(htmlView);

    Dimension minimumSize = new Dimension(100, 50);
    htmlView.setMinimumSize(minimumSize);
    treeView.setMinimumSize(minimumSize);
    splitPane.setDividerLocation(100);
    splitPane.setPreferredSize(new Dimension(500, 300));

    //Add the split pane to this panel.
    add(splitPane);
  }

  public void setHtmlPaneUrl(String url) {
    try {
      htmlPane.setPage(url);
    } catch (IOException ex) {
      throw new RuntimeException("Error fetching url: " + url, ex);
    }
  }

  public void setHtmlPaneContent(String title, String bodyString) {
    String htmlString = "<html><head><title>" + title + "</title></head><body>" + bodyString + "</body></html>";
    htmlPane.setContentType("text/html");
    htmlPane.setText(htmlString);
  }

  public void valueChanged(TreeSelectionEvent e) {
  }
  /**
   * Create the GUI and show it.  For thread safety,
   * this method should be invoked from the
   * event dispatch thread.
   */
  private static void createAndShowGUI(String name, TreePanel panel, boolean exitOnClose) {
    if (useSystemLookAndFeel) {
      try {
        UIManager.setLookAndFeel(
            UIManager.getSystemLookAndFeelClassName());
      } catch (Exception e) {
        System.err.println("Couldn't use system look and feel.");
      }
    }

    //Create and set up the window.
    JFrame frame = new JFrame(name);
    if (exitOnClose) {
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    //Add content to the window.
    frame.add(panel);

    //Display the window.
    frame.pack();
    frame.setVisible(true);
  }

  public static void create(String name, TreePanel panel) {
    //Schedule a job for the event dispatch thread:
    //creating and showing this application's GUI.
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI(name, panel, false);
      }
    });
  }

}