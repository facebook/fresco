/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.testing;

import java.util.concurrent.atomic.AtomicInteger;

import android.graphics.drawable.Drawable;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.controller.ForwardingControllerListener;
import com.facebook.drawee.drawable.DrawableTestUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.*;

/**
 * Drawee mocks
 */
public class DraweeMocks {

  /**
   * Creates a mock DraweeController with some methods stubbed.
   * @return mock DraweeController
   */
  public static DraweeController mockController() {
    DraweeController controller = mock(AbstractDraweeController.class);
    stubGetAndSetHierarchy(controller);
    return controller;
  }

  /**
   * Stubs setHierarchy and getHierarchy methods.
   * @param controller controller to stub methods of
   */
  public static void stubGetAndSetHierarchy(DraweeController controller) {
    final DraweeHierarchy[] dhHolder = new DraweeHierarchy[1];
    doAnswer(
        new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            return dhHolder[0];
          }
        }).when(controller).getHierarchy();
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            return dhHolder[0] = (DraweeHierarchy) invocation.getArguments()[0];
          }
        }).when(controller).setHierarchy(any(DraweeHierarchy.class));
  }

  /**
   * Stubs addControllerListener
   * @param controller
   * @return forwarding listener
   */
  public static ControllerListener stubControllerListener(
      final DraweeController controller) {
    final ForwardingControllerListener forwardingListener = new ForwardingControllerListener();
    if (!(controller instanceof AbstractDraweeController)) {
      return null;
    }
    AbstractDraweeController abstractController = (AbstractDraweeController) controller;
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            forwardingListener.addListener((ControllerListener) invocation.getArguments()[0]);
            return null;
          }
        }).when(abstractController).addControllerListener(any(ControllerListener.class));
    return forwardingListener;
  }

  /**
   * Creates a mock GenericDraweeHierarchy with some methods stubbed.
   * @param topLevelDrawable drawable to return on {@code getTopLevelDrawable()}
   * @return mock GenericDraweeHierarchy
   */
  public static GenericDraweeHierarchy mockDraweeHierarchyOf(Drawable topLevelDrawable) {
    GenericDraweeHierarchy gdh = mock(GenericDraweeHierarchy.class);
    when(gdh.getTopLevelDrawable()).thenReturn(topLevelDrawable);
    return gdh;
  }

  /**
   * Creates a mock GenericDraweeHierarchy with some methods stubbed.
   * @return mock GenericDraweeHierarchy
   */
  public static GenericDraweeHierarchy mockDraweeHierarchy() {
    return mockDraweeHierarchyOf(DrawableTestUtils.mockDrawable());
  }

  /**
   * Creates a mock GenericDraweeHierarchyBuilder that builds a new DH instance each time.
   * @return mock GenericDraweeHierarchyBuilder
   */
  public static GenericDraweeHierarchyBuilder mockGenericDraweeHierarchyBuilder() {
    GenericDraweeHierarchyBuilder builder =
        mock(GenericDraweeHierarchyBuilder.class, CALLS_REAL_METHODS);
    doAnswer(
        new Answer<Object>() {
          @Override
          public DraweeHierarchy answer(InvocationOnMock invocation) throws Throwable {
            return mockDraweeHierarchy();
          }
        }).when(builder).build();
    return builder;
  }

  /**
   * Creates a mock GenericDraweeHierarchyBuilder with stubbed build.
   * @param drawableHierarchies drawable hierarchies to return on {@code build()}
   * @return mock GenericDraweeHierarchyBuilder
   */
  public static GenericDraweeHierarchyBuilder mockBuilderOf(
      GenericDraweeHierarchy... drawableHierarchies) {
    GenericDraweeHierarchyBuilder builder =
        mock(GenericDraweeHierarchyBuilder.class, CALLS_REAL_METHODS);
    final Supplier<GenericDraweeHierarchy> gdhProvider = supplierOf(drawableHierarchies);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            return gdhProvider.get();
          }
        }).when(builder).build();
    return builder;
  }

  /**
   * Creates a supplier of T.
   * @param values values to return on {@code get()}
   * @return supplier of T
   */
  public static <T> Supplier<T> supplierOf(final T... values) {
    final AtomicInteger index = new AtomicInteger(0);
    return new Supplier<T>() {
      @Override
      public T get() {
        if (index.get() < values.length) {
          return values[index.getAndIncrement()];
        } else {
          return values[values.length - 1];
        }
      }
    };
  }
}
