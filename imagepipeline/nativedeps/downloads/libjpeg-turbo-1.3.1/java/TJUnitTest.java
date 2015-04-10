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
 * This program tests the various code paths in the TurboJPEG JNI Wrapper
 */

import java.io.*;
import java.util.*;
import java.awt.image.*;
import javax.imageio.*;
import java.nio.*;
import org.libjpegturbo.turbojpeg.*;

public class TJUnitTest {

  private static final String classname =
    new TJUnitTest().getClass().getName();

  private static void usage() {
    System.out.println("\nUSAGE: java " + classname + " [options]\n");
    System.out.println("Options:\n");
    System.out.println("-yuv = test YUV encoding/decoding support\n");
    System.out.println("-bi = test BufferedImage support\n");
    System.exit(1);
  }

  private static final String[] subNameLong = {
    "4:4:4", "4:2:2", "4:2:0", "GRAY", "4:4:0"
  };
  private static final String[] subName = {
    "444", "422", "420", "GRAY", "440"
  };

  private static final String[] pixFormatStr = {
    "RGB", "BGR", "RGBX", "BGRX", "XBGR", "XRGB", "Grayscale",
    "RGBA", "BGRA", "ABGR", "ARGB"
  };

  private static final int[] alphaOffset = {
    -1, -1, -1, -1, -1, -1, -1, 3, 3, 0, 0
  };

  private static final int[] _3byteFormats = {
    TJ.PF_RGB, TJ.PF_BGR
  };
  private static final int[] _3byteFormatsBI = {
    BufferedImage.TYPE_3BYTE_BGR
  };
  private static final int[] _4byteFormats = {
    TJ.PF_RGBX, TJ.PF_BGRX, TJ.PF_XBGR, TJ.PF_XRGB
  };
  private static final int[] _4byteFormatsBI = {
    BufferedImage.TYPE_INT_BGR, BufferedImage.TYPE_INT_RGB,
    BufferedImage.TYPE_4BYTE_ABGR, BufferedImage.TYPE_4BYTE_ABGR_PRE,
    BufferedImage.TYPE_INT_ARGB, BufferedImage.TYPE_INT_ARGB_PRE
  };
  private static final int[] onlyGray = {
    TJ.PF_GRAY
  };
  private static final int[] onlyGrayBI = {
    BufferedImage.TYPE_BYTE_GRAY
  };
  private static final int[] onlyRGB = {
    TJ.PF_RGB
  };

  private static final int YUVENCODE = 1;
  private static final int YUVDECODE = 2;
  private static int yuv = 0;
  private static boolean bi = false;

  private static int exitStatus = 0;

  private static int biTypePF(int biType) {
    ByteOrder byteOrder = ByteOrder.nativeOrder();
    switch(biType) {
      case BufferedImage.TYPE_3BYTE_BGR:
        return TJ.PF_BGR;
      case BufferedImage.TYPE_4BYTE_ABGR:
      case BufferedImage.TYPE_4BYTE_ABGR_PRE:
        return TJ.PF_XBGR;
      case BufferedImage.TYPE_BYTE_GRAY:
        return TJ.PF_GRAY;
      case BufferedImage.TYPE_INT_BGR:
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          return TJ.PF_XBGR;
        else
          return TJ.PF_RGBX;
      case BufferedImage.TYPE_INT_RGB:
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          return TJ.PF_XRGB;
        else
          return TJ.PF_BGRX;
      case BufferedImage.TYPE_INT_ARGB:
      case BufferedImage.TYPE_INT_ARGB_PRE:
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          return TJ.PF_ARGB;
        else
          return TJ.PF_BGRA;
    }
    return 0;
  }

  private static String biTypeStr(int biType) {
    switch(biType) {
      case BufferedImage.TYPE_3BYTE_BGR:
        return "3BYTE_BGR";
      case BufferedImage.TYPE_4BYTE_ABGR:
        return "4BYTE_ABGR";
      case BufferedImage.TYPE_4BYTE_ABGR_PRE:
        return "4BYTE_ABGR_PRE";
      case BufferedImage.TYPE_BYTE_GRAY:
        return "BYTE_GRAY";
      case BufferedImage.TYPE_INT_BGR:
        return "INT_BGR";
      case BufferedImage.TYPE_INT_RGB:
        return "INT_RGB";
      case BufferedImage.TYPE_INT_ARGB:
        return "INT_ARGB";
      case BufferedImage.TYPE_INT_ARGB_PRE:
        return "INT_ARGB_PRE";
    }
    return "Unknown";
  }

