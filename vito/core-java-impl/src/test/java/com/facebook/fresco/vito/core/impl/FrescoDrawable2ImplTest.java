/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import static org.mockito.Mockito.mock;

import android.graphics.drawable.Drawable;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.fresco.vito.core.NopDrawable;
import com.facebook.imagepipeline.image.BaseCloseableImage;
import com.facebook.imagepipeline.image.CloseableImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FrescoDrawable2ImplTest {

  private FrescoDrawable2Impl mFrescoDrawable;

  private CountDownLatch mLatch;
  private CloseableImage mCloseableImage;
  private CloseableReference<CloseableImage> mCloseableReference;

  @Before
  public void setup() {
    mFrescoDrawable = new FrescoDrawable2Impl(false, null, new BaseVitoImagePerfListener());
    mLatch = new CountDownLatch(1);
    mCloseableImage = new DummyCloseableImage();
    mCloseableReference =
        CloseableReference.of(
            mCloseableImage,
            new ResourceReleaser<CloseableImage>() {
              @Override
              public void release(CloseableImage value) {
                value.close();
                mLatch.countDown();
              }
            });
  }

  @Test
  public void testFrescoDrawable_whenDrawableClosed_thenReleaseActualImageReference()
      throws InterruptedException {
    mFrescoDrawable.setImage(NopDrawable.INSTANCE, mCloseableReference);
    mCloseableReference.close();
    Assert.assertFalse(mCloseableImage.isClosed());

    mFrescoDrawable.close();

    Assert.assertNull(mFrescoDrawable.imageReference);
    Assert.assertTrue(mCloseableImage.isClosed());
    Assert.assertTrue(mLatch.await(3, TimeUnit.SECONDS));
  }

  @Test
  public void testFrescoDrawable_whenNewImageDrawableSet_thenReleaseOldImageReference()
      throws InterruptedException {
    mFrescoDrawable.setImage(NopDrawable.INSTANCE, mCloseableReference);
    mCloseableReference.close();

    final CloseableImage closeableImage = new DummyCloseableImage();
    mFrescoDrawable.setImage(NopDrawable.INSTANCE, CloseableReference.of(closeableImage));

    Assert.assertTrue(mCloseableImage.isClosed());
    Assert.assertTrue(mLatch.await(3, TimeUnit.SECONDS));
  }

  @Test
  public void testFrescoDrawable_whenNewNullImageDrawableSet_thenReleaseOldImageReference()
      throws InterruptedException {
    mFrescoDrawable.setImage(NopDrawable.INSTANCE, mCloseableReference);
    mCloseableReference.close();

    mFrescoDrawable.setImage(null, null);

    Assert.assertTrue(mCloseableImage.isClosed());
    Assert.assertTrue(mLatch.await(3, TimeUnit.SECONDS));
  }

  @Test
  public void testFrescoDrawable_whenImageDrawableSet_thenReleaseImageReference()
      throws InterruptedException {
    mFrescoDrawable.setImage(NopDrawable.INSTANCE, mCloseableReference);
    mCloseableReference.close();

    Drawable drawable = mock(Drawable.class);
    mFrescoDrawable.setImageDrawable(drawable);

    Assert.assertNull(mFrescoDrawable.imageReference);
    Assert.assertTrue(mCloseableImage.isClosed());
    Assert.assertTrue(mLatch.await(3, TimeUnit.SECONDS));
  }

  @Test
  public void testFrescoDrawable_whenImageDrawableReset_thenReleaseImageReference()
      throws InterruptedException {
    mFrescoDrawable.setImage(NopDrawable.INSTANCE, mCloseableReference);
    mCloseableReference.close();

    mFrescoDrawable.setImageDrawable(null);

    Assert.assertNull(mFrescoDrawable.imageReference);
    Assert.assertTrue(mCloseableImage.isClosed());
    Assert.assertTrue(mLatch.await(3, TimeUnit.SECONDS));
  }

  static class DummyCloseableImage extends BaseCloseableImage {

    private boolean mClosed = false;

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
}
