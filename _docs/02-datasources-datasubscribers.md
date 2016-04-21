---
docid: datasources-datasubscribers
title: DataSources and DataSubscribers
layout: docs
permalink: /docs/datasources-datasubscribers.html
prev: using-image-pipeline.html
next: closeable-references.html
---

A [DataSource](../javadoc/reference/com/facebook/datasource/DataSource.html) is, like a Java [Future](http://developer.android.com/reference/java/util/concurrent/Future.html), the result of an asynchronous computation. The different is that, unlike a Future, a DataSource can return you a whole series of results from a single command, not just one.

After submitting an image request, the image pipeline returns a data source. To get a result out if it, you need to use a [DataSubscriber](../javadoc/reference/com/facebook/datasource/DataSubscriber.html).

### Executors

When subscribing to a data source, an executor must be provided. The purpose of executors is to execute runnables (in our case the subscriber callback methods) on a specific thread and with specific policy.
Fresco provides several [executors] (https://github.com/facebook/fresco/tree/0f3d52318631f2125e080d2a19f6fa13a31efb31/fbcore/src/main/java/com/facebook/common/executors) and one should carefully choose which one to be used:

* If you need to do any UI stuff from your callback (accessing views, drawables, etc.), you must use `UiThreadImmediateExecutorService.getInstance()`. Android view system is not thread safe and is only to be accessed from the main thread (the UI thread).
* If the callback is lightweight, and does not do any UI related stuff, you can simply use `CallerThreadExecutor.getInstance()`. This executor executes runnables on the caller's thread. Depending on what is the calling thread, callback may be executed either on the UI or a background thread. There are no guarantees which thread it is going to be and because of that this executor should be used with great caution. And again, only for lightweight non-UI related stuff.
* If you need to do some expensive non-UI related work (database access, disk read/write, or any other slow operation), this should NOT be done either with `CallerThreadExecutor` nor with the `UiThreadExecutorService`, but with one of the background thread executors. See [DefaultExecutorSupplier.forBackgroundTasks] (https://github.com/facebook/fresco/blob/0f3d52318631f2125e080d2a19f6fa13a31efb31/imagepipeline/src/main/java/com/facebook/imagepipeline/core/DefaultExecutorSupplier.java) for an example implementation.

### Getting result from a data source

This is a generic example of how to get a result from a data source of `CloseableReference<T>` for arbitrary type `T`. The result is valid only in the scope of the `onNewResultImpl` callback. As soon as the callback gets executed, the result is no longer valid. See the next example if the result needs to be kept around.

```java
    DataSource<CloseableReference<T>> dataSource = ...;

    DataSubscriber<CloseableReference<T>> dataSubscriber =
        new BaseDataSubscriber<CloseableReference<T>>() {
          @Override
          protected void onNewResultImpl(
              DataSource<CloseableReference<T>> dataSource) {
            if (!dataSource.isFinished()) {
              // if we are not interested in the intermediate images,
              // we can just return here.
              return;
            }
            CloseableReference<T> ref = dataSource.getResult();
            if (ref != null) {
              try {
                // do somethign with the result
                T result = ref.get();
                ...
              } finally {
                CloseableReference.closeSafely(ref);
              }
            }
          }

          @Override
          protected void onFailureImpl(DataSource<CloseableReference<T>> dataSource) {
            Throwable t = dataSource.getFailureCause();
            // handle failure
          }
        };

    dataSource.subscribe(dataSubscriber, executor);
```

### Keeping result from a data source

The above example closes the reference as soon as the callback gets executed. If the result needs to be kept around, you must keep the corresponding `CloseableReference` for as long as the result is needed. This can be done as follows:

```java
    DataSource<CloseableReference<T>> dataSource = ...;

    DataSubscriber<CloseableReference<T>> dataSubscriber =
        new BaseDataSubscriber<CloseableReference<T>>() {
          @Override
          protected void onNewResultImpl(
              DataSource<CloseableReference<T>> dataSource) {
            if (!dataSource.isFinished()) {
              // if we are not interested in the intermediate images,
              // we can just return here.
              return;
            }
            // keep the closeable reference
            mRef = dataSource.getResult();
            // do something with the result
            T result = mRef.get();
            ...
          }

          @Override
          protected void onFailureImpl(DataSource<CloseableReference<T>> dataSource) {
            Throwable t = dataSource.getFailureCause();
            // handle failure
          }
        };

    dataSource.subscribe(dataSubscriber, executor);
```

IMPORTANT: once you don't need the result anymore, you must close the reference. Not doing so may cause memory leaks.
See [closeable references](closeable-references.html) for more details.

```java
    CloseableReference.closeSafely(mRef);
    mRef = null;
```

### To get encoded image...

```java
    DataSource<CloseableReference<PooledByteBuffer>> dataSource =
        mImagePipeline.fetchEncodedImage(imageRequest, CALLER_CONTEXT);
```

Image pipeline uses `PooledByteBuffer` for encoded images. This is our `T` in the above examples. Here is an example of creating an `InputStream` out of `PooledByteBuffer` so that we can read the image bytes:

```java
      InputStream is = new PooledByteBufferInputStream(result);
      try {
        // Example: get the image format
        ImageFormat imageFormat = ImageFormatChecker.getImageFormat(is);
        // Example: write input stream to a file
        Files.copy(is, path);
      } catch (...) {
        ...
      } finally {
        Closeables.closeQuietly(is);
      }
```

### To get decoded image...

```java
DataSource<CloseableReference<CloseableImage>>
    dataSource = imagePipeline.fetchDecodedImage(imageRequest, callerContext);
```

Image pipeline uses `CloseableImage` for decoded images. This is our `T` in the above examples. Here is an example of getting a `Bitmap` out of `CloseableImage`:

```java
	CloseableImage image = ref.get();
	if (image instanceof CloseableBitmap) {
	  // do something with the bitmap
	  Bitmap bitmap = (CloseableBitmap image).getUnderlyingBitmap();
	  ...
	}
```


### I just want a bitmap...

If your request to the pipeline is for a single [Bitmap](http://developer.android.com/reference/android/graphics/Bitmap.html), you can take advantage of our easier-to-use [BaseBitmapDataSubscriber](../javadoc/reference/com/facebook/imagepipeline/datasource/BaseBitmapDataSubscriber):

```java
dataSource.subscribe(new BaseBitmapDataSubscriber() {
    @Override
    public void onNewResultImpl(@Nullable Bitmap bitmap) {
      // You can use the bitmap here, but in limited ways.
      // No need to do any cleanup.
    }

    @Override
    public void onFailureImpl(DataSource dataSource) {
      // No cleanup required here.
    }
  },
  executor);
```

A snap to use, right? There are caveats.

This subscriber doesn't work for animated images as those can not be represented as a single bitmap.

You can **not** assign the bitmap to any variable not in the scope of the `onNewResultImpl` method. The reason is, as already explained in the above examples that, after the subscriber has finished executing, the image pipeline will recycle the bitmap and free its memory. If you try to draw the bitmap after that, your app will crash with an `IllegalStateException.`

You can still safely pass the Bitmap to an Android [notification](https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#setLargeIcon\(android.graphics.Bitmap\)) or [remote view](http://developer.android.com/reference/android/widget/RemoteViews.html#setImageViewBitmap\(int, android.graphics.Bitmap\)). If Android needs your Bitmap in order to pass it to a system process, it makes a copy of the Bitmap data in ashmem - the same heap used by Fresco. So Fresco's automatic cleanup will work without issue.

If those requirements prevent you from using `BaseBitmapDataSubscriber`, you can go with a more generic approach as explained above.