  private static double getTime() {
    return (double)System.nanoTime() / 1.0e9;
  }

  private static void initBuf(byte[] buf, int w, int pitch, int h, int pf,
                              int flags) throws Exception {
    int roffset = TJ.getRedOffset(pf);
    int goffset = TJ.getGreenOffset(pf);
    int boffset = TJ.getBlueOffset(pf);
    int aoffset = alphaOffset[pf];
    int ps = TJ.getPixelSize(pf);
    int index, row, col, halfway = 16;

    Arrays.fill(buf, (byte)0);
    if (pf == TJ.PF_GRAY) {
      for (row = 0; row < h; row++) {
        for (col = 0; col < w; col++) {
          if ((flags & TJ.FLAG_BOTTOMUP) != 0)
            index = pitch * (h - row - 1) + col;
          else
            index = pitch * row + col;
          if (((row / 8) + (col / 8)) % 2 == 0)
            buf[index] = (row < halfway) ? (byte)255 : 0;
          else
            buf[index] = (row < halfway) ? 76 : (byte)226;
        }
      }
      return;
    }
    for (row = 0; row < h; row++) {
      for (col = 0; col < w; col++) {
        if ((flags & TJ.FLAG_BOTTOMUP) != 0)
          index = pitch * (h - row - 1) + col * ps;
        else
          index = pitch * row + col * ps;
        if (((row / 8) + (col / 8)) % 2 == 0) {
          if (row < halfway) {
            buf[index + roffset] = (byte)255;
            buf[index + goffset] = (byte)255;
            buf[index + boffset] = (byte)255;
          }
        } else {
          buf[index + roffset] = (byte)255;
          if (row >= halfway)
            buf[index + goffset] = (byte)255;
        }
        if (aoffset >= 0)
          buf[index + aoffset] = (byte)255;
      }
    }
  }

  private static void initIntBuf(int[] buf, int w, int pitch, int h, int pf,
                                 int flags) throws Exception {
    int rshift = TJ.getRedOffset(pf) * 8;
    int gshift = TJ.getGreenOffset(pf) * 8;
    int bshift = TJ.getBlueOffset(pf) * 8;
    int ashift = alphaOffset[pf] * 8;
    int index, row, col, halfway = 16;

    Arrays.fill(buf, 0);
    for (row = 0; row < h; row++) {
      for (col = 0; col < w; col++) {
        if ((flags & TJ.FLAG_BOTTOMUP) != 0)
          index = pitch * (h - row - 1) + col;
        else
          index = pitch * row + col;
        if (((row / 8) + (col / 8)) % 2 == 0) {
          if (row < halfway) {
            buf[index] |= (255 << rshift);
            buf[index] |= (255 << gshift);
            buf[index] |= (255 << bshift);
          }
        } else {
          buf[index] |= (255 << rshift);
          if (row >= halfway)
            buf[index] |= (255 << gshift);
        }
        if (ashift >= 0)
          buf[index] |= (255 << ashift);
      }
    }
  }

  private static void initImg(BufferedImage img, int pf, int flags)
                              throws Exception {
    WritableRaster wr = img.getRaster();
    int imgType = img.getType();
    if (imgType == BufferedImage.TYPE_INT_RGB ||
        imgType == BufferedImage.TYPE_INT_BGR ||
        imgType == BufferedImage.TYPE_INT_ARGB ||
        imgType == BufferedImage.TYPE_INT_ARGB_PRE) {
      SinglePixelPackedSampleModel sm =
        (SinglePixelPackedSampleModel)img.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
      int[] buf = db.getData();
      initIntBuf(buf, img.getWidth(), pitch, img.getHeight(), pf, flags);
    } else {
      ComponentSampleModel sm = (ComponentSampleModel)img.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
      byte[] buf = db.getData();
      initBuf(buf, img.getWidth(), pitch, img.getHeight(), pf, flags);
    }
  }

