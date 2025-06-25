/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.factory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import android.graphics.Rect;
import com.facebook.animated.webp.WebPImage;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableBackend;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.animated.impl.AnimatedDrawableBackendProvider;
import com.facebook.imagepipeline.animated.impl.AnimatedImageCompositor;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TrivialBufferPooledByteBuffer;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link AnimatedImageFactory} */
@RunWith(RobolectricTestRunner.class)
public class AnimatedImageFactoryWebPImplTest {

  private static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

  private static ResourceReleaser<PooledByteBuffer> FAKE_RESOURCE_RELEASER =
      new ResourceReleaser<PooledByteBuffer>() {

        @Override
        public void release(PooledByteBuffer value) {}
      };

  private static ResourceReleaser<Bitmap> FAKE_BITMAP_RESOURCE_RELEASER =
      new ResourceReleaser<Bitmap>() {

        @Override
        public void release(Bitmap value) {}
      };

  private AnimatedDrawableBackendProvider mMockAnimatedDrawableBackendProvider;
  private PlatformBitmapFactory mMockBitmapFactory;
  private AnimatedImageFactory mAnimatedImageFactory;
  private MockedStatic<WebPImage> webPImageMockedStatic;
  private MockedConstruction<AnimatedImageCompositor> animatedImageCompositorConstruction;

  private WebPImage mWebPImageMock;

  @Before
  public void setup() {
    webPImageMockedStatic = mockStatic(WebPImage.class);

    mWebPImageMock = mock(WebPImage.class);

    mMockAnimatedDrawableBackendProvider = mock(AnimatedDrawableBackendProvider.class);
    mMockBitmapFactory = mock(PlatformBitmapFactory.class);

    mAnimatedImageFactory =
        new AnimatedImageFactoryImpl(
            mMockAnimatedDrawableBackendProvider, mMockBitmapFactory, false);

    ((AnimatedImageFactoryImpl) mAnimatedImageFactory).sWebpAnimatedImageDecoder = mWebPImageMock;
  }

  @After
  public void tearDown() {
    if (webPImageMockedStatic != null) {
      webPImageMockedStatic.close();
    }
    if (animatedImageCompositorConstruction != null) {
      animatedImageCompositorConstruction.close();
    }
  }

  @Test
  public void testCreateDefaultsUsingPointer() {
    WebPImage mockWebPImage = mock(WebPImage.class);

    // Expect a call to WebPImage.createFromByteBuffer
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(mWebPImageMock.decodeFromNativeMemory(
            eq(byteBuffer.getNativePtr()), eq(byteBuffer.size()), any(ImageDecodeOptions.class)))
        .thenReturn(mockWebPImage);

    testCreateDefaults(mockWebPImage, byteBuffer);
  }

  @Test
  public void testCreateDefaultsUsingByteBuffer() {
    WebPImage mockWebPImage = mock(WebPImage.class);

    // Expect a call to WebPImage.createFromByteBuffer
    TrivialBufferPooledByteBuffer byteBuffer = createDirectByteBuffer();
    when(mWebPImageMock.decodeFromByteBuffer(
            eq(byteBuffer.getByteBuffer()), any(ImageDecodeOptions.class)))
        .thenReturn(mockWebPImage);

    testCreateDefaults(mockWebPImage, byteBuffer);
  }

  @Test
  public void testCreateWithPreviewBitmapUsingPointer() throws Exception {
    WebPImage mockWebPImage = mock(WebPImage.class);
    Bitmap mockBitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);

    // Expect a call to WebPImage.createFromByteBuffer
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(mWebPImageMock.decodeFromNativeMemory(
            eq(byteBuffer.getNativePtr()), eq(byteBuffer.size()), any(ImageDecodeOptions.class)))
        .thenReturn(mockWebPImage);
    when(mockWebPImage.getWidth()).thenReturn(50);
    when(mockWebPImage.getHeight()).thenReturn(50);

