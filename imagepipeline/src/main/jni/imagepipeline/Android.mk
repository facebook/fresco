# Copyright 2004-present Facebook. All Rights Reserved.

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := imagepipeline
LOCAL_SRC_FILES := \
	decoded_image.cpp \
	exceptions.cpp \
	init.cpp \
	jpeg/jpeg_codec.cpp \
	jpeg/jpeg_error_handler.cpp \
	jpeg/jpeg_memory_io.cpp \
	jpeg/jpeg_stream_wrappers.cpp \
	png/png_codec.cpp \
	png/png_stream_wrappers.cpp \
	streams.cpp \
	transformations.cpp \
	webp/webp_codec.cpp \
	JpegTranscoder.cpp \
	WebpTranscoder.cpp


CXX11_FLAGS := -std=c++11
LOCAL_CFLAGS += $(CXX11_FLAGS)
LOCAL_CFLAGS += -DLOG_TAG=\"libimagepipeline\"
LOCAL_EXPORT_CPPFLAGS := $(CXX11_FLAGS)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)
LOCAL_LDLIBS := -llog
LOCAL_SHARED_LIBRARIES += webp

ifeq ($(BUCK_BUILD), 1)
  LOCAL_SHARED_LIBRARIES += fb_jpegturbo
  LOCAL_CFLAGS += $(BUCK_DEP_CFLAGS)
  LOCAL_LDFLAGS += $(BUCK_DEP_LDFLAGS)
  include $(BUILD_SHARED_LIBRARY)
else
  LOCAL_LDLIBS += -lz
  LOCAL_STATIC_LIBRARIES += fb_jpegturbo
  LOCAL_STATIC_LIBRARIES += fb_png
  include $(BUILD_SHARED_LIBRARY)
  $(call import-module,libpng-1.6.10)
endif


$(call import-module,libjpeg-turbo-1.3.x)
$(call import-module,libwebp-0.4.2)
