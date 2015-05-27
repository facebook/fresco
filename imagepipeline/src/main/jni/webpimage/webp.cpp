/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#define LOG_TAG "WebPImage"

#include <jni.h>
#include <array>
#include <memory>
#include <utility>
#include <vector>
#include <android/bitmap.h>
#include <android/log.h>

#include "webp/decode.h"
#include "webp/demux.h"

#include "jni_helpers.h"

using namespace facebook;

#define EXTRA_LOGGING false

/**
 * A holder for WebPDemuxer and its buffer. WebPDemuxer is needed by both WebPImage and
 * instances of WebPFrameIterator and it can't be released until all of them are done with it. This
 * wrapper is meant to be used inside of a std::shared_ptr to manage the resource.
 */
class WebPDemuxerWrapper {

public:
  WebPDemuxerWrapper(
      std::unique_ptr<WebPDemuxer, decltype(&WebPDemuxDelete)>&& pDemuxer,
      std::vector<uint8_t>&& pBuffer) :
    m_pDemuxer(std::move(pDemuxer)),
    m_pBuffer(std::move(pBuffer)) {
  }

  virtual ~WebPDemuxerWrapper() {
    //FBLOGD("Deleting Demuxer");
  }

  WebPDemuxer* get() {
    return m_pDemuxer.get();
  }

  size_t getBufferSize() {
    return m_pBuffer.size();
  }

private:
  std::unique_ptr<WebPDemuxer, decltype(&WebPDemuxDelete)> m_pDemuxer;
  std::vector<uint8_t> m_pBuffer;
};


/**
 * Native context for WebPImage.
 */
struct WebPImageNativeContext {

  /* Reference to the Demuxer */
  std::shared_ptr<WebPDemuxerWrapper> spDemuxer;

  /* Cached width of the image */
  int pixelWidth;

  /* Cached height of the image */
  int pixelHeight;

  /* Cached number of the frames in the image */
  int numFrames;

  /** Cached loop count for the image. 0 means infinite. */
  int loopCount;

  /** Duration of all the animation (the sum of all the frames duration) */
  int durationMs;

  /** Array of each frame's duration (size of array is numFrames) */
  std::vector<jint> frameDurationsMs;

  /** Reference counter. Instance is deleted when it goes from 1 to 0 */
  size_t refCount;

#if EXTRA_LOGGING
  ~WebPImageNativeContext() {
    __android_log_write(ANDROID_LOG_DEBUG, LOG_TAG, "WebPImageNativeContext destructor");
  }
#endif
};

/**
 * Native context for WebPFrame.
 */
struct WebPFrameNativeContext {
  /* Reference to the Demuxer */
  std::shared_ptr<WebPDemuxerWrapper> spDemuxer;

  /** Frame number for the image. Starts at 1. */
  int frameNum;

  /** X offset for the frame relative to the image canvas */
  int xOffset;

  /** Y offset for the frame relative to the image canvas */
  int yOffset;

  /** Display duration for the frame in ms*/
  int durationMs;

  /** Width of this frame */
  int width;

  /** Height of this frame */
  int height;

  /** Whether the next frame might need to be blended with this frame */
  bool disposeToBackgroundColor;

  /** Whether this frame needs to be blended with the previous frame */
  bool blendWithPreviousFrame;

  /** Raw encoded bytes for the frame. Points to existing memory managed by WebPDemuxerWrapper */
  const uint8_t* pPayload;

  /** Size of payload in bytes */
  size_t payloadSize;

  /** Reference counter. Instance is deleted when it goes from 1 to 0 */
  size_t refCount;

#if EXTRA_LOGGING
  ~WebPFrameNativeContext() {
    __android_log_write(ANDROID_LOG_DEBUG, LOG_TAG, "WebPFrameNativeContext destructor");
  }
#endif
};


// Class Names.
static const char* const kWebPImageClassPathName =
    "com/facebook/imagepipeline/webp/WebPImage";
static const char* const kWebPFrameClassPathName =
    "com/facebook/imagepipeline/webp/WebPFrame";

// Cached fields related to WebPImage
static jclass sClazzWebPImage;
static jmethodID sWebPImageConstructor;
static jfieldID sWebPImageFieldNativeContext;

