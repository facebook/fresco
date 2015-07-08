/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#define LOG_TAG "GifImage"

#include <jni.h>
#include <algorithm>
#include <memory>
#include <mutex>
#include <utility>
#include <vector>
#include <android/bitmap.h>
#include <android/log.h>

#include "gif_lib.h"
#include "jni_helpers.h"

using namespace facebook;

#define EXTRA_LOGGING false

static void DGifCloseFile2(GifFileType* pGifFile) {
  int errorCode;
  DGifCloseFile(pGifFile, &errorCode);
}

class DataWrapper {

public:
  DataWrapper(std::vector<uint8_t>&& pBuffer) :
    m_pBuffer(std::move(pBuffer)), m_position(0) {
  }

  uint8_t* getBuffer() {
    return m_pBuffer.data();
  }

  size_t getBufferSize() {
    return m_pBuffer.size();
  }

  size_t getPosition() {
    return m_position;
  }

  void setPosition(size_t position) {
    m_position = position;
  }

private:
  std::vector<uint8_t> m_pBuffer;
  size_t m_position;
};

class GifWrapper {

public:
  GifWrapper(
      std::unique_ptr<GifFileType, decltype(&DGifCloseFile2)>&& pGifFile,
      std::shared_ptr<DataWrapper>& pData) :
          m_spGifFile(std::move(pGifFile)),
          m_spData(pData) {
    m_rasterBits.reserve(m_spGifFile->SWidth * m_spGifFile->SHeight);
  }

  virtual ~GifWrapper() {
    //FBLOGD("Deleting GifWrapper");
  }

  GifFileType* get() {
    return m_spGifFile.get();
  }

  DataWrapper* getData() {
    return m_spData.get();
  }

  void addFrameByteOffset(size_t offset) {
    m_vectorFrameByteOffsets.push_back(offset);
  }

  size_t getFrameByteOffset(int frameNum) {
    return m_vectorFrameByteOffsets[frameNum];
  }

  uint8_t* getRasterBits() {
    return m_rasterBits.data();
  }

  size_t getRasterBitsSize() {
    return m_rasterBits.size();
  }

  std::mutex& getRasterMutex() {
    return m_rasterMutex;
  }

private:
  std::unique_ptr<GifFileType, decltype(&DGifCloseFile2)> m_spGifFile;
  std::shared_ptr<DataWrapper> m_spData;
  std::vector<int> m_vectorFrameByteOffsets;
  std::vector<uint8_t> m_rasterBits;
  std::mutex m_rasterMutex;
};

/**
 * Native context for GifImage.
 */
struct GifImageNativeContext {

  /** Reference to the GifWrapper */
  std::shared_ptr<GifWrapper> spGifWrapper;

  /** Cached width of the image */
  int pixelWidth;

  /** Cached height of the image */
  int pixelHeight;

  /** Cached number of the frames in the image */
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
  ~GifImageNativeContext() {
    __android_log_write(ANDROID_LOG_DEBUG, LOG_TAG, "GifImageNativeContext destructor");
  }
#endif

};

/**
 * Native context for GifFrame.
 */
struct GifFrameNativeContext {

  /* Reference to the GifWrapper */
  std::shared_ptr<GifWrapper> spGifWrapper;

  /** Frame number for the image. Starts at 0. */
  int frameNum;

  /** X offset for the frame relative to the image canvas */
  int xOffset;

  /** Y offset for the frame relative to the image canvas */
  int yOffset;

  /** Display duration for the frame in ms */
  int durationMs;

  /** Width of this frame */
  int width;

  /** Height of this frame */
  int height;

  /** How the GIF is disposed. See DISPOSAL_* constants in gif_lib.h */
  int disposalMode;

  /** Palette index of the transparency color, or -1 for none */
  int transparentIndex;

  /** Reference counter. Instance is deleted when it goes from 1 to 0 */
  size_t refCount;

#if EXTRA_LOGGING
  ~GifFrameNativeContext() {
    __android_log_write(ANDROID_LOG_DEBUG, LOG_TAG, "GifFrameNativeContext destructor");
  }
#endif
};

/**
 * giflib takes a callback function for reading fromt the file.
 */
static int directByteBufferReadFun(
   GifFileType* gifFileType,
   GifByteType* bytes,
   int size) {
  DataWrapper* pData = (DataWrapper*) gifFileType->UserData;
  size_t position = pData->getPosition();
  size_t bufferSize = pData->getBufferSize();
  if (position + size > bufferSize) {
    size = bufferSize - position;
  }
  if (size > 0) {
    memcpy(bytes, pData->getBuffer() + position, size);
    pData->setPosition(position + size);
  }
  return size;
}

/**
 * Struct to represent a 32-bit ARGB in the correct order.
 */
