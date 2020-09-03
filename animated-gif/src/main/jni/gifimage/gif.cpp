/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#define LOG_TAG "GifImage"

#include <jni.h>
#include <algorithm>
#include <cstdio>
#include <memory>
#include <mutex>
#include <unistd.h>
#include <utility>
#include <vector>
#include <android/bitmap.h>
#include <android/log.h>

#include "gif_lib.h"
#include "jni_helpers.h"
#include "locks.h"

using namespace facebook;

#define APPLICATION_EXT_NETSCAPE "NETSCAPE2.0"
#define APPLICATION_EXT_NETSCAPE_LEN sizeof(APPLICATION_EXT_NETSCAPE) - 1

#define EXTRA_LOGGING false

#define LOOP_COUNT_MISSING -1;

static void DGifCloseFile2(GifFileType* pGifFile) {
  int errorCode;
  DGifCloseFile(pGifFile, &errorCode);
}

class DataWrapper {
public:
  DataWrapper() {}

  virtual ~DataWrapper() {}

  virtual size_t read(GifByteType* dest, size_t size) = 0;

  virtual size_t getBufferSize() = 0;

  virtual size_t getPosition() = 0;

  virtual bool setPosition(size_t position) = 0;
};

class BytesDataWrapper : public DataWrapper {
public:
  BytesDataWrapper(std::vector<uint8_t>&& pBuffer) : DataWrapper(),
    m_pBuffer(std::move(pBuffer)), m_position(0) {
    m_length = m_pBuffer.size();
  }

  size_t read(GifByteType* dest, size_t size) override {
    size_t readSize = m_position + size > m_length ? m_length - m_position : size;
    memcpy(dest, m_pBuffer.data() + m_position, readSize);
    m_position += readSize;
    return readSize;
  }

  size_t getBufferSize() override {
    return m_length;
  }

  size_t getPosition() override {
    return m_position;
  }

  bool setPosition(size_t position) override {
    if (position < m_length) {
      m_position = position;
      return true;
    } else {
      return false;
    }
  }

private:
  std::vector<uint8_t> m_pBuffer;
  size_t m_position;
  size_t m_length;
};

class FileDataWrapper : public DataWrapper {
public:
  static FileDataWrapper* create(JNIEnv* pEnv, int fd) {
    fd = dup(fd);
    FILE* file = fdopen(fd, "rb");
    if (file == nullptr) {
      throwIllegalStateException(pEnv, "Unable to open file: %s", strerror(errno));
      return nullptr;
    }
    if (fseek(file, 0, SEEK_END) != 0) {
      throwIllegalStateException(pEnv, "Unable to seek to end of file: %s", strerror(errno));
      return nullptr;
    }
    long size = ftell(file);
    if (size < 0) {
      throwIllegalStateException(pEnv, "Unable to get file size: %s", strerror(errno));
      return nullptr;
    }
    if (fseek(file, 0, SEEK_SET) != 0) {
      throwIllegalStateException(pEnv, "Unable to seek to beginning of file: %s", strerror(errno));
      return nullptr;
    }
    return new FileDataWrapper(file, size);
  }

  FileDataWrapper(FILE* file, size_t length) : DataWrapper(), m_file(file), m_length(length) {}

  ~FileDataWrapper() override {
    fclose(m_file);
  }

  size_t read(GifByteType* dest, size_t size) override {
    return fread(dest, 1, size, m_file);
  }

  size_t getBufferSize() override {
    return m_length;
  }

  size_t getPosition() override {
    long position = ftell(m_file);
    return position >= 0 ? position : 0;
  }

  bool setPosition(size_t position) override {
    return fseek(m_file, position, SEEK_SET) == 0;
  }

private:
  FILE* m_file;
  size_t m_length;
};