  private static void checkVal(int row, int col, int v, String vname, int cv)
                               throws Exception {
    v = (v < 0) ? v + 256 : v;
    if (v < cv - 1 || v > cv + 1) {
      throw new Exception("Comp. " + vname + " at " + row + "," + col +
                          " should be " + cv + ", not " + v);
    }
  }

  private static void checkVal0(int row, int col, int v, String vname)
                                throws Exception {
    v = (v < 0) ? v + 256 : v;
    if (v > 1) {
      throw new Exception("Comp. " + vname + " at " + row + "," + col +
                          " should be 0, not " + v);
    }
  }

  private static void checkVal255(int row, int col, int v, String vname)
                                  throws Exception {
    v = (v < 0) ? v + 256 : v;
    if (v < 254) {
      throw new Exception("Comp. " + vname + " at " + row + "," + col +
                          " should be 255, not " + v);
    }
  }

  private static int checkBuf(byte[] buf, int w, int pitch, int h, int pf,
                              int subsamp, TJScalingFactor sf, int flags)
                              throws Exception {
    int roffset = TJ.getRedOffset(pf);
    int goffset = TJ.getGreenOffset(pf);
    int boffset = TJ.getBlueOffset(pf);
    int aoffset = alphaOffset[pf];
    int ps = TJ.getPixelSize(pf);
    int index, row, col, retval = 1;
    int halfway = 16 * sf.getNum() / sf.getDenom();
    int blockSize = 8 * sf.getNum() / sf.getDenom();

    try {
      for (row = 0; row < halfway; row++) {
        for (col = 0; col < w; col++) {
          if ((flags & TJ.FLAG_BOTTOMUP) != 0)
            index = pitch * (h - row - 1) + col * ps;
          else
            index = pitch * row + col * ps;
          byte r = buf[index + roffset];
          byte g = buf[index + goffset];
          byte b = buf[index + boffset];
          byte a = aoffset >= 0 ? buf[index + aoffset] : (byte)255;
          if (((row / blockSize) + (col / blockSize)) % 2 == 0) {
            if (row < halfway) {
              checkVal255(row, col, r, "R");
              checkVal255(row, col, g, "G");
              checkVal255(row, col, b, "B");
            } else {
              checkVal0(row, col, r, "R");
              checkVal0(row, col, g, "G");
              checkVal0(row, col, b, "B");
            }
          } else {
            if (subsamp == TJ.SAMP_GRAY) {
              if (row < halfway) {
                checkVal(row, col, r, "R", 76);
                checkVal(row, col, g, "G", 76);
                checkVal(row, col, b, "B", 76);
              } else {
                checkVal(row, col, r, "R", 226);
                checkVal(row, col, g, "G", 226);
                checkVal(row, col, b, "B", 226);
              }
            } else {
              checkVal255(row, col, r, "R");
              if (row < halfway) {
                checkVal0(row, col, g, "G");
              } else {
                checkVal255(row, col, g, "G");
              }
              checkVal0(row, col, b, "B");
            }
          }
          checkVal255(row, col, a, "A");
        }
      }
    } catch(Exception e) {
      System.out.println("\n" + e.getMessage());
      retval = 0;
    }

    if (retval == 0) {
      for (row = 0; row < h; row++) {
        for (col = 0; col < w; col++) {
          int r = buf[pitch * row + col * ps + roffset];
          int g = buf[pitch * row + col * ps + goffset];
          int b = buf[pitch * row + col * ps + boffset];
          if (r < 0) r += 256;
          if (g < 0) g += 256;
          if (b < 0) b += 256;
          System.out.format("%3d/%3d/%3d ", r, g, b);
        }
        System.out.print("\n");
      }
    }
    return retval;
  }

