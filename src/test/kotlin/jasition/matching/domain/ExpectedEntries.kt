package jasition.matching.domain

import jasition.cqrs.EventId
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.quote.QuoteEntry
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.trade.event.TradeSideEntry

fun expectedBookEntry(
    event: OrderPlacedEvent,
    eventId: EventId = event.eventId,
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
    quoteEntry: QuoteEntry,
    side: Side,
    eventId: EventId = event.eventId,
    sizes: EntrySizes = EntrySizes(side.priceWithSize(quoteEntry)!!.size),
    status: EntryStatus = EntryStatus.NEW
): BookEntry =
    BookEntry(
        price = side.priceWithSize(quoteEntry)?.price,
        whenSubmitted = event.whenHappened,
        eventId = eventId,
        requestId = expectedClientRequestId(event, quoteEntry),
        whoRequested = event.whoRequested,
        isQuote = true,
        entryType = EntryType.LIMIT,
        side = side,
        timeInForce = event.timeInForce,
        sizes = sizes,
        status = status
    )

fun expectedTradeSideEntry(
    event: MassQuotePlacedEvent,
    quoteEntry: QuoteEntry,
    side: Side,
    eventId: EventId = event.eventId,
    sizes: EntrySizes,
    status: EntryStatus
): TradeSideEntry {
    return TradeSideEntry(
        requestId = expectedClientRequestId(event, quoteEntry),
        whoRequested = event.whoRequested,
        isQuote = true,
        entryType = EntryType.LIMIT,
        sizes = sizes,
        side = side,
        price = side.priceWithSize(quoteEntry)?.price,
        timeInForce = event.timeInForce,
        whenSubmitted = event.whenHappened,
        eventId = eventId,
        status = status
    )
}

fun expectedClientRequestId(
    event: MassQuotePlacedEvent,
    quoteEntry: QuoteEntry
): ClientRequestId = ClientRequestId(
    current = quoteEntry.quoteEntryId,
    collectionId = quoteEntry.quoteSetId,
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
    sizes: EntrySizes,
    status: EntryStatus
): TradeSideEntry {
    return TradeSideEntry(
        requestId = bookEntry.requestId,
        whoRequested = bookEntry.whoRequested,
        isQuote = bookEntry.isQuote,
        entryType = bookEntry.entryType,
        sizes = sizes,
        side = bookEntry.side,
        price = bookEntry.key.price,
        timeInForce = bookEntry.timeInForce,
        whenSubmitted = bookEntry.key.whenSubmitted,
        eventId = eventId,
        status = status
    )
}