class GifWrapper {

public:
  GifWrapper(
      std::unique_ptr<GifFileType, decltype(&DGifCloseFile2)>&& pGifFile,
      std::shared_ptr<DataWrapper>& pData) :
          m_spGifFile(std::move(pGifFile)),
          m_spData(pData),
          m_rasterBits(m_spGifFile->SWidth * m_spGifFile->SHeight) {
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

  size_t getFrameSize() {
    return m_vectorFrameByteOffsets.size();
  }

  int getLoopCount() {
    return m_loopCount;
  }

  uint8_t* getRasterBits() {
    return m_rasterBits.data();
  }

  size_t getRasterBitsCapacity() {
    return m_rasterBits.capacity();
  }

  void resizeRasterBuffer(size_t bufferSize) {
    m_rasterBits.resize(bufferSize);
  }

  std::mutex& getRasterMutex() {
    return m_rasterMutex;
  }

  void setLoopCount(int pLoopCount) {
    m_loopCount = pLoopCount;
  }

  bool isAnimated() {
    return m_animated;
  }

  void setAnimated(bool animated) {
    m_animated = animated;
  }

  RWLock* getSavedImagesRWLock() {
    return &m_savedImagesRWLock;
  }

private:
  int m_loopCount = LOOP_COUNT_MISSING;
  bool m_animated = false;
  std::unique_ptr<GifFileType, decltype(&DGifCloseFile2)> m_spGifFile;
  std::shared_ptr<DataWrapper> m_spData;
  std::vector<int> m_vectorFrameByteOffsets;
  std::vector<uint8_t> m_rasterBits;
  std::mutex m_rasterMutex;
  mutable RWLock m_savedImagesRWLock;
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
 * giflib takes a callback function for reading from the file.
 */
static int directByteBufferReadFun(
   GifFileType* gifFileType,
   GifByteType* bytes,
   int size) {
  DataWrapper* pData = (DataWrapper*) gifFileType->UserData;
  if (size > 0) {
    return pData->read(bytes, size);
  }
  return 0;
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
    "com/facebook/animated/gif/GifImage";
static const char* const kGifFrameClassPathName =
    "com/facebook/animated/gif/GifFrame";

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
  int resultCode = GIF_ERROR;
  // If a GIF has multiple graphic control extension blocks, we use the last one
  for (int i = 0; i < pSavedImage->ExtensionBlockCount; i++) {
    ExtensionBlock* pExtensionBlock = &pSavedImage->ExtensionBlocks[i];
    if (pExtensionBlock->Function == GRAPHICS_EXT_FUNC_CODE) {
      resultCode = DGifExtensionToGCB(pExtensionBlock->ByteCount, pExtensionBlock->Bytes, pGcp);
    }
  }
  return resultCode == GIF_OK;
}

/**
 * Reads a single frame by reading data using giflib. The method expects the data source
 * referenced by pGifFile to point to the first byte of the encoded frame data. When the method
 * returns, the data source will point to the byte just past the encoded frame data. Unlike standard
 * decoding with giflib, the raster data is written to the passed-in buffer instead of being
 * written to the SavedImage structure. This is the key to how we avoid caching all the decoded
 * frame pixels in memory.
 *
 * @param pGifWrapper the gif wrapper containing the giflib struct and additional data
 * @param decodeFrame if set to true, next frame will be decoded to pGifWrapper bits buffer,
       otherwise it will only decode frame data and skip it
 * @param addToSavedImages if set to true, will add an additional SavedImage to
 *     pGifFile->SavedImages
 * @param maxDimension Maximum allowed dimension of each decoded frame
 * @return a gif error code
 */
int readSingleFrame(
    GifWrapper* pGifWrapper,
    bool decodeFramePixels,
    bool addToSavedImages,
    int maxDimension) {

  GifFileType *pGifFile = pGifWrapper->get();

  int imageCount = pGifFile->ImageCount;
  int imageDescResult = GIF_ERROR;
  {
    WriterLock wlock_{pGifWrapper->getSavedImagesRWLock()};
    imageDescResult = DGifGetImageDesc(pGifFile);
  }

  // DGifGetImageDesc may have changed the count, temporarily restoring until we know whether
  // the frame was read successfully.
  pGifFile->ImageCount = imageCount;

  if (imageDescResult == GIF_ERROR) {
    return GIF_ERROR;
  }

  ReaderLock rlock_{pGifWrapper->getSavedImagesRWLock()};
  SavedImage* pSavedImage = &pGifFile->SavedImages[imageCount];

  // Check size of image. Note: Frames with 0 width or height should be allowed.
  if (pSavedImage->ImageDesc.Width < 0 || pSavedImage->ImageDesc.Height < 0 ||
      pSavedImage->ImageDesc.Width > maxDimension || pSavedImage->ImageDesc.Height > maxDimension) {
    return GIF_ERROR;
  }

  // Check for image size overflow.
  if (pSavedImage->ImageDesc.Width > 0 &&
      pSavedImage->ImageDesc.Height > 0 &&
      pSavedImage->ImageDesc.Width > (INT_MAX / pSavedImage->ImageDesc.Height)) {
    return GIF_ERROR;
  }

  if (decodeFramePixels) {
    // Reserve larger raster bits buffer if needed
    size_t imageSize = pSavedImage->ImageDesc.Width * pSavedImage->ImageDesc.Height;
    pGifWrapper->resizeRasterBuffer(imageSize);

    // Decode frame image and save it to temporary raster bits buffer
    uint8_t* pRasterBits = pGifWrapper->getRasterBits();
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

  if (addToSavedImages) {
    // giflib wasn't designed to work with decoding arbitrary frames on the fly. By default, it
    // keeps adding more images to the SavedImages array, and we reset the value after calling
    // DGifGetImageDesc. Now, as the result of decoding is known to be successful, we can increment
    // the value to represent correct number of images.
    pGifFile->ImageCount = imageCount + 1;
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
 * Tries to parse known application extensions of a given SavedImage and adds the information
 * to the GifWrapper accordingly. Currently, this method only parses the Netscape 2.0 looping
 * extension which can indicate how often a GIF animation shall be played.
 *
 * @param pSavedImage saved image that might contain several ExtensionBlocks
 * @param pGifWrapper gif wrapper containing the giflib struct and additional data
 */
void parseApplicationExtensions(SavedImage* pSavedImage, GifWrapper* pGifWrapper) {
  const int extensionCount = pSavedImage->ExtensionBlockCount;
  for (int j = 0; j < extensionCount; j++) {
    const ExtensionBlock* extensionBlock = &pSavedImage->ExtensionBlocks[j];

    if (extensionBlock->Function != APPLICATION_EXT_FUNC_CODE) {
      continue;
    }

    // Check for Netscape 2.0 looping block
    if (extensionBlock->ByteCount == APPLICATION_EXT_NETSCAPE_LEN &&
        strncmp(
          APPLICATION_EXT_NETSCAPE,
          (const char*) extensionBlock->Bytes,
          APPLICATION_EXT_NETSCAPE_LEN) == 0) {

      // The data sub-block has been added as the following extension block
      ExtensionBlock* subBlock = NULL;
      if (j + 1 < extensionCount) {
        subBlock = &pSavedImage->ExtensionBlocks[j + 1];
      }

      if (subBlock != NULL &&
          subBlock->Function == CONTINUE_EXT_FUNC_CODE &&
          subBlock->ByteCount == 3) {
        // The loop count is stored little endian
        const int loopCount = subBlock->Bytes[1] | subBlock->Bytes[2] << 8;
        pGifWrapper->setLoopCount(loopCount);

        // The looping extension is the only block that we are interested in
        break;
      }
    }
  }
}

/**
 * A heavily modified version of giflib's DGifSlurp. This uses some hacks to avoid caching the
 * decoded pixel data for each frame in memory. Like DGifSlurp, GifFileType will contain the
 * results of slurping the GIF but there will be no frame pixel data cached in
 * SavedImage.RasterBits.
 *
 * @param pGifWrapper the gif wrapper containing the giflib struct and additional data
 * @param maxDimension Maximum allowed dimension of each frame
 * @param forceStatic whether GIF will be loaded as static image
 * @return a gif error code
 */
int modifiedDGifSlurp(GifWrapper* pGifWrapper, int maxDimension, bool forceStatic) {
  GifFileType* pGifFile = pGifWrapper->get();
  GifRecordType recordType;

  pGifFile->ExtensionBlocks = NULL;
  pGifFile->ExtensionBlockCount = 0;
  bool isStop = false;
  do {
    if (DGifGetRecordType(pGifFile, &recordType) == GIF_ERROR) {
      break;
    }

    switch (recordType) {
      case IMAGE_DESC_RECORD_TYPE:
        // Set the flag whether gif is animated, but give up slurping after the first frame,
        // when static image is requested.
        if (pGifFile->ImageCount >= 1) {
          pGifWrapper->setAnimated(true);
          if (forceStatic) {
            isStop = true;
            break;
          }
        }

        // We save the byte offset where each frame begins. This allows us to avoid storing
        // the pixel data for each frame and instead decode it on the fly.
        pGifWrapper->addFrameByteOffset(pGifWrapper->getData()->getPosition());

        if (readSingleFrame(
              pGifWrapper,
              false, // Don't decode frame pixels
              true,  // Add to saved images
              maxDimension // Max dimension
              ) == GIF_ERROR) {
          isStop = true;
        }
        break;

      case EXTENSION_RECORD_TYPE:
        if (decodeExtension(pGifFile) == GIF_ERROR) {
          isStop = true;
        }
        break;

      case TERMINATE_RECORD_TYPE:
        isStop = true;
        break;

       default:    // Should be trapped by DGifGetRecordType.
        break;
      }
  } while (!isStop);
  isStop = false;

  // parse application extensions
  const int imageCount = pGifFile->ImageCount;
  ReaderLock rlock_{pGifWrapper->getSavedImagesRWLock()};
  for (int i = 0; i < imageCount; i++) {
    parseApplicationExtensions(&pGifFile->SavedImages[i], pGifWrapper);
  }

  return pGifWrapper->getFrameSize() > 0 ? GIF_OK : GIF_ERROR;
}

/**
 * Creates a new GifImage from the specified data.
 *
 * @param spDataWrapper the wrapper providing bytes
 * @param maxDimension Maximum allowed dimension of canvas and each decoded frame
 * @param forceStatic Whether GIF should be decoded as static image
 * @return a newly allocated GifImage
 */
jobject createFromDataWrapper(JNIEnv* pEnv, std::shared_ptr<DataWrapper> spDataWrapper, int maxDimension, bool forceStatic) {
  std::unique_ptr<GifImageNativeContext> spNativeContext(new GifImageNativeContext());
  if (!spNativeContext) {
    throwOutOfMemoryError(pEnv, "Unable to allocate native context");
    return 0;
  }

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
  if (wxh < 1 || wxh > SIZE_MAX || width > maxDimension || height > maxDimension) {
    throwIllegalStateException(pEnv, "Invalid dimensions");
    return nullptr;
  }

  // Create the GifWrapper
  spNativeContext->spGifWrapper = std::shared_ptr<GifWrapper>(
    new GifWrapper(std::move(spGifFileIn), spDataWrapper));

  GifFileType* pGifFile = spNativeContext->spGifWrapper->get();

  spNativeContext->pixelWidth = width;
  spNativeContext->pixelHeight = height;

  int error = modifiedDGifSlurp(spNativeContext->spGifWrapper.get(), maxDimension, forceStatic);
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
  ReaderLock rlock_{spNativeContext->spGifWrapper->getSavedImagesRWLock()};
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

  // Cache loop count
  spNativeContext->loopCount = spNativeContext->spGifWrapper->getLoopCount();

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
 * Creates a new GifImage from the specified buffer.
 *
 * @param vBuffer the vector containing the bytes
 * @return a newly allocated GifImage
 */
jobject GifImage_nativeCreateFromByteVector(JNIEnv* pEnv, std::vector<uint8_t>& vBuffer, int maxDimension, bool forceStatic) {
  // Create the DataWrapper
  std::shared_ptr<DataWrapper> spDataWrapper =
      std::shared_ptr<DataWrapper>(new BytesDataWrapper(std::move(vBuffer)));
  return createFromDataWrapper(pEnv, spDataWrapper, maxDimension, forceStatic);
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
 * @param maxDimension Maximum allowed dimension of canvas and each decoded frame
 * @param forceStatic Whether GIF should be decoded as static image
 * @return a newly allocated GifImage
 */
jobject GifImage_nativeCreateFromDirectByteBuffer(JNIEnv* pEnv, jclass clazz, jobject byteBuffer, jint maxDimension, jboolean forceStatic) {
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
  return GifImage_nativeCreateFromByteVector(pEnv, vBuffer, maxDimension, forceStatic);
}

/**
 * Creates a new GifImage from the specified native pointer. The data is copied into memory
 managed by GifImage.
 *
 * @param nativePtr the native memory pointer
 * @param sizeInBytes size in bytes of the buffer
* @param maxDimension Maximum allowed dimension of canvas and each decoded frame
 * @param forceStatic whether GIF will be loaded as static image
 * @return a newly allocated GifImage
 */
jobject GifImage_nativeCreateFromNativeMemory(
    JNIEnv* pEnv,
    jclass clazz,
    jlong nativePtr,
    jint sizeInBytes,
    jint maxDimension,
    jboolean forceStatic) {

  jbyte* const pointer = (jbyte*) nativePtr;
  std::vector<uint8_t> vBuffer(pointer, pointer + sizeInBytes);
  return GifImage_nativeCreateFromByteVector(pEnv, vBuffer, maxDimension, forceStatic);
}

/**
 * Creates a new GifImage from the specified byte buffer. The data from the byte buffer is copied
 * into native memory managed by GifImage.
 *
 * @param fileDescriptor File descriptor to open
 * @param maxDimension Maximum allowed dimension of canvas and each decoded frame
 * @param forceStatic Whether GIF should be decoded as static image
 * @return a newly allocated GifImage
 */
jobject GifImage_nativeCreateFromFileDescriptor(JNIEnv* pEnv, jclass clazz, jint fileDescriptor, jint maxDimension, jboolean forceStatic) {
  // Create the DataWrapper
  std::shared_ptr<FileDataWrapper> spDataWrapper =
      std::shared_ptr<FileDataWrapper>(FileDataWrapper::create(pEnv, fileDescriptor));
  if (pEnv->ExceptionCheck() || !spDataWrapper) {
    return 0;
  }
  return createFromDataWrapper(pEnv, spDataWrapper, maxDimension, forceStatic);
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

  ReaderLock rlock_{spNativeContext->spGifWrapper->getSavedImagesRWLock()};
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
  size += spNativeContext->spGifWrapper->getRasterBitsCapacity();
  return size;
}

/**
 * Gets information whether {@link GifImage} is animated (has more than 1 frame).
 * It will return `true`, even if animated file was opened as static image.
 *
 * @return whether {@link GifImage} is animated image
 */
jint GifImage_nativeIsAnimated(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifImageNativeContext(pEnv, thiz);
  if (!spNativeContext) {
    throwIllegalStateException(pEnv, "Already disposed");
    return 0;
  }

  return spNativeContext->spGifWrapper->isAnimated();
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
  if (pColorMap == NULL) {
      return TRANSPARENT;
  }
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
  if (!pGifWrapper->getData()->setPosition(byteOffset)) {
    // Unable to position to frame, ignore it
    return;
  }

  // Now we kick off the decoding process.
  int readRes = readSingleFrame(pGifWrapper,
                                true, // Decode frame pixels
                                false, // Don't add frame to saved images
                                INT_MAX // Don't limit the size, it was checked in modifiedDGifSlurp
                                );
  if (readRes != GIF_OK) {
    // Probably, broken canvas, and we can ignore it
    return;
  }

  // Get the right color table to use.
  ColorMapObject* pColorMap = spNativeContext->spGifWrapper->get()->SColorMap;
  ReaderLock rlock_{pGifWrapper->getSavedImagesRWLock()};
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

/**
 * Gets the color (as an int, as in Android) of the transparent pixel of this frame
 *
 * @return the color (as an int, as in Android) of the transparent pixel of this frame
 */
jint GifFrame_nativeGetTransparentPixelColor(JNIEnv* pEnv, jobject thiz) {
  auto spNativeContext = getGifFrameNativeContext(pEnv, thiz);
  auto pGifWrapper = spNativeContext->spGifWrapper;

  //
  // Get the right color table to use, then get index of transparent pixel into that table
  //
  int frameNum = spNativeContext->frameNum;
  ColorMapObject* pColorMap = pGifWrapper->get()->SColorMap;
  ReaderLock rlock_{pGifWrapper->getSavedImagesRWLock()};
  SavedImage* pSavedImage = &pGifWrapper->get()->SavedImages[frameNum];

  if (pSavedImage->ImageDesc.ColorMap != NULL) {
    // use local color table
    pColorMap = pSavedImage->ImageDesc.ColorMap;
    if (pColorMap->ColorCount != (1 << pColorMap->BitsPerPixel)) {
      pColorMap = sDefaultColorMap;
    }
  }

  int colorIndex = spNativeContext->transparentIndex;

  if (pColorMap != NULL  &&  colorIndex >= 0) {
    PixelType32 color = getColorFromTable(colorIndex, pColorMap);

    //
    // convert PixelType32 to Android-style int color value.
    // the c++ compiler will optimize these four lines of bit-shifting -- there is no need to
    // collapse them into a single confusing expression
    //
    int alphaShifted  = color.alpha   << 24;
    int redShifted    = color.red     << 16;
    int greenShifted  = color.green   <<  8;
    int blueShifted   = color.blue    <<  0;

    int iColor = alphaShifted | redShifted | greenShifted | blueShifted;

    return iColor;
  } else {
    return 0; // in android, 0 == Color.TRANSPARENT
  }
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
    "(Ljava/nio/ByteBuffer;IZ)Lcom/facebook/animated/gif/GifImage;",
    (void*)GifImage_nativeCreateFromDirectByteBuffer },
  { "nativeCreateFromNativeMemory",
    "(JIIZ)Lcom/facebook/animated/gif/GifImage;",
    (void*)GifImage_nativeCreateFromNativeMemory },
  { "nativeCreateFromFileDescriptor",
    "(IIZ)Lcom/facebook/animated/gif/GifImage;",
    (void*)GifImage_nativeCreateFromFileDescriptor },
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
  { "nativeGetLoopCount",
    "()I",
    (void*)GifImage_nativeGetLoopCount },
  { "nativeGetFrame",
    "(I)Lcom/facebook/animated/gif/GifFrame;",
    (void*)GifImage_nativeGetFrame },
  { "nativeGetSizeInBytes",
    "()I",
    (void*)GifImage_nativeGetSizeInBytes },
  { "nativeIsAnimated",
    "()Z",
    (void*)GifImage_nativeIsAnimated },
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
  { "nativeGetTransparentPixelColor",
    "()I",
    (void*)GifFrame_nativeGetTransparentPixelColor },
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
