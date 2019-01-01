package jasition.matching.domain

import jasition.cqrs.EventId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.EntryStatus
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.quote.command.QuoteEntry
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.trade.event.TradeSideEntry

fun expectedEntryAddedToBookEvent(
    event: OrderPlacedEvent,
    books: Books,
    expectedBookEntry: BookEntry
): EntryAddedToBookEvent =
    EntryAddedToBookEvent(
        eventId = event.eventId.next(),
        bookId = books.bookId,
        entry = expectedBookEntry,
        whenHappened = event.whenHappened
    )

fun expectedBookEntry(
    event: OrderPlacedEvent,
    eventId: EventId = event.eventId.next(),
    status: EntryStatus = event.status,
    sizes: EntrySizes = event.sizes
) = BookEntry(
    price = event.price,
    whenSubmitted = event.whenHappened,
    eventId = eventId,
    requestId = event.requestId,
    whoRequested = event.whoRequested,
    isQuote = false,
    entryType = event.entryType,
    side = event.side,
    timeInForce = event.timeInForce,
    sizes = sizes,
    status = status
)

fun expectedBookEntry(
    event: MassQuotePlacedEvent,
    eventId: EventId = event.eventId,
    entry: QuoteEntry,
    side: Side,
    sizes: EntrySizes,
    status: EntryStatus
): BookEntry =
    BookEntry(
        price = side.price(entry),
        whenSubmitted = event.whenHappened,
        eventId = eventId,
        requestId = expectedClientRequestId(event, entry),
        whoRequested = event.whoRequested,
        isQuote = true,
        entryType = entry.entryType,
        side = side,
        timeInForce = event.timeInForce,
        sizes = sizes,
        status = status
    )

fun expectedTradeSideEntry(
    event: MassQuotePlacedEvent,
    entry: QuoteEntry,
    side: Side,
    eventId: EventId = event.eventId,
    sizes: EntrySizes,
    status: EntryStatus
): TradeSideEntry {
    return TradeSideEntry(
        requestId = expectedClientRequestId(event, entry),
        whoRequested = event.whoRequested,
        isQuote = true,
        entryType = entry.entryType,
        sizes = sizes,
        side = side,
        price = side.price(entry),
        timeInForce = event.timeInForce,
        whenSubmitted = event.whenHappened,
        eventId = eventId,
        status = status
    )
}

fun expectedClientRequestId(
    event: MassQuotePlacedEvent,
    entry: QuoteEntry
): ClientRequestId = ClientRequestId(
    current = entry.quoteEntryId,
    collectionId = entry.quoteSetId,
    parentId = event.quoteId
)

fun expectedTradeSideEntry(
    event: OrderPlacedEvent,
    eventId: EventId = event.eventId,
    sizes: EntrySizes,
    status: EntryStatus
): TradeSideEntry {
    return TradeSideEntry(
        requestId = event.requestId,
        whoRequested = event.whoRequested,
        isQuote = false,
        entryType = event.entryType,
        sizes = sizes,
        side = event.side,
        price = event.price,
        timeInForce = event.timeInForce,
        whenSubmitted = event.whenHappened,
        eventId = eventId,
        status = status
    )
}

fun expectedTradeSideEntry(
    bookEntry: BookEntry,
    eventId: EventId = bookEntry.key.eventId,
    size: EntrySizes,
    status: EntryStatus
): TradeSideEntry {
    return TradeSideEntry(
        requestId = bookEntry.requestId,
        whoRequested = bookEntry.whoRequested,
        isQuote = bookEntry.isQuote,
        entryType = bookEntry.entryType,
        sizes = size,
        side = bookEntry.side,
        price = bookEntry.key.price,
        timeInForce = bookEntry.timeInForce,
        whenSubmitted = bookEntry.key.whenSubmitted,
        eventId = eventId,
        status = status
    )
}