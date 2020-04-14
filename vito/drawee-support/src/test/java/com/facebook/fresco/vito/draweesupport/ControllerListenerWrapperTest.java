/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import org.junit.Before;
import org.junit.Test;

public class ControllerListenerWrapperTest {

  private static final int ID = 123;
  private static final String STRING_ID = "v123";
  private static final String CALLER_CONTEXT = "caller-context";

  private ControllerListener<ImageInfo> mControllerListener;
  private ControllerListenerWrapper mControllerListenerWrapper;

  @Before
  public void setup() {
    mControllerListener = (ControllerListener<ImageInfo>) mock(ControllerListener.class);
    mControllerListenerWrapper = new ControllerListenerWrapper(mControllerListener);
  }

  @Test
  public void testOnSubmit() {
    mControllerListenerWrapper.onSubmit(ID, CALLER_CONTEXT);

    verify(mControllerListener).onSubmit(eq(STRING_ID), eq(CALLER_CONTEXT));
    verifyNoMoreInteractions(mControllerListener);
  }

  @Test
  public void testOnPlaceholderSet() {
    final Drawable placeholder = mock(Drawable.class);
    mControllerListenerWrapper.onPlaceholderSet(ID, placeholder);

    verifyZeroInteractions(mControllerListener);
  }

  @Test
  public void testOnIntermediateImageFailed() {
    final Throwable throwable = new Throwable("Message");
    mControllerListenerWrapper.onIntermediateImageFailed(ID, throwable);

    verify(mControllerListener).onIntermediateImageFailed(eq(STRING_ID), eq(throwable));
    verifyNoMoreInteractions(mControllerListener);
  }

  @Test
  public void testOnFailure() {
    final Throwable throwable = new Throwable("Message");
    final Drawable errorDrawable = mock(Drawable.class);
    mControllerListenerWrapper.onFailure(ID, errorDrawable, throwable);

    verify(mControllerListener).onFailure(eq(STRING_ID), eq(throwable));
    verifyNoMoreInteractions(mControllerListener);
  }

  @Test
  public void testOnRelease() {
    mControllerListenerWrapper.onRelease(ID);

    verify(mControllerListener).onRelease(eq(STRING_ID));
    verifyNoMoreInteractions(mControllerListener);
  }

  @Test
  public void testOnIntermediateImageSet() {
    final ImageInfo imageInfo = mock(ImageInfo.class);
    mControllerListenerWrapper.onIntermediateImageSet(ID, imageInfo);

    verify(mControllerListener).onIntermediateImageSet(eq(STRING_ID), eq(imageInfo));
    verifyNoMoreInteractions(mControllerListener);
  }

  @Test
  public void testOnFinalImageSetNormalDrawable() {
    final Drawable drawable = mock(Drawable.class);
    final ImageInfo imageInfo = mock(ImageInfo.class);
    mControllerListenerWrapper.onFinalImageSet(ID, ImageOrigin.DISK, imageInfo, drawable);

    verify(mControllerListener)
        .onFinalImageSet(eq(STRING_ID), eq(imageInfo), isNull(Animatable.class));
    verifyNoMoreInteractions(mControllerListener);
  }

  @Test
  public void testOnFinalImageSetAnimatedDrawable() {
    final AnimatedDrawable drawable = mock(AnimatedDrawable.class);
    final ImageInfo imageInfo = mock(ImageInfo.class);
    mControllerListenerWrapper.onFinalImageSet(ID, ImageOrigin.DISK, imageInfo, drawable);

    verify(mControllerListener).onFinalImageSet(eq(STRING_ID), eq(imageInfo), eq(drawable));
    verifyNoMoreInteractions(mControllerListener);
  }

  @Test
  public void testOnFinalImageSetImageOriginListener() {
    final ImageOriginListener listener = mock(ImageOriginListener.class);
    final ImageInfo imageInfo = mock(ImageInfo.class);
    ControllerListenerWrapper wrapper =
        ControllerListenerWrapper.create(mControllerListener).setImageOriginListener(listener);

    wrapper.onFinalImageSet(ID, ImageOrigin.DISK, imageInfo, null);

    verify(mControllerListener)
        .onFinalImageSet(eq(STRING_ID), eq(imageInfo), isNull(Animatable.class));
    verify(listener).onImageLoaded(eq(STRING_ID), eq(ImageOrigin.DISK), eq(true), anyString());
    verifyNoMoreInteractions(mControllerListener);
  }

  @Test
  public void testCreateWrapper() {
    ControllerListenerWrapper wrapper = ControllerListenerWrapper.create(mControllerListener);
    assertThat(wrapper).isNotNull();
  }

  @Test
  public void testCreateNullWrapper() {
    ControllerListenerWrapper wrapper = ControllerListenerWrapper.create(null);
    assertThat(wrapper).isNull();
  }

  public abstract static class AnimatedDrawable extends Drawable implements Animatable {}
}
