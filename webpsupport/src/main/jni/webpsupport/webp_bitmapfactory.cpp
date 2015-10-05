/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#include <android/bitmap.h>
#include <jni.h>
#include <jni/ALog.h>
#include <webp/demux.h>
#include <webp/decode.h>

#include <memory>
#include <vector>

#include <jni/fbjni.h>
#include <jni/Environment.h>

#include <sys/types.h>
#include <unistd.h>

using namespace facebook::jni;

static constexpr const char* kWebpBitmapFactoryName = "com/facebook/webpsupport/WebpBitmapFactory";

static const unsigned int kDefaultBufferSize = 8 * 1024;

static alias_ref<jclass> getOptionClass() {
  static auto optionClass = findClassStatic("android/graphics/BitmapFactory$Options");
  return optionClass;
}

static alias_ref<jclass> getWebpBitmapFactoryClass() {
  static auto webpBitmapFactoryClass = findClassStatic(kWebpBitmapFactoryName);
  return webpBitmapFactoryClass;
}

jobject createBitmap(jint width, jint height) {
  static auto javaCreateBitmap = getWebpBitmapFactoryClass()->getStaticMethod<jobject(jint, jint)>("createBitmap", "(II)Landroid/graphics/Bitmap;");
  return javaCreateBitmap(getWebpBitmapFactoryClass(), width, height).release();
}

void setPaddingDefaultValues(alias_ref<jobject> padding) {
  static auto javaRect = findClassStatic("android/graphics/Rect");
  static auto top = javaRect->getField<jint>("top");
  static auto left = javaRect->getField<jint>("left");
  static auto bottom = javaRect->getField<jint>("bottom");
  static auto right = javaRect->getField<jint>("right");

  padding->setFieldValue(top, -1);
  padding->setFieldValue(left, -1);
  padding->setFieldValue(bottom, -1);
  padding->setFieldValue(right, -1);
}

jboolean getInJustDecodeBounds(alias_ref<jobject> options) {
  static auto inJustDecodeBounds = getOptionClass()->getField<jboolean>("inJustDecodeBounds");
  return options->getFieldValue(inJustDecodeBounds);
}

jobject getInBitmap(alias_ref<jobject> options) {
  static auto inBitmap = getOptionClass()->getField<jobject>("inBitmap", "Landroid/graphics/Bitmap;");
  return options->getFieldValue(inBitmap).release();
}

jint getInSampleSize(alias_ref<jobject> options) {
  static auto inSampleSize = getOptionClass()->getField<jint>("inSampleSize");
  return options->getFieldValue(inSampleSize);
}

jbyteArray getInTempStorage(alias_ref<jobject> options) {
  static auto inTempStorage = getOptionClass()->getField<jbyteArray>("inTempStorage");
  return options->getFieldValue(inTempStorage).release();
}

jboolean getInScaled(alias_ref<jobject> options) {
  static auto inScaled = getOptionClass()->getField<jboolean>("inScaled");
  return options->getFieldValue(inScaled);
}

jint getInDensity(alias_ref<jobject> options) {
  static auto inDensity = getOptionClass()->getField<jint>("inDensity");
  return options->getFieldValue(inDensity);
}

jint getInScreenDensity(alias_ref<jobject> options) {
  static auto inScreenDensity = getOptionClass()->getField<jint>("inScreenDensity");
  return options->getFieldValue(inScreenDensity);
}

jint getInTargetDensity(alias_ref<jobject> options) {
  static auto inTargetDensity = getOptionClass()->getField<jint>("inTargetDensity");
  return options->getFieldValue(inTargetDensity);
}

void setOutDimensions(alias_ref<jobject> options, jint outWidthValue, jint outHeightValue) {
  static auto outWidth = getOptionClass()->getField<jint>("outWidth");
  static auto outHeight = getOptionClass()->getField<jint>("outHeight");

  options->setFieldValue(outWidth, outWidthValue);
  options->setFieldValue(outHeight, outHeightValue);
}

jint getDescriptor(alias_ref<jobject> fileDescriptor) {
  static auto fileDescriptorClass = findClassStatic("java/io/FileDescriptor");
  static auto descriptorField = fileDescriptorClass->getField<jint>("descriptor");
  return fileDescriptor->getFieldValue(descriptorField);
}

bool shouldPremultiply(alias_ref<jobject> options) {
  static auto javaShouldPremultiply = getWebpBitmapFactoryClass()->getStaticMethod<jboolean(jobject)>("shouldPremultiply", "(Landroid/graphics/BitmapFactory$Options;)Z");
  return javaShouldPremultiply(getWebpBitmapFactoryClass(), options.get()) == JNI_TRUE;
}

std::vector<uint8_t> readStreamFully(JNIEnv* env, jobject is, alias_ref<jbyteArray> inTempStorage) {
  static auto inputStreamReadMethod = findClassStatic("java/io/InputStream")->getMethod<jint(jbyteArray)>("read");

  // read start
  std::vector<uint8_t> read_buffer;

  if (inTempStorage.get() == nullptr) {
    inTempStorage = make_byte_array(kDefaultBufferSize).release();
  }

  while (true) {
    const int chunk_size = inputStreamReadMethod(is, inTempStorage.get());

    if (chunk_size < 0) {
      return read_buffer;
    }

    if (chunk_size > 0) {
      auto data = inTempStorage->pin();

      read_buffer.insert(read_buffer.end(), data.get(), data.get() + chunk_size);
    }
  }
}