  private static int checkIntBuf(int[] buf, int w, int pitch, int h, int pf,
                                 int subsamp, TJScalingFactor sf, int flags)
                                 throws Exception {
    int rshift = TJ.getRedOffset(pf) * 8;
    int gshift = TJ.getGreenOffset(pf) * 8;
    int bshift = TJ.getBlueOffset(pf) * 8;
    int ashift = alphaOffset[pf] * 8;
    int index, row, col, retval = 1;
    int halfway = 16 * sf.getNum() / sf.getDenom();
    int blockSize = 8 * sf.getNum() / sf.getDenom();

    try {
      for (row = 0; row < halfway; row++) {
        for (col = 0; col < w; col++) {
          if ((flags & TJ.FLAG_BOTTOMUP) != 0)
            index = pitch * (h - row - 1) + col;
          else
            index = pitch * row + col;
          int r = (buf[index] >> rshift) & 0xFF;
          int g = (buf[index] >> gshift) & 0xFF;
          int b = (buf[index] >> bshift) & 0xFF;
          int a = ashift >= 0 ? (buf[index] >> ashift) & 0xFF : 255;
          if (((row / blockSize) + (col / blockSize)) % 2 == 0) {
            if (row < halfway) {
              checkVal255(row, col, r, "R");
              checkVal255(row, col, g, "G");
              checkVal255(row, col, b, "B");
            } else {
              checkVal0(row, col, r, "R");
              checkVal0(row, col, g, "G");
              checkVal0(row, col, b, "B");
            }
          } else {
            if (subsamp == TJ.SAMP_GRAY) {
              if (row < halfway) {
                checkVal(row, col, r, "R", 76);
                checkVal(row, col, g, "G", 76);
                checkVal(row, col, b, "B", 76);
              } else {
                checkVal(row, col, r, "R", 226);
                checkVal(row, col, g, "G", 226);
                checkVal(row, col, b, "B", 226);
              }
            } else {
              checkVal255(row, col, r, "R");
              if (row < halfway) {
                checkVal0(row, col, g, "G");
              } else {
                checkVal255(row, col, g, "G");
              }
              checkVal0(row, col, b, "B");
            }
          }
          checkVal255(row, col, a, "A");
        }
      }
    } catch(Exception e) {
      System.out.println("\n" + e.getMessage());
      retval = 0;
    }

    if (retval == 0) {
      for (row = 0; row < h; row++) {
        for (col = 0; col < w; col++) {
          int r = (buf[pitch * row + col] >> rshift) & 0xFF;
          int g = (buf[pitch * row + col] >> gshift) & 0xFF;
          int b = (buf[pitch * row + col] >> bshift) & 0xFF;
          if (r < 0) r += 256;
          if (g < 0) g += 256;
          if (b < 0) b += 256;
          System.out.format("%3d/%3d/%3d ", r, g, b);
        }
        System.out.print("\n");
      }
    }
    return retval;
  }

  private static int checkImg(BufferedImage img, int pf, int subsamp,
                              TJScalingFactor sf, int flags) throws Exception {
    WritableRaster wr = img.getRaster();
    int imgType = img.getType();
    if (imgType == BufferedImage.TYPE_INT_RGB ||
        imgType == BufferedImage.TYPE_INT_BGR ||
        imgType == BufferedImage.TYPE_INT_ARGB ||
        imgType == BufferedImage.TYPE_INT_ARGB_PRE) {
      SinglePixelPackedSampleModel sm =
        (SinglePixelPackedSampleModel)img.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
      int[] buf = db.getData();
      return checkIntBuf(buf, img.getWidth(), pitch, img.getHeight(), pf,
                         subsamp, sf, flags);
    } else {
      ComponentSampleModel sm = (ComponentSampleModel)img.getSampleModel();
      int pitch = sm.getScanlineStride();
      DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
      byte[] buf = db.getData();
      return checkBuf(buf, img.getWidth(), pitch, img.getHeight(), pf, subsamp,
                      sf, flags);
    }
  }

  private static int PAD(int v, int p) {
    return ((v + (p) - 1) & (~((p) - 1)));
  }

