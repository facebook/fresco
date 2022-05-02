# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := gifimage

LOCAL_SRC_FILES := \
  jni.cpp \
  gif.cpp \
  jni_helpers.cpp \

CXX11_FLAGS := -std=c++11
LOCAL_CFLAGS += $(CXX11_FLAGS)
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += $(FRESCO_CPP_CFLAGS)
LOCAL_EXPORT_CPPFLAGS := $(CXX11_FLAGS)
LOCAL_LDLIBS += -latomic -ljnigraphics
LOCAL_LDFLAGS += $(FRESCO_CPP_LDFLAGS)
LOCAL_LDLIBS += -llog -ldl -landroid
LOCAL_STATIC_LIBRARIES += gif
include $(BUILD_SHARED_LIBRARY)
$(call import-module, giflib)
