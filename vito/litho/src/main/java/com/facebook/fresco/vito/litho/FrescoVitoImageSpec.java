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
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.core.util.ObjectsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.facebook.datasource.DataSource;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoController;
import com.facebook.fresco.vito.core.FrescoDrawable;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.core.FrescoState;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.impl.DefaultFrescoContext;
import com.facebook.imagepipeline.multiuri.MultiUri;
import com.facebook.litho.AccessibilityRole;
import com.facebook.litho.BoundaryWorkingRange;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.ComponentLayout;
import com.facebook.litho.Diff;
import com.facebook.litho.Output;
import com.facebook.litho.Size;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.FromBoundsDefined;
import com.facebook.litho.annotations.FromPrepare;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.MountingType;
import com.facebook.litho.annotations.OnBind;
import com.facebook.litho.annotations.OnBoundsDefined;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnDetached;
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

@MountSpec(isPureRender = true)
public class FrescoVitoImageSpec {

  @PropDefault protected static final float imageAspectRatio = 1f;

  private static final Handler sHandler = new Handler(Looper.getMainLooper());
  private static final long RELEASE_DELAY_MS = 16 * 5; // Roughly 5 frames.

  @OnCreateMountContent(mountingType = MountingType.DRAWABLE)
  static FrescoDrawable onCreateMountContent(Context c) {
    return new FrescoDrawable(false);
  }

  @OnCreateInitialState
  static void onCreateInitialState(
      ComponentContext context,
      StateValue<FrescoState> lastFrescoState,
      StateValue<AtomicReference<DataSource<Void>>> prefetchData,
      @Prop(optional = true) final @Nullable Uri uri,
      @Prop(optional = true) final @Nullable MultiUri multiUri,
      @Prop(optional = true) final @Nullable ImageOptions imageOptions,
      @Prop(optional = true) final @Nullable FrescoContext frescoContext,
      @Prop(optional = true) final @Nullable Object callerContext,
      @Prop(optional = true) final @Nullable ImageListener imageListener) {
    lastFrescoState.set(
        getController(context, frescoContext)
            .createState(
                uri,
                multiUri,
                imageOptions != null ? imageOptions : ImageOptions.defaults(),
                callerContext,
                context.getResources(),
                imageListener));
    prefetchData.set(new AtomicReference<DataSource<Void>>());
  }

