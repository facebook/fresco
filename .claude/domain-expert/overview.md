<!-- analysis-metadata: { "date": "2026-03-25", "time_window": "30d", "max_diffs": 50, "diffs_analyzed": 7 } -->
# Fresco Core Library - Codebase Overview

**Team**: MF Client - QoE
**Oncall**: fresco
**Last Updated**: 2026-03-25

## Executive Summary

Fresco is Meta's powerful image loading and display library for Android. It provides a sophisticated image pipeline with multi-level caching, memory management, animated image support, and multiple view binding options (Drawee, Vito).

---

## Directory Structure

```
fresco/
├── imagepipeline/              # Core image loading pipeline
├── imagepipeline-base/         # Base abstractions & interfaces
├── imagepipeline-native/       # JNI/Native code for decoding & memory
├── imagepipeline-backends/     # Network layer backends (OkHttp, HttpUrlConnection)
├── imagepipeline-test/         # Test utilities
│
├── drawee/                     # Legacy view binding (DraweeView)
├── drawee-backends/            # Drawee backend implementations
├── drawee-span/                # TextSpan integration
│
├── vito/                       # Modern view binding (Kotlin-first)
│   ├── core/                   # Core Vito interfaces
│   ├── core-impl/              # Implementation
│   ├── litho/                  # Litho integration
│   ├── compose/                # Jetpack Compose support
│   ├── view/                   # View-based integration
│   ├── options/                # Image options configuration
│   └── provider/               # Provider implementations
│
├── animated-base/              # Animated image abstractions
├── animated-gif/               # GIF decoder
├── animated-gif-lite/          # Lightweight GIF decoder
├── animated-webp/              # WebP animation support
│
├── memory-types/               # Memory chunk implementations
│   ├── ashmem/                 # Ashmem memory (Android-specific)
│   ├── nativememory/           # Native heap memory
│   └── simple/                 # Buffer-based memory
│
├── native-filters/             # Native image filters
├── native-imagetranscoder/     # Native image transcoding
├── static-webp/                # Static WebP support
│
├── ui-common/                  # Shared UI utilities
├── ui-core/                    # Core UI components
├── urimod/                     # URI modification utilities
└── tools/                      # Build & development tools
```

---

## Tech Stack

| Category | Technology |
|----------|------------|
| **Language** | Kotlin (primary), Java (legacy/native interfaces) |
| **Build System** | Buck2 (`fb_android_library`) |
| **Native Code** | C++ via JNI |
| **Testing** | JUnit, Robolectric, Mockito |

---

## Module Boundaries

| Module | Responsibility | Dependencies |
|--------|----------------|--------------|
| `imagepipeline-base` | Interfaces & abstractions | Minimal |
| `imagepipeline` | Core pipeline logic | imagepipeline-base |
| `drawee` | View binding (legacy) | imagepipeline |
| `vito/core` | Modern view binding interfaces | imagepipeline-base |
| `vito/core-impl` | Vito implementation | vito/core, imagepipeline |
| `animated-base` | Animation abstractions | imagepipeline-base |
| `animated-gif` | GIF decoding | animated-base, native |

---

## Current vs Legacy Code

| Area | Legacy | Current |
|------|--------|---------|
| **View Binding** | Drawee (DraweeView) | Vito (VitoView, Compose) |
| **Controller** | AbstractDraweeController | FrescoController2, KFrescoController |
| **Language** | Java | Kotlin |
| **Configuration** | Programmatic | MobileConfig (experiments) |

**Migration**: New code should use Vito. Drawee is maintained for backward compatibility.

---

## Architecture Note

The image pipeline uses a **producer-consumer chain**: `BitmapMemoryCacheGetProducer` → `ThreadHandoffProducer` → `BitmapMemoryCacheProducer` → `DecodeProducer` → `EncodedMemoryCacheProducer` / `DiskCacheReadProducer` / `NetworkFetchProducer` (decode fans out to multiple upstream sources). `CloseableReference` provides memory-safe reference counting for native resources — every `.of()` or `.clone()` must have a corresponding `.close()` (see `common-patterns-core.md` pattern #3). `DataSource` delivers results asynchronously with progress updates and cancellation.
