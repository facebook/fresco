/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import android.graphics.drawable.Drawable;
import com.facebook.common.internal.Supplier;
import com.facebook.drawee.drawable.DrawableTestUtils;
import com.facebook.drawee.drawable.VisibilityAwareDrawable;
import com.facebook.drawee.drawable.VisibilityCallback;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DraweeMocksTest {

  @Before
  public void setUp() {}

  @Test
  public void testMockProviderOf() {
    Object obj = mock(Object.class);
    Supplier<Object> provider = DraweeMocks.supplierOf(obj);
    assertThat(provider.get()).isEqualTo(obj);
    assertThat(provider.get()).isEqualTo(obj);
    assertThat(provider.get()).isEqualTo(obj);
    assertThat(provider.get()).isEqualTo(obj);
    assertThat(provider.get()).isEqualTo(obj);

    Object obj1 = mock(Object.class);
    Object obj2 = mock(Object.class);
    Object obj3 = mock(Object.class);
    Supplier<Object> multiProvider = DraweeMocks.supplierOf(obj1, obj2, obj3);
    assertThat(multiProvider.get()).isEqualTo(obj1);
    assertThat(multiProvider.get()).isEqualTo(obj2);
    assertThat(multiProvider.get()).isEqualTo(obj3);
    assertThat(multiProvider.get()).isEqualTo(obj3);
    assertThat(multiProvider.get()).isEqualTo(obj3);
  }

  @Test
  public void testMockBuilderOfDrawableHierarchies() {
    GenericDraweeHierarchy gdh = DraweeMocks.mockDraweeHierarchy();
    GenericDraweeHierarchyBuilder builder = DraweeMocks.mockBuilderOf(gdh);
    assertThat(builder.build()).isEqualTo(gdh);
    assertThat(builder.build()).isEqualTo(gdh);
    assertThat(builder.build()).isEqualTo(gdh);
    assertThat(builder.build()).isEqualTo(gdh);
    assertThat(builder.build()).isEqualTo(gdh);

    GenericDraweeHierarchy gdh1 = DraweeMocks.mockDraweeHierarchy();
    GenericDraweeHierarchy gdh2 = DraweeMocks.mockDraweeHierarchy();
    GenericDraweeHierarchy gdh3 = DraweeMocks.mockDraweeHierarchy();
    GenericDraweeHierarchyBuilder multiBuilder = DraweeMocks.mockBuilderOf(gdh1, gdh2, gdh3);
    assertThat(multiBuilder.build()).isEqualTo(gdh1);
    assertThat(multiBuilder.build()).isEqualTo(gdh2);
    assertThat(multiBuilder.build()).isEqualTo(gdh3);
    assertThat(multiBuilder.build()).isEqualTo(gdh3);
    assertThat(multiBuilder.build()).isEqualTo(gdh3);
  }

  @Test
  public void testMockDrawable_VisibilityCallback() {
    boolean reset = true;
    Drawable drawable = DrawableTestUtils.mockDrawable();
    assertThat(drawable instanceof VisibilityAwareDrawable).isTrue();

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
