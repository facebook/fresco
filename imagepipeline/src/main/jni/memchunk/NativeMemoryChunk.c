#include <string.h>
#include <stdint.h>

#include <jni.h>

#define ARRAY_SIZE(a) (sizeof(a) / sizeof((a)[0]))
#define JLONG_TO_PTR(j) ((void*) (intptr_t) (j))
#define PTR_TO_JLONG(p) ((jlong) (intptr_t) (p))

/**
 * Explicitly marks result of an expression as unused.
 *
 * We instruct the compiler to be very strict when it comes to code
 * like unused arguments that could indicate a bug. Because of that, handling
 * intentionally unused arguments requires tricks like casting to void. This
 * macro provides a readable name for this operation.
 */
#define UNUSED(p) ((void) (p))

static jclass jRuntimeException_class;

static jlong NativeMemoryChunk_nativeAllocate(
    JNIEnv* env,
    jclass clzz,
    jint size) {
  UNUSED(clzz);
  void* pointer = malloc(size);
  if (!pointer) {
    (*env)->ThrowNew(env, jRuntimeException_class, "could not allocate memory");
    return 0;
  }
  return PTR_TO_JLONG(pointer);
}

static void NativeMemoryChunk_nativeFree(
    JNIEnv* env,
    jclass clzz,
    jlong lpointer) {
  UNUSED(env);
  UNUSED(clzz);
  free(JLONG_TO_PTR(lpointer));
}

static void NativeMemoryChunk_nativeCopyToByteArray(
    JNIEnv* env,
    jclass clzz,
    jlong lpointer,
    jbyteArray byteArray,
    jint offset,
    jint count) {
  UNUSED(clzz);
  (*env)->SetByteArrayRegion(
      env,
      byteArray,
      offset,
      count,
      JLONG_TO_PTR(lpointer));
}

static void NativeMemoryChunk_nativeCopyFromByteArray(
    JNIEnv* env,
    jclass clzz,
    jlong lpointer,
    jbyteArray byteArray,
    jint offset,
    jint count) {
  UNUSED(clzz);
  (*env)->GetByteArrayRegion(
      env,
      byteArray,
      offset,
      count,
      JLONG_TO_PTR(lpointer));
}

static void NativeMemoryChunk_nativeMemcpy(
    JNIEnv* env,
    jclass clzz,
    jlong dst,
    jlong src,
    jint count) {
  UNUSED(env);
  UNUSED(clzz);
  memcpy(JLONG_TO_PTR(dst), JLONG_TO_PTR(src), count);
}

static jbyte NativeMemoryChunk_nativeReadByte(
    JNIEnv* env,
    jclass clzz,
    jlong lpointer){
  UNUSED(env);
  UNUSED(clzz);
  jbyte* jbyte_ptr = JLONG_TO_PTR(lpointer);
  return *jbyte_ptr;
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

/**
 * Executed when libmemchunk.so is loaded.
 *
 * Responsibilites:
 * - looks up and stores global references to Java classes used by native code
 * - registers native methods of NativeMemoryChunk
 */
__attribute__((visibility("default")))
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
  UNUSED(reserved);

  JNIEnv* env;
  if ((*vm)->GetEnv(vm, (void**) &env, JNI_VERSION_1_6) != JNI_OK) {
    return JNI_ERR;
  }

  jclass runtimeException = (*env)->FindClass(
      env,
      "java/lang/RuntimeException");
  if (!runtimeException) {
    return JNI_ERR;
  }
  jRuntimeException_class = (*env)->NewGlobalRef(env, runtimeException);

  jclass chunk_class = (*env)->FindClass(
      env,
      "com/facebook/imagepipeline/memory/NativeMemoryChunk");
  if (!chunk_class) {
    return JNI_ERR;
  }

  jint result = (*env)->RegisterNatives(
      env,
      chunk_class,
      gNativeMemoryChunkMethods,
      ARRAY_SIZE(gNativeMemoryChunkMethods));
  if (result != JNI_OK) {
    return JNI_ERR;
  }

  return JNI_VERSION_1_6;
}
