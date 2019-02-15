package edu.stanford.graphics.shapenet.jme3.viewer;

import com.jme3.post.SceneProcessor;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.system.*;
import com.jme3.texture.FrameBuffer;
import com.jme3.util.BufferUtils;
import edu.stanford.graphics.shapenet.util.ImageWriter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;

/**
 * Screen Processor for displaying an rendered image
 *   (used for displaying images rendered in offscreen view)
 *
 * @author Angel Chang
 */
public class ImageDisplaySceneProcessor implements SceneProcessor {

  private ImageDisplay display;
  private Renderer renderer;
  private com.jme3.system.Timer timer = new NanoTimer();

  private int width;
  private int height;
  private ByteBuffer cpuBuf;
  private BufferedImage image;

  private JFrame frame;
  private boolean enabled = true;

  private boolean updating = true;
  private boolean popup = true;
  private boolean updatingOnce = true;
  private double scaling = 1.0;
  private Callable<Integer> callable;

  private String screenshotDir;
  private boolean capture = false;
  private boolean blankScreen = false;

  public ImageDisplaySceneProcessor() {
  }

  public ImageDisplaySceneProcessor(boolean enabled) {
    this.enabled = enabled;
  }

  public ImageDisplaySceneProcessor(boolean enabled, boolean popup, boolean updating, double scaling) {
    this.enabled = enabled;
    this.popup = popup;
    this.updating = updating;
    this.scaling = scaling;
  }


  private class ImageDisplay extends JPanel {

    private long t;
    private long total;
    private int frames;
    private int fps;

    @Override
    public void paintComponent(Graphics gfx) {
      super.paintComponent(gfx);
      Graphics2D g2d = (Graphics2D) gfx;

      g2d.scale(scaling,scaling);

      if (t == 0)
        t = timer.getTime();

//            g2d.setBackground(Color.BLACK);
//            g2d.clearRect(0,0,width,height);

      if (image != null) { // can be null if not popup
        synchronized (image) {
          if (!blankScreen)
            g2d.drawImage(image, null, 0, 0);
          else {
            g2d.clearRect(0, 0, width, height);
            blankScreen = false;
          }
        }
      }

      long t2 = timer.getTime();
      long dt = t2 - t;
      total += dt;
      frames ++;
      t = t2;

      if (total > 1000){
        fps = frames;
        total = 0;
        frames = 0;
      }

      //g2d.setColor(Color.white);
      //g2d.drawString("FPS: "+fps, 0, getHeight() - 100);
    }
  }

  public void createDisplayFrame(){
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
      frame = new JFrame("Render Display");
      display = new ImageDisplay();
      display.setPreferredSize(new Dimension(width, height));
      frame.getContentPane().add(display);
      frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosed(WindowEvent e) {
          setEnabled(false);
        }
      });
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setResizable(false);
      frame.setVisible(enabled);
      }
    });
  }

  // updates the actual image we see
  private void showUpdate() {
    synchronized (image) {
      ImageWriter.rgbaToabgr(cpuBuf, image);
    }

    if (display != null)
      display.repaint();
  }

  // TODO: method to clear the screen
  public void clearScreen() {
    blankScreen = true;
    showUpdate();
  }

  // record changes even if not updating, so we can update on the press of a button
  public void updateImageContents(FrameBuffer frameBuffer){
    cpuBuf.clear();
    renderer.readFrameBuffer(frameBuffer, cpuBuf);
    if (updating) showUpdate();
  }

  public void updateOnce() {
    callable = null;
    updatingOnce = true;
  }

  public void updateOnce(Callable<Integer> callable) {
    this.callable = callable;
    updatingOnce = true; // flag to be called on post-frame
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    frame.setVisible(this.enabled);
  }

  public void setUpdating(boolean updating) {
    this.updating = updating;
  }

  public boolean getUpdating() {
    return updating;
  }

  public ImageDisplay getDisplay() {
    if (!popup && display == null) {
      // Create display early
      display = new ImageDisplay();
    }
    return display;
  }


  public void initialize(RenderManager rm, ViewPort vp) {
    reshape(vp, vp.getCamera().getWidth(), vp.getCamera().getHeight());
    renderer = rm.getRenderer();
    if (popup)
      createDisplayFrame();
    timer.reset();
  }

  public void reshape(ViewPort vp, int w, int h) {
    width = w;
    height = h;
    cpuBuf = BufferUtils.createByteBuffer(width * height * 4);
    if (image != null) {
      synchronized (image) {
        Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics g = image.getGraphics();
        g.drawImage(scaledImage, 0, 0, null);
        g.dispose();
      }
    } else {
      image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
    }
  }

  public void saveScreenshot(String filename) {
    screenshotDir = filename;
    capture = true;
  }


  public boolean isInitialized() {
    return renderer != null;
  }

  public void preFrame(float tpf) {
  }

  public void postQueue(RenderQueue rq) {
  }

  /**
   * Update the CPU image's contents after the scene has
   * been rendered to the framebuffer.
   */
  public void postFrame(FrameBuffer out) {
    if (enabled) {
      updateImageContents(out);

      if (updatingOnce) {
        updatingOnce = false;
        showUpdate();
        if (callable != null)
          try {
            callable.call();
          } catch (Exception e) {
            e.printStackTrace();
          }
      }

      if (capture) {
        capture = false;

        try {
          File outputFile = new File(screenshotDir);
          ImageIO.write(image, "png", outputFile);
          System.out.println("Saved screenshot to " + screenshotDir);
        } catch (IOException e) {
          System.out.println("Error saving file");
        }
      }
    }
  }

  public void cleanup() {
  }


}