// Cached fields related to WebPFrame
static jclass sClazzWebPFrame;
static jmethodID sWebPFrameConstructor;
static jfieldID sWebPFrameFieldNativeContext;


////////////////////////////////////////////////////////////////
/// Related to WebPImage
////////////////////////////////////////////////////////////////

/**
 * Creates a new WebPImage from the specified buffer.
 *
 * @param vBuffer the vector containing the bytes
 * @return a newly allocated WebPImage
 */
jobject WebPImage_nativeCreateFromByteVector(JNIEnv* pEnv, std::vector<uint8_t>& vBuffer) {
  std::unique_ptr<WebPImageNativeContext> spNativeContext(new WebPImageNativeContext());
  if (!spNativeContext) {
    throwOutOfMemoryError(pEnv, "Unable to allocate native context");
    return 0;
  }

  // WebPData is on the stack as its only used during the call to WebPDemux.
  WebPData webPData;
  webPData.bytes = vBuffer.data();
  webPData.size = vBuffer.size();

  // Create the WebPDemuxer
  auto spDemuxer = std::unique_ptr<WebPDemuxer, decltype(&WebPDemuxDelete)> {
    WebPDemux(&webPData),
    WebPDemuxDelete
  };
  if (!spDemuxer) {
    // We may want to consider first using functions that will return a useful error code
    // if it fails to parse.
    throwIllegalArgumentException(pEnv, "Failed to create demuxer");
    //FBLOGW("unable to get demuxer");
    return 0;
  }

  spNativeContext->pixelWidth = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_CANVAS_WIDTH);
  spNativeContext->pixelHeight = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_CANVAS_HEIGHT);
  spNativeContext->numFrames = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_FRAME_COUNT);
  spNativeContext->loopCount = WebPDemuxGetI(spDemuxer.get(), WEBP_FF_LOOP_COUNT);

  // Compute cached fields that require iterating the frames.
  jint durationMs = 0;
  std::vector<jint> frameDurationsMs;
  WebPIterator iter;
  if (WebPDemuxGetFrame(spDemuxer.get(), 1, &iter)) {
    do {
      durationMs += iter.duration;
      frameDurationsMs.push_back(iter.duration);
    } while (WebPDemuxNextFrame(&iter));
    WebPDemuxReleaseIterator(&iter);
  }
  spNativeContext->durationMs = durationMs;
  spNativeContext->frameDurationsMs = frameDurationsMs;

  // Ownership of pDemuxer and vBuffer is transferred to WebPDemuxerWrapper here.
  // Note, according to Rob Arnold, createNew assumes we throw exceptions but we don't. Though
  // he claims this won't happen in practice cause "Linux will overcommit pages, we should only
  // get this error if we run out of virtual address space." Also, Daniel C may be working
  // on converting to exceptions.
  spNativeContext->spDemuxer = std::shared_ptr<WebPDemuxerWrapper>(
    new WebPDemuxerWrapper(std::move(spDemuxer), std::move(vBuffer)));

  // Create the WebPImage with the native context.
  jobject ret = pEnv->NewObject(
      sClazzWebPImage,
      sWebPImageConstructor,
      (jlong) spNativeContext.get());
  if (ret != nullptr) {
    // Ownership was transferred.
    spNativeContext->refCount = 1;
    spNativeContext.release();
  }
  return ret;
}

/**
 * Releases a reference to the WebPImageNativeContext and deletes it when the reference count
 * reaches 0
 */
void WebPImageNativeContext_releaseRef(JNIEnv* pEnv, jobject thiz, WebPImageNativeContext* p) {
  pEnv->MonitorEnter(thiz);
  p->refCount--;
  if (p->refCount == 0) {
    delete p;
  }
  pEnv->MonitorExit(thiz);
}

/**
 * Functor for getWebPImageNativeContext that releases the reference.
 */
struct WebPImageNativeContextReleaser {
  JNIEnv* pEnv;
  jobject webpImage;

  WebPImageNativeContextReleaser(JNIEnv* pEnv, jobject webpImage) :
      pEnv(pEnv), webpImage(webpImage) {}