struct PixelType32 {
  uint8_t red;
  uint8_t green;
  uint8_t blue;
  uint8_t alpha;
};

/**
 * Transparent pixel constant.
 */
static const PixelType32 TRANSPARENT {0, 0, 0, 0};

// Class Names.
static const char* const kGifImageClassPathName =
    "com/facebook/imagepipeline/gif/GifImage";
static const char* const kGifFrameClassPathName =
    "com/facebook/imagepipeline/gif/GifFrame";

// Cached fields related to GifImage
static jclass sClazzGifImage;
static jmethodID sGifImageConstructor;
static jfieldID sGifImageFieldNativeContext;

// Cached fields related to GifFrame
static jclass sClazzGifFrame;
static jmethodID sGifFrameConstructor;
static jfieldID sGifFrameFieldNativeContext;

// Static default color map.
static ColorMapObject* sDefaultColorMap;

static ColorMapObject* genDefColorMap(void) {
  ColorMapObject* pColorMap = GifMakeMapObject(256, NULL);
  if (pColorMap != NULL) {
    int iColor;
    for (iColor = 0; iColor < 256; iColor++) {
      pColorMap->Colors[iColor].Red = (GifByteType) iColor;
      pColorMap->Colors[iColor].Green = (GifByteType) iColor;
      pColorMap->Colors[iColor].Blue = (GifByteType) iColor;
    }
  }
  return pColorMap;
}

////////////////////////////////////////////////////////////////
/// Related to GifImage
////////////////////////////////////////////////////////////////

bool getGraphicsControlBlockForImage(SavedImage* pSavedImage, GraphicsControlBlock* pGcp) {
  for (int i = 0; i < pSavedImage->ExtensionBlockCount; i++) {
    ExtensionBlock* pExtensionBlock = &pSavedImage->ExtensionBlocks[i];
    if (pExtensionBlock->Function == GRAPHICS_EXT_FUNC_CODE) {
      DGifExtensionToGCB(pExtensionBlock->ByteCount, pExtensionBlock->Bytes, pGcp);
      return true;
    }
  }
  return false;
}

/**
 * Reads a single frame by reading data using giflib. The method expects the data source
 * referenced by pGifFile to point to the first byte of the encoded frame data. When the method
 * returns, the data source will point to the byte just past the encoded frame data. Unlike standard
 * decoding with giflib, the raster data is written to the passed-in buffer instead of being
 * written to the SavedImage structure. This is the key to how we avoid caching all the decoded
 * frame pixels in memory.
 *
 * @param pGifFile the gif data structure to read to and write to
 * @param pRasterBits the buffer to write the decoded frame pixels to. If null, the data is
 *    not actually decoded and instead just skipped.
 * @param doNotAddToSavedImages if set to true, will not add an additional SavedImage to
 *     pGifFile->SavedImages
 * @return a gif error code
 */
int readSingleFrame(
    GifFileType* pGifFile,
    uint8_t* pRasterBits,
    bool doNotAddToSavedImages) {
  if (DGifGetImageDesc(pGifFile) == GIF_ERROR) {
    return GIF_ERROR;
  }
  SavedImage* pSavedImage = &pGifFile->SavedImages[pGifFile->ImageCount - 1];

  // Check size of image.
  if (pSavedImage->ImageDesc.Width <= 0 &&
      pSavedImage->ImageDesc.Height <= 0 &&
      pSavedImage->ImageDesc.Width > (INT_MAX / pSavedImage->ImageDesc.Height)) {
    return GIF_ERROR;
  }

  size_t imageSize = pSavedImage->ImageDesc.Width * pSavedImage->ImageDesc.Height;
  if (imageSize > (unsigned)(pGifFile->SWidth * pGifFile->SHeight)) {
    return GIF_ERROR;
  }

  if (pRasterBits != nullptr) {
    // We're were asked to decode and write the results to pRasterBits.
    if (pSavedImage->ImageDesc.Interlace) {
      // The way an interlaced image should be read - offsets and jumps...
      int interlacedOffset[] = { 0, 4, 2, 1 };
      int interlacedJumps[] = { 8, 8, 4, 2 };
      // Need to perform 4 passes on the image.
      for (int i = 0; i < 4; i++) {
        for (int j = interlacedOffset[i];
             j < pSavedImage->ImageDesc.Height;
             j += interlacedJumps[i]) {
          GifPixelType* pLine = pRasterBits + j * pSavedImage->ImageDesc.Width;
          int lineLength = pSavedImage->ImageDesc.Width;
          if (DGifGetLine(pGifFile, pLine, lineLength) == GIF_ERROR) {
            return GIF_ERROR;
          }
        }
      }
    } else {
      if (DGifGetLine(pGifFile, pRasterBits, imageSize) == GIF_ERROR) {
        return GIF_ERROR;
      }
    }
  } else {
    // Don't decode. Just read the encoded data to skip past it.
    int codeSize;
    GifByteType* pCodeBlock;
    if (DGifGetCode(pGifFile, &codeSize, &pCodeBlock) == GIF_ERROR) {
      return GIF_ERROR;
    }
    while (pCodeBlock != NULL) {
      if (DGifGetCodeNext(pGifFile, &pCodeBlock) == GIF_ERROR) {
        return GIF_ERROR;
      }
    }
  }

  if (pGifFile->ExtensionBlocks) {
    pSavedImage->ExtensionBlocks = pGifFile->ExtensionBlocks;
    pSavedImage->ExtensionBlockCount = pGifFile->ExtensionBlockCount;

    pGifFile->ExtensionBlocks = nullptr;
    pGifFile->ExtensionBlockCount = 0;
  }

  if (doNotAddToSavedImages) {
    // giflib wasn't designed to work with decoding arbitrary frames on the fly. By default, it will
    // keep adding more images to the SavedImages array. To avoid that, we just decrement the image
    // count. It basically means the array remains larger by one GifFileType. We decrement it so
    // it doesn't grow by one every time we decode.
    pGifFile->ImageCount--;
  }

  return GIF_OK;
}

