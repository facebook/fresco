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
package com.facebook.fresco.samples.showcase.imageformat.keyframes;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import com.facebook.common.internal.Closeables;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatCheckerUtils;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.keyframes.KeyframesDrawableBuilder;
import com.facebook.keyframes.deserializers.KFImageDeserializer;
import com.facebook.keyframes.model.KFImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Decoder and related classes for loading and displaying Keyframe animated images.
 */
public class KeyframesDecoderExample {

  public static final ImageFormat IMAGE_FORMAT_KEYFRAMES
      = new ImageFormat("KEYFRAMES", "keyframes");

  public static ImageFormat.FormatChecker createFormatChecker() {
    return new KeyframesFormatChecker();
  }

  public static ImageDecoder createDecoder() {
    return new KeyframesDecoder();
  }

  public static DrawableFactory createDrawableFactory() {
    return new KeyframesDrawableFactory();
  }

  private static class KeyframesFormatChecker implements ImageFormat.FormatChecker {

    private static final int HEADER_SIZE = 100;
    private static final byte[] JSON_OBJECT_FIRST_BYTE =
        ImageFormatCheckerUtils.asciiBytes("{");

    @Override
    public int getHeaderSize() {
      return HEADER_SIZE;
    }

    /**
     * Although keyframe files do not have an explicit header, we try to match by looking for a JSON
     * file start and known mandatory keys.
     */
    @Nullable
    @Override
    public ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
      // JSON files must start with a opening curly brace
      if (!ImageFormatCheckerUtils.startsWithPattern(
          headerBytes,
          JSON_OBJECT_FIRST_BYTE)) {
        return null;
      }

      // we expect the format version to be in the header of any proper keyframe file
      final String expectedFormatVersionKey = "formatVersion";
      final String headerString = new String(headerBytes);
      if (!headerString.contains(expectedFormatVersionKey)) {
        return null;
      }
      return IMAGE_FORMAT_KEYFRAMES;
    }
  }

  private static class KeyframesDecoder implements ImageDecoder {

    @Override
    public CloseableImage decode(
        EncodedImage encodedImage,
        int length,
        QualityInfo qualityInfo,
        ImageDecodeOptions options) {
      InputStream encodedInputStream = null;
      try {
        encodedInputStream = encodedImage.getInputStream();
        return new CloseableKeyframesImage(
            KFImageDeserializer.deserialize(encodedInputStream));
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      } finally {
        Closeables.closeQuietly(encodedInputStream);
      }
    }
  }

  private static class CloseableKeyframesImage extends CloseableImage {

    private boolean mClosed;
    private final KFImage mImage;

    CloseableKeyframesImage(KFImage image) {
      mImage = image;
    }

    KFImage getImage() {
      return mImage;
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
      return (int) mImage.getCanvasSize()[0];
    }

    @Override
    public int getHeight() {
      return (int) mImage.getCanvasSize()[1];
    }

    @Override
    public boolean isStateful() {
      return true;
    }
  }

  private static class KeyframesDrawableFactory implements DrawableFactory {

    @Override
    public boolean supportsImageType(CloseableImage image) {
      return image instanceof CloseableKeyframesImage;
    }

    @Nullable
    @Override
    public Drawable createDrawable(CloseableImage image) {
      KFImage kfImage = ((CloseableKeyframesImage) image).getImage();
      return new AnimatableKeyframesDrawable(
          new KeyframesDrawableBuilder().withImage(kfImage).build());
    }
  }
}
