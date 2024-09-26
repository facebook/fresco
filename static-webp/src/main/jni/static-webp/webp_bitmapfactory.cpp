/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#include <android/bitmap.h>
#include <jni.h>

#include "exceptions.h"
#include "java_globals.h"
#include "logging.h"
#include "webp.h"
#include "webp/decode.h"
#include "webp/demux.h"

#include <memory>
#include <vector>

#include <sys/types.h>
#include <unistd.h>

#define RETURN_NULL_IF_EXCEPTION(env) \
  if (env->ExceptionOccurred()) {     \
    return {};                        \
  }

#define RETURN_IF_ERROR                    \
  if (env->ExceptionCheck() == JNI_TRUE) { \
    return JNI_ERR;                        \
  }

#define CREATE_AS_GLOBAL(destClass, className)      \
  destClass = env->FindClass(className);            \
  destClass = (jclass)env->NewGlobalRef(destClass); \
  RETURN_IF_ERROR

jclass jRuntimeExceptionWebp_class;

namespace {

static constexpr const char* kHandlerClassName =
    "com/facebook/webpsupport/WebpBitmapFactoryImpl";

jclass webpBitmapFactoryClass;
jclass fileDescriptorClass;

std::vector<uint8_t>
readStreamFully(JNIEnv* env, jobject is, jbyteArray inTempStorage) {
  // read start
  std::vector<uint8_t> read_buffer;

  jclass inputStreamJClass = env->FindClass("java/io/InputStream");
  jmethodID readMethodId = env->GetMethodID(inputStreamJClass, "read", "([B)I");

  while (true) {
    const int chunk_size = env->CallIntMethod(is, readMethodId, inTempStorage);

    if (chunk_size < 0) {
      return read_buffer;
    }

    if (chunk_size > 0) {
      jbyte* data = env->GetByteArrayElements(inTempStorage, nullptr);
      RETURN_NULL_IF_EXCEPTION(env);

      read_buffer.insert(read_buffer.end(), data, data + chunk_size);
      env->ReleaseByteArrayElements(inTempStorage, data, JNI_ABORT);
      RETURN_NULL_IF_EXCEPTION(env);
    }
  }
}

jboolean setOutDimensions(
    JNIEnv* env,
    jobject bitmapOptions,
    int image_width,
    int image_height) {
  static jmethodID decodeBoundsMethodID = env->GetStaticMethodID(
      webpBitmapFactoryClass,
      "setOutDimensions",
      "(Landroid/graphics/BitmapFactory$Options;II)Z");
  jboolean hadDecodeBounds = env->CallStaticBooleanMethod(
      webpBitmapFactoryClass,
      decodeBoundsMethodID,
      bitmapOptions,
      image_width,
      image_height);
  return hadDecodeBounds;
}

void setBitmapSize(
    JNIEnv* env,
    jobject bitmapOptions,
    int image_width,
    int image_height) {
  static jmethodID setBitmapSizeMethodID = env->GetStaticMethodID(
      webpBitmapFactoryClass,
      "setBitmapSize",
      "(Landroid/graphics/BitmapFactory$Options;II)V");
  env->CallStaticVoidMethod(
      webpBitmapFactoryClass,
      setBitmapSizeMethodID,
      bitmapOptions,
      image_width,
      image_height);
}

jobject createBitmap(
    JNIEnv* env,
    int image_width,
    int image_height,
    jobject bitmapOptions) {
  static jmethodID createBitmapFunction = env->GetStaticMethodID(
      webpBitmapFactoryClass,
      "createBitmap",
      "(IILandroid/graphics/BitmapFactory$Options;)Landroid/graphics/Bitmap;");
  jobject bitmap = env->CallStaticObjectMethod(
      webpBitmapFactoryClass,
      createBitmapFunction,
      image_width,
      image_height,
      bitmapOptions);
  return bitmap;
}

jobject doDecode(
    JNIEnv* env,
    uint8_t* encoded_image,
    unsigned encoded_image_length,
    jobject bitmapOptions,
    jfloat scale) {
  // Options manipulation is taken from
  // https://github.com/android/platform_frameworks_base/blob/master/core/jni/android/graphics/BitmapFactory.cpp
  int image_width = 0;
  int image_height = 0;

  jobject bitmap = nullptr;

  WebPGetInfo(encoded_image, encoded_image_length, &image_width, &image_height);

  WebPDecoderConfig config;
  WebPInitDecoderConfig(&config);

  if ((bitmapOptions != nullptr) &&
      (setOutDimensions(env, bitmapOptions, image_width, image_height))) {
    return {};
  }

  if (scale != 1.0f) {
    image_width = int(image_width * scale + 0.5f);
    image_height = int(image_height * scale + 0.5f);
    config.options.use_scaling = 1;
    config.options.scaled_width = image_width;
    config.options.scaled_height = image_height;
  }

  bitmap = createBitmap(env, image_width, image_height, bitmapOptions);
  RETURN_NULL_IF_EXCEPTION(env);

  void* raw_pixels = nullptr;

  int rc = AndroidBitmap_lockPixels(env, bitmap, (void**)&raw_pixels);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    env->ThrowNew(jRuntimeExceptionWebp_class, "Decode error locking pixels");
    return JNI_FALSE;
  }