/**
 * Decodes an extension as part of modifiedDGifSlurp.
 *
 * @param pGifFile the gif data structure to read to and write to
 * @return a gif error code
 */
int decodeExtension(GifFileType* pGifFile) {
  GifByteType* pExtData;
  int extFunction;

  if (DGifGetExtension(pGifFile, &extFunction, &pExtData) == GIF_ERROR) {
    return GIF_ERROR;
  }

  // Create an extension block with our data.
  if (pExtData != nullptr) {
    if (GifAddExtensionBlock(
            &pGifFile->ExtensionBlockCount,
            &pGifFile->ExtensionBlocks,
            extFunction,
            pExtData[0],
            &pExtData[1]) == GIF_ERROR) {
      return GIF_ERROR;
    }
  }
  while (pExtData != nullptr) {
    if (DGifGetExtensionNext(pGifFile, &pExtData) == GIF_ERROR) {
      return GIF_ERROR;
    }
    // Continue the extension block.
    if (pExtData != NULL) {
      if (GifAddExtensionBlock(
          &pGifFile->ExtensionBlockCount,
          &pGifFile->ExtensionBlocks,
          CONTINUE_EXT_FUNC_CODE,
          pExtData[0],
          &pExtData[1]) == GIF_ERROR) {
        return GIF_ERROR;
      }
    }
  }
  return GIF_OK;
}

/**
 * A heavily modified version of giflib's DGifSlurp. This uses some hacks to avoid caching the
 * decoded pixel data for each frame in memory. Like DGifSlurp, GifFileType will contain the
 * results of slurping the GIF but there will be no frame pixel data cached in
 * SavedImage.RasterBits.
 *
 * @param pGifWrapper the gif wrapper containing the giflib struct and additional data
 * @return a gif error code
 */
int modifiedDGifSlurp(GifWrapper* pGifWrapper) {
  GifFileType* pGifFile = pGifWrapper->get();
  GifRecordType recordType;

  pGifFile->ExtensionBlocks = NULL;
  pGifFile->ExtensionBlockCount = 0;
  do {
    if (DGifGetRecordType(pGifFile, &recordType) == GIF_ERROR) {
      return GIF_ERROR;
    }

    switch (recordType) {
      case IMAGE_DESC_RECORD_TYPE:
        // We save the byte offset where each frame begins. This allows us to avoid storing
        // the pixel data for each frame and instead decode it on the fly.
        pGifWrapper->addFrameByteOffset(pGifWrapper->getData()->getPosition());

        if (readSingleFrame(
              pGifWrapper->get(),
              nullptr,
              false) == GIF_ERROR) {
          return GIF_ERROR;
        }
        break;

      case EXTENSION_RECORD_TYPE:
        if (decodeExtension(pGifFile) == GIF_ERROR) {
          return GIF_ERROR;
        }
        break;

      case TERMINATE_RECORD_TYPE:
        break;

       default:    // Should be trapped by DGifGetRecordType.
        break;
      }
    } while (recordType != TERMINATE_RECORD_TYPE);
    return GIF_OK;
}

/**
 * Creates a new GifImage from the specified buffer.
 *
 * @param vBuffer the vector containing the bytes
 * @return a newly allocated GifImage
 */
