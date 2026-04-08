---
oncalls: ['fresco']
description: >
  Fresco Core Library architecture overview - ImagePipeline, Vito, producers, caching
apply_to_regex: 'libraries/fresco/.*\.(kt|java)$'
---

# Fresco Core Library Overview

**Team**: MF Client - QoE | **Oncall**: fresco

## Architecture

```
Application Layer
       ↓
┌─────────────────┐
│   Vito/Drawee   │  ← View binding (Vito = modern Kotlin, Drawee = legacy)
└────────┬────────┘
         ↓
┌─────────────────┐
│   Controller    │  ← Request lifecycle management
└────────┬────────┘
         ↓
┌─────────────────┐
│  ImagePipeline  │  ← Core orchestration
└────────┬────────┘
         ↓
┌─────────────────┐
│ Producer Chain  │  ← Fetch → Decode → Transform → Cache
└────────┬────────┘
         ↓
┌─────────────────────────────────────┐
│  Memory Cache → Disk Cache → Network │
└─────────────────────────────────────┘
```

## Directory Structure

| Directory | Purpose |
|-----------|---------|
| `imagepipeline/` | Core pipeline, producers, caching |
| `vito/` | Modern Kotlin view binding |
| `drawee/` | Legacy view binding |
| `animated-gif/`, `animated-webp/` | Animation support |
| `memory-types/` | Memory management, pools |

## Key Classes

- **ImagePipeline**: Entry point for image requests
- **FrescoController2**: Modern controller for Vito
- **CloseableReference<T>**: Reference-counted memory management
- **DataSource<T>**: Async result wrapper
- **Producer<T>**: Pipeline stage interface
