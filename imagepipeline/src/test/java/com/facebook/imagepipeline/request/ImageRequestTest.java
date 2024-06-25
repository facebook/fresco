/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import android.net.Uri;
import com.facebook.imagepipeline.common.ImageDecodeOptionsBuilder;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.common.RotationOptions;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import java.io.File;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ImageRequestTest {

  @Test
  public void testCreatingRequestFromExistingRequest() {
    ImageRequest original =
        ImageRequestBuilder.newBuilderWithSource(Uri.parse("http://frescolib.org/image.jpg"))
            .setCacheChoice(ImageRequest.CacheChoice.SMALL)
            .setImageDecodeOptions(new ImageDecodeOptionsBuilder().build())
            .setLocalThumbnailPreviewsEnabled(true)
            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
            .setPostprocessor(
                new BasePostprocessor() {
                  @Override
                  public String getName() {
                    return super.getName();
                  }
                })
            .setProgressiveRenderingEnabled(true)
            .setRequestListener(new RequestLoggingListener())
            .setResizeOptions(new ResizeOptions(20, 20))
            .setRotationOptions(RotationOptions.forceRotation(RotationOptions.ROTATE_90))
            .setRequestPriority(Priority.HIGH)
            .build();

    ImageRequest copy = ImageRequestBuilder.fromRequest(original).build();

    assertThat(copy).isEqualTo(original);
  }

  @Test
  public void testCreatingRequestWithDynamicCacheChoice_success() {
    ImageRequest request =
        ImageRequestBuilder.newBuilderWithSource(Uri.parse("http://frescolib.org/image.jpg"))
            .setCacheChoice(ImageRequest.CacheChoice.DYNAMIC)
            .setDiskCacheId("dynamic_cache_id")
            .setImageDecodeOptions(new ImageDecodeOptionsBuilder().build())
            .setLocalThumbnailPreviewsEnabled(true)
            .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
            .setPostprocessor(
                new BasePostprocessor() {
                  @Override
                  public String getName() {
                    return super.getName();
                  }
                })
            .setProgressiveRenderingEnabled(true)
            .setRequestListener(new RequestLoggingListener())
            .setResizeOptions(new ResizeOptions(20, 20))
            .setRotationOptions(RotationOptions.forceRotation(RotationOptions.ROTATE_90))
            .setRequestPriority(Priority.HIGH)
            .build();

    assertThat(request).isNotNull();
    assertThat(request.getCacheChoice()).isEqualTo(ImageRequest.CacheChoice.DYNAMIC);
    assertThat(request.getDiskCacheId()).isEqualTo("dynamic_cache_id");
  }

  @Test
  public void testCreatingRequestWithDynamicCacheChoice_nodiskCacheId() {
    assertThatThrownBy(
            () -> {
              ImageRequest request =
                  ImageRequestBuilder.newBuilderWithSource(
                          Uri.parse("http://frescolib.org/image.jpg"))
                      .setCacheChoice(ImageRequest.CacheChoice.DYNAMIC)
                      .setImageDecodeOptions(new ImageDecodeOptionsBuilder().build())
                      .setLocalThumbnailPreviewsEnabled(true)
                      .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
                      .setPostprocessor(
                          new BasePostprocessor() {
                            @Override
                            public String getName() {
                              return super.getName();
                            }
                          })
                      .setProgressiveRenderingEnabled(true)
                      .setRequestListener(new RequestLoggingListener())
                      .setResizeOptions(new ResizeOptions(20, 20))
                      .setRotationOptions(RotationOptions.forceRotation(RotationOptions.ROTATE_90))
                      .setRequestPriority(Priority.HIGH)
                      .build();
            })
        .isInstanceOf(ImageRequestBuilder.BuilderException.class)
        .hasMessage("Invalid request builder: Disk cache id must be set for dynamic cache choice");
  }

  @Test
  public void testCreatingRequestWithNONDynamicCacheChoice_withDiskCacheId() {
    assertThatThrownBy(
            () -> {
              ImageRequest request =
                  ImageRequestBuilder.newBuilderWithSource(
                          Uri.parse("http://frescolib.org/image.jpg"))
                      .setCacheChoice(ImageRequest.CacheChoice.SMALL)
                      .setDiskCacheId("dynamic_cache_id")
                      .setImageDecodeOptions(new ImageDecodeOptionsBuilder().build())
                      .setLocalThumbnailPreviewsEnabled(true)
                      .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.DISK_CACHE)
                      .setPostprocessor(
                          new BasePostprocessor() {
                            @Override
                            public String getName() {
                              return super.getName();
                            }
                          })
                      .setProgressiveRenderingEnabled(true)
                      .setRequestListener(new RequestLoggingListener())
                      .setResizeOptions(new ResizeOptions(20, 20))
                      .setRotationOptions(RotationOptions.forceRotation(RotationOptions.ROTATE_90))
                      .setRequestPriority(Priority.HIGH)
                      .build();
            })
        .isInstanceOf(ImageRequestBuilder.BuilderException.class)
        .hasMessage(
            "Invalid request builder: Ensure that if you want to use a disk cache id, you set the"
                + " CacheChoice to DYNAMIC");
  }

  @Test
  public void testImageRequestForLocalFile_normal() {
    final File file = new File("/foo/photos/penguin.jpg");
    final ImageRequest imageRequest = ImageRequest.fromFile(file);

    assertThat(imageRequest.getSourceFile()).isNotNull();
    assertThat(imageRequest.getSourceFile().getAbsolutePath()).isEqualTo(file.getAbsolutePath());
  }

  @Test
  public void testImageRequestForLocalFile_withSpaces() {
    final File file = new File("/foo/photos folder/penguin crowd.jpg");
    final ImageRequest imageRequest = ImageRequest.fromFile(file);

    assertThat(imageRequest.getSourceFile()).isNotNull();
    assertThat(imageRequest.getSourceFile().getAbsolutePath()).isEqualTo(file.getAbsolutePath());
  }

  @Test
  public void testImageRequestForLocalFile_withSpecialCharacters() {
    final File file = new File("/foo/photos#folder/with spaces/penguin?_&*-...jpg");
    final ImageRequest imageRequest = ImageRequest.fromFile(file);

    assertThat(imageRequest.getSourceFile()).isNotNull();
    assertThat(imageRequest.getSourceFile().getAbsolutePath()).isEqualTo(file.getAbsolutePath());
  }
}
