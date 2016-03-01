# Copyright (c) 2015-present, Facebook, Inc.
# All rights reserved.
#
# This source code is licensed under the BSD-style license found in the
# LICENSE file in the root directory of this source tree. An additional grant
# of patent rights can be found in the PATENTS file in the same directory.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := bitmaps
LOCAL_SRC_FILES := \
	Bitmaps.c

LOCAL_CFLAGS += -Wall -Wextra -Werror -std=c11
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_LDLIBS := -ljnigraphics

include $(BUILD_SHARED_LIBRARY)
