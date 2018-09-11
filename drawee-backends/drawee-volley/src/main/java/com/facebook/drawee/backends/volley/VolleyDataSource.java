/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.volley;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.facebook.datasource.AbstractDataSource;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;

/**
 * {@link DataSource} that wraps Volley {@link ImageLoader}.
 */
public class VolleyDataSource extends AbstractDataSource<Bitmap> {
  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private ImageLoader.ImageContainer mImageContainer;

  public VolleyDataSource(
      final ImageLoader imageLoader,
      final Uri imageRequest,
      final AbstractDraweeControllerBuilder.CacheLevel cacheLevel) {

    String uriString = imageRequest.toString();
    int maxWidth = 0;
    int maxHeight = 0;

    if (cacheLevel != AbstractDraweeControllerBuilder.CacheLevel.FULL_FETCH) {
      if (!imageLoader.isCached(uriString, maxWidth, maxHeight)) {
        mImageContainer = null;
        setFailure(new NullPointerException("Image not found in bitmap-cache."));
        return;
      }
    }

    mImageContainer = imageLoader.get(
        uriString,
        new ImageLoader.ImageListener() {
          @Override
          public void onErrorResponse(VolleyError error) {
            setFailure(error.getCause());
          }
          @Override
          public void onResponse(final ImageLoader.ImageContainer response, boolean isImmediate) {
            if (response.getBitmap() != null) {
              setResult(response.getBitmap(), true);
            }
          }
        },
        maxWidth,
        maxHeight);
  }

  @Override
  public boolean close() {
    if (mImageContainer != null) {
      // Prevent ConcurrentModificationException in Volley
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          if (mImageContainer != null) {
            mImageContainer.cancelRequest();
          }
        }
      });
    }
    return super.close();
  }
}
