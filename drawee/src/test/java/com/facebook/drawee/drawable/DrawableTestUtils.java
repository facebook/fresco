/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DrawableTestUtils {

  public static abstract class FakeDrawable extends Drawable implements VisibilityAwareDrawable {
    @Override
    public void setVisibilityCallback(VisibilityCallback visibilityCallback) {}
  }

  /**
   * Creates a mock Drawable with some methods stubbed.
   * @return mock Drawable
   */
  public static Drawable mockDrawable() {
    return mockDrawable(FakeDrawable.class);
  }

  /**
   * Creates a mock BitmapDrawable with some methods stubbed.
   * @return mock Drawable
   */
  public static BitmapDrawable mockBitmapDrawable() {
    return mockDrawable(BitmapDrawable.class);
  }

  /**
   * Creates a mock Drawable with some methods stubbed.
   * @return mock Drawable
   */
  public static <D extends Drawable> D mockDrawable(Class<D> drawableClassToMock) {
    D drawable = mock(drawableClassToMock);
    when(drawable.mutate()).thenReturn(drawable);
    stubGetAndSetBounds(drawable);
    stubGetAndSetCallback(drawable);
    stubSetVisibilityCallback(drawable);
    stubSetAlpha(drawable);
    stubGetPaint(drawable);
    stubGetBitmap(drawable);
    return drawable;
  }

  /**
   * Stubs setBounds and getBounds methods.
   * @param drawable drawable to stub methods of
   */
  public static void stubGetAndSetBounds(Drawable drawable) {
    final Rect rect = new Rect();
    when(drawable.getBounds()).thenReturn(rect);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            rect.set(
                (Integer) invocation.getArguments()[0],
                (Integer) invocation.getArguments()[1],
                (Integer) invocation.getArguments()[2],
                (Integer) invocation.getArguments()[3]);
            return null;
          }
        }).when(drawable).setBounds(anyInt(), anyInt(), anyInt(), anyInt());
  }

  /**
   * Stubs setCallback and getCallback methods.
   * @param drawable drawable to stub methods of
   */
  @TargetApi(11)
  public static void stubGetAndSetCallback(final Drawable drawable) {
    final AtomicReference<Drawable.Callback> callback =
        new AtomicReference<Drawable.Callback>();
    when(drawable.getCallback()).thenReturn(callback.get());
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            callback.set((Drawable.Callback) invocation.getArguments()[0]);
            return null;
          }
        }).when(drawable).setCallback(any(Drawable.Callback.class));
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            if (callback.get() != null) {
              callback.get().invalidateDrawable(drawable);
            }
            return null;
          }
        }).when(drawable).invalidateSelf();
  }

  /**
   * Stubs setVisibilityCallback methods.
   * @param drawable drawable to stub methods of
   */
  public static void stubSetVisibilityCallback(final Drawable drawable) {
    final AtomicBoolean isVisible = new AtomicBoolean(true);
    final AtomicReference<VisibilityCallback> callback =
        new AtomicReference<VisibilityCallback>();
    if (!(drawable instanceof VisibilityAwareDrawable)) {
      return;
    }
    VisibilityAwareDrawable visibilityAwareDrawable = (VisibilityAwareDrawable) drawable;
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            callback.set((VisibilityCallback) invocation.getArguments()[0]);
            return null;
          }
        }).when(visibilityAwareDrawable).setVisibilityCallback(any(VisibilityCallback.class));
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            if (callback.get() != null) {
              isVisible.set((Boolean) invocation.getArguments()[0]);
              callback.get().onVisibilityChange(isVisible.get());
            }
            return null;
          }
        }).when(drawable).setVisible(anyBoolean(), anyBoolean());
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            return isVisible.get();
          }
        }).when(drawable).isVisible();
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            if (callback.get() != null) {
              callback.get().onDraw();
            }
            return null;
          }
        }).when(drawable).draw(any(Canvas.class));
  }

  /**
   * Stubs setAlpha method.
   * @param drawable to stub method of
   */
  public static void stubSetAlpha(final Drawable drawable) {
    final AtomicInteger atomicInteger = new AtomicInteger(255);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            Integer alpha = (Integer) invocation.getArguments()[0];
            drawable.invalidateSelf();
            atomicInteger.set(alpha);
            return null;
          }
        }).when(drawable).setAlpha(anyInt());
  }

  /**
   * Stubs getPaint for BitmapDrawables.
   * @param drawable drawable to stub methods of
   */
  public static void stubGetPaint(Drawable drawable) {
    if (!(drawable instanceof BitmapDrawable)) {
      return;
    }
    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
    final Paint paint = new Paint();
    when(bitmapDrawable.getPaint()).thenReturn(paint);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            paint.setColorFilter((ColorFilter) invocation.getArguments()[0]);
            return null;
          }
        }).when(bitmapDrawable).setColorFilter(any(ColorFilter.class));
  }

  /**
   * Stubs getBitmap for BitmapDrawables.
   * @param drawable drawable to stub methods of
   */
  public static void stubGetBitmap(Drawable drawable) {
    if (!(drawable instanceof BitmapDrawable)) {
      return;
    }
    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
    final Bitmap bitmap = mock(Bitmap.class);
    when(bitmapDrawable.getBitmap()).thenReturn(bitmap);
  }
}
