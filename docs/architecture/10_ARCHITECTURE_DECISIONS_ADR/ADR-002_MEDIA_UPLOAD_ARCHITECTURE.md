# ADR-002 Media Upload Architecture


## Status

Accepted


## Context

ROJAN Media system requires reliable upload management for:

- Logo
- Cover
- Gallery


During Media validation, two issues were discovered:

1. Android large image processing could block the UI thread.

2. Spring Multipart default limit (1MB) rejected optimized images before reaching Backend business validation.

The Backend already had business limits:

- Image: 8MB
- Document: 20MB

but the infrastructure transport layer was lower than the business layer.


## Decision

ROJAN Media upload follows a two-layer responsibility model.


### Client Responsibilities

Client applications are responsible for:

- Selecting image
- Safe decoding
- Resize according to Media Policy
- JPEG compression (~80%)
- Uploading optimized media


Client applications are NOT responsible for:

- Media persistence
- Media ownership
- Permission decisions
- Business validation


### Backend Responsibilities

Backend is the owner of Media Domain.

Backend responsibilities:

- Authentication validation
- Permission validation
- Accept optimized upload
- Validate business limits
- Create MediaAsset
- Attach media references
- Manage media lifecycle


## Media Policy Reference

Resize and compression targets are defined by:

/docs/architecture/07_MEDIA_ARCHITECTURE/MEDIA_POLICY.md

Current targets:

- Logo: 1024px
- Cover: 1600px
- Gallery: 2048px


## Transport Limits

Backend infrastructure:

max-file-size:
25MB

max-request-size:
30MB


These limits exist only to allow transport of optimized media.

They do not replace business validation.


## Business Limits

Application-level limits remain:

Image:
8MB

Document:
20MB


Enforced by:

UploadMediaUseCase


## Reason

Before this decision:

Spring Multipart default limits silently rejected uploads before reaching:

- Media validation
- GlobalExceptionHandler
- Business rules


This caused raw upload failures instead of structured API responses.

The transport layer was aligned above the business validation layer.


## Impact

Positive:

- Large optimized images upload reliably.
- Media Policy remains unchanged.
- Backend remains the single source of truth.
- Existing API contracts remain unchanged.


Affected components:

- Android Manager Media Pipeline
- Backend Media Upload Configuration


No impact:

- Permission model
- Salon ownership model
- Media Domain ownership


## Failure Handling

Expected oversized uploads must return structured API errors.

Current business validation response:

413 MEDIA_SIZE_EXCEEDED


Future improvement:

Add defensive handling for multipart parser level failures.


## Migration Plan

No migration required.

This ADR documents already implemented changes:

Backend:

Commit:
f2b70a9

Android Media Pipeline:

Validated through End-to-End Media testing.


## Future Evolution

For larger scale:

- Direct storage upload
- Upload sessions
- Async media processing
- CDN generated renditions
