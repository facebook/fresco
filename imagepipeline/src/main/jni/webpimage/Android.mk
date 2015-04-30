# Copyright 2004-present Facebook. All Rights Reserved.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := webpimage

LOCAL_SRC_FILES := \
  jni.cpp \
  webp.cpp \
  jni_helpers.cpp \

CXX11_FLAGS := -std=c++11
LOCAL_CFLAGS += $(CXX11_FLAGS)
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += $(FRESCO_CPP_CFLAGS)
LOCAL_EXPORT_CPPFLAGS := $(CXX11_FLAGS)
LOCAL_LDLIBS += -llog -ljnigraphics
LOCAL_LDFLAGS += $(FRESCO_CPP_LDFLAGS)

LOCAL_SHARED_LIBRARIES += webp
include $(BUILD_SHARED_LIBRARY)
$(call import-module, libwebp-0.4.2)