jobject GifImage_nativeCreateFromByteVector(JNIEnv* pEnv, std::vector<uint8_t>& vBuffer) {
  std::unique_ptr<GifImageNativeContext> spNativeContext(new GifImageNativeContext());
  if (!spNativeContext) {
    throwOutOfMemoryError(pEnv, "Unable to allocate native context");
    return 0;
  }

  // Create the DataWrapper
  std::shared_ptr<DataWrapper> spDataWrapper =
      std::shared_ptr<DataWrapper>(new DataWrapper(std::move(vBuffer)));

  int gifError = 0;
  auto spGifFileIn = std::unique_ptr<GifFileType, decltype(&DGifCloseFile2)> {
      DGifOpen(
          (void*) spDataWrapper.get(),
          &directByteBufferReadFun,
          &gifError),
      DGifCloseFile2
  };

  if (spGifFileIn == nullptr) {
    throwIllegalStateException(pEnv, "Error %d", gifError);
    return nullptr;
  }

  int width = spGifFileIn->SWidth;
  int height = spGifFileIn->SHeight;
  size_t wxh = width * height;
  if (wxh < 1 || wxh > SIZE_MAX) {
    throwIllegalStateException(pEnv, "Invalid dimensions");
    return nullptr;
  }

  // Create the GifWrapper
  spNativeContext->spGifWrapper = std::shared_ptr<GifWrapper>(
    new GifWrapper(std::move(spGifFileIn), spDataWrapper));

  GifFileType* pGifFile = spNativeContext->spGifWrapper->get();
  if (spNativeContext->spGifWrapper->getData()->getPosition() < 0) {
    throwIllegalStateException(pEnv, "Error %d", D_GIF_ERR_NOT_READABLE);
    return nullptr;
  }

  spNativeContext->pixelWidth = width;
  spNativeContext->pixelHeight = height;

  int error = modifiedDGifSlurp(spNativeContext->spGifWrapper.get());
  if (error != GIF_OK) {
    throwIllegalStateException(pEnv, "Failed to slurp image %d", error);
    return nullptr;
  }

  if (pGifFile->ImageCount < 1) {
    throwIllegalStateException(pEnv, "No frames in image");
    return nullptr;
  }
  spNativeContext->numFrames = pGifFile->ImageCount;

  // Compute cached fields that require iterating the frames.
  int durationMs = 0;
  std::vector<jint> frameDurationsMs;
  for (int i = 0; i < pGifFile->ImageCount; i++) {
    SavedImage* pSavedImage = &pGifFile->SavedImages[i];
    GraphicsControlBlock gcp;
    if (getGraphicsControlBlockForImage(pSavedImage, &gcp)) {
      int frameDurationMs = gcp.DelayTime * 10;
      durationMs += frameDurationMs;
      frameDurationsMs.push_back(frameDurationMs);
    } else {
      frameDurationsMs.push_back(0);
    }
  }
  spNativeContext->durationMs = durationMs;
  spNativeContext->frameDurationsMs = frameDurationsMs;

  // Create the GifImage with the native context.
  jobject ret = pEnv->NewObject(
      sClazzGifImage,
      sGifImageConstructor,
      (jlong) spNativeContext.get());
  if (ret != nullptr) {
    // Ownership was transferred.
    spNativeContext->refCount = 1;
    spNativeContext.release();
  }
  return ret;
}

/**
 * Releases a reference to the GifPImageNativeContext and deletes it when the reference count
 * reaches 0
 */
void GifImageNativeContext_releaseRef(JNIEnv* pEnv, jobject thiz, GifImageNativeContext* p) {
  pEnv->MonitorEnter(thiz);
  p->refCount--;
  if (p->refCount == 0) {
    delete p;
  }
  pEnv->MonitorExit(thiz);
}

/**
 * Functor for getGifImageNativeContext that releases the reference.
 */
struct GifImageNativeContextReleaser {
  JNIEnv* pEnv;
  jobject gifImage;

  GifImageNativeContextReleaser(JNIEnv* pEnv, jobject gifImage) : pEnv(pEnv), gifImage(gifImage) {}
  void operator()(GifImageNativeContext* pNativeContext) {
    GifImageNativeContext_releaseRef(pEnv, gifImage, pNativeContext);
  }
};

/**
 * Gets the GifImageNativeContext from the mNativeContext of the GifImage object. This returns
 * a reference counted pointer.
 *
 * @return the referenced counted pointer which will be a nullptr in the case where the object has
 *    already been disposed
 */
std::unique_ptr<GifImageNativeContext, GifImageNativeContextReleaser>
    getGifImageNativeContext(JNIEnv* pEnv, jobject thiz) {

  GifImageNativeContextReleaser releaser(pEnv, thiz);
  std::unique_ptr<GifImageNativeContext, GifImageNativeContextReleaser> ret(nullptr, releaser);
  pEnv->MonitorEnter(thiz);
  GifImageNativeContext* pNativeContext =
      (GifImageNativeContext*) pEnv->GetLongField(thiz, sGifImageFieldNativeContext);
  if (pNativeContext != nullptr) {
    pNativeContext->refCount++;
    ret.reset(pNativeContext);
  }
  pEnv->MonitorExit(thiz);
  return ret;
}

