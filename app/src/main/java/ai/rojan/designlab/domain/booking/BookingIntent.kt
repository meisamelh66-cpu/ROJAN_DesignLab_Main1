package ai.rojan.designlab.domain.booking

/**
 * How a booking session was entered — pure business/domain concept, no
 * Compose or Navigation dependency. Extensible: adding a new entry
 * point later means adding a new case here, not restructuring anything.
 */
enum class BookingIntent {
    UNKNOWN,
    SEARCH,
    SERVICE,
    SALON,
    SPECIALIST,
    PACKAGE,
    PROMOTION,
    AI_RECOMMENDATION,
    BOOK_AGAIN,
    FAVORITE,
    RECENT_VISIT,
    NOTIFICATION,
    DEEP_LINK,
    QR_CODE,
}
