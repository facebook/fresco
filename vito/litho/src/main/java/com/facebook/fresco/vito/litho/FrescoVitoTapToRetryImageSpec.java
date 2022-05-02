/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.fresco.ui.common.OnFadeListener;
import com.facebook.fresco.vito.listener.BaseImageListener;
import com.facebook.fresco.vito.listener.ForwardingImageListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.AccessibilityRole;
import com.facebook.litho.ClickEvent;
import com.facebook.litho.Component;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.EventHandler;
import com.facebook.litho.LongClickEvent;
import com.facebook.litho.StateValue;
import com.facebook.litho.TouchEvent;
import com.facebook.litho.annotations.LayoutSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateLayout;
import com.facebook.litho.annotations.OnEvent;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnUpdateState;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.State;
import com.facebook.litho.widget.Image;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
@LayoutSpec
public class FrescoVitoTapToRetryImageSpec {

  @PropDefault protected static final int maxTapCount = Integer.MAX_VALUE;

  @PropDefault
  protected static final float imageAspectRatio = FrescoVitoImage2Spec.imageAspectRatio;

  @PropDefault
  protected static final FrescoVitoImage2Spec.Prefetch prefetch = FrescoVitoImage2Spec.prefetch;

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext c,
      StateValue<Boolean> isTapToRetry,
      StateValue<Integer> tapCount,
      StateValue<Boolean> useFallbackImageSource,
      @Prop(optional = true) boolean isInitialTapToLoad) {
    isTapToRetry.set(isInitialTapToLoad);
    tapCount.set(isInitialTapToLoad ? 1 : 0);
    useFallbackImageSource.set(false);
  }

  @OnCreateLayout
  static Component onCreateLayout(
      final ComponentContext c,
      @Prop final ImageSource imageSource,
      @Prop(optional = true) final @Nullable ImageSource fallbackImageSource,
      @Prop(optional = true) final @Nullable Object callerContext,
      @Prop(optional = true, resType = ResType.FLOAT) final float imageAspectRatio,
      @Prop(optional = true) final @Nullable EventHandler<ClickEvent> imageClickHandler,
      @Prop(optional = true) final @Nullable EventHandler<LongClickEvent> imageLongClickHandler,
      @Prop(optional = true) final @Nullable EventHandler<TouchEvent> imageTouchHandler,
      @Prop(optional = true) final @Nullable ImageListener imageListener,
      @Prop(optional = true) final @Nullable ImageOptions imageOptions,
      @Prop(optional = true) final int maxTapCount,
      @Prop(optional = true) final OnFadeListener onFadeListener,
      @Prop(optional = true) final @Nullable FrescoVitoImage2Spec.Prefetch prefetch,
      @Prop(optional = true) final @Nullable RequestListener prefetchRequestListener,
      @Prop(resType = ResType.DRAWABLE, optional = true) final @Nullable Drawable retryImage,
      @Prop(optional = true) final @Nullable ScalingUtils.ScaleType retryImageScaleType,
      @State final boolean isTapToRetry,
      @State final boolean useFallbackImageSource,
      @State final int tapCount) {
    if (isTapToRetry) {
      Drawable scaledRetryDrawable =
          (retryImageScaleType == null || retryImage == null)
              ? retryImage
              : new ScaleTypeDrawable(retryImage, retryImageScaleType);
      return Image.create(c)
          .drawable(scaledRetryDrawable)
          .scaleType(ImageView.ScaleType.FIT_XY)
          .clickHandler(FrescoVitoTapToRetryImage.onRetryClickEvent(c))
          .aspectRatio(imageAspectRatio)
          .build();
    }
    final ImageListener internalListener =
        new BaseImageListener() {
          @Override
          public void onFailure(long id, @Nullable Drawable error, @Nullable Throwable throwable) {
            if (tapCount < maxTapCount) {
              FrescoVitoTapToRetryImage.onImageFailure(c);
            }
          }
        };

    boolean useFallback = fallbackImageSource != null && useFallbackImageSource;

    return FrescoVitoImage2.create(c)
        .callerContext(callerContext)
        .imageAspectRatio(imageAspectRatio)
        .imageListener(ForwardingImageListener.create(internalListener, imageListener))
        .imageOptions(imageOptions)
        .imageSource(useFallback ? fallbackImageSource : imageSource)
        .onFadeListener(onFadeListener)
        .prefetch(prefetch)
        .clickHandler(imageClickHandler)
        .longClickHandler(imageLongClickHandler)
        .touchHandler(imageTouchHandler)
        .prefetchRequestListener(prefetchRequestListener)
        .build();
  }

  @OnPopulateAccessibilityNode
  static void onPopulateAccessibilityNode(
      ComponentContext c, View host, AccessibilityNodeInfoCompat node) {
    node.setClassName(AccessibilityRole.IMAGE);
  }

  @OnUpdateState
  static void onImageFailure(
      StateValue<Boolean> isTapToRetry,
      StateValue<Integer> tapCount,
      StateValue<Boolean> useFallbackImageSource) {
    Integer oldTapCount = tapCount.get();
    int newTapCount = oldTapCount == null ? 1 : oldTapCount + 1;
    tapCount.set(newTapCount);
    isTapToRetry.set(true);
    useFallbackImageSource.set(true);
  }

  @OnUpdateState
  static void onTapToRetry(StateValue<Boolean> isTapToRetry) {
    isTapToRetry.set(false);
  }

  @OnEvent(ClickEvent.class)
  static void onRetryClickEvent(ComponentContext c) {
    FrescoVitoTapToRetryImage.onTapToRetry(c);
  }
}