  void operator()(WebPImageNativeContext* pNativeContext) {
    WebPImageNativeContext_releaseRef(pEnv, webpImage, pNativeContext);
  }
};

/**
 * Gets the WebPImageNativeContext from the mNativeContext of the WebPImage object. This returns
 * a reference counted shared_ptr.
 *
 * @return the shared_ptr which will be a nullptr in the case where the object has already been
 *    disposed
 */
std::unique_ptr<WebPImageNativeContext, WebPImageNativeContextReleaser>
    getWebPImageNativeContext(JNIEnv* pEnv, jobject thiz) {

  // A deleter that decrements the reference and possibly deletes the instance.
  WebPImageNativeContextReleaser releaser(pEnv, thiz);
  std::unique_ptr<WebPImageNativeContext, WebPImageNativeContextReleaser> ret(nullptr, releaser);
  pEnv->MonitorEnter(thiz);
  WebPImageNativeContext* pNativeContext =
      (WebPImageNativeContext*) pEnv->GetLongField(thiz, sWebPImageFieldNativeContext);
  if (pNativeContext != nullptr) {
    pNativeContext->refCount++;
    ret.reset(pNativeContext);
  }
  pEnv->MonitorExit(thiz);
  return ret;
}

/**
 * Creates a new WebPImage from the specified byte buffer. The data from the byte buffer is copied
 * into native memory managed by WebPImage.
 *
 * @param byteBuffer A java.nio.ByteBuffer. Must be direct. Assumes data is the entire capacity
 *      of the buffer
 * @return a newly allocated WebPImage
 */
jobject WebPImage_nativeCreateFromDirectByteBuffer(JNIEnv* pEnv, jclass clazz, jobject byteBuffer) {
  jbyte* bbufInput = (jbyte*) pEnv->GetDirectBufferAddress(byteBuffer);
  if (!bbufInput) {
    throwIllegalArgumentException(pEnv, "ByteBuffer must be direct");
    return 0;
  }

  jlong capacity = pEnv->GetDirectBufferCapacity(byteBuffer);
  if (pEnv->ExceptionCheck()) {
    return 0;
  }

  std::vector<uint8_t> vBuffer(bbufInput, bbufInput + capacity);
  return WebPImage_nativeCreateFromByteVector(pEnv, vBuffer);
}

/**
 * Creates a new WebPImage from the specified native pointer. The data is copied into memory
 managed by WebPImage.
 *
 * @param nativePtr the native memory pointer
 * @param sizeInBytes size in bytes of the buffer
 * @return a newly allocated WebPImage
 */
jobject WebPImage_nativeCreateFromNativeMemory(
    JNIEnv* pEnv,
    jclass clazz,
    jlong nativePtr,
    jint sizeInBytes) {

  jbyte* const pointer = (jbyte*) nativePtr;
  std::vector<uint8_t> vBuffer(pointer, pointer + sizeInBytes);
  return WebPImage_nativeCreateFromByteVector(pEnv, vBuffer);
}

/**
 * Gets the width of the image.
 *
 * @return the width of the image
 */
jint WebPImage_nativeGetWidth(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return 0;
  }

  return spNativeContext->pixelWidth;
}

/**
 * Gets the height of the image.
 *
 * @return the height of the image
 */
jint WebPImage_nativeGetHeight(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return 0;
  }
  return spNativeContext->pixelHeight;
}

/**
 * Gets the number of frames in the image.
 *
 * @return the number of frames in the image
 */
jint WebPImage_nativeGetFrameCount(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return 0;
  }
  return spNativeContext->numFrames;
}

/**
 * Gets the duration of the animated image.
 *
 * @return the duration of the animated image in milliseconds
 */
jint WebPImage_nativeGetDuration(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return 0;
  }
  return spNativeContext->durationMs;
}

/**
 * Gets the number of loops to run the animation for.
 *
 * @return the number of loops, or 0 to indicate infinite
 */
jint WebPImage_nativeGetLoopCount(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return 0;
  }
  return spNativeContext->loopCount;
}

