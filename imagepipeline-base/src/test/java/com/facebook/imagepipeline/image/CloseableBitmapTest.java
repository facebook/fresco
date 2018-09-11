/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import static org.mockito.Mockito.*;

import android.graphics.Bitmap;
import android.media.ExifInterface;
import com.facebook.common.references.ResourceReleaser;
import junit.framework.Assert;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.robolectric.*;

/**
 * Basic tests for closeable bitmap
 */
@RunWith(RobolectricTestRunner.class)
public class CloseableBitmapTest  {

  @Mock public Bitmap mBitmap;
  @Mock public ResourceReleaser<Bitmap> mResourceReleaser;
  private CloseableStaticBitmap mCloseableStaticBitmap;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    mCloseableStaticBitmap =
        new CloseableStaticBitmap(
            mBitmap,
            mResourceReleaser,
            ImmutableQualityInfo.FULL_QUALITY,
            0,
            ExifInterface.ORIENTATION_UNDEFINED);
  }

  @Test
  public void testBasic() throws Exception {
    Assert.assertFalse(mCloseableStaticBitmap.isClosed());
    Assert.assertSame(mBitmap, mCloseableStaticBitmap.getUnderlyingBitmap());

    // close it now
    mCloseableStaticBitmap.close();
    Assert.assertTrue(mCloseableStaticBitmap.isClosed());
    Assert.assertNull(mCloseableStaticBitmap.getUnderlyingBitmap());
    verify(mResourceReleaser).release(mBitmap);

    // close it again
    mCloseableStaticBitmap.close();
    Assert.assertTrue(mCloseableStaticBitmap.isClosed());
    Assert.assertNull(mCloseableStaticBitmap.getUnderlyingBitmap());
  }

  @Test
  public void testFinalize() throws Throwable {
    mCloseableStaticBitmap.finalize();
    Assert.assertTrue(mCloseableStaticBitmap.isClosed());
    Assert.assertNull(mCloseableStaticBitmap.getUnderlyingBitmap());
    verify(mResourceReleaser).release(mBitmap);
  }
}
