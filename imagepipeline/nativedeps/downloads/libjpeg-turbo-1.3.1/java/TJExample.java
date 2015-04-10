/*
 * Copyright (C)2011-2012, 2014 D. R. Commander.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the libjpeg-turbo Project nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS",
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * This program demonstrates how to compress and decompress JPEG files using
 * the TurboJPEG JNI wrapper
 */

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.nio.*;
import javax.imageio.*;
import javax.swing.*;
import org.libjpegturbo.turbojpeg.*;

public class TJExample implements TJCustomFilter {

  public static final String classname = new TJExample().getClass().getName();

  private static void usage() throws Exception {
    System.out.println("\nUSAGE: java " + classname + " <Input file> <Output file> [options]\n");
    System.out.println("Input and output files can be any image format that the Java Image I/O");
    System.out.println("extensions understand.  If either filename ends in a .jpg extension, then");
    System.out.println("TurboJPEG will be used to compress or decompress the file.\n");
    System.out.println("Options:\n");
    System.out.println("-scale M/N = if the input image is a JPEG file, scale the width/height of the");
    System.out.print("             output image by a factor of M/N (M/N = ");
    for (int i = 0; i < sf.length; i++) {
      System.out.print(sf[i].getNum() + "/" + sf[i].getDenom());
      if (sf.length == 2 && i != sf.length - 1)
        System.out.print(" or ");
      else if (sf.length > 2) {
        if (i != sf.length - 1)
          System.out.print(", ");
        if (i == sf.length - 2)
          System.out.print("or ");
      }
    }
    System.out.println(")\n");
    System.out.println("-samp <444|422|420|gray> = If the output image is a JPEG file, this specifies");
    System.out.println("                           the level of chrominance subsampling to use when");
    System.out.println("                           recompressing it.  Default is to use the same level");
    System.out.println("                           of subsampling as the input, if the input is a JPEG");
    System.out.println("                           file, or 4:4:4 otherwise.\n");
    System.out.println("-q <1-100> = If the output image is a JPEG file, this specifies the JPEG");
    System.out.println("             quality to use when recompressing it (default = 95).\n");
    System.out.println("-hflip, -vflip, -transpose, -transverse, -rot90, -rot180, -rot270 =");
    System.out.println("     If the input image is a JPEG file, perform the corresponding lossless");
    System.out.println("     transform prior to decompression (these options are mutually exclusive)\n");
    System.out.println("-grayscale = If the input image is a JPEG file, perform lossless grayscale");
    System.out.println("     conversion prior to decompression (can be combined with the other");
    System.out.println("     transforms above)\n");
    System.out.println("-crop X,Y,WxH = If the input image is a JPEG file, perform lossless cropping");
    System.out.println("     prior to decompression.  X,Y specifies the upper left corner of the");
    System.out.println("     cropping region, and WxH specifies its width and height.  X,Y must be");
    System.out.println("     evenly divible by the MCU block size (8x8 if the source image was");
    System.out.println("     compressed using no subsampling or grayscale, or 16x8 for 4:2:2 or 16x16");
    System.out.println("     for 4:2:0.)\n");
    System.out.println("-display = Display output image (Output file need not be specified in this");
    System.out.println("     case.)\n");
    System.out.println("-fastupsample = Use the fastest chrominance upsampling algorithm available in");
    System.out.println("     the underlying codec\n");
    System.out.println("-fastdct = Use the fastest DCT/IDCT algorithms available in the underlying");
    System.out.println("     codec\n");
    System.out.println("-accuratedct = Use the most accurate DCT/IDCT algorithms available in the");
    System.out.println("     underlying codec\n");
    System.exit(1);
  }

  private static final String[] sampName = {
    "4:4:4", "4:2:2", "4:2:0", "Grayscale", "4:4:0"
  };

