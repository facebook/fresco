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
package com.facebook.samples.decoders.color;

import javax.annotation.Nullable;

import java.io.IOException;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.v4.graphics.ColorUtils;

import com.facebook.common.internal.ByteStreams;
import com.facebook.drawee.backends.pipeline.DrawableFactory;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imageformat.ImageFormatCheckerUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;

/**
 * Example for a simple decoder that can decode color images that have the following format:
 *
 *     <color>#FF5722</color>
 */
public class ColorImageExample {

  /**
   * XML color tag that our colors must start with.
   */
  public static final String COLOR_TAG = "<color>";

  /**
   * Custom {@link ImageFormat} for color images.
   */
  public static final ImageFormat COLOR = new ImageFormat("COLOR", "color");

  /**
   * Create a new image format checker for {@link #COLOR}.
   * @return the image format checker
   */
  public static ImageFormat.FormatChecker getChecker() {
    return new ColorFormatChecker();
  }

  /**
   * Create a new decoder that can decode {@link #COLOR} images.
   * @return the decoder
   */
  public static ImageDecoder getDecoder() {
    return new ColorDecoder();
  }

  /**
   * Custom color format checker that verifies that the header of the file
   * corresponds to our {@link #COLOR_TAG}.
   */
  public static class ColorFormatChecker implements ImageFormat.FormatChecker {

    public static final byte[] HEADER = ImageFormatCheckerUtils.asciiBytes(COLOR_TAG);

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
        return COLOR;
      }
      return null;
    }
  }

  /**
   * Custom closeable color image that holds a single color int value.
   */
  public static class CloseableColorImage extends CloseableImage {

    @ColorInt
    private final int mColor;

    private boolean mClosed = false;

    public CloseableColorImage(int color) {
      mColor = color;
    }

    @ColorInt
    public int getColor() {
      return mColor;
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
   * Decodes a color XML tag: <color>#rrggbb</color>
   */
  public static class ColorDecoder implements ImageDecoder {

    @Override
    public CloseableImage decode(
        EncodedImage encodedImage,
        int length,
        QualityInfo qualityInfo,
        ImageDecodeOptions options) {
      try {
        // Read the file as a string
        String text = new String(ByteStreams.toByteArray(encodedImage.getInputStream()));

        // Check if the string matches "<color>#"
        if (!text.startsWith(COLOR_TAG + "#")) {
          return null;
        }

        // Parse the int value between # and <
        int startIndex = COLOR_TAG.length() + 1;
        int endIndex = text.lastIndexOf('<');
        int color = Integer.parseInt(text.substring(startIndex, endIndex), 16);

        // Add the alpha component so that we actually see the color
        color = ColorUtils.setAlphaComponent(color, 255);

        // Return the CloseableImage
        return new CloseableColorImage(color);
      } catch (IOException e) {
        e.printStackTrace();
      }
      // Return nothing if an error occurred
      return null;
    }
  }

  /**
   * Color drawable factory that is able to render a {@link CloseableColorImage} by creating
   * a new {@link ColorDrawable} for the given color.
   */
  public static class ColorDrawableFactory implements DrawableFactory {

    @Override
    public boolean supportsImageType(CloseableImage image) {
      // We can only handle CloseableColorImages
      return image instanceof CloseableColorImage;
    }

    @Nullable
    @Override
    public Drawable createDrawable(CloseableImage image) {
      // Just return a simple ColorDrawable with the given color value
      return new ColorDrawable(((CloseableColorImage)image).getColor());
    }
  }
}
