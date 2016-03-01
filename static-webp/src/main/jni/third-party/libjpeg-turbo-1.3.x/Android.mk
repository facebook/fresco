LOCAL_PATH:= $(call my-dir)

JPEGTURBO_CFLAGS := -DJPEG_LIB_VERSION=80 -Wno-attributes

JPEGTURBO_SRC_FILES := \
	jcapimin.c jcapistd.c jccoefct.c jccolor.c \
	jcdctmgr.c jchuff.c jcinit.c jcmainct.c jcmarker.c jcmaster.c \
	jcomapi.c jcparam.c jcphuff.c jcprepct.c jcsample.c jctrans.c \
	jdapimin.c jdapistd.c jdatadst.c jdatasrc.c jdcoefct.c jdcolor.c \
	jddctmgr.c jdhuff.c jdinput.c jdmainct.c jdmarker.c jdmaster.c \
	jdmerge.c jdphuff.c jdpostct.c jdsample.c jdtrans.c jerror.c \
	jfdctflt.c jfdctfst.c jfdctint.c jidctflt.c jidctfst.c jidctint.c \
	jidctred.c jquant1.c jquant2.c jutils.c jmemmgr.c \
	jaricom.c jcarith.c jdarith.c \
	transupp.c jmemnobs.c

# switch between SIMD supported and non supported architectures
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
JPEGTURBO_SRC_FILES += \
	simd/jsimd_arm_neon.S.neon \
	simd/jsimd_arm.c
else
JPEGTURBO_SRC_FILES += jsimd_none.c
endif

# fb_jpegturbo module
include $(CLEAR_VARS)
LOCAL_MODULE:= fb_jpegturbo
LOCAL_SRC_FILES := $(JPEGTURBO_SRC_FILES)
LOCAL_CFLAGS := $(JPEGTURBO_CFLAGS)
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH) 
include $(BUILD_STATIC_LIBRARY)