/**
 * Gets the duration of each frame of the animated image.
 *
 * @return an array that is the size of the number of frames containing the duration of each frame
 *     in milliseconds
 */
jintArray WebPImage_nativeGetFrameDurations(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return NULL;
  }
  jintArray result = pEnv->NewIntArray(spNativeContext->numFrames);
  if (result == nullptr) {
    // pEnv->NewIntArray will have already instructed the environment to throw an exception.
    return nullptr;
  }

  pEnv->SetIntArrayRegion(
      result,
      0,
      spNativeContext->numFrames,
      spNativeContext->frameDurationsMs.data());
  return result;
}

/**
 * Gets the Frame at the specified index.
 *
 * @param index the index of the frame
 * @return a newly created WebPFrame for the specified frame
 */
jobject WebPImage_nativeGetFrame(JNIEnv* pEnv, jobject thiz, jint index) {
  auto spNativeContext = getWebPImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return nullptr;
  }


  auto spIter = std::unique_ptr<WebPIterator, decltype(&WebPDemuxReleaseIterator)> {
    new WebPIterator(),
    WebPDemuxReleaseIterator
  };

  // Note, in WebP, frame numbers are one-based.
  if (!WebPDemuxGetFrame(spNativeContext->spDemuxer->get(), index + 1, spIter.get())) {
    throwIllegalStateException(pEnv, "unable to get frame");
    return nullptr;
  }

  std::unique_ptr<WebPFrameNativeContext> spFrameNativeContext(new WebPFrameNativeContext());
  if (!spFrameNativeContext) {
    throwOutOfMemoryError(pEnv, "Unable to allocate WebPFrameNativeContext");
    return nullptr;
  }

  spFrameNativeContext->spDemuxer = spNativeContext->spDemuxer;
  spFrameNativeContext->frameNum = spIter->frame_num;
  spFrameNativeContext->xOffset = spIter->x_offset;
  spFrameNativeContext->yOffset = spIter->y_offset;
  spFrameNativeContext->durationMs = spIter->duration;
  spFrameNativeContext->width = spIter->width;
  spFrameNativeContext->height = spIter->height;
  spFrameNativeContext->disposeToBackgroundColor =
      spIter->dispose_method == WEBP_MUX_DISPOSE_BACKGROUND;
  spFrameNativeContext->blendWithPreviousFrame = spIter->blend_method == WEBP_MUX_BLEND;
  spFrameNativeContext->pPayload = spIter->fragment.bytes;
  spFrameNativeContext->payloadSize = spIter->fragment.size;

  jobject ret = pEnv->NewObject(
      sClazzWebPFrame,
      sWebPFrameConstructor,
      (jlong) spFrameNativeContext.get());
  if (ret != nullptr) {
    // Ownership was transferred.
    spFrameNativeContext->refCount = 1;
    spFrameNativeContext.release();
  }
  return ret;
}

/**
 * Releases a reference to the WebPFrameNativeContext and deletes it when the reference count
 * reaches 0
 */
void WebPFrameNativeContext_releaseRef(JNIEnv* pEnv, jobject thiz, WebPFrameNativeContext* p) {
  pEnv->MonitorEnter(thiz);
  p->refCount--;
  if (p->refCount == 0) {
    delete p;
  }
  pEnv->MonitorExit(thiz);
}

/**
 * Functor for getWebPFrameNativeContext.
 */
struct WebPFrameNativeContextReleaser {
  JNIEnv* pEnv;
  jobject webpFrame;

  WebPFrameNativeContextReleaser(JNIEnv* pEnv, jobject webpFrame) :
      pEnv(pEnv), webpFrame(webpFrame) {}

  void operator()(WebPFrameNativeContext* pNativeContext) {
    WebPFrameNativeContext_releaseRef(pEnv, webpFrame, pNativeContext);
  }
};

/**
 * Gets the WebPFrameNativeContext from the mNativeContext of the WebPFrame object. This returns
 * a reference counted pointer.
 *
 * @return the reference counted pointer which will be a nullptr in the case where the object has
 *    already been disposed
 */
