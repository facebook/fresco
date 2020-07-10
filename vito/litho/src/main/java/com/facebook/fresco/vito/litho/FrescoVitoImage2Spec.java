/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho;

import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.view.View;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.core.PrefetchConfig;
import com.facebook.fresco.vito.core.PrefetchTarget;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.litho.AccessibilityRole;
import com.facebook.litho.BoundaryWorkingRange;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.CachedValue;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.MountingType;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCalculateCachedValue;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnEnteredRange;
import com.facebook.litho.annotations.OnExitedRange;
import com.facebook.litho.annotations.OnMeasure;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnPopulateAccessibilityNode;
import com.facebook.litho.annotations.OnPrepare;
import com.facebook.litho.annotations.OnRegisterRanges;
import com.facebook.litho.annotations.OnUnbind;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.ResType;
import com.facebook.litho.annotations.ShouldUpdate;
import com.facebook.litho.annotations.State;
import com.facebook.litho.utils.MeasureUtils;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

/** Simple Fresco Vito component for Litho */
@MountSpec(isPureRender = true, canPreallocate = true, poolSize = 15)
public class FrescoVitoImage2Spec {

  @PropDefault protected static final float imageAspectRatio = 1f;

  @OnCreateMountContent(mountingType = MountingType.DRAWABLE)
  static FrescoDrawable2 onCreateMountContent(Context c) {
    return FrescoVitoProvider.getController().createDrawable();
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context,
      StateValue<AtomicReference<DataSource<Void>>> workingRangePrefetchData) {
    if (FrescoVitoProvider.getConfig().getPrefetchConfig().prefetchWithWorkingRange()) {
      workingRangePrefetchData.set(new AtomicReference<DataSource<Void>>());
    }
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
      @Prop(optional = true) final @Nullable ImageSource imageSource,
      @Prop(optional = true) final @Nullable ImageOptions imageOptions) {
    ImageSource finalImageSource;
    if (imageSource != null) {
      finalImageSource = imageSource;
    } else {
      finalImageSource = ImageSourceProvider.forUri(uri);
    }
    return FrescoVitoProvider.getImagePipeline()
        .createImageRequest(c.getResources(), finalImageSource, imageOptions);
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest,
      Output<DataSource<Void>> prefetchDataSource) {
    PrefetchConfig config = FrescoVitoProvider.getConfig().getPrefetchConfig();
    if (config.prefetchInOnPrepare()) {
      prefetchDataSource.set(
          FrescoVitoProvider.getPrefetcher()
              .prefetch(config.prefetchTargetOnPrepare(), imageRequest, callerContext));
    }
  }

  @OnMount
  static void onMount(
      ComponentContext c,
      final FrescoDrawable2 frescoDrawable,
      @Prop(optional = true) final @Nullable Object callerContext,
      @Prop(optional = true) final @Nullable ImageListener imageListener,
      @CachedValue VitoImageRequest imageRequest,
      @FromPrepare DataSource<Void> prefetchDataSource,
      @FromBoundsDefined Rect viewportDimensions,
      @State final @Nullable AtomicReference<DataSource<Void>> workingRangePrefetchData) {
    FrescoVitoProvider.getController()
        .fetch(frescoDrawable, imageRequest, callerContext, imageListener, viewportDimensions);
    if (prefetchDataSource != null) {
      prefetchDataSource.close();
    }
    if (FrescoVitoProvider.getConfig().getPrefetchConfig().cancelPrefetchWhenFetched()) {
      cancelWorkingRangePrefetch(workingRangePrefetchData);
    }
  }

