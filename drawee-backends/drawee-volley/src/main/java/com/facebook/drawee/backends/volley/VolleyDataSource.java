/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.volley;

import android.graphics.Bitmap;
import android.net.Uri;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.AbstractDataSource;

/**
 * {@link DataSource} that wraps Volley {@link ImageLoader}.
 */
public class VolleyDataSource extends AbstractDataSource<Bitmap> {
  private ImageLoader.ImageContainer mImageContainer;

  public VolleyDataSource(
      final ImageLoader imageLoader,
      final Uri imageRequest,
      final boolean bitmapCacheOnly) {

    // TODO: add VolleyImageRequest {uri, resizeOptions, bitmapCacheOnly, ...}
    String uriString = imageRequest.toString();
    int maxWidth = 0;
    int maxHeight = 0;

    if (bitmapCacheOnly) {
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
      mImageContainer.cancelRequest();
    }
    return super.close();
  }
}