  private static int checkBufYUV(byte[] buf, int size, int w, int h,
                                 int subsamp) throws Exception {
    int row, col;
    int hsf = TJ.getMCUWidth(subsamp) / 8, vsf = TJ.getMCUHeight(subsamp) / 8;
    int pw = PAD(w, hsf), ph = PAD(h, vsf);
    int cw = pw / hsf, ch = ph / vsf;
    int ypitch = PAD(pw, 4), uvpitch = PAD(cw, 4);
    int retval = 1;
    int correctsize = ypitch * ph +
                      (subsamp == TJ.SAMP_GRAY ? 0 : uvpitch * ch * 2);
    int halfway = 16;

    try {
      if (size != correctsize)
        throw new Exception("Incorrect size " + size + ".  Should be " +
                            correctsize);

      for (row = 0; row < ph; row++) {
        for (col = 0; col < pw; col++) {
          byte y = buf[ypitch * row + col];
          if (((row / 8) + (col / 8)) % 2 == 0) {
            if (row < halfway)
              checkVal255(row, col, y, "Y");
            else
              checkVal0(row, col, y, "Y");
          } else {
            if (row < halfway)
              checkVal(row, col, y, "Y", 76);
            else
              checkVal(row, col, y, "Y", 226);
          }
        }
      }
      if (subsamp != TJ.SAMP_GRAY) {
        halfway = 16 / vsf;
        for (row = 0; row < ch; row++) {
          for (col = 0; col < cw; col++) {
            byte u = buf[ypitch * ph + (uvpitch * row + col)],
                 v = buf[ypitch * ph + uvpitch * ch + (uvpitch * row + col)];
            if (((row * vsf / 8) + (col * hsf / 8)) % 2 == 0) {
              checkVal(row, col, u, "U", 128);
              checkVal(row, col, v, "V", 128);
            } else {
              if (row < halfway) {
                checkVal(row, col, u, "U", 85);
                checkVal255(row, col, v, "V");
              } else {
                checkVal0(row, col, u, "U");
                checkVal(row, col, v, "V", 149);
              }
            }
          }
        }
      }
    } catch(Exception e) {
      System.out.println("\n" + e.getMessage());
      retval = 0;
    }

    if (retval == 0) {
      for (row = 0; row < ph; row++) {
        for (col = 0; col < pw; col++) {
          int y = buf[ypitch * row + col];
          if (y < 0) y += 256;
          System.out.format("%3d ", y);
        }
        System.out.print("\n");
      }
      System.out.print("\n");
      for (row = 0; row < ch; row++) {
        for (col = 0; col < cw; col++) {
          int u = buf[ypitch * ph + (uvpitch * row + col)];
          if (u < 0) u += 256;
          System.out.format("%3d ", u);
        }
        System.out.print("\n");
      }
      System.out.print("\n");
      for (row = 0; row < ch; row++) {
        for (col = 0; col < cw; col++) {
          int v = buf[ypitch * ph + uvpitch * ch + (uvpitch * row + col)];
          if (v < 0) v += 256;
          System.out.format("%3d ", v);
        }
        System.out.print("\n");
      }
    }

    return retval;
  }

  private static void writeJPEG(byte[] jpegBuf, int jpegBufSize,
                                String filename) throws Exception {
    File file = new File(filename);
    FileOutputStream fos = new FileOutputStream(file);
    fos.write(jpegBuf, 0, jpegBufSize);
    fos.close();
  }

