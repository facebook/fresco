/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.graphics.Rect;

import com.facebook.imageformat.ImageFormat;
import com.facebook.imageformat.ImageFormatChecker;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imageutils.JfifUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * Add image transform meta data producer
 *
 * <p>Extracts meta data from the results passed down from the next producer, and adds it to the
 * result that it returns to the consumer.
 */
public class AddImageTransformMetaDataProducer implements Producer<EncodedImage> {
  private final Producer<EncodedImage> mNextProducer;

  public AddImageTransformMetaDataProducer(Producer<EncodedImage> nextProducer) {
    mNextProducer = nextProducer;
  }

  @Override
  public void produceResults(Consumer<EncodedImage> consumer, ProducerContext context) {
    mNextProducer.produceResults(new AddImageTransformMetaDataConsumer(consumer), context);
  }

  private static class AddImageTransformMetaDataConsumer extends DelegatingConsumer<
      EncodedImage, EncodedImage> {

    private AddImageTransformMetaDataConsumer(Consumer<EncodedImage> consumer) {
      super(consumer);
    }

    @Override
    protected void onNewResultImpl(EncodedImage newResult, boolean isLast) {
      if (newResult == null) {
        getConsumer().onNewResult(null, isLast);
        return;
      }
      EncodedImage encodedImage = setEncodedImageMetaData(newResult);
      getConsumer().onNewResult(encodedImage, isLast);
    }
  }

  // Returns the encoded image with the dimensions set if that information is available.
  private static EncodedImage setEncodedImageMetaData(EncodedImage encodedImage) {
    final ImageFormat imageFormat = ImageFormatChecker.getImageFormat_WrapIOException(
        encodedImage.getInputStream());
    encodedImage.setImageFormat(imageFormat);
    if (imageFormat == ImageFormat.JPEG) {
      Rect dimensions = JfifUtil.getDimensions(encodedImage.getInputStream());
      if (dimensions != null) {
        // We don't know for sure that the rotation angle is set at this point. But it might
        // never get set, so let's assume that if we've got the dimensions then we've got the
        // rotation angle, else we'll never propagate intermediate results.
        encodedImage.setRotationAngle(getRotationAngle(encodedImage));
        encodedImage.setWidth(dimensions.width());
        encodedImage.setHeight(dimensions.height());
      }
    }
    return encodedImage;
  }

  // Gets the correction angle based on the image's orientation
  private static int getRotationAngle(final EncodedImage encodedImage) {
    InputStream is = encodedImage.getInputStream();
    try {
      return JfifUtil.getAutoRotateAngleFromOrientation(JfifUtil.getOrientation(is));
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    }
  }
}
