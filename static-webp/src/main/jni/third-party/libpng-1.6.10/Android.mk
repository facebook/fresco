LOCAL_PATH:= $(call my-dir)

LIBPNG_SRC_FILES:= \
	png.c \
	pngerror.c \
	pngget.c \
	pngmem.c \
	pngpread.c \
	pngread.c \
	pngrio.c \
	pngrtran.c \
	pngrutil.c \
	pngset.c \
	pngtrans.c \
	pngwio.c \
	pngwrite.c \
	pngwtran.c \
	pngwutil.c


include $(CLEAR_VARS)
LOCAL_MODULE:= libfb_png
LOCAL_SRC_FILES:= $(LIBPNG_SRC_FILES)
LOCAL_CFLAGS:= $(LIBPNG_CFLAGS)
LOCAL_EXPORT_C_INCLUDES:= $(LOCAL_PATH)
include $(BUILD_STATIC_LIBRARY)
