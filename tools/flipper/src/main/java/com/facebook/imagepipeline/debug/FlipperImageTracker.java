/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.CacheKeyUtil;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfData;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.backends.pipeline.info.VisibilityState;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

/** Fresco image tracker for Sonar */
public class FlipperImageTracker implements DebugImageTracker, ImagePerfDataListener {

  private static final int MAX_IMAGES_TO_TRACK = 1000;

  private final Map<ImageRequest, ImageDebugData> mImageRequestDebugDataMap;
  private final Map<CacheKey, ImageDebugData> mImageDebugDataMap;

  public FlipperImageTracker() {
    mImageRequestDebugDataMap = new LruMap<>(MAX_IMAGES_TO_TRACK);
    mImageDebugDataMap = new LruMap<>(MAX_IMAGES_TO_TRACK);
  }

  @Override
  public synchronized void trackImage(ImageRequest imageRequest, CacheKey cacheKey) {
    ImageDebugData imageDebugData = mImageRequestDebugDataMap.get(imageRequest);
    if (imageDebugData == null) {
      imageDebugData = new ImageDebugData(imageRequest);
      mImageDebugDataMap.put(cacheKey, imageDebugData);
      mImageRequestDebugDataMap.put(imageRequest, imageDebugData);
    }
    imageDebugData.addCacheKey(cacheKey);
    imageDebugData.addResourceId(CacheKeyUtil.getFirstResourceId(cacheKey));
  }

  @Override
  public synchronized void trackImageRequest(ImageRequest imageRequest, String requestId) {
    ImageDebugData imageDebugData = mImageRequestDebugDataMap.get(imageRequest);
    if (imageDebugData == null) {
      imageDebugData = new ImageDebugData(imageRequest);
      mImageRequestDebugDataMap.put(imageRequest, imageDebugData);
    }
    imageDebugData.addRequestId(requestId);
  }

  public synchronized ImageDebugData trackImage(String localPath, CacheKey key) {
    ImageDebugData data = new ImageDebugData(localPath);
    mImageDebugDataMap.put(key, data);
    return data;
  }

  public synchronized ImageDebugData trackImage(CacheKey key) {
    ImageDebugData data = new ImageDebugData();
    mImageDebugDataMap.put(key, data);
    return data;
  }

  public synchronized String getUriString(CacheKey key) {
    ImageDebugData imageDebugData = getImageDebugData(key);
    if (imageDebugData != null) {
      ImageRequest imageRequest = imageDebugData.getImageRequest();
      if (imageRequest != null) {
        return imageRequest.getSourceUri().toString();
      }
    }
    return key.getUriString();
  }

  @Nullable
  public synchronized String getLocalPath(CacheKey key) {
    ImageDebugData imageDebugData = getImageDebugData(key);
    if (imageDebugData != null) {
      return imageDebugData.getLocalPath();
    }
    return null;
  }

  @Nullable
  public synchronized ImageDebugData getImageDebugData(CacheKey key) {
    return mImageDebugDataMap.get(key);
  }

  @Nullable
  public synchronized ImageDebugData getDebugDataForRequestId(String requestId) {
    for (ImageDebugData debugData : mImageRequestDebugDataMap.values()) {
      Set<String> requestIds = debugData.getRequestIds();
      if (requestIds != null && requestIds.contains(requestId)) {
        return debugData;
      }
    }
    return null;
  }

  @Nullable
  public synchronized ImageDebugData getDebugDataForResourceId(String resourceId) {
    for (ImageDebugData debugData : mImageRequestDebugDataMap.values()) {
      Set<String> ids = debugData.getResourceIds();
      if (ids != null && ids.contains(resourceId)) {
        return debugData;
      }
    }
    return null;
  }

  @Nullable
  public synchronized CacheKey getCacheKey(String imageId) {
    for (Map.Entry<CacheKey, ImageDebugData> entry : mImageDebugDataMap.entrySet()) {
      if (entry.getValue().getUniqueId().equals(imageId)) {
        return entry.getKey();
      }
    }
    return null;
  }

  @Override
  public synchronized void onImageLoadStatusUpdated(
      ImagePerfData imagePerfData, @ImageLoadStatus int imageLoadStatus) {
    if (imagePerfData == null || imagePerfData.getImageRequest() == null) {
      return;
    }
    ImageDebugData debugData = mImageRequestDebugDataMap.get(imagePerfData.getImageRequest());
    if (debugData != null) {
      debugData.setImagePerfData(imagePerfData);
    } else {
      ImageDebugData imageDebugData = new ImageDebugData(imagePerfData.getImageRequest());
      imageDebugData.setImagePerfData(imagePerfData);
      mImageRequestDebugDataMap.put(imagePerfData.getImageRequest(), imageDebugData);
    }
  }

  @Override
  public synchronized void onImageVisibilityUpdated(
      ImagePerfData imagePerfData, @VisibilityState int visibilityState) {
    // ignore
  }

  public static class ImageDebugData {

    private final @Nullable ImageRequest mImageRequest;

    private @Nullable ImagePerfData mImagePerfData;
    private @Nullable Set<CacheKey> mCacheKeys;
    private @Nullable Set<String> mRequestIds;
    private @Nullable Set<String> mResourceIds;
    private @Nullable String mLocalPath;

    public ImageDebugData() {
      this(null, null);
    }

    public ImageDebugData(@Nullable ImageRequest imageRequest) {
      this(imageRequest, null);
    }

    public ImageDebugData(@Nullable String localPath) {
      this(null, localPath);
    }

    public ImageDebugData(@Nullable ImageRequest imageRequest, @Nullable String localPath) {
      mImageRequest = imageRequest;
      mLocalPath = localPath;
    }

    @Nullable
    public ImageRequest getImageRequest() {
      return mImageRequest;
    }

    @Nullable
    public Set<CacheKey> getCacheKeys() {
      return mCacheKeys;
    }

    public void addCacheKey(CacheKey cacheKey) {
      if (mCacheKeys == null) {
        mCacheKeys = new HashSet<>();
      }
      mCacheKeys.add(cacheKey);
    }

    @Nullable
    public Set<String> getRequestIds() {
      return mRequestIds;
    }

    public String getUniqueId() {
      return Integer.toString(hashCode());
    }

    public void addRequestId(String requestId) {
      if (mRequestIds == null) {
        mRequestIds = new HashSet<>();
      }
      mRequestIds.add(requestId);
    }

    public void addResourceId(String resourceId) {
      if (resourceId == null) {
        return;
      }
      if (mResourceIds == null) {
        mResourceIds = new HashSet<>();
      }
      mResourceIds.add(resourceId);
    }

    @Nullable
    public ImagePerfData getImagePerfData() {
      return mImagePerfData;
    }

    public void setImagePerfData(@Nullable ImagePerfData imagePerfData) {
      mImagePerfData = imagePerfData;
    }

    @Nullable
    public Set<String> getResourceIds() {
      return mResourceIds;
    }

    @Nullable
    public String getLocalPath() {
      return mLocalPath;
    }
  }
}
