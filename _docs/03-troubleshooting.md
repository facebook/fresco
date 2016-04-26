---
docid: troubleshooting
title: Troubleshooting
layout: docs
permalink: /docs/troubleshooting.html
prev: closeable-references.html
next: gotchas.html
---

##  Troubleshooting

### Image is displayed with repeated edges

This is a known limitation when rounding is used. See [Rounding](http://frescolib.org/docs/rounded-corners-and-circles.html#_) for more information and how to workaround.


### Image doesn't load

You can get more information from the image pipeline by examining the verbose logcat as explained later in this section. Here are some common reasons why image loads might fail:

#### File not available

For example, an incorrect path for local files or an unavailable network URI is given.

Try opening a network URI in a mobile browser. If it doesn't work, the issue is likely neither in Fresco nor your app.

For a local file, try opening a file input stream directly from your app:

```
FileInputStream fis = new FileInputStream(new File(localUri.getPath()));
```

If that throws an exception, the issue is likely not in Fresco, **but** it may be in your app. One possibility is a permission issue, such as trying to access the SD card without requiring the necessary permission in your application manifest. Another possibility is that the pathy is not correct - perhaps you forgot to properly escape it. Finally, the file may simply not exist.

#### OOMs and failing to allocate a bitmap

The most common reason for this happening is loading too big images. If the image to be loaded is of considerably bigger size than the view hosting it, it should be [resized] (http://frescolib.org/docs/resizing-rotating.html#_).

#### Bitmap too large to be uploaded to a texture

Android cannot display images more than 2048 pixels long in either dimension. This is beyond the capability of the OpenGL rendering system. Fresco will resize your image if it exceeds this limit.


### Investigating issues with logcat

There are various issues one might encounter when it comes to image handling. With Fresco, most of them can be diagnosed by simply looking at the `VERBOSE` logcat. This should be your starting point when investigating an issue with Fresco.

#### Setting up logcat

By default, Fresco does not write out all its logs. You need to [configure the image pipeline](configure-image-pipeline.html#_) to do so.

```java
Set<RequestListener> requestListeners = new HashSet<>();
requestListeners.add(new RequestLoggingListener());
ImagePipelineConfig config = ImagePipelineConfig.newBuilder(context)
   // other setters
   .setRequestListeners(requestListeners)
   .build();
Fresco.initialize(context, config);
FLog.setMinimumLoggingLevel(FLog.VERBOSE);
```

#### Examining logcat

All of Fresco's logs can be examined by this command:

```
adb logcat -v threadtime | grep -iE 'LoggingListener|AbstractDraweeController|BufferedDiskCache'
```

The output shows what is happening with the image requests within the image pipeline. It looks something like this:

```
08-12 09:11:14.791 6690 6690 V unknown:AbstractDraweeController: controller 28ebe0eb 0 -> 1: initialize
08-12 09:11:14.791 6690 6690 V unknown:AbstractDraweeController: controller 28ebe0eb 1: onDetach
08-12 09:11:14.791 6690 6690 V unknown:AbstractDraweeController: controller 28ebe0eb 1: setHierarchy: null
08-12 09:11:14.791 6690 6690 V unknown:AbstractDraweeController: controller 28ebe0eb 1: setHierarchy: com.facebook.drawee.generic.GenericDraweeHierarchy@2bb88e4
08-12 09:11:14.791 6690 6690 V unknown:AbstractDraweeController: controller 28ebe0eb 1: onAttach: request needs submit
08-12 09:11:14.791 6690 6690 V unknown:PipelineDraweeController: controller 28ebe0eb: getDataSource
08-12 09:11:14.791 6690 6690 V unknown:RequestLoggingListener: time 11201791: onRequestSubmit: {requestId: 1, callerContext: null, isPrefetch: false}
08-12 09:11:14.792 6690 6690 V unknown:RequestLoggingListener: time 11201791: onProducerStart: {requestId: 1, producer: BitmapMemoryCacheGetProducer}
08-12 09:11:14.792 6690 6690 V unknown:RequestLoggingListener: time 11201792: onProducerFinishWithSuccess: {requestId: 1, producer: BitmapMemoryCacheGetProducer, elapsedTime: 1 ms, extraMap: {cached_value_found=false}}
08-12 09:11:14.792 6690 6690 V unknown:RequestLoggingListener: time 11201792: onProducerStart: {requestId: 1, producer: BackgroundThreadHandoffProducer}
08-12 09:11:14.792 6690 6690 V unknown:AbstractDraweeController: controller 28ebe0eb 1: submitRequest: dataSource: 36e95857
08-12 09:11:14.792 6690 6734 V unknown:RequestLoggingListener: time 11201792: onProducerFinishWithSuccess: {requestId: 1, producer: BackgroundThreadHandoffProducer, elapsedTime: 0 ms, extraMap: null}
08-12 09:11:14.792 6690 6734 V unknown:RequestLoggingListener: time 11201792: onProducerStart: {requestId: 1, producer: BitmapMemoryCacheProducer}
08-12 09:11:14.792 6690 6734 V unknown:RequestLoggingListener: time 11201792: onProducerFinishWithSuccess: {requestId: 1, producer: BitmapMemoryCacheProducer, elapsedTime: 0 ms, extraMap: {cached_value_found=false}}
08-12 09:11:14.792 6690 6734 V unknown:RequestLoggingListener: time 11201792: onProducerStart: {requestId: 1, producer: EncodedMemoryCacheProducer}
08-12 09:11:14.792 6690 6734 V unknown:RequestLoggingListener: time 11201792: onProducerFinishWithSuccess: {requestId: 1, producer: EncodedMemoryCacheProducer, elapsedTime: 0 ms, extraMap: {cached_value_found=false}}
08-12 09:11:14.792 6690 6734 V unknown:RequestLoggingListener: time 11201792: onProducerStart: {requestId: 1, producer: DiskCacheProducer}
08-12 09:11:14.792 6690 6735 V unknown:BufferedDiskCache: Did not find image for http://www.example.com/image.jpg in staging area
08-12 09:11:14.793 6690 6735 V unknown:BufferedDiskCache: Disk cache read for http://www.example.com/image.jpg
08-12 09:11:14.793 6690 6735 V unknown:BufferedDiskCache: Disk cache miss for http://www.example.com/image.jpg
08-12 09:11:14.793 6690 6735 V unknown:RequestLoggingListener: time 11201793: onProducerFinishWithSuccess: {requestId: 1, producer: DiskCacheProducer, elapsedTime: 1 ms, extraMap: {cached_value_found=false}}
08-12 09:11:14.793 6690 6735 V unknown:RequestLoggingListener: time 11201793: onProducerStart: {requestId: 1, producer: NetworkFetchProducer}
08-12 09:11:15.161 6690 7358 V unknown:RequestLoggingListener: time 11202161: onProducerFinishWithSuccess: {requestId: 1, producer: NetworkFetchProducer, elapsedTime: 368 ms, extraMap: null}
08-12 09:11:15.162 6690 6742 V unknown:BufferedDiskCache: About to write to disk-cache for key http://www.example.com/image.jpg
08-12 09:11:15.162 6690 6734 V unknown:RequestLoggingListener: time 11202162: onProducerStart: {requestId: 1, producer: DecodeProducer}
08-12 09:11:15.163 6690 6742 V unknown:BufferedDiskCache: Successful disk-cache write for key http://www.example.com/image.jpg
08-12 09:11:15.169 6690 6734 V unknown:RequestLoggingListener: time 11202169: onProducerFinishWithSuccess: {requestId: 1, producer: DecodeProducer, elapsedTime: 7 ms, extraMap: {hasGoodQuality=true, queueTime=0, bitmapSize=600x400, isFinal=true}}
08-12 09:11:15.169 6690 6734 V unknown:RequestLoggingListener: time 11202169: onRequestSuccess: {requestId: 1, elapsedTime: 378 ms}
08-12 09:11:15.184 6690 6690 V unknown:AbstractDraweeController: controller 28ebe0eb 1: set_final_result @ onNewResult: image: CloseableReference 2fd41bb0
```

In this case, we see that the controller `28ebe0eb` associated with a DraweeView started datasource `36e95857` which issued image request `1`. We can now see that the image was not found in the bitmap cache, nor in the encoded memory cache, nor in the disk cache, and so the network fetch had to be performed. The fetch was successful, the image was decoded and the request finished successfully. Finally, the datasource notified the controller which then set the resulting image to the hierarchy (`set_final_result`).
