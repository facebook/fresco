/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import android.content.Context;
import android.net.Uri;
import android.view.View;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.PrefetchTarget;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.FrescoContextProvider;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.litho.AccessibilityRole;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.Size;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.utils.MeasureUtils;
import javax.annotation.Nullable;

/** Simple Fresco Vito component for Litho */
@MountSpec(isPureRender = true)
public class FrescoVitoImage2Spec {

  @PropDefault protected static final float imageAspectRatio = 1f;

  @OnCreateMountContent
  static FrescoDrawable2 onCreateMountContent(Context c) {
    return new FrescoDrawable2();
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext c,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(optional = true, resType = ResType.FLOAT) float imageAspectRatio) {
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, imageAspectRatio, size);
  }

  @OnCalculateCachedValue(name = "imageRequest")
  static VitoImageRequest onCalculateImageRequest(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Uri uri,
      @Prop(optional = true) final @Nullable MultiUri multiUri,
      @Prop(optional = true) final @Nullable ImageOptions imageOptions,
      @Prop(optional = true) final @Nullable Object callerContext) {
    return FrescoContextProvider.getImagePipeline()
        .createImageRequest(c.getResources(), uri, multiUri, imageOptions, callerContext);
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest) {
    FrescoContextProvider.getPrefetcher()
        .prefetch(PrefetchTarget.MEMORY_DECODED, imageRequest, callerContext);
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      final FrescoDrawable2 frescoDrawable,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest) {
    FrescoContextProvider.getController().fetch(frescoDrawable, imageRequest, callerContext);
  }

  @OnBind
  static void onBind(
      ComponentContext c,
      final FrescoDrawable2 frescoDrawable,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest) {
    // We fetch in both mount and bind in case an unbind event triggered a delayed release.
    // We'll only trigger an actual fetch if needed. Most of the time, this will be a no-op.
    FrescoContextProvider.getController().fetch(frescoDrawable, imageRequest, callerContext);
  }

  @OnUnbind
  static void onUnbind(ComponentContext c, FrescoDrawable2 frescoDrawable) {
    frescoDrawable.scheduleReleaseDelayed();
  }

  @OnUnmount
  static void onUnmount(ComponentContext c, FrescoDrawable2 frescoDrawable) {
    frescoDrawable.scheduleReleaseNextFrame();
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
      @Prop(optional = true) Diff<Uri> uri,
      @Prop(optional = true) Diff<MultiUri> multiUri,
      @Prop(optional = true) Diff<ImageOptions> imageOptions,
      @Prop(optional = true, resType = ResType.FLOAT) Diff<Float> imageAspectRatio) {
    return !ObjectsCompat.equals(uri.getPrevious(), uri.getNext())
        || !ObjectsCompat.equals(multiUri.getPrevious(), multiUri.getNext())
        || !ObjectsCompat.equals(imageOptions.getPrevious(), imageOptions.getNext())
        || !ObjectsCompat.equals(imageAspectRatio.getPrevious(), imageAspectRatio.getNext());
  }

  @OnPopulateAccessibilityNode
  static void onPopulateAccessibilityNode(View host, AccessibilityNodeInfoCompat node) {
    node.setClassName(AccessibilityRole.IMAGE);
  }
}
