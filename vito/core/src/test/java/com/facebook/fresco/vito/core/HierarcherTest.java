/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import static org.mockito.Mockito.mock;

import android.content.res.Resources;
import android.graphics.PointF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.vito.drawable.VitoDrawableFactory;
import com.facebook.fresco.vito.options.ImageOptions;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HierarcherTest {

  private Hierarcher mHierarcher;
  private final Resources mResources = mock(Resources.class);
  private final VitoDrawableFactory mVitoDrawableFactory = mock(VitoDrawableFactory.class);

  @Before
  public void setup() {
    mHierarcher = new HierarcherImpl(mVitoDrawableFactory);
  }

  @Test
  public void testBuildProgressDrawable() {
    final Drawable drawable = new ColorDrawable(0x0);
    final ImageOptions imageOptions =
        ImageOptions.create()
            .progress(drawable)
            .progressScaleType(ScalingUtils.ScaleType.FIT_CENTER)
            .build();

    final Drawable actual = mHierarcher.buildProgressDrawable(mResources, imageOptions);
    Assert.assertTrue(actual instanceof ScaleTypeDrawable);
    final ScaleTypeDrawable scaleTypeActual = (ScaleTypeDrawable) actual;
    Assert.assertEquals(ScalingUtils.ScaleType.FIT_CENTER, scaleTypeActual.getScaleType());
    Assert.assertEquals(drawable, scaleTypeActual.getCurrent());
  }

  @Test
  public void testBuildActualImageWrapper() {
    final PointF expectedFocusPoint = new PointF(1, 2);
    final ImageOptions imageOptions =
        ImageOptions.create()
            .scale(ScalingUtils.ScaleType.FIT_CENTER)
            .focusPoint(expectedFocusPoint)
            .build();

    final Drawable actual = mHierarcher.buildActualImageWrapper(imageOptions);
    Assert.assertTrue(actual instanceof ScaleTypeDrawable);
    final ScaleTypeDrawable scaleTypeActual = (ScaleTypeDrawable) actual;
    Assert.assertEquals(ScalingUtils.ScaleType.FIT_CENTER, scaleTypeActual.getScaleType());
    Assert.assertEquals(expectedFocusPoint, scaleTypeActual.getFocusPoint());
  }
}
