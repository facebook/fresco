#include <cstring>
#include <type_traits>

#include "JavaClasses.h"
#include "Logging.h"
#include "NativeMemoryChunk.h"

static jlong NativeMemoryChunk_nativeAllocate(
    JNIEnv* env,
    jclass clzz,
    jint size) {
  jbyte* const pointer = new jbyte[size];
  if (pointer == nullptr) {
    env->ThrowNew(
        jRuntimeException_class,
        "could not allocate memory");
    return 0;
  }
  return (jlong) pointer;
}

static void NativeMemoryChunk_nativeFree(
    JNIEnv* env,
    jclass clzz,
    jlong lpointer) {
  jbyte* const pointer = (jbyte*) lpointer;
  delete[](pointer);
}

static void NativeMemoryChunk_nativeCopyToByteArray(
    JNIEnv* env,
    jclass clzz,
    jlong lpointer,
    jbyteArray byteArray,
    jint offset,
    jint count) {
  jbyte* const pointer = (jbyte*) lpointer;
  env->SetByteArrayRegion(byteArray, offset, count, pointer);
}

static void NativeMemoryChunk_nativeCopyFromByteArray(
    JNIEnv* env,
    jclass clzz,
    jlong lpointer,
    jbyteArray byteArray,
    jint offset,
    jint count) {
  jbyte* const pointer = (jbyte*) lpointer;
  env->GetByteArrayRegion(byteArray, offset, count, pointer);
}

static void NativeMemoryChunk_nativeMemcpy(
    JNIEnv* env,
    jclass clzz,
    jlong dst,
    jlong src,
    jint count) {
  jbyte* const dstPtr = (jbyte*) dst;
  jbyte* const srcPtr = (jbyte*) src;
  memcpy(dstPtr, srcPtr, count);
}

static jbyte NativeMemoryChunk_nativeReadByte(
    JNIEnv* env,
    jclass clzz,
    jlong lpointer){
  jbyte* const srcPtr = (jbyte*) lpointer;
  return *srcPtr;
}

static JNINativeMethod gNativeMemoryChunkMethods[] = {
  { "nativeAllocate", "(I)J", (void*) NativeMemoryChunk_nativeAllocate },
  { "nativeFree", "(J)V", (void*) NativeMemoryChunk_nativeFree },
  { "nativeCopyToByteArray", "(J[BII)V",
    (void*) NativeMemoryChunk_nativeCopyToByteArray },
  { "nativeCopyFromByteArray", "(J[BII)V",
    (void*) NativeMemoryChunk_nativeCopyFromByteArray },
  { "nativeMemcpy", "(JJI)V", (void*) NativeMemoryChunk_nativeMemcpy },
  { "nativeReadByte", "(J)B", (void*) NativeMemoryChunk_nativeReadByte },
};

bool registerNativeMemoryChunkMethods(JNIEnv* env) {
  auto chunkClass = env->FindClass("com/facebook/imagepipeline/memory/NativeMemoryChunk");
  if (chunkClass == nullptr) {
    LOGE("could not find NativeMemoryChunk class");
    return false;
  }

  auto result = env->RegisterNatives(
    chunkClass,
    gNativeMemoryChunkMethods,
    std::extent<decltype(gNativeMemoryChunkMethods)>::value);

  if (result != 0) {
    LOGE("could not register NativeMemoryChunk methods");
    return false;
  }

  return true;
}
