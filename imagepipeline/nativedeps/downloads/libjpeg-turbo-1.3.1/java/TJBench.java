/*
 * Copyright (C)2009-2014 D. R. Commander.  All Rights Reserved.
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

import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import java.util.*;
import org.libjpegturbo.turbojpeg.*;

class TJBench {

  static final int YUVENCODE = 1;
  static final int YUVDECODE = 2;

  static int flags = 0, yuv = 0, quiet = 0, pf = TJ.PF_BGR;
  static boolean decompOnly, doTile;

  static final String[] pixFormatStr = {
    "RGB", "BGR", "RGBX", "BGRX", "XBGR", "XRGB", "GRAY"
  };

  static final String[] subNameLong = {
    "4:4:4", "4:2:2", "4:2:0", "GRAY", "4:4:0"
  };

  static final String[] subName = {
    "444", "422", "420", "GRAY", "440"
  };

  static TJScalingFactor sf;
  static int xformOp = TJTransform.OP_NONE, xformOpt = 0;
  static double benchTime = 5.0;


  static final double getTime() {
    return (double)System.nanoTime() / 1.0e9;
  }


  static String sigFig(double val, int figs) {
    String format;
    int digitsAfterDecimal = figs - (int)Math.ceil(Math.log10(Math.abs(val)));
    if (digitsAfterDecimal < 1)
      format = new String("%.0f");
    else
      format = new String("%." + digitsAfterDecimal + "f");
    return String.format(format, val);
  }


  static byte[] loadImage(String fileName, int[] w, int[] h, int pixelFormat)
                          throws Exception {
    BufferedImage img = ImageIO.read(new File(fileName));
    if (img == null)
      throw new Exception("Could not read " + fileName);
    w[0] = img.getWidth();
    h[0] = img.getHeight();
    int[] rgb = img.getRGB(0, 0, w[0], h[0], null, 0, w[0]);
    int ps = TJ.getPixelSize(pixelFormat);
    int rindex = TJ.getRedOffset(pixelFormat);
    int gindex = TJ.getGreenOffset(pixelFormat);
    int bindex = TJ.getBlueOffset(pixelFormat);
    byte[] dstBuf = new byte[w[0] * h[0] * ps];
    int pixels = w[0] * h[0], dstPtr = 0, rgbPtr = 0;
    while (pixels-- > 0) {
      dstBuf[dstPtr + rindex] = (byte)((rgb[rgbPtr] >> 16) & 0xff);
      dstBuf[dstPtr + gindex] = (byte)((rgb[rgbPtr] >> 8) & 0xff);
      dstBuf[dstPtr + bindex] = (byte)(rgb[rgbPtr] & 0xff);
      dstPtr += ps;
      rgbPtr++;
    }
    return dstBuf;
  }


  static void saveImage(String fileName, byte[] srcBuf, int w, int h,
                        int pixelFormat) throws Exception {
    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    int pixels = w * h, srcPtr = 0;
    int ps = TJ.getPixelSize(pixelFormat);
    int rindex = TJ.getRedOffset(pixelFormat);
    int gindex = TJ.getGreenOffset(pixelFormat);
    int bindex = TJ.getBlueOffset(pixelFormat);
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++, srcPtr += ps) {
        int pixel = (srcBuf[srcPtr + rindex] & 0xff) << 16 |
                    (srcBuf[srcPtr + gindex] & 0xff) << 8 |
                    (srcBuf[srcPtr + bindex] & 0xff);
        img.setRGB(x, y, pixel);
      }
    }
    ImageIO.write(img, "bmp", new File(fileName));
  }


  /* Decompression test */
  static void decompTest(byte[] srcBuf, byte[][] jpegBuf, int[] jpegSize,
                         byte[] dstBuf, int w, int h, int subsamp,
                         int jpegQual, String fileName, int tilew, int tileh)
                         throws Exception {
    String qualStr = new String(""), sizeStr, tempStr;
    TJDecompressor tjd;
    double start, elapsed;
    int ps = TJ.getPixelSize(pf), i;
    int yuvSize = TJ.bufSizeYUV(w, h, subsamp), bufsize;
    int scaledw = (yuv == YUVDECODE) ? w : sf.getScaled(w);
    int scaledh = (yuv == YUVDECODE) ? h : sf.getScaled(h);
    int pitch = scaledw * ps;

    if (jpegQual > 0)
      qualStr = new String("_Q" + jpegQual);

    tjd = new TJDecompressor();

    int bufSize = (yuv == YUVDECODE ? yuvSize : pitch * scaledh);
    if (dstBuf == null)
      dstBuf = new byte[bufSize];

    /* Set the destination buffer to gray so we know whether the decompressor
       attempted to write to it */
    Arrays.fill(dstBuf, (byte)127);

    /* Execute once to preload cache */
    tjd.setJPEGImage(jpegBuf[0], jpegSize[0]);
    if (yuv == YUVDECODE)
      tjd.decompressToYUV(dstBuf, flags);
    else
      tjd.decompress(dstBuf, 0, 0, scaledw, pitch, scaledh, pf, flags);

    /* Benchmark */
    for (i = 0, start = getTime(); (elapsed = getTime() - start) < benchTime;
         i++) {
      int tile = 0;
      if (yuv == YUVDECODE)
        tjd.decompressToYUV(dstBuf, flags);
      else {
        for (int y = 0; y < h; y += tileh) {
          for (int x = 0; x < w; x += tilew, tile++) {
            int width = doTile ? Math.min(tilew, w - x) : scaledw;
            int height = doTile ? Math.min(tileh, h - y) : scaledh;
            tjd.setJPEGImage(jpegBuf[tile], jpegSize[tile]);
            tjd.decompress(dstBuf, x, y, width, pitch, height, pf, flags);
          }
        }
      }
    }

    tjd = null;
    for (i = 0; i < jpegBuf.length; i++)
      jpegBuf[i] = null;
    jpegBuf = null;  jpegSize = null;
    System.gc();

    if (quiet != 0)
      System.out.println(
        sigFig((double)(w * h) / 1000000. * (double)i / elapsed, 4));
    else {
      System.out.format("D--> Frame rate:           %f fps\n",
                        (double)i / elapsed);
      System.out.format("     Dest. throughput:     %f Megapixels/sec\n",
                        (double)(w * h) / 1000000. * (double)i / elapsed);
    }

    if (yuv == YUVDECODE) {
      tempStr = fileName + "_" + subName[subsamp] + qualStr + ".yuv";
      FileOutputStream fos = new FileOutputStream(tempStr);
      fos.write(dstBuf, 0, yuvSize);
      fos.close();
    } else {
      if (sf.getNum() != 1 || sf.getDenom() != 1)
        sizeStr = new String(sf.getNum() + "_" + sf.getDenom());
      else if (tilew != w || tileh != h)
        sizeStr = new String(tilew + "x" + tileh);
      else
        sizeStr = new String("full");
      if (decompOnly)
        tempStr = new String(fileName + "_" + sizeStr + ".bmp");
      else
        tempStr = new String(fileName + "_" + subName[subsamp] + qualStr +
                             "_" + sizeStr + ".bmp");
      saveImage(tempStr, dstBuf, scaledw, scaledh, pf);
      int ndx = tempStr.indexOf('.');
      tempStr = new String(tempStr.substring(0, ndx) + "-err.bmp");
      if (srcBuf != null && sf.getNum() == 1 && sf.getDenom() == 1) {
        if (quiet == 0)
          System.out.println("Compression error written to " + tempStr + ".");
        if (subsamp == TJ.SAMP_GRAY) {
          for (int y = 0, index = 0; y < h; y++, index += pitch) {
            for (int x = 0, index2 = index; x < w; x++, index2 += ps) {
              int rindex = index2 + TJ.getRedOffset(pf);
              int gindex = index2 + TJ.getGreenOffset(pf);
              int bindex = index2 + TJ.getBlueOffset(pf);
              int lum = (int)((double)(srcBuf[rindex] & 0xff) * 0.299 +
                              (double)(srcBuf[gindex] & 0xff) * 0.587 +
                              (double)(srcBuf[bindex] & 0xff) * 0.114 + 0.5);
              if (lum > 255) lum = 255;
              if (lum < 0) lum = 0;
              dstBuf[rindex] = (byte)Math.abs((dstBuf[rindex] & 0xff) - lum);
              dstBuf[gindex] = (byte)Math.abs((dstBuf[gindex] & 0xff) - lum);
              dstBuf[bindex] = (byte)Math.abs((dstBuf[bindex] & 0xff) - lum);
            }
          }
        } else {
          for (int y = 0; y < h; y++)
            for (int x = 0; x < w * ps; x++)
              dstBuf[pitch * y + x] =
                (byte)Math.abs((dstBuf[pitch * y + x] & 0xff) -
                               (srcBuf[pitch * y + x] & 0xff));
        }
        saveImage(tempStr, dstBuf, w, h, pf);
      }
    }
  }


  static void doTestYUV(byte[] srcBuf, int w, int h, int subsamp,
                        String fileName) throws Exception {
    TJCompressor tjc;
    byte[] dstBuf;
    double start, elapsed;
    int ps = TJ.getPixelSize(pf), i;
    int yuvSize = 0;

    yuvSize = TJ.bufSizeYUV(w, h, subsamp);
    dstBuf = new byte[yuvSize];

    if (quiet == 0)
      System.out.format(">>>>>  %s (%s) <--> YUV %s  <<<<<\n",
        pixFormatStr[pf],
        (flags & TJ.FLAG_BOTTOMUP) != 0 ? "Bottom-up" : "Top-down",
        subNameLong[subsamp]);

    if (quiet == 1)
      System.out.format("%s\t%s\t%s\tN/A\t", pixFormatStr[pf],
                        (flags & TJ.FLAG_BOTTOMUP) != 0 ? "BU" : "TD",
                        subNameLong[subsamp]);

    tjc = new TJCompressor(srcBuf, 0, 0, w, 0, h, pf);
    tjc.setSubsamp(subsamp);

    /* Execute once to preload cache */
    tjc.encodeYUV(dstBuf, flags);

    /* Benchmark */
    for (i = 0, start = getTime();
         (elapsed = getTime() - start) < benchTime; i++)
      tjc.encodeYUV(dstBuf, flags);

    if (quiet == 1)
      System.out.format("%-4d  %-4d\t", w, h);
    if (quiet != 0) {
      System.out.format("%s%c%s%c",
        sigFig((double)(w * h) / 1000000. * (double) i / elapsed, 4),
        quiet == 2 ? '\n' : '\t',
        sigFig((double)(w * h * ps) / (double)yuvSize, 4),
        quiet == 2 ? '\n' : '\t');
    } else {
      System.out.format("\n%s size: %d x %d\n", "Image", w, h);
      System.out.format("C--> Frame rate:           %f fps\n",
                        (double)i / elapsed);
      System.out.format("     Output image size:    %d bytes\n", yuvSize);
      System.out.format("     Compression ratio:    %f:1\n",
                        (double)(w * h * ps) / (double)yuvSize);
      System.out.format("     Source throughput:    %f Megapixels/sec\n",
                        (double)(w * h) / 1000000. * (double)i / elapsed);
      System.out.format("     Output bit stream:    %f Megabits/sec\n",
                        (double)yuvSize * 8. / 1000000. * (double)i / elapsed);
    }
    String tempStr = fileName + "_" + subName[subsamp] + ".yuv";
    FileOutputStream fos = new FileOutputStream(tempStr);
    fos.write(dstBuf, 0, yuvSize);
    fos.close();
    if (quiet == 0)
      System.out.println("Reference image written to " + tempStr);
  }


  static void doTest(byte[] srcBuf, int w, int h, int subsamp, int jpegQual,
                     String fileName) throws Exception {
    TJCompressor tjc;
    byte[] tmpBuf;
    byte[][] jpegBuf;
    int[] jpegSize;
    double start, elapsed;
    int totalJpegSize = 0, tilew, tileh, i;
    int ps = TJ.getPixelSize(pf), ntilesw = 1, ntilesh = 1, pitch = w * ps;

    if (yuv == YUVENCODE) {
      doTestYUV(srcBuf, w, h, subsamp, fileName);
      return;
    }

    tmpBuf = new byte[pitch * h];

    if (quiet == 0)
      System.out.format(">>>>>  %s (%s) <--> JPEG %s Q%d  <<<<<\n",
        pixFormatStr[pf],
        (flags & TJ.FLAG_BOTTOMUP) != 0 ? "Bottom-up" : "Top-down",
        subNameLong[subsamp], jpegQual);

    tjc = new TJCompressor();

    for (tilew = doTile ? 8 : w, tileh = doTile ? 8 : h; ;
         tilew *= 2, tileh *= 2) {
      if (tilew > w)
        tilew = w;
      if (tileh > h)
        tileh = h;
      ntilesw = (w + tilew - 1) / tilew;
      ntilesh = (h + tileh - 1) / tileh;

      jpegBuf = new byte[ntilesw * ntilesh][TJ.bufSize(tilew, tileh, subsamp)];
      jpegSize = new int[ntilesw * ntilesh];

      /* Compression test */
      if (quiet == 1)
        System.out.format("%s\t%s\t%s\t%d\t", pixFormatStr[pf],
                          (flags & TJ.FLAG_BOTTOMUP) != 0 ? "BU" : "TD",
                          subNameLong[subsamp], jpegQual);
      for (i = 0; i < h; i++)
        System.arraycopy(srcBuf, w * ps * i, tmpBuf, pitch * i, w * ps);
      tjc.setSourceImage(srcBuf, 0, 0, tilew, pitch, tileh, pf);
      tjc.setJPEGQuality(jpegQual);
      tjc.setSubsamp(subsamp);

      /* Execute once to preload cache */
      tjc.compress(jpegBuf[0], flags);

      /* Benchmark */
      for (i = 0, start = getTime();
           (elapsed = getTime() - start) < benchTime; i++) {
        int tile = 0;
        totalJpegSize = 0;
        for (int y = 0; y < h; y += tileh) {
          for (int x = 0; x < w; x += tilew, tile++) {
            int width = Math.min(tilew, w - x);
            int height = Math.min(tileh, h - y);
            tjc.setSourceImage(srcBuf, x, y, width, pitch, height, pf);
            tjc.compress(jpegBuf[tile], flags);
            jpegSize[tile] = tjc.getCompressedSize();
            totalJpegSize += jpegSize[tile];
          }
        }
      }

      if (quiet == 1)
        System.out.format("%-4d  %-4d\t", tilew, tileh);
      if (quiet != 0) {
        System.out.format("%s%c%s%c",
          sigFig((double)(w * h) / 1000000. * (double) i / elapsed, 4),
          quiet == 2 ? '\n' : '\t',
          sigFig((double)(w * h * ps) / (double)totalJpegSize, 4),
          quiet == 2 ? '\n' : '\t');
      } else {
        System.out.format("\n%s size: %d x %d\n", doTile ? "Tile" : "Image",
                          tilew, tileh);
        System.out.format("C--> Frame rate:           %f fps\n",
                          (double)i / elapsed);
        System.out.format("     Output image size:    %d bytes\n",
                          totalJpegSize);
        System.out.format("     Compression ratio:    %f:1\n",
                          (double)(w * h * ps) / (double)totalJpegSize);
        System.out.format("     Source throughput:    %f Megapixels/sec\n",
                          (double)(w * h) / 1000000. * (double)i / elapsed);
        System.out.format("     Output bit stream:    %f Megabits/sec\n",
          (double)totalJpegSize * 8. / 1000000. * (double)i / elapsed);
      }
      if (tilew == w && tileh == h) {
        String tempStr = fileName + "_" + subName[subsamp] + "_" + "Q" +
                         jpegQual + ".jpg";
        FileOutputStream fos = new FileOutputStream(tempStr);
        fos.write(jpegBuf[0], 0, jpegSize[0]);
        fos.close();
        if (quiet == 0)
          System.out.println("Reference image written to " + tempStr);
      }

      /* Decompression test */
      decompTest(srcBuf, jpegBuf, jpegSize, tmpBuf, w, h, subsamp, jpegQual,
                 fileName, tilew, tileh);

      if (tilew == w && tileh == h) break;
    }
  }


  static void doDecompTest(String fileName) throws Exception {
    TJTransformer tjt;
    byte[][] jpegBuf;
    byte[] srcBuf;
    int[] jpegSize;
    int totalJpegSize;
    int w = 0, h = 0, subsamp = -1, _w, _h, _tilew, _tileh,
      _ntilesw, _ntilesh, _subsamp, x, y;
    int ntilesw = 1, ntilesh = 1;
    double start, elapsed;
    int ps = TJ.getPixelSize(pf), tile;

    FileInputStream fis = new FileInputStream(fileName);
    int srcSize = (int)fis.getChannel().size();
    srcBuf = new byte[srcSize];
    fis.read(srcBuf, 0, srcSize);
    fis.close();

    int index = fileName.indexOf('.');
    if (index >= 0)
      fileName = new String(fileName.substring(0, index));

    tjt = new TJTransformer();

    tjt.setJPEGImage(srcBuf, srcSize);
    w = tjt.getWidth();
    h = tjt.getHeight();
    subsamp = tjt.getSubsamp();

    if (quiet == 1) {
      System.out.println("All performance values in Mpixels/sec\n");
      System.out.format("Bitmap\tBitmap\tJPEG\t%s %s \tXform\tComp\tDecomp\n",
                        (doTile ? "Tile " : "Image"),
                        (doTile ? "Tile " : "Image"));
      System.out.println("Format\tOrder\tSubsamp\tWidth Height\tPerf \tRatio\tPerf\n");
    } else if (quiet == 0) {
      System.out.format(">>>>>  JPEG %s --> %s (%s)  <<<<<\n",
        subNameLong[subsamp], pixFormatStr[pf],
        (flags & TJ.FLAG_BOTTOMUP) != 0 ? "Bottom-up" : "Top-down");
    }

    for (int tilew = doTile ? 16 : w, tileh = doTile ? 16 : h; ;
         tilew *= 2, tileh *= 2) {
      if (tilew > w)
        tilew = w;
      if (tileh > h)
        tileh = h;
      ntilesw = (w + tilew - 1) / tilew;
      ntilesh = (h + tileh - 1) / tileh;

      _w = w;  _h = h;  _tilew = tilew;  _tileh = tileh;
      if (quiet == 0) {
        System.out.format("\n%s size: %d x %d", (doTile ? "Tile" : "Image"),
                          _tilew, _tileh);
        if (sf.getNum() != 1 || sf.getDenom() != 1)
          System.out.format(" --> %d x %d", sf.getScaled(_w),
                            sf.getScaled(_h));
        System.out.println("");
      } else if (quiet == 1) {
        System.out.format("%s\t%s\t%s\t", pixFormatStr[pf],
                          (flags & TJ.FLAG_BOTTOMUP) != 0 ? "BU" : "TD",
                          subNameLong[subsamp]);
        System.out.format("%-4d  %-4d\t", tilew, tileh);
      }

      _subsamp = subsamp;
      if (doTile || xformOp != TJTransform.OP_NONE || xformOpt != 0) {
        if (xformOp == TJTransform.OP_TRANSPOSE ||
            xformOp == TJTransform.OP_TRANSVERSE ||
            xformOp == TJTransform.OP_ROT90 ||
            xformOp == TJTransform.OP_ROT270) {
          _w = h;  _h = w;  _tilew = tileh;  _tileh = tilew;
        }

        if ((xformOpt & TJTransform.OPT_GRAY) != 0)
          _subsamp = TJ.SAMP_GRAY;
        if (xformOp == TJTransform.OP_HFLIP ||
            xformOp == TJTransform.OP_ROT180)
          _w = _w - (_w % TJ.getMCUWidth(_subsamp));
        if (xformOp == TJTransform.OP_VFLIP ||
            xformOp == TJTransform.OP_ROT180)
          _h = _h - (_h % TJ.getMCUHeight(_subsamp));
        if (xformOp == TJTransform.OP_TRANSVERSE ||
            xformOp == TJTransform.OP_ROT90)
          _w = _w - (_w % TJ.getMCUHeight(_subsamp));
        if (xformOp == TJTransform.OP_TRANSVERSE ||
            xformOp == TJTransform.OP_ROT270)
          _h = _h - (_h % TJ.getMCUWidth(_subsamp));
        _ntilesw = (_w + _tilew - 1) / _tilew;
        _ntilesh = (_h + _tileh - 1) / _tileh;

        TJTransform[] t = new TJTransform[_ntilesw * _ntilesh];
        jpegBuf = new byte[_ntilesw * _ntilesh][TJ.bufSize(_tilew, _tileh, subsamp)];

        for (y = 0, tile = 0; y < _h; y += _tileh) {
          for (x = 0; x < _w; x += _tilew, tile++) {
            t[tile] = new TJTransform();
            t[tile].width = Math.min(_tilew, _w - x);
            t[tile].height = Math.min(_tileh, _h - y);
            t[tile].x = x;
            t[tile].y = y;
            t[tile].op = xformOp;
            t[tile].options = xformOpt | TJTransform.OPT_TRIM;
            if ((t[tile].options & TJTransform.OPT_NOOUTPUT) != 0 &&
                jpegBuf[tile] != null)
              jpegBuf[tile] = null;
          }
        }

        start = getTime();
        tjt.transform(jpegBuf, t, flags);
        jpegSize = tjt.getTransformedSizes();
        elapsed = getTime() - start;

        t = null;

        for (tile = 0, totalJpegSize = 0; tile < _ntilesw * _ntilesh; tile++)
          totalJpegSize += jpegSize[tile];

        if (quiet != 0) {
          System.out.format("%s%c%s%c",
            sigFig((double)(w * h) / 1000000. / elapsed, 4),
            quiet == 2 ? '\n' : '\t',
            sigFig((double)(w * h * ps) / (double)totalJpegSize, 4),
            quiet == 2 ? '\n' : '\t');
        } else if (quiet == 0) {
          System.out.format("X--> Frame rate:           %f fps\n",
                            1.0 / elapsed);
          System.out.format("     Output image size:    %d bytes\n",
                            totalJpegSize);
          System.out.format("     Compression ratio:    %f:1\n",
                            (double)(w * h * ps) / (double)totalJpegSize);
          System.out.format("     Source throughput:    %f Megapixels/sec\n",
                            (double)(w * h) / 1000000. / elapsed);
          System.out.format("     Output bit stream:    %f Megabits/sec\n",
                            (double)totalJpegSize * 8. / 1000000. / elapsed);
        }
      } else {
        if (quiet == 1)
          System.out.print("N/A\tN/A\t");
        jpegBuf = new byte[1][TJ.bufSize(_tilew, _tileh, subsamp)];
        jpegSize = new int[1];
        jpegSize[0] = srcSize;
        System.arraycopy(srcBuf, 0, jpegBuf[0], 0, srcSize);
      }

      if (w == tilew)
        _tilew = _w;
      if (h == tileh)
        _tileh = _h;
      if ((xformOpt & TJTransform.OPT_NOOUTPUT) == 0)
        decompTest(null, jpegBuf, jpegSize, null, _w, _h, _subsamp, 0,
                   fileName, _tilew, _tileh);
      else if (quiet == 1)
        System.out.println("N/A");

      jpegBuf = null;
      jpegSize = null;

      if (tilew == w && tileh == h) break;
    }
  }


  static void usage() throws Exception {
    int i;
    TJScalingFactor[] scalingFactors = TJ.getScalingFactors();
    int nsf = scalingFactors.length;
    String className = new TJBench().getClass().getName();

    System.out.println("\nUSAGE: java " + className);
    System.out.println("       <Inputfile (BMP)> <Quality> [options]\n");
    System.out.println("       java " + className);
    System.out.println("       <Inputfile (JPG)> [options]\n");
    System.out.println("Options:\n");
    System.out.println("-alloc = Dynamically allocate JPEG image buffers");
    System.out.println("-bottomup = Test bottom-up compression/decompression");
    System.out.println("-tile = Test performance of the codec when the image is encoded as separate");
    System.out.println("     tiles of varying sizes.");
    System.out.println("-forcemmx, -forcesse, -forcesse2, -forcesse3 =");
    System.out.println("     Force MMX, SSE, SSE2, or SSE3 code paths in the underlying codec");
    System.out.println("-rgb, -bgr, -rgbx, -bgrx, -xbgr, -xrgb =");
    System.out.println("     Test the specified color conversion path in the codec (default: BGR)");
    System.out.println("-fastupsample = Use the fastest chrominance upsampling algorithm available in");
    System.out.println("     the underlying codec");
    System.out.println("-fastdct = Use the fastest DCT/IDCT algorithms available in the underlying");
    System.out.println("     codec");
    System.out.println("-accuratedct = Use the most accurate DCT/IDCT algorithms available in the");
    System.out.println("     underlying codec");
    System.out.println("-subsamp <s> = When testing JPEG compression, this option specifies the level");
    System.out.println("     of chrominance subsampling to use (<s> = 444, 422, 440, 420, or GRAY).");
    System.out.println("     The default is to test Grayscale, 4:2:0, 4:2:2, and 4:4:4 in sequence.");
    System.out.println("-quiet = Output results in tabular rather than verbose format");
    System.out.println("-yuvencode = Encode RGB input as planar YUV rather than compressing as JPEG");
    System.out.println("-yuvdecode = Decode JPEG image to planar YUV rather than RGB");
    System.out.println("-scale M/N = scale down the width/height of the decompressed JPEG image by a");
    System.out.print  ("     factor of M/N (M/N = ");
    for (i = 0; i < nsf; i++) {
      System.out.format("%d/%d", scalingFactors[i].getNum(),
                        scalingFactors[i].getDenom());
      if (nsf == 2 && i != nsf - 1)
        System.out.print(" or ");
      else if (nsf > 2) {
        if (i != nsf - 1)
          System.out.print(", ");
        if (i == nsf - 2)
          System.out.print("or ");
      }
      if (i % 8 == 0 && i != 0)
        System.out.print("\n     ");
    }
    System.out.println(")");
    System.out.println("-hflip, -vflip, -transpose, -transverse, -rot90, -rot180, -rot270 =");
    System.out.println("     Perform the corresponding lossless transform prior to");
    System.out.println("     decompression (these options are mutually exclusive)");
    System.out.println("-grayscale = Perform lossless grayscale conversion prior to decompression");
    System.out.println("     test (can be combined with the other transforms above)");
    System.out.println("-benchtime <t> = Run each benchmark for at least <t> seconds (default = 5.0)\n");
    System.out.println("NOTE:  If the quality is specified as a range (e.g. 90-100), a separate");
    System.out.println("test will be performed for all quality values in the range.\n");
    System.exit(1);
  }


  public static void main(String[] argv) {
    byte[] srcBuf = null;  int w = 0, h = 0;
    int minQual = -1, maxQual = -1;
    int minArg = 1;  int retval = 0;
    int subsamp = -1;

    try {

      if (argv.length < minArg)
        usage();

      String tempStr = argv[0].toLowerCase();
      if (tempStr.endsWith(".jpg") || tempStr.endsWith(".jpeg"))
        decompOnly = true;

      System.out.println("");

      if (argv.length > minArg) {
        for (int i = minArg; i < argv.length; i++) {
          if (argv[i].equalsIgnoreCase("-yuvencode")) {
            System.out.println("Testing YUV planar encoding\n");
            yuv = YUVENCODE;  maxQual = minQual = 100;
          }
          if (argv[i].equalsIgnoreCase("-yuvdecode")) {
            System.out.println("Testing YUV planar decoding\n");
            yuv = YUVDECODE;
          }
        }
      }

      if (!decompOnly && yuv != YUVENCODE) {
        minArg = 2;
        if (argv.length < minArg)
          usage();
        try {
          minQual = Integer.parseInt(argv[1]);
        } catch (NumberFormatException e) {}
        if (minQual < 1 || minQual > 100)
          throw new Exception("Quality must be between 1 and 100.");
        int dashIndex = argv[1].indexOf('-');
        if (dashIndex > 0 && argv[1].length() > dashIndex + 1) {
          try {
            maxQual = Integer.parseInt(argv[1].substring(dashIndex + 1));
          } catch (NumberFormatException e) {}
        }
        if (maxQual < 1 || maxQual > 100)
          maxQual = minQual;
      }

      if (argv.length > minArg) {
        for (int i = minArg; i < argv.length; i++) {
          if (argv[i].equalsIgnoreCase("-tile")) {
            doTile = true;  xformOpt |= TJTransform.OPT_CROP;
          }
          if (argv[i].equalsIgnoreCase("-forcesse3")) {
            System.out.println("Forcing SSE3 code\n");
            flags |= TJ.FLAG_FORCESSE3;
          }
          if (argv[i].equalsIgnoreCase("-forcesse2")) {
            System.out.println("Forcing SSE2 code\n");
            flags |= TJ.FLAG_FORCESSE2;
          }
          if (argv[i].equalsIgnoreCase("-forcesse")) {
            System.out.println("Forcing SSE code\n");
            flags |= TJ.FLAG_FORCESSE;
          }
          if (argv[i].equalsIgnoreCase("-forcemmx")) {
            System.out.println("Forcing MMX code\n");
            flags |= TJ.FLAG_FORCEMMX;
          }
          if (argv[i].equalsIgnoreCase("-fastupsample")) {
            System.out.println("Using fast upsampling code\n");
            flags |= TJ.FLAG_FASTUPSAMPLE;
          }
          if (argv[i].equalsIgnoreCase("-fastdct")) {
            System.out.println("Using fastest DCT/IDCT algorithm\n");
            flags |= TJ.FLAG_FASTDCT;
          }
          if (argv[i].equalsIgnoreCase("-accuratedct")) {
            System.out.println("Using most accurate DCT/IDCT algorithm\n");
            flags |= TJ.FLAG_ACCURATEDCT;
          }
          if (argv[i].equalsIgnoreCase("-rgb"))
            pf = TJ.PF_RGB;
          if (argv[i].equalsIgnoreCase("-rgbx"))
            pf = TJ.PF_RGBX;
          if (argv[i].equalsIgnoreCase("-bgr"))
            pf = TJ.PF_BGR;
          if (argv[i].equalsIgnoreCase("-bgrx"))
            pf = TJ.PF_BGRX;
          if (argv[i].equalsIgnoreCase("-xbgr"))
            pf = TJ.PF_XBGR;
          if (argv[i].equalsIgnoreCase("-xrgb"))
            pf = TJ.PF_XRGB;
          if (argv[i].equalsIgnoreCase("-bottomup"))
            flags |= TJ.FLAG_BOTTOMUP;
          if (argv[i].equalsIgnoreCase("-quiet"))
            quiet = 1;
          if (argv[i].equalsIgnoreCase("-qq"))
            quiet = 2;
          if (argv[i].equalsIgnoreCase("-scale") && i < argv.length - 1) {
            int temp1 = 0, temp2 = 0;
            boolean match = false, scanned = true;
            Scanner scanner = new Scanner(argv[++i]).useDelimiter("/");
            try {
              temp1 = scanner.nextInt();
              temp2 = scanner.nextInt();
            } catch(Exception e) {}
            if (temp2 <= 0) temp2 = 1;
            if (temp1 > 0) {
              TJScalingFactor[] scalingFactors = TJ.getScalingFactors();
              for (int j = 0; j < scalingFactors.length; j++) {
                if ((double)temp1 / (double)temp2 ==
                    (double)scalingFactors[j].getNum() /
                    (double)scalingFactors[j].getDenom()) {
                  sf = scalingFactors[j];
                  match = true;   break;
                }
              }
              if (!match) usage();
            } else
              usage();
          }
          if (argv[i].equalsIgnoreCase("-hflip"))
            xformOp = TJTransform.OP_HFLIP;
          if (argv[i].equalsIgnoreCase("-vflip"))
            xformOp = TJTransform.OP_VFLIP;
          if (argv[i].equalsIgnoreCase("-transpose"))
            xformOp = TJTransform.OP_TRANSPOSE;
          if (argv[i].equalsIgnoreCase("-transverse"))
            xformOp = TJTransform.OP_TRANSVERSE;
          if (argv[i].equalsIgnoreCase("-rot90"))
            xformOp = TJTransform.OP_ROT90;
          if (argv[i].equalsIgnoreCase("-rot180"))
            xformOp = TJTransform.OP_ROT180;
          if (argv[i].equalsIgnoreCase("-rot270"))
            xformOp = TJTransform.OP_ROT270;
          if (argv[i].equalsIgnoreCase("-grayscale"))
            xformOpt |= TJTransform.OPT_GRAY;
          if (argv[i].equalsIgnoreCase("-nooutput"))
            xformOpt |= TJTransform.OPT_NOOUTPUT;
          if (argv[i].equalsIgnoreCase("-benchtime") && i < argv.length - 1) {
            double temp = -1;
            try {
              temp = Double.parseDouble(argv[++i]);
            } catch (NumberFormatException e) {}
            if (temp > 0.0)
              benchTime = temp;
            else
              usage();
          }
          if (argv[i].equalsIgnoreCase("-subsamp") && i < argv.length - 1) {
            i++;
            if (argv[i].toUpperCase().startsWith("G"))
              subsamp = TJ.SAMP_GRAY;
            else if (argv[i].equals("444"))
              subsamp = TJ.SAMP_444;
            else if (argv[i].equals("422"))
              subsamp = TJ.SAMP_422;
            else if (argv[i].equals("440"))
              subsamp = TJ.SAMP_440;
            else if (argv[i].equals("420"))
              subsamp = TJ.SAMP_420;
          }
          if (argv[i].equalsIgnoreCase("-?"))
            usage();
        }
      }

      if (sf == null)
        sf = new TJScalingFactor(1, 1);

      if ((sf.getNum() != 1 || sf.getDenom() != 1) && doTile) {
        System.out.println("Disabling tiled compression/decompression tests, because those tests do not");
        System.out.println("work when scaled decompression is enabled.");
        doTile = false;
      }

      if (yuv != 0 && doTile) {
        System.out.println("Disabling tiled compression/decompression tests, because those tests do not");
        System.out.println("work when YUV encoding or decoding is enabled.\n");
        doTile = false;
      }

      if (!decompOnly) {
        int[] width = new int[1], height = new int[1];
        srcBuf = loadImage(argv[0], width, height, pf);
        w = width[0];  h = height[0];
        int index = -1;
        if ((index = argv[0].indexOf('.')) >= 0)
          argv[0] = argv[0].substring(0, index);
      }

      if (quiet == 1 && !decompOnly) {
        System.out.println("All performance values in Mpixels/sec\n");
        System.out.format("Bitmap\tBitmap\tJPEG\tJPEG\t%s %s \tComp\tComp\tDecomp\n",
          (doTile ? "Tile " : "Image"), (doTile ? "Tile " : "Image"));
        System.out.println("Format\tOrder\tSubsamp\tQual\tWidth Height\tPerf \tRatio\tPerf\n");
      }

      if (decompOnly) {
        doDecompTest(argv[0]);
        System.out.println("");
        System.exit(retval);
      }

      System.gc();
      if (subsamp >= 0 && subsamp < TJ.NUMSAMP) {
        for (int i = maxQual; i >= minQual; i--)
          doTest(srcBuf, w, h, subsamp, i, argv[0]);
        System.out.println("");
      } else {
        for (int i = maxQual; i >= minQual; i--)
          doTest(srcBuf, w, h, TJ.SAMP_GRAY, i, argv[0]);
        System.out.println("");
        System.gc();
        for (int i = maxQual; i >= minQual; i--)
          doTest(srcBuf, w, h, TJ.SAMP_420, i, argv[0]);
        System.out.println("");
        System.gc();
        for (int i = maxQual; i >= minQual; i--)
          doTest(srcBuf, w, h, TJ.SAMP_422, i, argv[0]);
        System.out.println("");
        System.gc();
        for (int i = maxQual; i >= minQual; i--)
          doTest(srcBuf, w, h, TJ.SAMP_444, i, argv[0]);
        System.out.println("");
      }

    } catch (Exception e) {
      System.out.println("ERROR: " + e.getMessage());
      e.printStackTrace();
      retval = -1;
    }

    System.exit(retval);
  }

}