  config.output.colorspace = MODE_rgbA;
  config.output.u.RGBA.rgba = (uint8_t*)raw_pixels;
  config.output.u.RGBA.stride = image_width * 4;
  config.output.u.RGBA.size = image_width * image_height * 4;
  config.output.is_external_memory = 1;

  WebPDecode(encoded_image, encoded_image_length, &config);

  rc = AndroidBitmap_unlockPixels(env, bitmap);
  if (rc != ANDROID_BITMAP_RESULT_SUCCESS) {
    env->ThrowNew(jRuntimeExceptionWebp_class, "Decode error unlocking pixels");
    return {};
  }

  if (bitmapOptions != nullptr) {
    setBitmapSize(env, bitmapOptions, image_width, image_height);
  }

  return bitmap;
}

jobject nativeDecodeStream(
    JNIEnv* env,
    jclass clazz,
    jobject is,
    jobject bitmapOptions,
    jfloat scale,
    jbyteArray inTempStorage) {
  auto encoded_image = readStreamFully(env, is, inTempStorage);
  if (!encoded_image.empty()) {
    return doDecode(
        env, encoded_image.data(), encoded_image.size(), bitmapOptions, scale);
  }
  return {};
}

jobject nativeDecodeByteArray(
    JNIEnv* env,
    jclass clazz,
    jbyteArray array,
    jint offset,
    jint length,
    jobject bitmapOptions,
    jfloat scale,
    jbyteArray inTempStorage) {
  // get image into decoded heap
  jbyte* data = env->GetByteArrayElements(array, nullptr);
  if (env->ExceptionCheck() == JNI_TRUE) {
    env->ReleaseByteArrayElements(inTempStorage, data, JNI_ABORT);
    RETURN_NULL_IF_EXCEPTION(env);
  }
  if (data == nullptr || offset + length > env->GetArrayLength(array)) {
    env->ReleaseByteArrayElements(array, data, JNI_ABORT);
    RETURN_NULL_IF_EXCEPTION(env);
  }
  jobject bitmap = doDecode(
      env,
      reinterpret_cast<uint8_t*>(data) + offset,
      length,
      bitmapOptions,
      scale);
  env->ReleaseByteArrayElements(array, data, JNI_ABORT);
  RETURN_NULL_IF_EXCEPTION(env);

  return bitmap;
}

jlong nativeSeek(
    JNIEnv* env,
    jclass clazz,
    jobject fileDescriptor,
    jlong offset,
    jboolean absolute) {
  jint descriptor = -1;
  if (fileDescriptorClass != nullptr) {
    jfieldID descriptorFieldID =
        env->GetFieldID(fileDescriptorClass, "descriptor", "I");
    if (descriptorFieldID != nullptr && fileDescriptor != nullptr) {
      descriptor = env->GetIntField(fileDescriptor, descriptorFieldID);
      if (descriptor != -1) {
        return lseek64(descriptor, offset, absolute ? SEEK_SET : SEEK_CUR);
      }
    }
  }
  return descriptor;
}

JNINativeMethod methods[] = {
    {"nativeDecodeStream",
     "(Ljava/io/InputStream;Landroid/graphics/BitmapFactory$Options;F[B)Landroid/graphics/Bitmap;",
     (void*)&nativeDecodeStream},
    {"nativeDecodeByteArray",
     "([BIILandroid/graphics/BitmapFactory$Options;F[B)Landroid/graphics/Bitmap;",
     (void*)&nativeDecodeByteArray},
    {"nativeSeek", "(Ljava/io/FileDescriptor;JZ)J", (void*)&nativeSeek},
};

int registerNativeMethods(
    JNIEnv* env,
    const char* className,
    JNINativeMethod* gMethods,
    int numMethods) {
  jclass clazz;
  clazz = env->FindClass(className);
  if (clazz == NULL) {
    return JNI_FALSE;
  }
  if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

int registerNatives(JNIEnv* env) {
  if (!registerNativeMethods(
          env,
          kHandlerClassName,
          methods,
          sizeof(methods) / sizeof(methods[0]))) {
    return JNI_FALSE;
  }
  return JNI_TRUE;
}

typedef union {
  JNIEnv* env;
  void* venv;
} UnionJNIEnvToVoid;

} // namespace

__attribute__((visibility("default"))) JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv* env;

  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return -1;
  }

  // find java classes
  CREATE_AS_GLOBAL(jRuntimeExceptionWebp_class, "java/lang/RuntimeException");
  CREATE_AS_GLOBAL(fileDescriptorClass, "java/io/FileDescriptor");

  UnionJNIEnvToVoid uenv;
  uenv.venv = NULL;

  if (vm->GetEnv(&uenv.venv, JNI_VERSION_1_6) != JNI_OK) {
    return -1;
  }
  env = uenv.env;

  if (registerNatives(env) != JNI_TRUE) {
    return -1;
  }

  // We do this only if a class in animated-webp is present in the classpath
  jclass animatedWebpClass =
      env->FindClass("com/facebook/animated/webp/WebPImage");
  if (animatedWebpClass) {
    if (initWebPImage(env) != JNI_OK) {
      return -1;
    }
  } else {
    jboolean flag = env->ExceptionCheck();
    if (flag) {
      env->ExceptionClear();
    }
  }
  return JNI_VERSION_1_6;
}