  private static int compTest(TJCompressor tjc, byte[] dstBuf, int w,
                              int h, int pf, String baseName, int subsamp,
                              int jpegQual, int flags) throws Exception {
    String tempstr;
    byte[] srcBuf = null;
    BufferedImage img = null;
    String pfStr;
    double t;
    int size = 0, ps, imgType = pf;

    if (bi) {
      pf = biTypePF(imgType);
      pfStr = biTypeStr(imgType);
    } else
      pfStr = pixFormatStr[pf];
    ps =  TJ.getPixelSize(pf);

    System.out.print(pfStr + " ");
    if (bi)
      System.out.print("(" + pixFormatStr[pf] + ") ");
    if ((flags & TJ.FLAG_BOTTOMUP) != 0)
      System.out.print("Bottom-Up");
    else
      System.out.print("Top-Down ");
    System.out.print(" -> " + subNameLong[subsamp] + " ");
    if (yuv == YUVENCODE)
      System.out.print("YUV ... ");
    else
      System.out.print("Q" + jpegQual + " ... ");

    if (bi) {
      img = new BufferedImage(w, h, imgType);
      initImg(img, pf, flags);
      tempstr = baseName + "_enc_" + pfStr + "_" +
                (((flags & TJ.FLAG_BOTTOMUP) != 0) ? "BU" : "TD") + "_" +
                subName[subsamp] + "_Q" + jpegQual + ".png";
      File file = new File(tempstr);
      ImageIO.write(img, "png", file);
    } else {
      srcBuf = new byte[w * h * ps + 1];
      initBuf(srcBuf, w, w * ps, h, pf, flags);
    }
    Arrays.fill(dstBuf, (byte)0);

    t = getTime();
    tjc.setSubsamp(subsamp);
    tjc.setJPEGQuality(jpegQual);
    if (bi) {
      if (yuv == YUVENCODE)
        tjc.encodeYUV(img, dstBuf, flags);
      else
        tjc.compress(img, dstBuf, flags);
    } else {
      tjc.setSourceImage(srcBuf, 0, 0, w, 0, h, pf);
      if (yuv == YUVENCODE)
        tjc.encodeYUV(dstBuf, flags);
      else
        tjc.compress(dstBuf, flags);
    }
    size = tjc.getCompressedSize();
    t = getTime() - t;

    if (yuv == YUVENCODE)
      tempstr = baseName + "_enc_" + pfStr + "_" +
                (((flags & TJ.FLAG_BOTTOMUP) != 0) ? "BU" : "TD") + "_" +
                subName[subsamp] + ".yuv";
    else
      tempstr = baseName + "_enc_" + pfStr + "_" +
                (((flags & TJ.FLAG_BOTTOMUP) != 0) ? "BU" : "TD") + "_" +
                subName[subsamp] + "_Q" + jpegQual + ".jpg";
    writeJPEG(dstBuf, size, tempstr);

    if (yuv == YUVENCODE) {
      if (checkBufYUV(dstBuf, size, w, h, subsamp) == 1)
        System.out.print("Passed.");
      else {
        System.out.print("FAILED!");
        exitStatus = -1;
      }
    } else
      System.out.print("Done.");
    System.out.format("  %.6f ms\n", t * 1000.);
    System.out.println("  Result in " + tempstr);

    return size;
  }

  private static void decompTest(TJDecompressor tjd, byte[] jpegBuf,
                                 int jpegSize, int w, int h, int pf,
                                 String baseName, int subsamp, int flags,
                                 TJScalingFactor sf) throws Exception {
    String pfStr, tempstr;
    double t;
    int scaledWidth = sf.getScaled(w);
    int scaledHeight = sf.getScaled(h);
    int temp1, temp2, imgType = pf;
    BufferedImage img = null;
    byte[] dstBuf = null;

    if (yuv == YUVENCODE) return;

    if (bi) {
      pf = biTypePF(imgType);
      pfStr = biTypeStr(imgType);
    } else
      pfStr = pixFormatStr[pf];

    System.out.print("JPEG -> ");
    if (yuv == YUVDECODE)
      System.out.print("YUV " + subNameLong[subsamp] + " ... ");
    else {
      System.out.print(pfStr + " ");
      if (bi)
        System.out.print("(" + pixFormatStr[pf] + ") ");
      if ((flags & TJ.FLAG_BOTTOMUP) != 0)
        System.out.print("Bottom-Up ");
      else
        System.out.print("Top-Down  ");
      if (!sf.isOne())
        System.out.print(sf.getNum() + "/" + sf.getDenom() + " ... ");
      else
        System.out.print("... ");
    }

    t = getTime();
    tjd.setJPEGImage(jpegBuf, jpegSize);
    if (tjd.getWidth() != w || tjd.getHeight() != h ||
        tjd.getSubsamp() != subsamp)
      throw new Exception("Incorrect JPEG header");

    temp1 = scaledWidth;
    temp2 = scaledHeight;
    temp1 = tjd.getScaledWidth(temp1, temp2);
    temp2 = tjd.getScaledHeight(temp1, temp2);
    if (temp1 != scaledWidth || temp2 != scaledHeight)
      throw new Exception("Scaled size mismatch");

    if (yuv == YUVDECODE)
      dstBuf = tjd.decompressToYUV(flags);
    else {
      if (bi)
        img = tjd.decompress(scaledWidth, scaledHeight, imgType, flags);
      else
        dstBuf = tjd.decompress(scaledWidth, 0, scaledHeight, pf, flags);
    }
    t = getTime() - t;

    if (bi) {
      tempstr = baseName + "_dec_" + pfStr + "_" +
                (((flags & TJ.FLAG_BOTTOMUP) != 0) ? "BU" : "TD") + "_" +
                subName[subsamp] + "_" +
                (double)sf.getNum() / (double)sf.getDenom() + "x" + ".png";
      File file = new File(tempstr);
      ImageIO.write(img, "png", file);
    }

    if (yuv == YUVDECODE) {
      if (checkBufYUV(dstBuf, dstBuf.length, w, h, subsamp) == 1)
        System.out.print("Passed.");
      else {
        System.out.print("FAILED!");  exitStatus = -1;
      }
    } else {
      if ((bi && checkImg(img, pf, subsamp, sf, flags) == 1) ||
          (!bi && checkBuf(dstBuf, scaledWidth,
                           scaledWidth * TJ.getPixelSize(pf), scaledHeight, pf,
                           subsamp, sf, flags) == 1))
        System.out.print("Passed.");
      else {
        System.out.print("FAILED!");
        exitStatus = -1;
      }
    }
    System.out.format("  %.6f ms\n", t * 1000.);
  }

