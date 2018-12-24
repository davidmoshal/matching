package jasition.matching.domain

import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.BookEntryKey
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.order.event.OrderPlacedEvent

fun expectedEntryAddedToBookEvent(
    orderPlacedEvent: OrderPlacedEvent,
    books: Books,
    expectedBookEntry: BookEntry
): EntryAddedToBookEvent = EntryAddedToBookEvent(
    eventId = orderPlacedEvent.eventId + 1,
    bookId = books.bookId,
    entry = expectedBookEntry,
    whenHappened = orderPlacedEvent.whenHappened
)

fun expectedBookEntry(orderPlacedEvent: OrderPlacedEvent): BookEntry = BookEntry(
    key = BookEntryKey(
        price = orderPlacedEvent.price,
        whenSubmitted = orderPlacedEvent.whenHappened,
        eventId = orderPlacedEvent.eventId + 1
    ),
    clientRequestId = orderPlacedEvent.requestId,
    client = orderPlacedEvent.whoRequested,
    entryType = orderPlacedEvent.entryType,
    side = orderPlacedEvent.side,
    timeInForce = orderPlacedEvent.timeInForce,
    size = orderPlacedEvent.size,
    status = orderPlacedEvent.entryStatus
)