    testCreateWithPreviewBitmap(mockWebPImage, byteBuffer, mockBitmap);
  }

  @Test
  public void testCreateWithPreviewBitmapUsingByteBuffer() throws Exception {
    WebPImage mockWebPImage = mock(WebPImage.class);
    Bitmap mockBitmap = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);

    // Expect a call to WebPImage.createFromByteBuffer
    TrivialBufferPooledByteBuffer byteBuffer = createDirectByteBuffer();
    when(mWebPImageMock.decodeFromByteBuffer(
            eq(byteBuffer.getByteBuffer()), any(ImageDecodeOptions.class)))
        .thenReturn(mockWebPImage);
    when(mockWebPImage.getWidth()).thenReturn(50);
    when(mockWebPImage.getHeight()).thenReturn(50);

    testCreateWithPreviewBitmap(mockWebPImage, byteBuffer, mockBitmap);
  }

  @Test
  public void testCreateWithDecodeAlFramesUsingPointer() throws Exception {
    WebPImage mockWebPImage = mock(WebPImage.class);

    Bitmap mockBitmap1 = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);
    Bitmap mockBitmap2 = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);

    // Expect a call to WebPImage.createFromByteBuffer
    TrivialPooledByteBuffer byteBuffer = createByteBuffer();
    when(mWebPImageMock.decodeFromNativeMemory(
            eq(byteBuffer.getNativePtr()), eq(byteBuffer.size()), any(ImageDecodeOptions.class)))
        .thenReturn(mockWebPImage);
    when(mockWebPImage.getWidth()).thenReturn(50);
    when(mockWebPImage.getHeight()).thenReturn(50);

    testCreateWithDecodeAlFrames(mockWebPImage, byteBuffer, mockBitmap1, mockBitmap2);
  }

  @Test
  public void testCreateWithDecodeAlFramesUsingByteBuffer() throws Exception {
    WebPImage mockWebPImage = mock(WebPImage.class);

    Bitmap mockBitmap1 = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);
    Bitmap mockBitmap2 = MockBitmapFactory.create(50, 50, DEFAULT_BITMAP_CONFIG);

    // Expect a call to WebPImage.createFromByteBuffer
    TrivialBufferPooledByteBuffer byteBuffer = createDirectByteBuffer();
    when(mWebPImageMock.decodeFromByteBuffer(
            eq(byteBuffer.getByteBuffer()), any(ImageDecodeOptions.class)))
        .thenReturn(mockWebPImage);
    when(mockWebPImage.getWidth()).thenReturn(50);
    when(mockWebPImage.getHeight()).thenReturn(50);

    testCreateWithDecodeAlFrames(mockWebPImage, byteBuffer, mockBitmap1, mockBitmap2);
  }

  private void testCreateDefaults(WebPImage mockWebPImage, PooledByteBuffer byteBuffer) {
    EncodedImage encodedImage =
        new EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER));
    encodedImage.setImageFormat(ImageFormat.UNKNOWN);

    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage)
            mAnimatedImageFactory.decodeWebP(
                encodedImage, ImageDecodeOptions.defaults(), DEFAULT_BITMAP_CONFIG);

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockWebPImage, imageResult.getImage());
    assertNull(imageResult.getPreviewBitmap());
    assertFalse(imageResult.hasDecodedFrame(0));

    // Should not have interacted with these.
    verifyNoMoreInteractions(mMockAnimatedDrawableBackendProvider);
    verifyNoMoreInteractions(mMockBitmapFactory);
  }

  private void testCreateWithPreviewBitmap(
      WebPImage mockWebPImage, PooledByteBuffer byteBuffer, Bitmap mockBitmap) throws Exception {
    // For decoding preview frame, expect some calls.
    final AnimatedDrawableBackend mockAnimatedDrawableBackend =
        createAnimatedDrawableBackendMock(1);

    when(mMockAnimatedDrawableBackendProvider.get(
            any(AnimatedImageResult.class), isNull(Rect.class)))
        .thenReturn(mockAnimatedDrawableBackend);
    when(mMockBitmapFactory.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of(mockBitmap, FAKE_BITMAP_RESOURCE_RELEASER));
    AnimatedImageCompositor mockCompositor = mock(AnimatedImageCompositor.class);

    animatedImageCompositorConstruction =
        mockConstruction(AnimatedImageCompositor.class, (mock, context) -> {});

    ImageDecodeOptions imageDecodeOptions =
        ImageDecodeOptions.newBuilder().setDecodePreviewFrame(true).build();
    EncodedImage encodedImage =
        new EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER));
    encodedImage.setImageFormat(ImageFormat.UNKNOWN);
    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage)
            mAnimatedImageFactory.decodeWebP(
                encodedImage, imageDecodeOptions, DEFAULT_BITMAP_CONFIG);

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockWebPImage, imageResult.getImage());
    assertNotNull(imageResult.getPreviewBitmap());
    assertFalse(imageResult.hasDecodedFrame(0));

    // Should not have interacted with these.
    verify(mMockAnimatedDrawableBackendProvider)
        .get(any(AnimatedImageResult.class), isNull(Rect.class));
    verifyNoMoreInteractions(mMockAnimatedDrawableBackendProvider);
    verify(mMockBitmapFactory).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG);
    verifyNoMoreInteractions(mMockBitmapFactory);
    verify(animatedImageCompositorConstruction.constructed().get(0)).renderFrame(0, mockBitmap);
  }

  private void testCreateWithDecodeAlFrames(
      WebPImage mockWebPImage, PooledByteBuffer byteBuffer, Bitmap mockBitmap1, Bitmap mockBitmap2)
      throws Exception {
    // For decoding preview frame, expect some calls.
    final AnimatedDrawableBackend mockAnimatedDrawableBackend =
        createAnimatedDrawableBackendMock(2);
    when(mMockAnimatedDrawableBackendProvider.get(
            any(AnimatedImageResult.class), isNull(Rect.class)))
        .thenReturn(mockAnimatedDrawableBackend);

    when(mMockBitmapFactory.createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG))
        .thenReturn(CloseableReference.of(mockBitmap1, FAKE_BITMAP_RESOURCE_RELEASER))
        .thenReturn(CloseableReference.of(mockBitmap2, FAKE_BITMAP_RESOURCE_RELEASER));
    AnimatedImageCompositor mockCompositor = mock(AnimatedImageCompositor.class);
    animatedImageCompositorConstruction =
        mockConstruction(AnimatedImageCompositor.class, (mock, context) -> {});

    ImageDecodeOptions imageDecodeOptions =
        ImageDecodeOptions.newBuilder()
            .setDecodePreviewFrame(true)
            .setDecodeAllFrames(true)
            .build();

    EncodedImage encodedImage =
        new EncodedImage(CloseableReference.of(byteBuffer, FAKE_RESOURCE_RELEASER));
    encodedImage.setImageFormat(ImageFormat.UNKNOWN);

    CloseableAnimatedImage closeableImage =
        (CloseableAnimatedImage)
            mAnimatedImageFactory.decodeWebP(
                encodedImage, imageDecodeOptions, DEFAULT_BITMAP_CONFIG);

    // Verify we got the right result
    AnimatedImageResult imageResult = closeableImage.getImageResult();
    assertSame(mockWebPImage, imageResult.getImage());
    assertNotNull(imageResult.getDecodedFrame(0));
    assertNotNull(imageResult.getDecodedFrame(1));
    assertNotNull(imageResult.getPreviewBitmap());

    // Should not have interacted with these.
    verify(mMockAnimatedDrawableBackendProvider)
        .get(any(AnimatedImageResult.class), isNull(Rect.class));
    verifyNoMoreInteractions(mMockAnimatedDrawableBackendProvider);
    verify(mMockBitmapFactory, times(2)).createBitmapInternal(50, 50, DEFAULT_BITMAP_CONFIG);
    verifyNoMoreInteractions(mMockBitmapFactory);
    verify(animatedImageCompositorConstruction.constructed().get(0)).renderFrame(0, mockBitmap1);
    verify(animatedImageCompositorConstruction.constructed().get(0)).renderFrame(1, mockBitmap2);
  }

  private static TrivialPooledByteBuffer createByteBuffer() {
    byte[] buf = new byte[16];
    return new TrivialPooledByteBuffer(buf);
  }

  private static TrivialBufferPooledByteBuffer createDirectByteBuffer() {
    byte[] buf = new byte[16];
    return new TrivialBufferPooledByteBuffer(buf);
  }

  /**
   * Creates the mock for the AnimatedDrawableBackend with the number of frame
   *
   * @param frameCount The number of frame to mock
   */
  private static AnimatedDrawableBackend createAnimatedDrawableBackendMock(final int frameCount) {
    // For decoding preview frame, expect some calls.
    final AnimatedDrawableBackend mockAnimatedDrawableBackend = mock(AnimatedDrawableBackend.class);
    when(mockAnimatedDrawableBackend.getFrameCount()).thenReturn(frameCount);
    when(mockAnimatedDrawableBackend.getWidth()).thenReturn(50);
    when(mockAnimatedDrawableBackend.getHeight()).thenReturn(50);
    return mockAnimatedDrawableBackend;
  }
}