  private static void decompTest(TJDecompressor tjd, byte[] jpegBuf,
                                 int jpegSize, int w, int h, int pf,
                                 String baseName, int subsamp,
                                 int flags) throws Exception {
    int i;
    if ((subsamp == TJ.SAMP_444 || subsamp == TJ.SAMP_GRAY) && yuv == 0) {
      TJScalingFactor[] sf = TJ.getScalingFactors();
      for (i = 0; i < sf.length; i++)
        decompTest(tjd, jpegBuf, jpegSize, w, h, pf, baseName, subsamp,
                   flags, sf[i]);
    } else
      decompTest(tjd, jpegBuf, jpegSize, w, h, pf, baseName, subsamp,
                 flags, new TJScalingFactor(1, 1));
  }

  private static void doTest(int w, int h, int[] formats, int subsamp,
                             String baseName) throws Exception {
    TJCompressor tjc = null;
    TJDecompressor tjd = null;
    int size;
    byte[] dstBuf;

    if (yuv == YUVENCODE)
      dstBuf = new byte[TJ.bufSizeYUV(w, h, subsamp)];
    else
      dstBuf = new byte[TJ.bufSize(w, h, subsamp)];

    try {
      tjc = new TJCompressor();
      tjd = new TJDecompressor();

      for (int pf : formats) {
        for (int i = 0; i < 2; i++) {
          int flags = 0;
          if (subsamp == TJ.SAMP_422 || subsamp == TJ.SAMP_420 ||
              subsamp == TJ.SAMP_440)
            flags |= TJ.FLAG_FASTUPSAMPLE;
          if (i == 1) {
            if (yuv == YUVDECODE) {
              tjc.close();
              tjd.close();
              return;
            } else
              flags |= TJ.FLAG_BOTTOMUP;
          }
          size = compTest(tjc, dstBuf, w, h, pf, baseName, subsamp, 100,
                          flags);
          decompTest(tjd, dstBuf, size, w, h, pf, baseName, subsamp, flags);
          if (pf >= TJ.PF_RGBX && pf <= TJ.PF_XRGB && !bi)
            decompTest(tjd, dstBuf, size, w, h, pf + (TJ.PF_RGBA - TJ.PF_RGBX),
                       baseName, subsamp, flags);
          System.out.print("\n");
        }
      }
      System.out.print("--------------------\n\n");
    } catch(Exception e) {
      if (tjc != null) tjc.close();
      if (tjd != null) tjd.close();
      throw e;
    }
    if (tjc != null) tjc.close();
    if (tjd != null) tjd.close();
  }

