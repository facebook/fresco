# Copyright 2004-present Facebook. All Rights Reserved.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := native-imagetranscoder
LOCAL_SRC_FILES := \
	decoded_image.cpp \
	exceptions_handler.cpp \
	init.cpp \
	jpeg/jpeg_codec.cpp \
	jpeg/jpeg_error_handler.cpp \
	jpeg/jpeg_memory_io.cpp \
	jpeg/jpeg_stream_wrappers.cpp \
	transformations.cpp \
	JpegTranscoder.cpp

CXX11_FLAGS := -std=c++11
LOCAL_CFLAGS += $(CXX11_FLAGS)
LOCAL_CFLAGS += -DLOG_TAG=\"libnative-imagetranscoder\"
LOCAL_CFLAGS += -fvisibility=hidden
LOCAL_CFLAGS += $(FRESCO_CPP_CFLAGS)
LOCAL_EXPORT_CPPFLAGS := $(CXX11_FLAGS)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS := -llog
LOCAL_LDFLAGS += $(FRESCO_CPP_LDFLAGS)

LOCAL_STATIC_LIBRARIES += fb_jpegturbo
LOCAL_LDFLAGS += -Wl,--exclude-libs,libfb_jpegturbo.a

include $(BUILD_SHARED_LIBRARY)
$(call import-module,libjpeg-turbo-1.5.3)
