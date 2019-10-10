/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#ifndef _BITMAPS_H_
#define _BITMAPS_H_

#ifdef __cplusplus
extern "C" {
#endif
jint registerBitmapsMethods(JNIEnv* env);
jint registerDalvikDecoderMethods(JNIEnv* env);
#ifdef __cplusplus
}
#endif

#endif /* _BITMAPS_H_ */
