/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.image.CloseableImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FrescoDrawableTest {

  private FrescoDrawable mFrescoDrawable;

  @Before
  public void setup() {
    mFrescoDrawable = new FrescoDrawable(true);
  }

  @Test
  public void testFrescoDrawableReleaseActualImageReference() throws InterruptedException {
    final CountDownLatch latch = new CountDownLatch(1);
    final CloseableImage closeableImage = new DummyCloseableImage();
    final CloseableReference<CloseableImage> closeableReference =
        CloseableReference.of(
            closeableImage,
            new ResourceReleaser<CloseableImage>() {
              @Override
              public void release(CloseableImage value) {
                latch.countDown();
              }
            });

    mFrescoDrawable.setImage(NopDrawable.INSTANCE, closeableReference);
    closeableReference.close();
    mFrescoDrawable.close();

    Assert.assertNull(mFrescoDrawable.mImageReference);
    Assert.assertTrue(latch.await(3, TimeUnit.SECONDS));
  }

  static class DummyCloseableImage extends CloseableImage {
    @Override
    public int getSizeInBytes() {
      return 0;
    }

    @Override
    public void close() {}

    @Override
    public boolean isClosed() {
      return false;
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
}
