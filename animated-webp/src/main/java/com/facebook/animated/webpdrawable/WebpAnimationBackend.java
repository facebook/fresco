/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.webpdrawable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import com.facebook.animated.webp.WebPFrame;
import com.facebook.animated.webp.WebPImage;
import com.facebook.fresco.animation.backend.AnimationBackend;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/** Animation backend that is used to draw webp frames. */
public class WebpAnimationBackend implements AnimationBackend {

  private final Rect mRenderDstRect = new Rect();
  private final Rect mRenderSrcRect = new Rect();
  private final WebPImage mWebPImage;

  private Rect mBounds;

  @GuardedBy("this")
  private @Nullable Bitmap mTempBitmap;

  public static WebpAnimationBackend create(String filePath) throws IOException {
    InputStream is = null;
    try {
      is = new BufferedInputStream(new FileInputStream(filePath));
      is.mark(Integer.MAX_VALUE);
      byte[] targetArray = new byte[is.available()];
      is.read(targetArray);

      WebPImage webPImage = WebPImage.createFromByteArray(targetArray);
      is.reset();

      return new WebpAnimationBackend(webPImage);
    } finally {
      closeSilently(is);
    }
  }

  private WebpAnimationBackend(WebPImage webPImage) {
    mWebPImage = webPImage;
  }

  @Override
  public boolean drawFrame(Drawable parent, Canvas canvas, int frameNumber) {
    WebPFrame frame = mWebPImage.getFrame(frameNumber);

    double xScale = (double) mBounds.width() / (double) parent.getIntrinsicWidth();
    double yScale = (double) mBounds.height() / (double) parent.getIntrinsicHeight();

    int frameWidth = (int) Math.round(frame.getWidth() * xScale);
    int frameHeight = (int) Math.round(frame.getHeight() * yScale);
    int xOffset = (int) (frame.getXOffset() * xScale);
    int yOffset = (int) (frame.getYOffset() * yScale);

    synchronized (this) {
      int renderedWidth = mBounds.width();
      int renderedHeight = mBounds.height();
      // Update the temp bitmap to be >= rendered dimensions
      prepareTempBitmapForThisSize(renderedWidth, renderedHeight);
      if (mTempBitmap == null) {
        return false;
      }
      frame.renderFrame(frameWidth, frameHeight, mTempBitmap);
      // Temporary bitmap can be bigger than frame, so we should draw only rendered area of bitmap
      mRenderSrcRect.set(0, 0, renderedWidth, renderedHeight);
      mRenderDstRect.set(xOffset, yOffset, xOffset + renderedWidth, yOffset + renderedHeight);

      canvas.drawBitmap(mTempBitmap, mRenderSrcRect, mRenderDstRect, null);
    }
    return true;
  }

  @Override
  public void setAlpha(int alpha) {
    // unimplemented
  }

  @Override
  public void setColorFilter(@Nullable ColorFilter colorFilter) {
    // unimplemented
  }

  @Override
  public synchronized void setBounds(Rect bounds) {
    mBounds = bounds;
  }

  @Override
  public int getIntrinsicWidth() {
    return mWebPImage.getWidth();
  }

  @Override
  public int getIntrinsicHeight() {
    return mWebPImage.getHeight();
  }

  @Override
  public int getSizeInBytes() {
    return 0;
  }

  @Override
  public void clear() {
    mWebPImage.dispose();
  }

  @Override
  public int getFrameCount() {
    return mWebPImage.getFrameCount();
  }

  @Override
  public int getFrameDurationMs(int frameNumber) {
    return mWebPImage.getFrameDurations()[frameNumber];
  }

  @Override
  public int getLoopCount() {
    return mWebPImage.getLoopCount();
  }

  private synchronized void prepareTempBitmapForThisSize(int width, int height) {
    // Different webp frames can be different size,
    // So we need to ensure we can fit next frame to temporary bitmap
    if (mTempBitmap != null
        && (mTempBitmap.getWidth() < width || mTempBitmap.getHeight() < height)) {
      clearTempBitmap();
    }
    if (mTempBitmap == null) {
      mTempBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }
    mTempBitmap.eraseColor(Color.TRANSPARENT);
  }

  private synchronized void clearTempBitmap() {
    if (mTempBitmap != null) {
      mTempBitmap.recycle();
      mTempBitmap = null;
    }
  }

  private static void closeSilently(@Nullable Closeable closeable) {
    if (closeable == null) {
      return;
    }
    try {
      closeable.close();
    } catch (IOException ignored) {
      // ignore
    }
  }
}
