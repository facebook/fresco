/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.flipper.plugins.fresco.objecthelper;

import static com.facebook.flipper.plugins.inspector.InspectorValue.Type.Color;

import android.text.TextUtils;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.flipper.core.FlipperArray;
import com.facebook.flipper.core.FlipperObject;
import com.facebook.flipper.plugins.inspector.InspectorValue;
import com.facebook.fresco.ui.common.ImagePerfData;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.debug.FlipperImageTracker;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.QualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.util.Map;
import javax.annotation.Nullable;

/** Serialization helper to create {@link FlipperObject}s. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class FlipperObjectHelper {

  public FlipperObject keyValuePair(String key, @Nullable String value) {
    // NULLSAFE_FIXME[Parameter Not Nullable]
    return new FlipperObject.Builder().put(key, value).build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable Map<String, String> stringMap) {
    if (stringMap == null) {
      return null;
    }
    FlipperObject.Builder optionsJson = new FlipperObject.Builder();
    for (Map.Entry<String, String> entry : stringMap.entrySet()) {
      optionsJson.put(entry.getKey(), entry.getValue());
    }
    return optionsJson.build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable ImageRequest imageRequest) {
    if (imageRequest == null) {
      return null;
    }
    FlipperObject.Builder optionsJson = new FlipperObject.Builder();
    return addImageRequestProperties(optionsJson, imageRequest).build();
  }

  @Nullable
  public FlipperObject toFlipperObject(
      @Nullable FlipperImageTracker.ImageDebugData imageDebugData) {
    if (imageDebugData == null) {
      return null;
    }
    FlipperObject.Builder optionsJson = new FlipperObject.Builder();
    optionsJson.put("imageId", imageDebugData.getUniqueId());
    optionsJson.put("imageRequest", toFlipperObject(imageDebugData.getImageRequest()));
    optionsJson.put(
        "requestId",
        imageDebugData.getRequestIds() != null
            ? TextUtils.join(", ", imageDebugData.getRequestIds())
            : "");
    optionsJson.put("imagePerfData", toFlipperObject(imageDebugData.getImagePerfData()));
    return optionsJson.build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable ImageDecodeOptions options) {
    if (options == null) {
      return null;
    }
    FlipperObject.Builder optionsJson = new FlipperObject.Builder();
    optionsJson.put("minDecodeIntervalMs", options.minDecodeIntervalMs);
    optionsJson.put("decodePreviewFrame", options.decodePreviewFrame);
    optionsJson.put("useLastFrameForPreview", options.useLastFrameForPreview);
    optionsJson.put("decodeAllFrames", options.decodeAllFrames);
    optionsJson.put("forceStaticImage", options.forceStaticImage);
    optionsJson.put("bitmapConfig", options.bitmapConfig.name());
    optionsJson.put(
        "customImageDecoder",
        options.customImageDecoder == null ? "" : options.customImageDecoder.toString());
    return optionsJson.build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable ResizeOptions resizeOptions) {
    if (resizeOptions == null) {
      return null;
    }
    FlipperObject.Builder optionsJson = new FlipperObject.Builder();
    optionsJson.put("width", resizeOptions.width);
    optionsJson.put("height", resizeOptions.height);
    optionsJson.put("maxBitmapSize", resizeOptions.maxBitmapDimension);
    optionsJson.put("roundUpFraction", resizeOptions.roundUpFraction);
    return optionsJson.build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable RotationOptions rotationOptions) {
    if (rotationOptions == null) {
      return null;
    }
    FlipperObject.Builder optionsJson = new FlipperObject.Builder();
    optionsJson.put("rotationEnabled", rotationOptions.rotationEnabled());
    optionsJson.put("canDeferUntilRendered", rotationOptions.canDeferUntilRendered());
    optionsJson.put("useImageMetadata", rotationOptions.useImageMetadata());
    if (!rotationOptions.useImageMetadata()) {
      optionsJson.put("forcedAngle", rotationOptions.getForcedAngle());
    }
    return optionsJson.build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable RoundingParams roundingParams) {
    if (roundingParams == null) {
      return null;
    }
    FlipperObject.Builder optionsJson = new FlipperObject.Builder();
    optionsJson.put("borderWidth", roundingParams.getBorderWidth());
    optionsJson.put("cornersRadii", toSonarArray(roundingParams.getCornersRadii()));
    optionsJson.put("padding", roundingParams.getPadding());
    optionsJson.put("roundAsCircle", roundingParams.getRoundAsCircle());
    optionsJson.put("roundingMethod", roundingParams.getRoundingMethod());
    optionsJson.put(
        "borderColor", InspectorValue.immutable(Color, roundingParams.getBorderColor()));
    optionsJson.put(
        "overlayColor", InspectorValue.immutable(Color, roundingParams.getOverlayColor()));
    return optionsJson.build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable ImagePerfData imagePerfData) {
    if (imagePerfData == null) {
      return null;
    }
    FlipperObject.Builder objectJson = new FlipperObject.Builder();
    // NULLSAFE_FIXME[Parameter Not Nullable]
    objectJson.put("requestId", imagePerfData.getRequestId());
    objectJson.put("controllerSubmitTimeMs", imagePerfData.getControllerSubmitTimeMs());
    objectJson.put("controllerFinalTimeMs", imagePerfData.getControllerFinalImageSetTimeMs());
    objectJson.put("imageRequestStartTimeMs", imagePerfData.getImageRequestStartTimeMs());
    objectJson.put("imageRequestEndTimeMs", imagePerfData.getImageRequestEndTimeMs());
    objectJson.put("imageOrigin", "UNKNOWN");
    objectJson.put("isPrefetch", imagePerfData.isPrefetch());
    // NULLSAFE_FIXME[Parameter Not Nullable]
    objectJson.put("callerContext", imagePerfData.getCallerContext());
    objectJson.put("imageRequest", toFlipperObject((ImageRequest) imagePerfData.getImageRequest()));
    objectJson.put("imageInfo", toFlipperObject((ImageInfo) imagePerfData.getImageInfo()));
    return objectJson.build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable ImageInfo imageInfo) {
    if (imageInfo == null) {
      return null;
    }
    FlipperObject.Builder objectJson = new FlipperObject.Builder();
    objectJson.put("imageWidth", imageInfo.getWidth());
    objectJson.put("imageHeight", imageInfo.getHeight());
    objectJson.put("qualityInfo", toFlipperObject(imageInfo.getQualityInfo()));
    return objectJson.build();
  }

  @Nullable
  public FlipperObject toFlipperObject(@Nullable QualityInfo qualityInfo) {
    if (qualityInfo == null) {
      return null;
    }
    FlipperObject.Builder objectJson = new FlipperObject.Builder();
    objectJson.put("quality", qualityInfo.getQuality());
    objectJson.put("isGoodEnoughQuality", qualityInfo.isOfGoodEnoughQuality());
    objectJson.put("isFullQuality", qualityInfo.isOfFullQuality());
    return objectJson.build();
  }

  public FlipperObject.Builder addImageRequestProperties(
      FlipperObject.Builder builder, @Nullable ImageRequest request) {
    if (request == null) {
      return builder;
    }
    builder
        .put("sourceUri", request.getSourceUri())
        .put("preferredWidth", request.getPreferredWidth())
        .put("preferredHeight", request.getPreferredHeight())
        .put("cacheChoice", request.getCacheChoice())
        .put("diskCacheEnabled", request.isDiskCacheEnabled())
        .put("localThumbnailPreviewsEnabled", request.getLocalThumbnailPreviewsEnabled())
        .put("lowestPermittedRequestLevel", request.getLowestPermittedRequestLevel())
        .put("priority", request.getPriority().name())
        .put("progressiveRenderingEnabled", request.getProgressiveRenderingEnabled())
        .put("postprocessor", String.valueOf(request.getPostprocessor()))
        .put("requestListener", String.valueOf(request.getRequestListener()))
        .put("imageDecodeOptions", toFlipperObject(request.getImageDecodeOptions()))
        // NULLSAFE_FIXME[Parameter Not Nullable]
        .put("bytesRange", request.getBytesRange())
        .put("resizeOptions", toFlipperObject(request.getResizeOptions()))
        .put("rotationOptions", toFlipperObject(request.getRotationOptions()));
    return builder;
  }

  private FlipperArray toSonarArray(float[] floats) {
    final FlipperArray.Builder builder = new FlipperArray.Builder();
    for (float f : floats) {
      builder.put(f);
    }
    return builder.build();
  }

  @Nullable
  public abstract FlipperArray fromCallerContext(@Nullable Object callerContext);
}
