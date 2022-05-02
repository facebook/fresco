/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#ifndef _JAVACLASSES_H_
#define _JAVACLASSES_H_

#include <jni.h>

extern jmethodID midInputStreamRead;
extern jmethodID midInputStreamSkip;
extern jmethodID midOutputStreamWrite;
extern jmethodID midOutputStreamWriteWithBounds;

extern jclass jRuntimeExceptionclass;

#endif /* _JAVACLASSES_H_ */
