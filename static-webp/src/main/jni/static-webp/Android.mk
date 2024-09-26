# Copyright (c) Meta Platforms, Inc. and affiliates.
#
# This source code is licensed under the MIT license found in the
# LICENSE file in the root directory of this source tree.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := static-webp
LOCAL_SRC_FILES := \
	webp_bitmapfactory.cpp \
	decoded_image.cpp \
	exceptions.cpp \
	streams.cpp \
	transformations.cpp \
  jni_helpers.cpp \
  webp.cpp \


CXX11_FLAGS := -std=c++11
LOCAL_CFLAGS += $(CXX11_FLAGS)
LOCAL_CFLAGS += -DLOG_TAG=\"libstatic-webp\"
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += $(FRESCO_CPP_CFLAGS)
LOCAL_EXPORT_CPPFLAGS := $(CXX11_FLAGS)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS := -latomic -llog -ljnigraphics
LOCAL_LDFLAGS += $(FRESCO_CPP_LDFLAGS)

LOCAL_SHARED_LIBRARIES += webp
LOCAL_SHARED_LIBRARIES += webpdemux
LOCAL_SHARED_LIBRARIES += webpmux

LOCAL_LDLIBS += -lz

include $(BUILD_SHARED_LIBRARY)
$(call import-module,libwebp-1.3.2)
