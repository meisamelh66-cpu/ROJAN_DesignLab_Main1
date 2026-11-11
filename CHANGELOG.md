# Changelog

All notable changes to the ROJAN Mobile component (Customer / Manager / Reception
flavors, this repository) are documented in this file. Format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning 2.0.0](https://semver.org/).

This file starts at `1.0.5` — it is the first entry ever written for this
component. It does not retroactively document every change made before this
point; only what is verified as part of this release.

## [1.0.5] - 2026-11-11

### Fixed

- Booking: the customer's selected payment method (`PaymentMethod.WALLET` /
  `PAY_AT_SALON`, chosen on `BookingConfirmationScreen`) now actually reaches
  the backend's booking-creation request instead of being silently dropped.
  Threaded end-to-end: `BookingConfirmationScreen` → `BookingConfirmationViewModel`
  → `BookingRepository.createBooking(paymentMethod)` → `BookingRepositoryImpl`
  → `CreateBookingRequestDto.paymentMethod` → backend `CreateBookingRequest` /
  `CreateBookingCommand` / `Booking` domain model → persisted
  `bookings.payment_method` → `BookingResponse.paymentMethod` (`22694b8`).
