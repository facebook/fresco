/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import com.facebook.common.logging.FLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ForwardingImageOriginListener implements ImageOriginListener {

  private static final String TAG = "ForwardingImageOriginListener";

  private final List<ImageOriginListener> mImageOriginListeners;

  public ForwardingImageOriginListener(Set<ImageOriginListener> requestListeners) {
    mImageOriginListeners = new ArrayList<>(requestListeners.size());
    for (ImageOriginListener requestListener : requestListeners) {
      if (requestListener != null) {
        mImageOriginListeners.add(requestListener);
      }
    }
  }

  public ForwardingImageOriginListener(ImageOriginListener... requestListeners) {
    mImageOriginListeners = new ArrayList<>(requestListeners.length);
    for (ImageOriginListener requestListener : requestListeners) {
      if (requestListener != null) {
        mImageOriginListeners.add(requestListener);
      }
    }
  }

  @Override
  public void onImageLoaded(String controllerId, int imageOrigin, boolean successful) {
    final int numberOfListeners = mImageOriginListeners.size();
    for (int i = 0; i < numberOfListeners; i++) {
      ImageOriginListener listener = mImageOriginListeners.get(i);
      try {
        listener.onImageLoaded(controllerId, imageOrigin, successful);
      } catch (Exception e) {
        FLog.e(TAG, "InternalListener exception in onImageLoaded", e);
      }
    }
  }
}