/**
 * Creates a new GifImage from the specified byte buffer. The data from the byte buffer is copied
 * into native memory managed by GifImage.
 *
 * @param byteBuffer A java.nio.ByteBuffer. Must be direct. Assumes data is the entire capacity
 *      of the buffer
 * @return a newly allocated GifImage
 */
jobject GifImage_nativeCreateFromDirectByteBuffer(JNIEnv* pEnv, jclass clazz, jobject byteBuffer) {
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
  return GifImage_nativeCreateFromByteVector(pEnv, vBuffer);
}

/**
 * Creates a new GifImage from the specified native pointer. The data is copied into memory
 managed by GifImage.
 *
 * @param nativePtr the native memory pointer
 * @param sizeInBytes size in bytes of the buffer
 * @return a newly allocated GifImage
 */
jobject GifImage_nativeCreateFromNativeMemory(
    JNIEnv* pEnv,
    jclass clazz,
    jlong nativePtr,
    jint sizeInBytes) {

  jbyte* const pointer = (jbyte*) nativePtr;
  std::vector<uint8_t> vBuffer(pointer, pointer + sizeInBytes);
  return GifImage_nativeCreateFromByteVector(pEnv, vBuffer);
}

/**
 * Gets the width of the image.
 *
 * @return the width of the image
 */
