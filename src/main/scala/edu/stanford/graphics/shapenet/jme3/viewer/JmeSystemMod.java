package edu.stanford.graphics.shapenet.jme3.viewer;

import com.jme3.util.Screenshots;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public final class JmeSystemMod {

    private static void fixAlphaAndSwapChannels(BufferedImage img) {
        final WritableRaster wr = img.getRaster();
        final DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
        final int[] cpuArray = db.getData();
        for (int i = 0; i < cpuArray.length; i++) {
            // Fix alpha channel (set to 0 for complete transparency)
            if (cpuArray[i] == -1) {
                cpuArray[i] &= 0x00ffffff;
            } else {
                // ABGR to ARGB
                int p = cpuArray[i];
                int b = p >> 16 & 0xff;
                int r = p & 0xff;
                cpuArray[i] = (p & 0xff00ff00) | r << 16 | b;
            }
        }
    }

    private static BufferedImage verticalFlip(BufferedImage original) {
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -original.getHeight());
        AffineTransformOp transformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        BufferedImage awtImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = awtImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2d.drawImage(original, transformOp, 0, 0);
        g2d.dispose();
        return awtImage;
    }

    public static void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        BufferedImage awtImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Screenshots.convertScreenShot2(imageData.asIntBuffer(), awtImage);
        fixAlphaAndSwapChannels(awtImage);

        ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        if (format.equals("jpg")) {
            JPEGImageWriteParam jpegParam = (JPEGImageWriteParam) writeParam;
            jpegParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpegParam.setCompressionQuality(0.95f);
        }

        awtImage = verticalFlip(awtImage);

        ImageOutputStream imgOut = new MemoryCacheImageOutputStream(outStream);
        writer.setOutput(imgOut);
        IIOImage outputImage = new IIOImage(awtImage, null, null);
        try {
            writer.write(null, outputImage, writeParam);
        } finally {
            imgOut.close();
            writer.dispose();
        }
    }
}
