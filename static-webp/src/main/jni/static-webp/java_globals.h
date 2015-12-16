/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#ifndef _JAVACLASSES_H_
#define _JAVACLASSES_H_

#include <jni.h>

extern jmethodID midInputStreamRead;
extern jmethodID midInputStreamSkip;
extern jmethodID midOutputStreamWrite;
extern jmethodID midOutputStreamWriteWithBounds;

extern jclass jRuntimeException_class;

#endif /* _JAVACLASSES_H_ */