jobject doDecode(
    JNIEnv* env,
    uint8_t* encoded_image,
    unsigned encoded_image_length,
    jobject padding,
    jobject bitmapOptions) {

  // Options manipulation is taken from https://github.com/android/platform_frameworks_base/blob/master/core/jni/android/graphics/BitmapFactory.cpp
  int image_width = 0;
  int image_height = 0;
  float scale = 1.0f;

  alias_ref<jobject> bitmap = nullptr;

  WebPGetInfo(
      encoded_image,
      encoded_image_length,
      &image_width,
      &image_height);

  WebPDecoderConfig config;
  WebPInitDecoderConfig(&config);

  if (bitmapOptions != nullptr) {
    if (getInJustDecodeBounds(bitmapOptions)) {
      setOutDimensions(bitmapOptions, image_width, image_height);
      return {};
    }

    bitmap = getInBitmap(bitmapOptions);

    int sampleSize = getInSampleSize(bitmapOptions);

    if (sampleSize > 1) {
      scale = 1.0f / (float) sampleSize;
    }

    if (getInScaled(bitmapOptions)) {
      const int density = getInDensity(bitmapOptions);
      const int targetDensity = getInTargetDensity(bitmapOptions);
      const int screenDensity = getInScreenDensity(bitmapOptions);

      if (density != 0 && targetDensity != 0 && density != screenDensity) {
        scale = (float) targetDensity / (float) density;
      }
    }
  }

  if (scale != 1.0f) {
    image_width = int(image_width * scale + 0.5f);
    image_height = int(image_height  * scale + 0.5f);

    config.options.use_scaling = 1;
    config.options.scaled_width = image_width;
    config.options.scaled_height = image_height;
  }

  if (padding != nullptr) {
    setPaddingDefaultValues(padding);
  }

  if (bitmap.get() == nullptr) {
    bitmap = createBitmap(image_width, image_height);
  }

  void* raw_pixels = nullptr;

  AndroidBitmap_lockPixels(env, bitmap.get(), (void**) &raw_pixels);
  FACEBOOK_JNI_THROW_PENDING_EXCEPTION();

  config.output.colorspace = shouldPremultiply(bitmapOptions) ? MODE_rgbA : MODE_RGBA;
  config.output.u.RGBA.rgba = (uint8_t*) raw_pixels;
  config.output.u.RGBA.stride = image_width * 4;
  config.output.u.RGBA.size = image_width * image_height * 4;
  config.output.is_external_memory = 1;

  WebPDecode(encoded_image, encoded_image_length, &config);

  AndroidBitmap_unlockPixels(env, bitmap.get());
  FACEBOOK_JNI_THROW_PENDING_EXCEPTION();

  if (bitmapOptions != nullptr) {
    setOutDimensions(bitmapOptions, image_width, image_height);
  }

  return bitmap.get();
}

static jobject nativeDecodeStream(
    JNIEnv* env,
    jclass clazz,
    jobject is,
    jobject padding,
    jobject bitmapOptions) {

  jbyteArray inTempStorage = nullptr;
  if (bitmapOptions != nullptr) {
    inTempStorage = getInTempStorage(bitmapOptions);
  }

  auto encoded_image = readStreamFully(env, is, inTempStorage);
  return doDecode(env, encoded_image.data(), encoded_image.size(), padding, bitmapOptions);
}

static jobject nativeDecodeByteArray(
    JNIEnv* env,
    jclass clazz,
    jbyteArray array,
    jint offset,
    jint length,
    jobject bitmapOptions) {

  auto array_ref = alias_ref<jbyteArray>(array);
  if ((unsigned)offset + (unsigned)length > array_ref->size()) {
    return nullptr;
  }

  auto data = array_ref->pin();

  jobject bitmap = doDecode(env, reinterpret_cast<uint8_t*>(data.get()) + offset, length, NULL, bitmapOptions);

  return bitmap;
}

static jlong nativeSeek(JNIEnv* env, jobject, jobject fileDescriptor, jlong offset, jboolean absolute) {
  jint descriptor = getDescriptor(fileDescriptor);
  return lseek64(descriptor, offset, absolute ? SEEK_SET : SEEK_CUR);
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
  return initialize(vm, [] {
    registerNatives(kWebpBitmapFactoryName, {
      makeNativeMethod("nativeDecodeStream", "(Ljava/io/InputStream;Landroid/graphics/Rect;Landroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;", nativeDecodeStream),
      makeNativeMethod("nativeDecodeByteArray", "([BIILandroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;", nativeDecodeByteArray),
      makeNativeMethod("nativeSeek", "(Ljava/io/FileDescriptor;JZ)J", nativeSeek),
    });
  });
}