  @OnBind
  static void onBind(
      ComponentContext c,
      final FrescoDrawable2 frescoDrawable,
      @Prop(optional = true) final @Nullable Object callerContext,
      @Prop(optional = true) final @Nullable ImageListener imageListener,
      @CachedValue VitoImageRequest imageRequest,
      @FromPrepare DataSource<Void> prefetchDataSource,
      @FromBoundsDefined Rect viewportDimensions,
      @State final @Nullable AtomicReference<DataSource<Void>> workingRangePrefetchData) {
    // We fetch in both mount and bind in case an unbind event triggered a delayed release.
    // We'll only trigger an actual fetch if needed. Most of the time, this will be a no-op.
    FrescoVitoProvider.getController()
        .fetch(frescoDrawable, imageRequest, callerContext, imageListener, viewportDimensions);
    if (prefetchDataSource != null) {
      prefetchDataSource.close();
    }
    if (FrescoVitoProvider.getConfig().getPrefetchConfig().cancelPrefetchWhenFetched()) {
      cancelWorkingRangePrefetch(workingRangePrefetchData);
    }
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext c,
      FrescoDrawable2 frescoDrawable,
      @FromPrepare DataSource<Void> prefetchDataSource) {
    FrescoVitoProvider.getController().releaseDelayed(frescoDrawable);
    if (prefetchDataSource != null) {
      prefetchDataSource.close();
    }
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext c,
      FrescoDrawable2 frescoDrawable,
      @FromPrepare DataSource<Void> prefetchDataSource) {
    FrescoVitoProvider.getController().release(frescoDrawable);
    if (prefetchDataSource != null) {
      prefetchDataSource.close();
    }
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
      @Prop(optional = true) Diff<Uri> uri,
      @Prop(optional = true) Diff<ImageSource> imageSource,
      @Prop(optional = true) Diff<ImageOptions> imageOptions,
      @Prop(optional = true, resType = ResType.FLOAT) Diff<Float> imageAspectRatio,
      @Prop(optional = true) Diff<ImageListener> imageListener) {
    return !ObjectsCompat.equals(uri.getPrevious(), uri.getNext())
        || !ObjectsCompat.equals(imageSource.getPrevious(), imageSource.getNext())
        || !ObjectsCompat.equals(imageOptions.getPrevious(), imageOptions.getNext())
        || !ObjectsCompat.equals(imageAspectRatio.getPrevious(), imageAspectRatio.getNext())
        || !ObjectsCompat.equals(imageListener.getPrevious(), imageListener.getNext());
  }

  @OnPopulateAccessibilityNode
  static void onPopulateAccessibilityNode(View host, AccessibilityNodeInfoCompat node) {
    node.setClassName(AccessibilityRole.IMAGE);
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c, ComponentLayout layout, Output<Rect> viewportDimensions) {
    final int width = layout.getWidth();
    final int height = layout.getHeight();
    int paddingX = 0, paddingY = 0;
    if (layout.isPaddingSet()) {
      paddingX = layout.getPaddingLeft() + layout.getPaddingRight();
      paddingY = layout.getPaddingTop() + layout.getPaddingBottom();
    }

    viewportDimensions.set(new Rect(0, 0, width - paddingX, height - paddingY));
  }

  @OnEnteredRange(name = "imagePrefetch")
  static void onEnteredWorkingRange(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Object callerContext,
      @CachedValue VitoImageRequest imageRequest,
      @FromPrepare DataSource<Void> prefetchDataSource,
      @State final @Nullable AtomicReference<DataSource<Void>> workingRangePrefetchData) {
    if (workingRangePrefetchData == null) {
      return;
    }
    cancelWorkingRangePrefetch(workingRangePrefetchData);
    PrefetchConfig prefetchConfig = FrescoVitoProvider.getConfig().getPrefetchConfig();
    if (prefetchConfig.prefetchWithWorkingRange()) {
      workingRangePrefetchData.set(
          FrescoVitoProvider.getPrefetcher()
              .prefetch(PrefetchTarget.MEMORY_DECODED, imageRequest, callerContext));

      if (prefetchDataSource != null
          && prefetchConfig.cancelOnPreparePrefetchWhenWorkingRangePrefetch()) {
        prefetchDataSource.close();
      }
    }
  }

  @OnExitedRange(name = "imagePrefetch")
  static void onExitedWorkingRange(
      ComponentContext c, @State final AtomicReference<DataSource<Void>> workingRangePrefetchData) {
    cancelWorkingRangePrefetch(workingRangePrefetchData);
  }

  @OnRegisterRanges
  static void registerWorkingRanges(ComponentContext c) {
    PrefetchConfig prefetchConfig = FrescoVitoProvider.getConfig().getPrefetchConfig();
    if (prefetchConfig.prefetchWithWorkingRange()) {
      FrescoVitoImage2.registerImagePrefetchWorkingRange(
          c, new BoundaryWorkingRange(prefetchConfig.prefetchWorkingRangeSize()));
    }
  }

  static void cancelWorkingRangePrefetch(
      final @Nullable AtomicReference<DataSource<Void>> prefetchData) {
    if (prefetchData == null) {
      return;
    }
    DataSource<Void> dataSource = prefetchData.get();
    if (dataSource != null) {
      dataSource.close();
    }
    prefetchData.set(null);
  }
}
