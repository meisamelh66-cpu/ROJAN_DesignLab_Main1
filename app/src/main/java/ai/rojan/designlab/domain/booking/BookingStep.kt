package ai.rojan.designlab.domain.booking

/**
 * A step in the booking flow — pure business concept, deliberately
 * containing no route strings or Navigation-library types. Navigation
 * (in the `navigation/` package) is solely responsible for mapping a
 * [BookingStep] to an actual route; business logic here must never know
 * a route exists.
 */
enum class BookingStep {
    SEARCH,
    SALON,
    SPECIALIST,
    SERVICE,
    DATE,
    TIME,
    CONFIRMATION,
    SUCCESS,
}
