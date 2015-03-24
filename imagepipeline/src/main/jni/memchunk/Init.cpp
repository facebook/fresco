#include <jni.h>

#include "JavaClasses.h"
#include "Logging.h"
#include "NativeMemoryChunk.h"

jclass jRuntimeException_class;

/**
 * Executed when libmemchunk.so is loaded.
 *
 * <p> Responsibilites:
 * - looks up and stores global references to Java classes used by native code
 * - registers native methods of NativeMemoryChunk
 *
 * <p> In case of method registration failure a RuntimeException is thrown.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
  JNIEnv* env;

  if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
    return -1;
  }

  // find java classes
  jclass runtimeException = env->FindClass("java/lang/RuntimeException");
  if (runtimeException == nullptr) {
    LOGE("could not find RuntimeException class");
    return -1;
  }
  jRuntimeException_class =
    reinterpret_cast<jclass>(env->NewGlobalRef(runtimeException));

  // register native methods
  bool result = registerNativeMemoryChunkMethods(env);
  if (!result) {
    if (!env->ExceptionCheck()) {
      env->ThrowNew(
        jRuntimeException_class,
        "could not register native methods");
    }
    return -1;
  }

  return JNI_VERSION_1_6;
}