std::unique_ptr<WebPFrameNativeContext, WebPFrameNativeContextReleaser>
    getWebPFrameNativeContext(JNIEnv* pEnv, jobject thiz) {

  WebPFrameNativeContextReleaser releaser(pEnv, thiz);
  std::unique_ptr<WebPFrameNativeContext, WebPFrameNativeContextReleaser> ret(nullptr, releaser);
  pEnv->MonitorEnter(thiz);
  WebPFrameNativeContext* pNativeContext =
      (WebPFrameNativeContext*) pEnv->GetLongField(thiz, sWebPFrameFieldNativeContext);
  if (pNativeContext != nullptr) {
    pNativeContext->refCount++;
    ret.reset(pNativeContext);
  }
  pEnv->MonitorExit(thiz);
  return ret;
}

/**
 * Gets the size in bytes used by the {@link WebPImage}. The implementation only takes into
 * account the encoded data buffer as the other data structures are relatively tiny.
 *
 * @return approximate size in bytes used by the {@link WebPImage}
 */
jint WebPImage_nativeGetSizeInBytes(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return 0;
  }
  return spNativeContext->spDemuxer->getBufferSize();
}

/**
 * Disposes the WebImage, freeing native resources.
 */
void WebImage_nativeDispose(JNIEnv* pEnv, jobject thiz) {
  pEnv->MonitorEnter(thiz);
  WebPImageNativeContext* pNativeContext =
      (WebPImageNativeContext*) pEnv->GetLongField(thiz, sWebPImageFieldNativeContext);
  if (pNativeContext != nullptr) {
    pEnv->SetLongField(thiz, sWebPImageFieldNativeContext, 0);
    WebPImageNativeContext_releaseRef(pEnv, thiz, pNativeContext);
  }

  pEnv->MonitorExit(thiz);
}

/**
 * Finalizer for WebImage that frees native resources.
 */
void WebImage_nativeFinalize(JNIEnv* pEnv, jobject thiz) {
  WebImage_nativeDispose(pEnv, thiz);
}


////////////////////////////////////////////////////////////////
/// Related to WebPFrame
////////////////////////////////////////////////////////////////

/**
 * Renders the frame to the specified pixel array. The array is expected to have a size that
 * is at least the the width and height of the frame. The frame is rendered where each pixel is
 * represented as a 32-bit BGRA pixel. The rendered stride is the same as the frame width. Note,
 * the number of pixels written to the array may be smaller than the canvas if the frame's
 * width/height is smaller than the canvas.
 *
 * @param jPixels the array to render into
 */
void WebPFrame_nativeRenderFrame(
    JNIEnv* pEnv,
    jobject thiz,
    jint width,
    jint height,
    jobject bitmap) {
  auto spNativeContext = getWebPFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return;
  }

  AndroidBitmapInfo bitmapInfo;
  if (AndroidBitmap_getInfo(pEnv, bitmap, &bitmapInfo) != ANDROID_BITMAP_RESULT_SUCCESS) {
    throwIllegalStateException(pEnv, "Bad bitmap");
    return;
  }

  if (width < 0 || height < 0) {
    throwIllegalArgumentException(pEnv, "Width or height is negative !");
    return;
  }
  
  if (bitmapInfo.width < (unsigned) width || bitmapInfo.height < (unsigned) height) {
    throwIllegalStateException(pEnv, "Width or height is too small");
    return;
  }

  if (bitmapInfo.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
    throwIllegalStateException(pEnv, "Wrong color format");
    return;
  }

  WebPDecoderConfig config;
  int ret = WebPInitDecoderConfig(&config);
  if (!ret) {
    throwIllegalStateException(pEnv, "WebPInitDecoderConfig failed");
    return;
  }

  const uint8_t* pPayload = spNativeContext->pPayload;
  size_t payloadSize = spNativeContext->payloadSize;

  ret = (WebPGetFeatures(pPayload , payloadSize, &config.input) == VP8_STATUS_OK);
  if (!ret) {
    throwIllegalStateException(pEnv, "WebPGetFeatures failed");
    return;
  }

  uint8_t* pixels;
  if (AndroidBitmap_lockPixels(pEnv, bitmap, (void**) &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
    throwIllegalStateException(pEnv, "Bad bitmap");
    return;
  }

  config.options.no_fancy_upsampling = 1;
  if (width != spNativeContext->width || height != spNativeContext->height) {
    config.options.use_scaling = true;
    config.options.scaled_width = width;
    config.options.scaled_height = height;
  }

  config.output.colorspace = MODE_rgbA;
  config.output.is_external_memory = 1;
  config.output.u.RGBA.rgba = pixels;
  config.output.u.RGBA.stride = bitmapInfo.stride;
  config.output.u.RGBA.size   = bitmapInfo.stride * bitmapInfo.height;

  ret = WebPDecode(pPayload, payloadSize, &config);
  AndroidBitmap_unlockPixels(pEnv, bitmap);
  if (ret != VP8_STATUS_OK) {
    throwIllegalStateException(pEnv, "Failed to decode frame");
  }
}