  @OnMeasure
  static void onMeasure(
      ComponentContext context,
      ComponentLayout layout,
      int widthSpec,
      int heightSpec,
      Size size,
      @Prop(optional = true, resType = ResType.FLOAT) float imageAspectRatio) {
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, imageAspectRatio, size);
  }

  @OnPrepare
  static void onPrepare(
      ComponentContext context,
      @Prop(optional = true) final @Nullable Uri uri,
      @Prop(optional = true) final @Nullable MultiUri multiUri,
      @Prop(optional = true) final @Nullable ImageOptions imageOptions,
      @Prop(optional = true) final @Nullable FrescoContext frescoContext,
      @Prop(optional = true) final @Nullable Object callerContext,
      @Prop(optional = true) final @Nullable ImageListener imageListener,
      @State(canUpdateLazily = true) final FrescoState lastFrescoState,
      Output<FrescoState> frescoState) {
    FrescoState maybeNewFrescoState =
        getController(context, frescoContext)
            .onPrepare(
                lastFrescoState,
                uri,
                multiUri,
                imageOptions != null ? imageOptions : ImageOptions.defaults(),
                callerContext,
                context.getResources(),
                imageListener);
    if (lastFrescoState != maybeNewFrescoState) {
      FrescoVitoImage.lazyUpdateLastFrescoState(context, maybeNewFrescoState);
    }
    frescoState.set(maybeNewFrescoState);
  }

  @OnMount
  static void onMount(
      ComponentContext context,
      final FrescoDrawable frescoDrawable,
      @Prop(optional = true) final @Nullable FrescoContext frescoContext,
      @Prop(optional = true) final @Nullable ImageListener imageListener,
      @FromPrepare final FrescoState frescoState,
      @FromBoundsDefined Rect viewportDimensions) {
    FrescoContext actualFrescoContext = resolveContext(context, frescoContext);
    if (actualFrescoContext.getExperiments().delayedReleaseInUnbind()) {
      cancelDetachRunnable(frescoState);
    }
    if (!actualFrescoContext.getExperiments().useBindCallbacks()) {
      frescoState.setFrescoDrawable(frescoDrawable);
      actualFrescoContext.getController().onAttach(frescoState, imageListener);
    }
  }

  @OnUnmount
  static void onUnmount(
      ComponentContext context,
      FrescoDrawable frescoDrawable,
      @Prop(optional = true) final @Nullable FrescoContext frescoContext,
      @FromPrepare final FrescoState frescoState) {
    FrescoContext actualFrescoContext = resolveContext(context, frescoContext);
    if (!actualFrescoContext.getExperiments().useBindCallbacks()
        && actualFrescoContext.getExperiments().releaseInUnmount()) {
      if (actualFrescoContext.getExperiments().delayedReleaseInUnbind()) {
        cancelDetachRunnable(frescoState);
      }
      frescoState.setFrescoDrawable(frescoDrawable);
      actualFrescoContext.getController().onDetach(frescoState);
    }
  }

  @OnBind
  static void onBind(
      ComponentContext context,
      final FrescoDrawable frescoDrawable,
      @Prop(optional = true) final @Nullable FrescoContext frescoContext,
      @Prop(optional = true) final @Nullable ImageListener imageListener,
      @FromPrepare final FrescoState frescoState,
      @FromBoundsDefined Rect viewportDimensions) {
    FrescoContext actualFrescoContext = resolveContext(context, frescoContext);
    if (actualFrescoContext.getExperiments().delayedReleaseInUnbind()) {
      cancelDetachRunnable(frescoState);
    }
    if (actualFrescoContext.getExperiments().useBindCallbacks()) {
      frescoState.setFrescoDrawable(frescoDrawable);
      actualFrescoContext.getController().onAttach(frescoState, imageListener);
    }
  }

  @OnUnbind
  static void onUnbind(
      ComponentContext context,
      final FrescoDrawable frescoDrawable,
      @Prop(optional = true) final @Nullable FrescoContext frescoContext,
      @FromPrepare final FrescoState frescoState) {
    final FrescoContext actualFrescoContext = resolveContext(context, frescoContext);
    if (actualFrescoContext.getExperiments().delayedReleaseInUnbind()) {

      cancelDetachRunnable(frescoState);

      Runnable newDetachRunnable =
          new Runnable() {
            @Override
            public void run() {
              frescoState.setFrescoDrawable(frescoDrawable);
              actualFrescoContext.getController().onDetach(frescoState);
            }
          };
      frescoState.setDetachRunnable(newDetachRunnable);
      sHandler.postDelayed(newDetachRunnable, RELEASE_DELAY_MS);

    } else if (actualFrescoContext.getExperiments().useBindCallbacks()) {
      frescoState.setFrescoDrawable(frescoDrawable);
      actualFrescoContext.getController().onDetach(frescoState);
    }
  }

  @OnDetached
  static void onDetached(
      ComponentContext context,
      @Prop(optional = true) final @Nullable FrescoContext frescoContext,
      @State final FrescoState frescoState) {
    FrescoContext actualFrescoContext = resolveContext(context, frescoContext);
    if (actualFrescoContext.getExperiments().releaseInDetach()) {
      actualFrescoContext.getController().onDetach(frescoState);
    }
  }

  @ShouldUpdate(onMount = true)
  static boolean shouldUpdate(
      @Prop(optional = true) Diff<Uri> uri,
      @Prop(optional = true) Diff<MultiUri> multiUri,
      @Prop(optional = true) Diff<ImageOptions> imageOptions,
      @Prop(optional = true) Diff<FrescoContext> frescoContext,
      @Prop(optional = true, resType = ResType.FLOAT) Diff<Float> imageAspectRatio) {
    return !ObjectsCompat.equals(uri.getPrevious(), uri.getNext())
        || !ObjectsCompat.equals(multiUri.getPrevious(), multiUri.getNext())
        || !ObjectsCompat.equals(imageOptions.getPrevious(), imageOptions.getNext())
        || !ObjectsCompat.equals(imageAspectRatio.getPrevious(), imageAspectRatio.getNext())
        || !ObjectsCompat.equals(frescoContext.getPrevious(), frescoContext.getNext());
  }

  @OnPopulateAccessibilityNode
  static void onPopulateAccessibilityNode(View host, AccessibilityNodeInfoCompat node) {
    node.setClassName(AccessibilityRole.IMAGE);
  }

  @OnEnteredRange(name = "imagePrefetch")
  static void onEnteredWorkingRange(
      ComponentContext c,
      @Prop(optional = true) final @Nullable Uri uri,
      @Prop(optional = true) final @Nullable MultiUri multiUri,
      @Prop(optional = true) final @Nullable ImageOptions imageOptions,
      @Prop(optional = true) final @Nullable Object callerContext,
      @Prop(optional = true) final @Nullable FrescoContext frescoContext,
      @State final AtomicReference<DataSource<Void>> prefetchData) {
    LithoPrefetchUtils.startPrefetch(
        resolveContext(c, frescoContext), uri, multiUri, imageOptions, callerContext, prefetchData);
  }

  @OnExitedRange(name = "imagePrefetch")
  static void onExitedWorkingRange(
      ComponentContext c, final AtomicReference<DataSource<Void>> prefetchData) {
    LithoPrefetchUtils.cancelPrefetch(prefetchData);
  }

  @OnRegisterRanges
  static void registerWorkingRanges(
      ComponentContext context, @Prop(optional = true) final FrescoContext frescoContext) {
    FrescoExperiments experiments = resolveContext(context, frescoContext).getExperiments();
    if (experiments.enableWorkingRangePrefetching()) {
      FrescoVitoImage.registerImagePrefetchWorkingRange(
          context, new BoundaryWorkingRange(experiments.workingRangePrefetchingSize()));
    }
  }

  static FrescoContext resolveContext(
      ComponentContext context, @Nullable FrescoContext contextOverride) {
    if (contextOverride != null) {
      return contextOverride;
    }
    return DefaultFrescoContext.get(context.getResources());
  }

  static FrescoController getController(
      ComponentContext context, @Nullable FrescoContext contextOverride) {
    return resolveContext(context, contextOverride).getController();
  }

  static void cancelDetachRunnable(FrescoState state) {
    Runnable runnable = state.removeDetachRunnable();
    if (runnable != null) {
      sHandler.removeCallbacks(runnable);
    }
  }

  @OnBoundsDefined
  static void onBoundsDefined(
      ComponentContext c,
      ComponentLayout layout,
      Output<Rect> viewportDimensions,
      @FromPrepare final FrescoState frescoState) {
    final int width = layout.getWidth();
    final int height = layout.getHeight();
    int paddingX = 0, paddingY = 0;
    if (layout.isPaddingSet()) {
      paddingX = layout.getPaddingLeft() + layout.getPaddingRight();
      paddingY = layout.getPaddingTop() + layout.getPaddingBottom();
    }

    viewportDimensions.set(new Rect(0, 0, width - paddingX, height - paddingY));

    if (frescoState != null) {
      frescoState.setTargetWidthPx(width);
      frescoState.setTargetHeightPx(height);
    }
  }
}