  private static void bufSizeTest() throws Exception {
    int w, h, i, subsamp;
    byte[] srcBuf, dstBuf;
    TJCompressor tjc = null;
    Random r = new Random();

    try {
      tjc = new TJCompressor();
      System.out.println("Buffer size regression test");
      for (subsamp = 0; subsamp < TJ.NUMSAMP; subsamp++) {
        for (w = 1; w < 48; w++) {
          int maxh = (w == 1) ? 2048 : 48;
          for (h = 1; h < maxh; h++) {
            if (h % 100 == 0)
              System.out.format("%04d x %04d\b\b\b\b\b\b\b\b\b\b\b", w, h);
            srcBuf = new byte[w * h * 4];
            if (yuv == YUVENCODE)
              dstBuf = new byte[TJ.bufSizeYUV(w, h, subsamp)];
            else
              dstBuf = new byte[TJ.bufSize(w, h, subsamp)];
            for (i = 0; i < w * h * 4; i++) {
              srcBuf[i] = (byte)(r.nextInt(2) * 255);
            }
            tjc.setSourceImage(srcBuf, 0, 0, w, 0, h, TJ.PF_BGRX);
            tjc.setSubsamp(subsamp);
            tjc.setJPEGQuality(100);
            if (yuv == YUVENCODE)
              tjc.encodeYUV(dstBuf, 0);
            else
              tjc.compress(dstBuf, 0);

            srcBuf = new byte[h * w * 4];
            if (yuv == YUVENCODE)
              dstBuf = new byte[TJ.bufSizeYUV(h, w, subsamp)];
            else
              dstBuf = new byte[TJ.bufSize(h, w, subsamp)];
            for (i = 0; i < h * w * 4; i++) {
              srcBuf[i] = (byte)(r.nextInt(2) * 255);
            }
            tjc.setSourceImage(srcBuf, 0, 0, h, 0, w, TJ.PF_BGRX);
            if (yuv == YUVENCODE)
              tjc.encodeYUV(dstBuf, 0);
            else
              tjc.compress(dstBuf, 0);
          }
        }
      }
      System.out.println("Done.      ");
    } catch(Exception e) {
      if (tjc != null) tjc.close();
      throw e;
    }
    if (tjc != null) tjc.close();
  }

  public static void main(String[] argv) {
    try {
      String testName = "javatest";
      boolean doyuv = false;
      for (int i = 0; i < argv.length; i++) {
        if (argv[i].equalsIgnoreCase("-yuv"))
          doyuv = true;
        if (argv[i].substring(0, 1).equalsIgnoreCase("-h") ||
            argv[i].equalsIgnoreCase("-?"))
          usage();
        if (argv[i].equalsIgnoreCase("-bi")) {
          bi = true;
          testName = "javabitest";
        }
      }
      if (doyuv) yuv = YUVENCODE;
      doTest(35, 39, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_444,
             testName);
      doTest(39, 41, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_444,
             testName);
      doTest(41, 35, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_422,
             testName);
      doTest(35, 39, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_422,
             testName);
      doTest(39, 41, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_420,
             testName);
      doTest(41, 35, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_420,
             testName);
      doTest(35, 39, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_440,
             testName);
      doTest(39, 41, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_440,
             testName);
      doTest(35, 39, bi ? onlyGrayBI : onlyGray, TJ.SAMP_GRAY, testName);
      doTest(39, 41, bi ? _3byteFormatsBI : _3byteFormats, TJ.SAMP_GRAY,
             testName);
      doTest(41, 35, bi ? _4byteFormatsBI : _4byteFormats, TJ.SAMP_GRAY,
             testName);
      if (!bi)
        bufSizeTest();
      if (doyuv && !bi) {
        System.out.print("\n--------------------\n\n");
        yuv = YUVDECODE;
        doTest(48, 48, onlyRGB, TJ.SAMP_444, "javatest_yuv0");
        doTest(35, 39, onlyRGB, TJ.SAMP_444, "javatest_yuv1");
        doTest(48, 48, onlyRGB, TJ.SAMP_422, "javatest_yuv0");
        doTest(39, 41, onlyRGB, TJ.SAMP_422, "javatest_yuv1");
        doTest(48, 48, onlyRGB, TJ.SAMP_420, "javatest_yuv0");
        doTest(41, 35, onlyRGB, TJ.SAMP_420, "javatest_yuv1");
        doTest(48, 48, onlyRGB, TJ.SAMP_440, "javatest_yuv0");
        doTest(35, 39, onlyRGB, TJ.SAMP_440, "javatest_yuv1");
        doTest(48, 48, onlyRGB, TJ.SAMP_GRAY, "javatest_yuv0");
        doTest(35, 39, onlyRGB, TJ.SAMP_GRAY, "javatest_yuv1");
        doTest(48, 48, onlyGray, TJ.SAMP_GRAY, "javatest_yuv0");
        doTest(39, 41, onlyGray, TJ.SAMP_GRAY, "javatest_yuv1");
      }
    } catch(Exception e) {
      e.printStackTrace();
      exitStatus = -1;
    }
    System.exit(exitStatus);
  }
}
