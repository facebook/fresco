/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.net.Uri;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.image.EncodedImage;
import javax.annotation.Nullable;

/**
 * Used by {@link NetworkFetcher} to encapsulate the state of one network fetch.
 *
 * <p>Implementations can subclass this to store additional fetch-scoped fields.
 */
public class FetchState {

  private final Consumer<EncodedImage> mConsumer;
  private final ProducerContext mContext;
  private long mLastIntermediateResultTimeMs;
  private int mOnNewResultStatusFlags;
  private @Nullable BytesRange mResponseBytesRange;

  public FetchState(
      Consumer<EncodedImage> consumer,
      ProducerContext context) {
    mConsumer = consumer;
    mContext = context;
    mLastIntermediateResultTimeMs = 0;
  }

  public Consumer<EncodedImage> getConsumer() {
    return mConsumer;
  }

  public ProducerContext getContext() {
    return mContext;
  }

  public String getId() {
    return mContext.getId();
  }

  public ProducerListener getListener() {
    return mContext.getListener();
  }

  public Uri getUri() {
    return mContext.getImageRequest().getSourceUri();
  }

  public long getLastIntermediateResultTimeMs() {
    return mLastIntermediateResultTimeMs;
  }

  public void setLastIntermediateResultTimeMs(long lastIntermediateResultTimeMs) {
    mLastIntermediateResultTimeMs = lastIntermediateResultTimeMs;
  }

  @Consumer.Status public int getOnNewResultStatusFlags() {
    return mOnNewResultStatusFlags;
  }

  /**
   * EXPERIMENTAL: Allows the fetcher to set extra status flags to be included in calls to
   * {@link Consumer#onNewResult(Object, int)}.
   */
  public void setOnNewResultStatusFlags(@Consumer.Status int onNewResultStatusFlags) {
    mOnNewResultStatusFlags = onNewResultStatusFlags;
  }

  @Nullable
  public BytesRange getResponseBytesRange() {
    return mResponseBytesRange;
  }

  /**
   * EXPERIMENTAL: Allows the fetcher to identify that the reponse is for an imcomplete portion of
   * the whole image by defining the range of bytes being provided.
   */
  public void setResponseBytesRange(BytesRange bytesRange) {
    mResponseBytesRange = bytesRange;
  }
}