/**
 * Gets the duration of the frame.
 *
 * @return the duration of the frame in milliseconds
 */
jint WebPFrame_nativeGetDurationMs(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->durationMs;
}

/**
 * Gets the width of the frame.
 *
 * @return the width of the frame
 */
jint WebPFrame_nativeGetWidth(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->width;
}

/**
 * Gets the height of the frame.
 *
 * @return the height of the frame
 */
jint WebPFrame_nativeGetHeight(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->height;
}

/**
 * Gets the x-offset of the frame relative to the image canvas.
 *
 * @return the x-offset of the frame
 */
jint WebPFrame_nativeGetXOffset(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->xOffset;
}

/**
 * Gets the y-offset of the frame relative to the image canvas.
 *
 * @return the y-offset of the frame
 */
jint WebPFrame_nativeGetYOffset(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->yOffset;
}

/**
 * Gets whether the current frame should be disposed to the background color (or may be needed
 * as the background of the next frame).
 *
 * @return whether the current frame should be disposed to the background color
 */
jboolean WebPFrame_nativeShouldDisposeToBackgroundColor(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->disposeToBackgroundColor;
}

/**
 * Gets whether the current frame should be alpha blended over the previous frame.
 *
 * @return whether the current frame should be alpha blended over the previous frame
 */
jboolean WebPFrame_nativeShouldBlendWithPreviousFrame(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getWebPFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->blendWithPreviousFrame;
}

/**
 * Disposes the WebPFrameIterator, freeing native resources.
 */
void WebPFrame_nativeDispose(JNIEnv* pEnv, jobject thiz) {
  pEnv->MonitorEnter(thiz);
  WebPFrameNativeContext* pNativeContext =
      (WebPFrameNativeContext*) pEnv->GetLongField(thiz, sWebPFrameFieldNativeContext);
  if (pNativeContext) {
    pEnv->SetLongField(thiz, sWebPFrameFieldNativeContext, 0);
    WebPFrameNativeContext_releaseRef(pEnv, thiz, pNativeContext);
  }
  pEnv->MonitorExit(thiz);
}

/**
 * Finalizer for WebPFrame that frees native resources.
 */
void WebPFrame_nativeFinalize(JNIEnv* pEnv, jobject thiz) {
  WebPFrame_nativeDispose(pEnv, thiz);
}

static JNINativeMethod sWebPImageMethods[] = {
  { "nativeCreateFromDirectByteBuffer",
    "(Ljava/nio/ByteBuffer;)Lcom/facebook/imagepipeline/webp/WebPImage;",
    (void*)WebPImage_nativeCreateFromDirectByteBuffer },
  { "nativeCreateFromNativeMemory",
    "(JI)Lcom/facebook/imagepipeline/webp/WebPImage;",
    (void*)WebPImage_nativeCreateFromNativeMemory },
  { "nativeGetWidth",
    "()I",
    (void*)WebPImage_nativeGetWidth },
  { "nativeGetHeight",
    "()I",
    (void*)WebPImage_nativeGetHeight },
  { "nativeGetDuration",
    "()I",
    (void*)WebPImage_nativeGetDuration },
  { "nativeGetFrameCount",
    "()I",
    (void*)WebPImage_nativeGetFrameCount },
  { "nativeGetFrameDurations",
    "()[I",
    (void*)WebPImage_nativeGetFrameDurations },
  { "nativeGetDuration",
    "()I",
    (void*)WebPImage_nativeGetDuration },
  { "nativeGetLoopCount",
    "()I",
    (void*)WebPImage_nativeGetLoopCount },
  { "nativeGetFrame",
    "(I)Lcom/facebook/imagepipeline/webp/WebPFrame;",
    (void*)WebPImage_nativeGetFrame },
  { "nativeGetSizeInBytes",
    "()I",
    (void*)WebPImage_nativeGetSizeInBytes },
  { "nativeDispose",
    "()V",
    (void*)WebImage_nativeDispose },
  { "nativeFinalize",
    "()V",
    (void*)WebImage_nativeFinalize },
};