  public static void main(String[] argv) {

    BufferedImage img = null;
    byte[] bmpBuf = null;
    TJTransform xform = new TJTransform();
    int flags = 0;

    try {

      sf = TJ.getScalingFactors();

      if (argv.length < 2) {
        usage();
      }

      TJScalingFactor scaleFactor = new TJScalingFactor(1, 1);
      String inFormat = "jpg", outFormat = "jpg";
      int outSubsamp = -1, outQual = 95;
      boolean display = false;

      if (argv.length > 1) {
        for (int i = 1; i < argv.length; i++) {
          if (argv[i].length() < 2)
            continue;
          if (argv[i].length() > 2 &&
              argv[i].substring(0, 3).equalsIgnoreCase("-sc")) {
            int match = 0;
            if (i < argv.length - 1) {
              String[] scaleArg = argv[++i].split("/");
              if (scaleArg.length == 2) {
                TJScalingFactor tempsf =
                  new TJScalingFactor(Integer.parseInt(scaleArg[0]),
                                      Integer.parseInt(scaleArg[1]));
                for (int j = 0; j < sf.length; j++) {
                  if (tempsf.equals(sf[j])) {
                    scaleFactor = sf[j];
                    match = 1;
                    break;
                  }
                }
              }
            }
            if (match != 1) usage();
          }
          if (argv[i].equalsIgnoreCase("-h") || argv[i].equalsIgnoreCase("-?"))
            usage();
          if (argv[i].length() > 2 &&
              argv[i].substring(0, 3).equalsIgnoreCase("-sa")) {
            if (i < argv.length - 1) {
              i++;
              if (argv[i].substring(0, 1).equalsIgnoreCase("g"))
                outSubsamp = TJ.SAMP_GRAY;
              else if (argv[i].equals("444"))
                outSubsamp = TJ.SAMP_444;
              else if (argv[i].equals("422"))
                outSubsamp = TJ.SAMP_422;
              else if (argv[i].equals("420"))
                outSubsamp = TJ.SAMP_420;
              else
                usage();
            } else
              usage();
          }
          if (argv[i].substring(0, 2).equalsIgnoreCase("-q")) {
            if (i < argv.length - 1) {
              int qual = Integer.parseInt(argv[++i]);
              if (qual >= 1 && qual <= 100)
                outQual = qual;
              else
                usage();
            } else
              usage();
          }
          if (argv[i].substring(0, 2).equalsIgnoreCase("-g"))
            xform.options |= TJTransform.OPT_GRAY;
          if (argv[i].equalsIgnoreCase("-hflip"))
            xform.op = TJTransform.OP_HFLIP;
          if (argv[i].equalsIgnoreCase("-vflip"))
            xform.op = TJTransform.OP_VFLIP;
          if (argv[i].equalsIgnoreCase("-transpose"))
            xform.op = TJTransform.OP_TRANSPOSE;
          if (argv[i].equalsIgnoreCase("-transverse"))
            xform.op = TJTransform.OP_TRANSVERSE;
          if (argv[i].equalsIgnoreCase("-rot90"))
            xform.op = TJTransform.OP_ROT90;
          if (argv[i].equalsIgnoreCase("-rot180"))
            xform.op = TJTransform.OP_ROT180;
          if (argv[i].equalsIgnoreCase("-rot270"))
            xform.op = TJTransform.OP_ROT270;
          if (argv[i].equalsIgnoreCase("-custom"))
            xform.cf = new TJExample();
          else if (argv[i].length() > 2 &&
                   argv[i].substring(0, 2).equalsIgnoreCase("-c")) {
            if (i >= argv.length - 1)
              usage();
            String[] cropArg = argv[++i].split(",");
            if (cropArg.length != 3)
              usage();
            String[] dimArg = cropArg[2].split("[xX]");
            if (dimArg.length != 2)
              usage();
            int tempx = Integer.parseInt(cropArg[0]);
            int tempy = Integer.parseInt(cropArg[1]);
            int tempw = Integer.parseInt(dimArg[0]);
            int temph = Integer.parseInt(dimArg[1]);
            if (tempx < 0 || tempy < 0 || tempw < 0 || temph < 0)
              usage();
            xform.x = tempx;
            xform.y = tempy;
            xform.width = tempw;
            xform.height = temph;
            xform.options |= TJTransform.OPT_CROP;
          }
          if (argv[i].substring(0, 2).equalsIgnoreCase("-d"))
            display = true;
          if (argv[i].equalsIgnoreCase("-fastupsample")) {
            System.out.println("Using fast upsampling code");
            flags |= TJ.FLAG_FASTUPSAMPLE;
          }
          if (argv[i].equalsIgnoreCase("-fastdct")) {
            System.out.println("Using fastest DCT/IDCT algorithm");
            flags |= TJ.FLAG_FASTDCT;
          }
          if (argv[i].equalsIgnoreCase("-accuratedct")) {
            System.out.println("Using most accurate DCT/IDCT algorithm");
            flags |= TJ.FLAG_ACCURATEDCT;
          }
        }
      }
      String[] inFileTokens = argv[0].split("\\.");
      if (inFileTokens.length > 1)
        inFormat = inFileTokens[inFileTokens.length - 1];
      String[] outFileTokens;
      if (display)
        outFormat = "bmp";
      else {
        outFileTokens = argv[1].split("\\.");
        if (outFileTokens.length > 1)
          outFormat = outFileTokens[outFileTokens.length - 1];
      }

      File file = new File(argv[0]);
      int width, height;

      if (inFormat.equalsIgnoreCase("jpg")) {
        FileInputStream fis = new FileInputStream(file);
        int inputSize = fis.available();
        if (inputSize < 1) {
          System.out.println("Input file contains no data");
          System.exit(1);
        }
        byte[] inputBuf = new byte[inputSize];
        fis.read(inputBuf);
        fis.close();

        TJDecompressor tjd;
        if (xform.op != TJTransform.OP_NONE || xform.options != 0 ||
            xform.cf != null) {
          TJTransformer tjt = new TJTransformer(inputBuf);
          TJTransform[] t = new TJTransform[1];
          t[0] = xform;
          t[0].options |= TJTransform.OPT_TRIM;
          TJDecompressor[] tjdx = tjt.transform(t, 0);
          tjd = tjdx[0];
        } else
          tjd = new TJDecompressor(inputBuf);

        width = tjd.getWidth();
        height = tjd.getHeight();
        int inSubsamp = tjd.getSubsamp();
        System.out.println("Source Image: " + width + " x " + height +
                           " pixels, " + sampName[inSubsamp] + " subsampling");
        if (outSubsamp < 0)
          outSubsamp = inSubsamp;

        if (outFormat.equalsIgnoreCase("jpg") &&
            (xform.op != TJTransform.OP_NONE || xform.options != 0) &&
            scaleFactor.isOne()) {
          file = new File(argv[1]);
          FileOutputStream fos = new FileOutputStream(file);
          fos.write(tjd.getJPEGBuf(), 0, tjd.getJPEGSize());
          fos.close();
          System.exit(0);
        }

        width = scaleFactor.getScaled(width);
        height = scaleFactor.getScaled(height);

        if (!outFormat.equalsIgnoreCase("jpg"))
          img = tjd.decompress(width, height, BufferedImage.TYPE_INT_RGB,
                               flags);
        else
          bmpBuf = tjd.decompress(width, 0, height, TJ.PF_BGRX, flags);
        tjd.close();
      } else {
        img = ImageIO.read(file);
        if (img == null)
          throw new Exception("Input image type not supported.");
        width = img.getWidth();
        height = img.getHeight();
        if (outSubsamp < 0) {
          if (img.getType() == BufferedImage.TYPE_BYTE_GRAY)
            outSubsamp = TJ.SAMP_GRAY;
          else
            outSubsamp = TJ.SAMP_444;
        }
      }
      System.gc();
      if (!display)
        System.out.print("Dest. Image (" + outFormat + "):  " + width + " x " +
                         height + " pixels");

      if (display) {
        ImageIcon icon = new ImageIcon(img);
        JLabel label = new JLabel(icon, JLabel.CENTER);
        JOptionPane.showMessageDialog(null, label, "Output Image",
                                      JOptionPane.PLAIN_MESSAGE);
      } else if (outFormat.equalsIgnoreCase("jpg")) {
        System.out.println(", " + sampName[outSubsamp] +
                           " subsampling, quality = " + outQual);
        TJCompressor tjc = new TJCompressor();
        int jpegSize;
        byte[] jpegBuf;

        tjc.setSubsamp(outSubsamp);
        tjc.setJPEGQuality(outQual);
        if (img != null)
          jpegBuf = tjc.compress(img, flags);
        else {
          tjc.setSourceImage(bmpBuf, 0, 0, width, 0, height, TJ.PF_BGRX);
          jpegBuf = tjc.compress(flags);
        }
        jpegSize = tjc.getCompressedSize();
        tjc.close();

        file = new File(argv[1]);
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(jpegBuf, 0, jpegSize);
        fos.close();
      } else {
        System.out.print("\n");
        file = new File(argv[1]);
        ImageIO.write(img, outFormat, file);
      }

    } catch(Exception e) {
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public void customFilter(ShortBuffer coeffBuffer, Rectangle bufferRegion,
                           Rectangle planeRegion, int componentIndex,
                           int transformIndex, TJTransform transform)
                           throws Exception {
    for (int i = 0; i < bufferRegion.width * bufferRegion.height; i++) {
      coeffBuffer.put(i, (short)(-coeffBuffer.get(i)));
    }
  }

  static TJScalingFactor[] sf = null;
};
