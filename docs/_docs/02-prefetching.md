---
docid: prefetching
title: Prefetching Images
layout: docs
permalink: /docs/prefetching.html
---

Prefetching images in advance of showing them can sometimes lead to shorter wait times for users. Remember, however, that there are trade-offs. Prefetching takes up your users' data, and uses up its share of CPU and memory. As a rule, prefetching is not recommended for most apps.

Nonetheless, the image pipeline allows you to prefetch to either disk or bitmap cache. Both will use more data for network URIs, but the disk cache will not do a decode and will therefore use less CPU.

__Note:__ Beware that if your network fetcher doesn't support priorities prefetch requests may slow down images which are immediately required on screen. Neither `OkHttpNetworkFetcher` nor `HttpUrlConnectionNetworkFetcher` currently support priorities.

Prefetch to disk:

```java
imagePipeline.prefetchToDiskCache(imageRequest, callerContext);
```

Prefetch to bitmap cache:

```java
imagePipeline.prefetchToBitmapCache(imageRequest, callerContext);
```

Cancelling prefetch:

```java
// keep the reference to the returned data source.
DataSource<Void> prefetchDataSource = imagePipeline.prefetchTo...;

// later on, if/when you need to cancel the prefetch:
prefetchDataSource.close();
```

Closing a prefetch data source after the prefetch has already completed is a no-op and completely safe to do.

### Example

See our [showcase app](https://github.com/facebook/fresco/blob/master/samples/showcase/src/main/java/com/facebook/fresco/samples/showcase/imagepipeline/ImagePipelinePrefetchFragment.java) for a practical example of how to use prefetching.
