package edu.stanford.graphics.shapenet.util;

import com.jme3.util.Screenshots;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ImageWriter {
    private static BufferedImage verticalFlip(BufferedImage original) {
        AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
        tx.translate(0, -original.getHeight());
        AffineTransformOp transformOp = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        BufferedImage awtImage = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_BGR);
        Graphics2D g2d = awtImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2d.drawImage(original, transformOp, 0, 0);
        g2d.dispose();
        return awtImage;
    }

     public static void rgbaToabgr(ByteBuffer rgbaBuf, BufferedImage out) {
        WritableRaster wr = out.getRaster();
        DataBufferByte db = (DataBufferByte) wr.getDataBuffer();

        byte[] cpuArray = db.getData();

        // copy native memory to java memory
        rgbaBuf.clear();
        rgbaBuf.get(cpuArray);
        rgbaBuf.clear();

        int width  = wr.getWidth();
        int height = wr.getHeight();

        // flip the components the way AWT likes them

        // calcuate half of height such that all rows of the array are written to
        // e.g. for odd heights, write 1 more scanline
        int heightdiv2ceil = height % 2 == 1 ? (height / 2) + 1 : height / 2;
        for (int y = 0; y < heightdiv2ceil; y++){
            for (int x = 0; x < width; x++){
                int inPtr  = (y * width + x) * 4;
                int outPtr = ((height-y-1) * width + x) * 4;

                byte b1 = cpuArray[inPtr+2];
                byte g1 = cpuArray[inPtr+1];
                byte r1 = cpuArray[inPtr+0];
                byte a1 = cpuArray[inPtr+3];

                byte b2 = cpuArray[outPtr+2];
                byte g2 = cpuArray[outPtr+1];
                byte r2 = cpuArray[outPtr+0];
                byte a2 = cpuArray[outPtr+3];

                cpuArray[outPtr+0] = a1;
                cpuArray[outPtr+1] = b1;
                cpuArray[outPtr+2] = g1;
                cpuArray[outPtr+3] = r1;

                cpuArray[inPtr+0] = a2;
                cpuArray[inPtr+1] = b2;
                cpuArray[inPtr+2] = g2;
                cpuArray[inPtr+3] = r2;
            }
        }
    }

    public static void writeImageFile(OutputStream outStream, String format, ByteBuffer imageData, int width, int height) throws IOException {
        int type = (format.equals("png"))? BufferedImage.TYPE_4BYTE_ABGR : BufferedImage.TYPE_INT_BGR;
        BufferedImage awtImage = new BufferedImage(width, height, type);
        if (type == BufferedImage.TYPE_4BYTE_ABGR) {
            rgbaToabgr(imageData, awtImage);
        } else {
            Screenshots.convertScreenShot2(imageData.asIntBuffer(), awtImage);
            awtImage = verticalFlip(awtImage);
        }
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName(format).next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();

        if (format.equals("jpg")) {
            JPEGImageWriteParam jpegParam = (JPEGImageWriteParam) writeParam;
            jpegParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            jpegParam.setCompressionQuality(0.95f);
        }

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
