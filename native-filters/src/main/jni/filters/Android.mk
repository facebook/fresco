# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := filters
LOCAL_SRC_FILES := \
	blur_filter.cpp \
	rounding_filter.cpp

LOCAL_CFLAGS += -Wall -Wextra -Werror -std=c++17
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_LDLIBS := -ljnigraphics
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
include $(BUILD_STATIC_LIBRARY)