static JNINativeMethod sWebPFrameMethods[] = {
  { "nativeRenderFrame",
    "(IILandroid/graphics/Bitmap;)V",
    (void*)WebPFrame_nativeRenderFrame },
  { "nativeGetDurationMs",
    "()I",
    (void*)WebPFrame_nativeGetDurationMs },
  { "nativeGetWidth",
    "()I",
    (void*)WebPFrame_nativeGetWidth },
  { "nativeGetHeight",
    "()I",
    (void*)WebPFrame_nativeGetHeight },
  { "nativeGetXOffset",
    "()I",
    (void*)WebPFrame_nativeGetXOffset },
  { "nativeGetYOffset",
    "()I",
    (void*)WebPFrame_nativeGetYOffset },
  { "nativeGetDurationMs",
    "()I",
    (void*)WebPFrame_nativeGetDurationMs },
  { "nativeShouldDisposeToBackgroundColor",
    "()Z",
    (void*)WebPFrame_nativeShouldDisposeToBackgroundColor },
  { "nativeShouldBlendWithPreviousFrame",
    "()Z",
    (void*)WebPFrame_nativeShouldBlendWithPreviousFrame },
  { "nativeDispose",
    "()V",
    (void*)WebPFrame_nativeDispose },
  { "nativeFinalize",
    "()V",
    (void*)WebPFrame_nativeFinalize },
};

/**
 * Called by JNI_OnLoad to initialize the classes.
 */
int initWebPImage(JNIEnv* pEnv) {
  // WebPImage
  sClazzWebPImage = findClassOrThrow(pEnv, kWebPImageClassPathName);
  if (sClazzWebPImage == NULL) {
    return JNI_ERR;
  }

  // WebPImage.mNativeContext
  sWebPImageFieldNativeContext = getFieldIdOrThrow(pEnv, sClazzWebPImage, "mNativeContext", "J");
  if (!sWebPImageFieldNativeContext) {
    return JNI_ERR;
  }

  // WebPImage.<init>
  sWebPImageConstructor = getMethodIdOrThrow(pEnv, sClazzWebPImage, "<init>", "(J)V");
  if (!sWebPImageConstructor) {
    return JNI_ERR;
  }

  int result = pEnv->RegisterNatives(
      sClazzWebPImage,
      sWebPImageMethods,
      std::extent<decltype(sWebPImageMethods)>::value);
  if (result != JNI_OK) {
    return result;
  }

  // WebPFrame
  sClazzWebPFrame = findClassOrThrow(pEnv, kWebPFrameClassPathName);
  if (sClazzWebPFrame == NULL) {
    return JNI_ERR;
  }

  // WebPFrame.mNativeContext
  sWebPFrameFieldNativeContext = getFieldIdOrThrow(pEnv, sClazzWebPFrame, "mNativeContext", "J");
  if (!sWebPFrameFieldNativeContext) {
    return JNI_ERR;
  }

  // WebPFrame.<init>
  sWebPFrameConstructor = getMethodIdOrThrow(pEnv, sClazzWebPFrame, "<init>", "(J)V");
  if (!sWebPFrameConstructor) {
    return JNI_ERR;
  }

  result = pEnv->RegisterNatives(
      sClazzWebPFrame,
      sWebPFrameMethods,
      std::extent<decltype(sWebPFrameMethods)>::value);
  if (result != JNI_OK) {
    return result;
  }

  return JNI_OK;
}
