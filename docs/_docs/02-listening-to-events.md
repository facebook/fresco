---
docid: listening-to-events
title: Listening to Events
layout: docs
permalink: /docs/listening-to-events.html
---

### Motivation

The image pipeline and the view controller in Fresco have built-in instrumentation interfaces. One can employ this to track both performance and to react to events.

Fresco comes with two main instrumentation interfaces:

 - The `RequestListener` is globally registered in the `ImagePipelineConfig` and logs all requests that are handled by the producer-consumer chain
 - The `ControllerListener` is added to an individual `DraweeView` and is convenient for reacting on events such as "this image is fully loaded"

### ControllerListener

While the `RequestListener` is a global listener, the `ControllerListener` is local to a certain `DraweeView`. It is a good way to react to changes to the displayed view such as "image failed to load" or "image is fully loaded". Again, it's best to extend `BaseControllerListener` for this.

A simple listener might look like the following:

```java
public class MyControllerListener extends new BaseControllerListener<ImageInfo>() {

  @Override
  public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
    Log.i("DraweeUpdate", "Image is fully loaded!");
  }

  @Override
  public void onIntermediateImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
    Log.i("DraweeUpdate", "Image is partly loaded! (maybe it's a progressive JPEG?)");
    if (imageInfo != null) {
      int quality = imageInfo.getQualityInfo().getQuality();
      Log.i("DraweeUpdate", "Image quality (number scans) is: " + quality);
    }
  }

  @Override
  public void onFailure(String id, Throwable throwable) {
    Log.i("DraweeUpdate", "Image failed to load: " + throwable.getMessage());
  }
}
```

You add it to your `DraweeController` in the following way:

```java
DraweeController controller = Fresco.newDraweeControllerBuilder()
    .setImageRequest(request)
    .setControllerListener(new MyControllerListener())
    .build();
mSimpleDraweeView.setController(controller);
```

### RequestListener

The `RequestListener` comes with a large interface of callback methods. Most importantly, you will notice that they all provide the unique `requestId` which allows to track a request across multiple stages.

Due to the large number of callbacks, it is advisable to extend from `BaseRequestListener` instead and only implement the methods you are interested in. You register your listener in the Application class as follows:

```java
final Set<RequestListener> listeners = new HashSet<>();
listeners.add(new MyRequestLoggingListener());

ImagePipelineConfig imagePipelineConfig = ImagePipelineConfig.newBuilder(this)
  .setRequestListeners(listeners)
  .build();

Fresco.initialize(this, imagePipelineConfig);
```

We will walk through the generated logging of one image request from the showcase app and discuss the individual meanings. You can observe these yourself in `adb logcat` when running the showcase app:

```java
RequestLoggingListener: time 2095589: onRequestSubmit: {requestId: 5, callerContext: null, isPrefetch: false}
```

`onRequestSubmit(...)` is called when an `ImageRequest` enters the image pipeline. Here you can make use of the caller context object to identify which feature of the app is sending the request.

```java
RequestLoggingListener: time 2095590: onProducerStart: {requestId: 5, producer: BitmapMemoryCacheGetProducer}
RequestLoggingListener: time 2095591: onProducerFinishWithSuccess: {requestId: 5, producer: BitmapMemoryCacheGetProducer, elapsedTime: 1 ms, extraMap: {cached_value_found=false}}
```

The `onProducerStart(...)` and `onProducerFinishWithSuccess(...)` (or `onProducerFinishWithFailure(...)`) are called for all producers along the pipeline. The one above is a check of the Bitmap cache.

```java
RequestLoggingListener: time 2095592: onProducerStart: {requestId: 5, producer: BackgroundThreadHandoffProducer}
RequestLoggingListener: time 2095593: onProducerFinishWithSuccess: {requestId: 5, producer: BackgroundThreadHandoffProducer, elapsedTime: 1 ms, extraMap: null}
RequestLoggingListener: time 2095594: onProducerStart: {requestId: 5, producer: BitmapMemoryCacheProducer}
RequestLoggingListener: time 2095594: onProducerFinishWithSuccess: {requestId: 5, producer: BitmapMemoryCacheProducer, elapsedTime: 0 ms, extraMap: {cached_value_found=false}}
RequestLoggingListener: time 2095595: onProducerStart: {requestId: 5, producer: EncodedMemoryCacheProducer}
RequestLoggingListener: time 2095596: onProducerFinishWithSuccess: {requestId: 5, producer: EncodedMemoryCacheProducer, elapsedTime: 1 ms, extraMap: {cached_value_found=false}}
RequestLoggingListener: time 2095596: onProducerStart: {requestId: 5, producer: DiskCacheProducer}
RequestLoggingListener: time 2095598: onProducerFinishWithSuccess: {requestId: 5, producer: DiskCacheProducer, elapsedTime: 2 ms, extraMap: {cached_value_found=false}}
RequestLoggingListener: time 2095598: onProducerStart: {requestId: 5, producer: PartialDiskCacheProducer}
RequestLoggingListener: time 2095602: onProducerFinishWithSuccess: {requestId: 5, producer: PartialDiskCacheProducer, elapsedTime: 4 ms, extraMap: {cached_value_found=false}}
```

We see more of these when the request is handed over to the background (`BackgroundThreadHandoffProducer`) and performs look-ups in the caches.

```java
RequestLoggingListener: time 2095602: onProducerStart: {requestId: 5, producer: NetworkFetchProducer}
RequestLoggingListener: time 2095745: onProducerEvent: {requestId: 5, stage: NetworkFetchProducer, eventName: intermediate_result; elapsedTime: 143 ms}
RequestLoggingListener: time 2095764: onProducerFinishWithSuccess: {requestId: 5, producer: NetworkFetchProducer, elapsedTime: 162 ms, extraMap: {queue_time=140, total_time=161, image_size=40502, fetch_time=21}}
RequestLoggingListener: time 2095764: onUltimateProducerReached: {requestId: 5, producer: NetworkFetchProducer, elapsedTime: -1 ms, success: true}
```

For this particular request, the `NetworkFetchProducer` is the "ultimate producer". This means, it is the one that provides the definite input source for fulfilling the request. If the image is cached, the `DiskCacheProducer` would be the "ultimate" producer.

```java
RequestLoggingListener: time 2095766: onProducerStart: {requestId: 5, producer: DecodeProducer}
RequestLoggingListener: time 2095786: onProducerFinishWithSuccess: {requestId: 5, producer: DecodeProducer, elapsedTime: 20 ms, extraMap: {imageFormat=JPEG, ,hasGoodQuality=true, bitmapSize=788x525, isFinal=true, requestedImageSize=unknown, encodedImageSize=788x525, sampleSize=1, queueTime=0}
RequestLoggingListener: time 2095788: onRequestSuccess: {requestId: 5, elapsedTime: 198 ms}
```

On the way up, the `DecodeProducer` also succeeds and finally the `onRequestSuccess(...)` method is called.

You will notice that most of these methods are given optional information as a `Map<String, String> extraMap`. The string constants to look-up the elements are usually public constants in the corresponding producer classes.
