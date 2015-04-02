// Copyright 2004-present Facebook. All Rights Reserved.

package com.facebook.imagepipeline.animated.factory;

import android.graphics.Bitmap;
import android.graphics.Rect;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.common.soloader.SoLoaderShim;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor;
import com.facebook.imagepipeline.animated.testing.TestAnimatedDrawableBackend;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import com.facebook.imagepipeline.webp.WebPImage;
import com.facebook.testing.robolectric.v2.WithTestDefaultsRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link AnimatedImageFactory}
 */
@RunWith(WithTestDefaultsRunner.class)
@PrepareOnlyThisForTest({
    WebPImage.class,
    AnimatedImageFactory.class,
    AnimatedImageCompositor.class})
public class AnimatedImageFactoryTest {

  static {
    SoLoaderShim.setInTestMode();
  }

  private static ResourceReleaser<PooledByteBuffer> FAKE_RESOURCE_RELEASER =
      new ResourceReleaser<PooledByteBuffer>() {

    @Override
    public void release(PooledByteBuffer value) {
    }
  };

  private static ResourceReleaser<Bitmap> FAKE_BITMAP_RESOURCE_RELEASER =
      new ResourceReleaser<Bitmap>() {

        @Override
        public void release(Bitmap value) {
        }
      };

  private AnimatedDrawableBackendProvider mMockAnimatedDrawableBackendProvider;
  private PlatformBitmapFactory mMockBitmapFactory;
  private AnimatedImageFactory mAnimatedImageFactory;

  @Before
  public void setup() {
    PowerMockito.mockStatic(WebPImage.class);

    mMockAnimatedDrawableBackendProvider = mock(AnimatedDrawableBackendProvider.class);
    mMockBitmapFactory = mock(PlatformBitmapFactory.class);

    mAnimatedImageFactory = new AnimatedImageFactory(
        mMockAnimatedDrawableBackendProvider,
        mMockBitmapFactory);
  }

  @Test
  public void testCreateDefaults() {
    WebPImage mockWebPImage = mock(WebPImage.class);

    // Expect a call to WebPImage.create
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(WebPImage.create(byteBuffer.getNativePtr(), byteBuffer.size()))
        .thenReturn(mockWebPImage);

    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage) mAnimatedImageFactory.decodeWebP(
            CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER),
            ImageDecodeOptions.defaults());

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockWebPImage, imageResult.getImage());
    assertNull(imageResult.getPreviewBitmap());
    assertFalse(imageResult.hasDecodedFrame(0));

    // Should not have interacted with these.
    verifyZeroInteractions(mMockAnimatedDrawableBackendProvider);
    verifyZeroInteractions(mMockBitmapFactory);
  }

  @Test
  public void testCreateWithPreviewBitmap() throws Exception {
    WebPImage mockWebPImage = mock(WebPImage.class);

    Bitmap mockBitmap = MockBitmapFactory.create(50, 50);

    // Expect a call to WebPImage.create
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(WebPImage.create(byteBuffer.getNativePtr(), byteBuffer.size()))
        .thenReturn(mockWebPImage);
    when(mockWebPImage.getWidth()).thenReturn(50);
    when(mockWebPImage.getHeight()).thenReturn(50);

    // For decoding preview frame, expect some calls.
    when(mMockAnimatedDrawableBackendProvider.get(
            any(AnimatedImageResult.class),
            isNull(Rect.class)))
        .thenReturn(new TestAnimatedDrawableBackend(50, 50, new int[]{100}));
    when(mMockBitmapFactory.createBitmap(50, 50))
        .thenReturn(CloseableReference.of(mockBitmap, FAKE_BITMAP_RESOURCE_RELEASER));
    AnimatedImageCompositor mockCompositor = mock(AnimatedImageCompositor.class);
    PowerMockito.whenNew(AnimatedImageCompositor.class)
        .withAnyArguments()
        .thenReturn(mockCompositor);

    ImageDecodeOptions imageDecodeOptions = ImageDecodeOptions.newBuilder()
        .setDecodePreviewFrame(true)
        .build();
    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage) mAnimatedImageFactory.decodeWebP(
            CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER),
            imageDecodeOptions);

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockWebPImage, imageResult.getImage());
    assertNotNull(imageResult.getPreviewBitmap());
    assertFalse(imageResult.hasDecodedFrame(0));

    // Should not have interacted with these.
    verify(mMockAnimatedDrawableBackendProvider).get(
        any(AnimatedImageResult.class),
        isNull(Rect.class));
    verifyNoMoreInteractions(mMockAnimatedDrawableBackendProvider);
    verify(mMockBitmapFactory).createBitmap(50, 50);
    verifyNoMoreInteractions(mMockBitmapFactory);
    verify(mockCompositor).renderFrame(0, mockBitmap);
  }

  @Test
  public void testCreateWithDecodeAlFrames() throws Exception {
    WebPImage mockWebPImage = mock(WebPImage.class);

    Bitmap mockBitmap1 = MockBitmapFactory.create(50, 50);
    Bitmap mockBitmap2 = MockBitmapFactory.create(50, 50);

    // Expect a call to WebPImage.create
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(WebPImage.create(byteBuffer.getNativePtr(), byteBuffer.size()))
        .thenReturn(mockWebPImage);
    when(mockWebPImage.getWidth()).thenReturn(50);
    when(mockWebPImage.getHeight()).thenReturn(50);

    // For decoding preview frame, expect some calls.
    when(
        mMockAnimatedDrawableBackendProvider.get(
            any(AnimatedImageResult.class),
            isNull(Rect.class)))
        .thenReturn(new TestAnimatedDrawableBackend(50, 50, new int[]{ 100, 200 }));
    when(mMockBitmapFactory.createBitmap(50, 50))
        .thenReturn(CloseableReference.of(mockBitmap1, FAKE_BITMAP_RESOURCE_RELEASER))
        .thenReturn(CloseableReference.of(mockBitmap2, FAKE_BITMAP_RESOURCE_RELEASER));
    AnimatedImageCompositor mockCompositor = mock(AnimatedImageCompositor.class);
    PowerMockito.whenNew(AnimatedImageCompositor.class)
        .withAnyArguments()
        .thenReturn(mockCompositor);

    ImageDecodeOptions imageDecodeOptions = ImageDecodeOptions.newBuilder()
        .setDecodePreviewFrame(true)
        .setDecodeAllFrames(true)
        .build();
    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage) mAnimatedImageFactory.decodeWebP(
            CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER),
            imageDecodeOptions);

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockWebPImage, imageResult.getImage());
    assertNotNull(imageResult.getDecodedFrame(0));
    assertNotNull(imageResult.getDecodedFrame(1));
    assertNotNull(imageResult.getPreviewBitmap());

    // Should not have interacted with these.
    verify(mMockAnimatedDrawableBackendProvider).get(
        any(AnimatedImageResult.class),
        isNull(Rect.class));
    verifyNoMoreInteractions(mMockAnimatedDrawableBackendProvider);
    verify(mMockBitmapFactory, times(2)).createBitmap(50, 50);
    verifyNoMoreInteractions(mMockBitmapFactory);
    verify(mockCompositor).renderFrame(0, mockBitmap1);
    verify(mockCompositor).renderFrame(1, mockBitmap2);
  }

  private TrivialPooledByteBuffer createByteBuffer() {
    byte[] buf = new byte[16];
    return new TrivialPooledByteBuffer(buf);
  }
}
