/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.testing;

import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.drawable.DrawableTestUtils;
import com.facebook.drawee.drawable.VisibilityAwareDrawable;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class DraweeMocksTest {

  @Before
  public void setUp() {
  }

  @Test
  public void testMockProviderOf() {
    Object obj = mock(Object.class);
    Supplier<Object> provider =
        DraweeMocks.supplierOf(obj);
    assertEquals(obj, provider.get());
    assertEquals(obj, provider.get());
    assertEquals(obj, provider.get());
    assertEquals(obj, provider.get());
    assertEquals(obj, provider.get());

    Object obj1 = mock(Object.class);
    Object obj2 = mock(Object.class);
    Object obj3 = mock(Object.class);
    Supplier<Object> multiProvider =
        DraweeMocks.supplierOf(obj1, obj2, obj3);
    assertEquals(obj1, multiProvider.get());
    assertEquals(obj2, multiProvider.get());
    assertEquals(obj3, multiProvider.get());
    assertEquals(obj3, multiProvider.get());
    assertEquals(obj3, multiProvider.get());
  }

  @Test
  public void testMockBuilderOfDrawableHierarchies() {
    GenericDraweeHierarchy gdh = DraweeMocks.mockDraweeHierarchy();
    GenericDraweeHierarchyBuilder builder =
        DraweeMocks.mockBuilderOf(gdh);
    assertEquals(gdh, builder.build());
    assertEquals(gdh, builder.build());
    assertEquals(gdh, builder.build());
    assertEquals(gdh, builder.build());
    assertEquals(gdh, builder.build());

    GenericDraweeHierarchy gdh1 = DraweeMocks.mockDraweeHierarchy();
    GenericDraweeHierarchy gdh2 = DraweeMocks.mockDraweeHierarchy();
    GenericDraweeHierarchy gdh3 = DraweeMocks.mockDraweeHierarchy();
    GenericDraweeHierarchyBuilder multiBuilder =
        DraweeMocks.mockBuilderOf(gdh1, gdh2, gdh3);
    assertEquals(gdh1, multiBuilder.build());
    assertEquals(gdh2, multiBuilder.build());
    assertEquals(gdh3, multiBuilder.build());
    assertEquals(gdh3, multiBuilder.build());
    assertEquals(gdh3, multiBuilder.build());
  }

  @Test
  public void testMockDrawable_VisibilityCallback() {
    boolean reset = true;
    Drawable drawable = DrawableTestUtils.mockDrawable();
    assertTrue(drawable instanceof VisibilityAwareDrawable);

    VisibilityAwareDrawable visibilityAwareDrawable = (VisibilityAwareDrawable) drawable;
    VisibilityCallback visibilityCallback = mock(VisibilityCallback.class);
    visibilityAwareDrawable.setVisibilityCallback(visibilityCallback);

    InOrder inOrder = inOrder(visibilityCallback);
    drawable.setVisible(false, reset);
    inOrder.verify(visibilityCallback).onVisibilityChange(false);
    drawable.setVisible(true, reset);
    inOrder.verify(visibilityCallback).onVisibilityChange(true);
    drawable.setVisible(false, reset);
    inOrder.verify(visibilityCallback).onVisibilityChange(false);
  }
}
