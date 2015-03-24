/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.drawable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import android.annotation.TargetApi;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    FakeDrawable drawable = mock(FakeDrawable.class);
    when(drawable.mutate()).thenReturn(drawable);
    stubGetAndSetBounds(drawable);
    stubGetAndSetCallback(drawable);
    stubSetVisibilityCallback(drawable);
    stubSetAlpha(drawable);
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
}
