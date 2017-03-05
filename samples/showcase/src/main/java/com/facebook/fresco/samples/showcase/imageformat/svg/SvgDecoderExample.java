/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.fresco.samples.showcase.imageformat.svg;

import javax.annotation.Nullable;

import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;

import com.facebook.drawee.backends.pipeline.DrawableFactory;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatCheckerUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;

/**
 * SVG example that defines all classes required to decode and render SVG images.
 */
public class SvgDecoderExample {

  public static final ImageFormat SVG_FORMAT = new ImageFormat("SVG_FORMAT", "svg");

  // We do not include the closing ">" since there can be additional information
  private static final String HEADER_TAG = "<svg";

  public static class SvgFormatChecker implements ImageFormat.FormatChecker {

    public static final byte[] HEADER = ImageFormatCheckerUtils.asciiBytes(HEADER_TAG);

    @Override
    public int getHeaderSize() {
      return HEADER.length;
    }

    @Nullable
    @Override
    public ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
      if (headerSize < getHeaderSize()) {
        return null;
      }
      if (ImageFormatCheckerUtils.startsWithPattern(headerBytes, HEADER)) {
        return SVG_FORMAT;
      }
      return null;
    }
  }

  public static class CloseableSvgImage extends CloseableImage {

    private final SVG mSvg;

    private boolean mClosed = false;

    public CloseableSvgImage(SVG svg) {
      mSvg = svg;
    }

    public SVG getSvg() {
      return mSvg;
    }

    @Override
    public int getSizeInBytes() {
      return 0;
    }

    @Override
    public void close() {
      mClosed = true;
    }

    @Override
    public boolean isClosed() {
      return mClosed;
    }

    @Override
    public int getWidth() {
      return 0;
    }

    @Override
    public int getHeight() {
      return 0;
    }
  }

  /**
   * Decodes a SVG_FORMAT image
   */
  public static class SvgDecoder implements ImageDecoder {

    @Override
    public CloseableImage decode(
        EncodedImage encodedImage,
        int length,
        QualityInfo qualityInfo,
        ImageDecodeOptions options) {
      try {
        SVG svg = SVG.getFromInputStream(encodedImage.getInputStream());
        return new CloseableSvgImage(svg);
      } catch (SVGParseException e) {
        e.printStackTrace();
      }
      return null;
    }
  }

  /**
   * SVG drawable factory that creates {@link PictureDrawable}s for SVG images.
   */
  public static class SvgDrawableFactory implements DrawableFactory {

    @Override
    public boolean supportsImageType(CloseableImage image) {
      return image instanceof CloseableSvgImage;
    }

    @Nullable
    @Override
    public Drawable createDrawable(CloseableImage image) {
      return new SvgPictureDrawable(((CloseableSvgImage) image).getSvg());
    }
  }

  public static class SvgPictureDrawable extends PictureDrawable {

    private final SVG mSvg;

    public SvgPictureDrawable(SVG svg) {
      super(null);
      mSvg = svg;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
      super.onBoundsChange(bounds);
      setPicture(mSvg.renderToPicture(bounds.width(), bounds.height()));
    }
  }
}