jint GifImage_nativeGetWidth(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
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
jint GifImage_nativeGetHeight(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
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
jint GifImage_nativeGetFrameCount(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
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
jint GifImage_nativeGetDuration(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
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
jint GifImage_nativeGetLoopCount(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
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
jintArray GifImage_nativeGetFrameDurations(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
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
 * @param index the index of the frame (0-based)
 * @return a newly created GifFrame for the specified frame
 */
jobject GifImage_nativeGetFrame(JNIEnv* pEnv, jobject thiz, jint index) {
 auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return nullptr;
  }

  GifFileType* pGifFile = spNativeContext->spGifWrapper->get();
  SavedImage* pSavedImage = &pGifFile->SavedImages[index];

  std::unique_ptr<GifFrameNativeContext> spFrameNativeContext(new GifFrameNativeContext());
  if (!spFrameNativeContext) {
    throwOutOfMemoryError(pEnv, "Unable to allocate GifFrameNativeContext");
    return nullptr;
  }

  spFrameNativeContext->spGifWrapper = spNativeContext->spGifWrapper;
  spFrameNativeContext->frameNum = index;
  spFrameNativeContext->xOffset = pSavedImage->ImageDesc.Left;
  spFrameNativeContext->yOffset = pSavedImage->ImageDesc.Top;
  spFrameNativeContext->durationMs = spNativeContext->frameDurationsMs[index];
  spFrameNativeContext->width = pSavedImage->ImageDesc.Width;
  spFrameNativeContext->height = pSavedImage->ImageDesc.Height;

  GraphicsControlBlock gcp;
  if (getGraphicsControlBlockForImage(pSavedImage, &gcp)) {
    spFrameNativeContext->transparentIndex = gcp.TransparentColor;
    spFrameNativeContext->disposalMode = gcp.DisposalMode;
  } else {
    spFrameNativeContext->transparentIndex = NO_TRANSPARENT_COLOR;
    spFrameNativeContext->disposalMode = DISPOSAL_UNSPECIFIED;
  }

  jobject ret = pEnv->NewObject(
      sClazzGifFrame,
      sGifFrameConstructor,
      (jlong) spFrameNativeContext.get());
  if (ret != nullptr) {
    // pEnv->NewObject will have already instructed the environment to throw an exception.
    spFrameNativeContext->refCount = 1;
    spFrameNativeContext.release();
  }
  return ret;
}

/**
 * Releases a reference to the WebPFrameNativeContext and deletes it when the reference count
 * reaches 0
 */
void GifFrameNativeContext_releaseRef(JNIEnv* pEnv, jobject thiz, GifFrameNativeContext* p) {
  pEnv->MonitorEnter(thiz);
  p->refCount--;
  if (p->refCount == 0) {
    delete p;
  }
  pEnv->MonitorExit(thiz);
}

/**
 * Functor for getGifFrameNativeContext.
 */
struct GifFrameNativeContextReleaser {
  JNIEnv* pEnv;
  jobject gifFrame;

  GifFrameNativeContextReleaser(JNIEnv* pEnv, jobject gifFrame) : pEnv(pEnv), gifFrame(gifFrame) {}
  void operator()(GifFrameNativeContext* pNativeContext) {
    GifFrameNativeContext_releaseRef(pEnv, gifFrame, pNativeContext);
  }
};

/**
 * Gets the GifFrameNativeContext from the mNativeContext of the GifFrame object. This returns
 * a reference counted pointer.
 *
 * @return the reference counted pointer which will be a nullptr in the case where the object has
 *    already been disposed
 */
std::unique_ptr<GifFrameNativeContext, GifFrameNativeContextReleaser>
    getGifFrameNativeContext(JNIEnv* pEnv, jobject thiz) {

  GifFrameNativeContextReleaser releaser(pEnv, thiz);
  std::unique_ptr<GifFrameNativeContext, GifFrameNativeContextReleaser> ret(nullptr, releaser);
  pEnv->MonitorEnter(thiz);
  GifFrameNativeContext* pNativeContext =
      (GifFrameNativeContext*) pEnv->GetLongField(thiz, sGifFrameFieldNativeContext);
  if (pNativeContext != nullptr) {
    pNativeContext->refCount++;
    ret.reset(pNativeContext);
  }
  pEnv->MonitorExit(thiz);
  return ret;
}

/**
 * Gets the size in bytes used by the {@link GifImage}. The implementation only takes into
 * account the encoded data buffer as the other data structures are relatively tiny.
 *
 * @return approximate size in bytes used by the {@link GifImage}
 */
jint GifImage_nativeGetSizeInBytes(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return 0;
  }

  // This is an approximate amount based on the data buffer and the saved images
  int size = 0;
  size += spNativeContext->spGifWrapper->getData()->getBufferSize();
  size += spNativeContext->spGifWrapper->getRasterBitsSize();
  return size;
}

/**
 * Disposes the GifImage, freeing native resources.
 */
void GifImage_nativeDispose(JNIEnv* pEnv, jobject thiz) {
  pEnv->MonitorEnter(thiz);
  GifImageNativeContext* pNativeContext =
      (GifImageNativeContext*) pEnv->GetLongField(thiz, sGifImageFieldNativeContext);
  if (pNativeContext != nullptr) {
    pEnv->SetLongField(thiz, sGifImageFieldNativeContext, 0);
    GifImageNativeContext_releaseRef(pEnv, thiz, pNativeContext);
  }
  pEnv->MonitorExit(thiz);
}

/**
 * Finalizer for GifImage that frees native resources.
 */
void GifImage_nativeFinalize(JNIEnv* pEnv, jobject thiz) {
  GifImage_nativeDispose(pEnv, thiz);
}


////////////////////////////////////////////////////////////////
/// Related to GifFrame
////////////////////////////////////////////////////////////////

/**
 * Packs a components of a pixel into a 32-bit PixelType32.
 */
static PixelType32 packARGB32(
    GifByteType alpha,
    GifByteType red,
    GifByteType green,
    GifByteType blue) {
  PixelType32 pixel;
  pixel.alpha = alpha;
  pixel.red = red;
  pixel.green = green;
  pixel.blue = blue;
  return pixel;
}

/**
 * Gets a color from the color table given an index.
 *
 * @param idx the index of the color
 * @param pColorMap the color map
 * @return a 32-bit pixel
 */
static PixelType32 getColorFromTable(int idx, const ColorMapObject* pColorMap) {
  int colIdx = (idx >= pColorMap->ColorCount) ? 0 : idx;
  GifColorType* pColor = &pColorMap->Colors[colIdx];
  return packARGB32(0xFF, pColor->Red, pColor->Green, pColor->Blue);
}

/**
 * Blits a line of an 8-bit GIF frame to a 32-bit destination performing the color conversion
 * along the way.
 *
 * @param pDest the 32-bit pixel destination to write into
 * @param pSource the 8-bit frame source where values are indices into the color table
 * @param pColorMap the color map
 * @param transparentIndex index to use for the transparent pixel
 * @param width number of pixels to copy
 */
static void blitLine(
    PixelType32* pDest,
    const GifByteType* pSource,
    const ColorMapObject* pColorMap,
    int transparentIndex,
    int width) {
  std::transform(pSource, pSource + width, pDest, [=] (uint8_t color) {
    if (color == transparentIndex) {
      return TRANSPARENT;
    }
    return getColorFromTable(color, pColorMap);
  });
}

/**
 * Blits an 8-bit GIF frame into a 32-bit destination performing the color conversion along the way.
 *
 * @param pDest the byte buffer to write into
 * @param destWidth the width of the destination
 * @param destHeight the height of the destination
 * @param pFrame the frame to read from
 * @param pColorMap the color map
 * @param transparentIndex index to use for the transparent pixel
 */
static void blitNormal(
    uint8_t* pDest,
    int destWidth,
    int destHeight,
    int destStride,
    const SavedImage* pFrame,
    const GifByteType* pSrcRasterBits,
    const ColorMapObject* cmap,
    int transparentIndex) {
  GifWord copyWidth = pFrame->ImageDesc.Width;
  if (copyWidth > destWidth) {
    copyWidth = destWidth;
  }

  GifWord copyHeight = pFrame->ImageDesc.Height;
  if (copyHeight > destHeight) {
    copyHeight = destHeight;
  }

  for (; copyHeight > 0; copyHeight--) {
    blitLine((PixelType32*) pDest, pSrcRasterBits, cmap, transparentIndex, copyWidth);
    pSrcRasterBits += pFrame->ImageDesc.Width;
    pDest += destStride;
  }
}

/**
 * Renders the frame to the specified pixel array. The array is expected to have a size that
 * is at least the the width and height of the frame. The frame is rendered where each pixel is
 * represented as a 32-bit BGRA pixel. The rendered stride is the same as the frame width. Note,
 * the number of pixels written to the array may be smaller than the canvas if the frame's
 * width/height is smaller than the canvas.
 *
 * @param jPixels the array to render into
 */
void GifFrame_nativeRenderFrame(
    JNIEnv* pEnv,
    jobject thiz,
    jint width,
    jint height,
    jobject bitmap) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
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
    throwIllegalArgumentException(pEnv, "Width or height is negative");
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

  GifWrapper *pGifWrapper = spNativeContext->spGifWrapper.get();

  // Note, there is some major hackery below since giflib is not intended to incrementally decode
  // arbitrary frames on demand.

  // We need to lock because the raster data and the data offset are shared resources and only
  // one thread can use them at a time.
  std::unique_lock<std::mutex> lock(pGifWrapper->getRasterMutex());

  // We set the data buffer that giflib will read from to be at the beginning of where the encoded
  // data for the frame starts. We know this offset because we stored it when we originally decoded
  // the GIF.
  int frameNum = spNativeContext->frameNum;
  int byteOffset = pGifWrapper->getFrameByteOffset(frameNum);
  pGifWrapper->getData()->setPosition(byteOffset);

  // Now we kick off the decoding process.
  readSingleFrame(pGifWrapper->get(), pGifWrapper->getRasterBits(), true);

  // Get the right color table to use.
  ColorMapObject* pColorMap = spNativeContext->spGifWrapper->get()->SColorMap;
  SavedImage* pSavedImage = &pGifWrapper->get()->SavedImages[frameNum];
  if (pSavedImage->ImageDesc.ColorMap != NULL) {
    // use local color table
    pColorMap = pSavedImage->ImageDesc.ColorMap;
    if (pColorMap->ColorCount != (1 << pColorMap->BitsPerPixel)) {
      pColorMap = sDefaultColorMap;
    }
  }

  uint8_t* pixels;
  if (AndroidBitmap_lockPixels(pEnv, bitmap, (void**) &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
    throwIllegalStateException(pEnv, "Bad bitmap");
    return;
  }
  blitNormal(
      pixels,
      width,
      height,
      bitmapInfo.stride,
      pSavedImage,
      spNativeContext->spGifWrapper->getRasterBits(),
      pColorMap,
      spNativeContext->transparentIndex);
  AndroidBitmap_unlockPixels(pEnv, bitmap);
}

/**
 * Gets the duration of the frame.
 *
 * @return the duration of the frame in milliseconds
 */
jint GifFrame_nativeGetDurationMs(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->durationMs;
}

jboolean GifFrame_nativeHasTransparency(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->transparentIndex >= 0;
}

/**
 * Gets the width of the frame.
 *
 * @return the width of the frame
 */
jint GifFrame_nativeGetWidth(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
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
jint GifFrame_nativeGetHeight(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
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
jint GifFrame_nativeGetXOffset(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
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
jint GifFrame_nativeGetYOffset(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->yOffset;
}

/**
 * Gets the constant of the disposal mode for the frame.
 *
 * @return one of the GIF disposal mode constants
 */
jint GifFrame_nativeGetDisposalMode(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return -1;
  }
  return spNativeContext->disposalMode;
}

/**
 * Disposes the GifFrame, freeing native resources.
 */
void GifFrame_nativeDispose(JNIEnv* pEnv, jobject thiz) {
  pEnv->MonitorEnter(thiz);
  GifFrameNativeContext* pNativeContext =
      (GifFrameNativeContext*) pEnv->GetLongField(thiz, sGifFrameFieldNativeContext);
  if (pNativeContext) {
    pEnv->SetLongField(thiz, sGifFrameFieldNativeContext, 0);
    GifFrameNativeContext_releaseRef(pEnv, thiz, pNativeContext);
  }
  pEnv->MonitorExit(thiz);
}

/**
 * Finalizer for GifFrame that frees native resources.
 */
void GifFrame_nativeFinalize(JNIEnv* pEnv, jobject thiz) {
  GifFrame_nativeDispose(pEnv, thiz);
}


static JNINativeMethod sGifImageMethods[] = {
  { "nativeCreateFromDirectByteBuffer",
    "(Ljava/nio/ByteBuffer;)Lcom/facebook/imagepipeline/gif/GifImage;",
    (void*)GifImage_nativeCreateFromDirectByteBuffer },
  { "nativeCreateFromNativeMemory",
    "(JI)Lcom/facebook/imagepipeline/gif/GifImage;",
    (void*)GifImage_nativeCreateFromNativeMemory },
  { "nativeGetWidth",
    "()I",
    (void*)GifImage_nativeGetWidth },
  { "nativeGetHeight",
    "()I",
    (void*)GifImage_nativeGetHeight },
  { "nativeGetDuration",
    "()I",
    (void*)GifImage_nativeGetDuration },
  { "nativeGetFrameCount",
    "()I",
    (void*)GifImage_nativeGetFrameCount },
  { "nativeGetFrameDurations",
    "()[I",
    (void*)GifImage_nativeGetFrameDurations },
  { "nativeGetDuration",
    "()I",
    (void*)GifImage_nativeGetDuration },
  { "nativeGetLoopCount",
    "()I",
    (void*)GifImage_nativeGetLoopCount },
  { "nativeGetFrame",
    "(I)Lcom/facebook/imagepipeline/gif/GifFrame;",
    (void*)GifImage_nativeGetFrame },
  { "nativeGetSizeInBytes",
    "()I",
    (void*)GifImage_nativeGetSizeInBytes },
  { "nativeDispose",
    "()V",
    (void*)GifImage_nativeDispose },
  { "nativeFinalize",
    "()V",
    (void*)GifImage_nativeFinalize }
};

static JNINativeMethod sGifFrameMethods[] = {
  { "nativeRenderFrame",
    "(IILandroid/graphics/Bitmap;)V",
    (void*)GifFrame_nativeRenderFrame },
  { "nativeGetDurationMs",
    "()I",
    (void*)GifFrame_nativeGetDurationMs },
  { "nativeGetWidth",
    "()I",
    (void*)GifFrame_nativeGetWidth },
  { "nativeGetHeight",
    "()I",
    (void*)GifFrame_nativeGetHeight },
  { "nativeGetXOffset",
    "()I",
    (void*)GifFrame_nativeGetXOffset },
  { "nativeGetYOffset",
    "()I",
    (void*)GifFrame_nativeGetYOffset },
  { "nativeGetDurationMs",
    "()I",
    (void*)GifFrame_nativeGetDurationMs },
  { "nativeHasTransparency",
    "()Z",
    (void*)GifFrame_nativeHasTransparency },
  { "nativeGetDisposalMode",
    "()I",
    (void*)GifFrame_nativeGetDisposalMode  },
  { "nativeDispose",
    "()V",
    (void*)GifFrame_nativeDispose },
  { "nativeFinalize",
    "()V",
    (void*)GifFrame_nativeFinalize },
};


/**
 * Called by JNI_OnLoad to initialize the classes.
 */
int initGifImage(JNIEnv* pEnv) {
  // GifImage
  sClazzGifImage = findClassOrThrow(pEnv, kGifImageClassPathName);
  if (sClazzGifImage == NULL) {
    return JNI_ERR;
  }

  // GifImage.mNativeContext
  sGifImageFieldNativeContext = getFieldIdOrThrow(pEnv, sClazzGifImage, "mNativeContext", "J");
  if (!sGifImageFieldNativeContext) {
    return JNI_ERR;
  }

  // GifImage.<init>
  sGifImageConstructor = getMethodIdOrThrow(pEnv, sClazzGifImage, "<init>", "(J)V");
  if (!sGifImageConstructor) {
    return JNI_ERR;
  }

  int result = pEnv->RegisterNatives(
      sClazzGifImage,
      sGifImageMethods,
      std::extent<decltype(sGifImageMethods)>::value);
  if (result != JNI_OK) {
    return result;
  }

  // GifFrame
  sClazzGifFrame = findClassOrThrow(pEnv, kGifFrameClassPathName);
  if (sClazzGifFrame == NULL) {
    return JNI_ERR;
  }

  // GifFrame.mNativeContext
  sGifFrameFieldNativeContext = getFieldIdOrThrow(pEnv, sClazzGifFrame, "mNativeContext", "J");
  if (!sGifFrameFieldNativeContext) {
    return JNI_ERR;
  }

  // GifFrame.<init>
  sGifFrameConstructor = getMethodIdOrThrow(pEnv, sClazzGifFrame, "<init>", "(J)V");
  if (!sGifFrameConstructor) {
    return JNI_ERR;
  }

  result = pEnv->RegisterNatives(
      sClazzGifFrame,
      sGifFrameMethods,
      std::extent<decltype(sGifFrameMethods)>::value);
  if (result != JNI_OK) {
    return result;
  }

  sDefaultColorMap = genDefColorMap();

  return JNI_OK;
}
