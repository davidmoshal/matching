package jasition.matching.domain

import io.vavr.collection.List
import io.vavr.collection.Seq
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.trade.event.TradeSideEntry
import java.time.Instant

fun aBooks(bookId: BookId, bookEntries: Seq<BookEntry> = List.empty()): Books =
    aBooksWithEntities(Books(bookId), bookEntries)

fun aBooksWithEntities(
    books: Books,
    bookEntries: Seq<BookEntry>,
    offset: Int = 0
): Books =
    if (offset >= bookEntries.size()) books
    else aBooksWithEntities(
        aBooksWithEntity(books, bookEntries.get(offset)),
        bookEntries, offset + 1
    )

fun aBooksWithEntity(books: Books, bookEntry: BookEntry): Books =
    bookEntry.toEntryAddedToBookEvent(books.bookId).play(books).aggregate

fun expectedEntryAddedToBookEvent(
    orderPlacedEvent: OrderPlacedEvent,
    books: Books,
    expectedBookEntry: BookEntry
): EntryAddedToBookEvent = EntryAddedToBookEvent(
    eventId = orderPlacedEvent.eventId.next(),
    bookId = books.bookId,
    entry = expectedBookEntry,
    whenHappened = orderPlacedEvent.whenHappened
)

fun expectedBookEntry(orderPlacedEvent: OrderPlacedEvent): BookEntry = BookEntry(
    price = orderPlacedEvent.price,
    whenSubmitted = orderPlacedEvent.whenHappened,
    eventId = orderPlacedEvent.eventId.next(),
    requestId = orderPlacedEvent.requestId,
    whoRequested = orderPlacedEvent.whoRequested,
    entryType = orderPlacedEvent.entryType,
    side = orderPlacedEvent.side,
    timeInForce = orderPlacedEvent.timeInForce,
    sizes = orderPlacedEvent.sizes,
    status = orderPlacedEvent.status
)

fun expectedBookEntry(
    orderPlacedEvent: OrderPlacedEvent,
    eventId: EventId,
    status: EntryStatus,
    sizes: EntrySizes
) = BookEntry(
    price = orderPlacedEvent.price,
    whenSubmitted = orderPlacedEvent.whenHappened,
    eventId = eventId,
    requestId = orderPlacedEvent.requestId,
    whoRequested = orderPlacedEvent.whoRequested,
    entryType = orderPlacedEvent.entryType,
    side = orderPlacedEvent.side,
    timeInForce = orderPlacedEvent.timeInForce,
    sizes = sizes,
    status = status
)


fun anOrderPlacedEvent(
    requestId: ClientRequestId = aClientRequestId(),
    whoRequested: Client = aFirmWithClient(),
    bookId: BookId = aBookId(),
    entryType: EntryType = EntryType.LIMIT,
    side: Side = Side.BUY,
    price: Price = aPrice(),
    timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    whenHappened: Instant = Instant.now(),
    eventId: EventId = anEventId(),
    sizes: EntrySizes = anEntrySizes()
): OrderPlacedEvent = OrderPlacedEvent(
    requestId = requestId,
    whoRequested = whoRequested,
    bookId = bookId,
    entryType = entryType,
    side = side,
    price = price,
    timeInForce = timeInForce,
    whenHappened = whenHappened,
    eventId = eventId,
    sizes = sizes
)

fun aBookId(bookId: String = "book"): BookId = BookId(bookId = bookId)

fun aFirmWithClient(
    firmId: String = "firm1",
    firmClientId: String = "firm1Client1"
): Client = Client(
    firmId = firmId,
    firmClientId = firmClientId
)

fun anotherFirmWithClient(
    firmId: String = "firm2",
    firmClientId: String = "firm1Client2"
): Client = Client(
    firmId = firmId,
    firmClientId = firmClientId
)

fun aFirmWithoutClient(
    firmId: String = "firm1"
): Client = Client(
    firmId = firmId,
    firmClientId = null
)

fun anotherFirmWithoutClient(
    firmId: String = "firm2"
): Client = Client(
    firmId = firmId,
    firmClientId = null
)

fun aBookEntryKey(
    price: Price? = aPrice(),
    whenSubmitted: Instant = Instant.now(),
    eventId: EventId = anEventId()
): BookEntryKey = BookEntryKey(
    price = price,
    whenSubmitted = whenSubmitted,
    eventId = eventId
)

fun aBookEntry(
    price: Price? = aPrice(),
    whenSubmitted: Instant = Instant.now(),
    eventId: EventId = anEventId(),
    requestId: ClientRequestId = aClientRequestId(),
    whoRequested: Client = aFirmWithClient(),
    entryType: EntryType = EntryType.LIMIT,
    side: Side = Side.BUY,
    timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    sizes: EntrySizes = anEntrySizes(),
    status: EntryStatus = EntryStatus.NEW
) = BookEntry(
    key = aBookEntryKey(price, whenSubmitted, eventId),
    requestId = requestId,
    whoRequested = whoRequested,
    entryType = entryType,
    side = side,
    timeInForce = timeInForce,
    sizes = sizes,
    status = status
)

fun anEntrySizes(i: Int = 20) = EntrySizes(i)

fun anEventId(value: Long = 1) = EventId(value)

fun aPrice(value: Long = 10) = Price(value = value)

fun aClientRequestId(
    current: String = "req",
    original: String? = null,
    parentId: String? = null
) = ClientRequestId(current, original, parentId)

fun anotherClientRequestId(
    current: String = "req2",
    original: String? = null,
    parentId: String? = null
) = ClientRequestId(current, original, parentId)

fun aTradingStatuses(
    manual: TradingStatus? = null,
    fastMarket: TradingStatus? = null,
    scheduled: TradingStatus? = null,
    default: TradingStatus = TradingStatus.OPEN_FOR_TRADING
): TradingStatuses = TradingStatuses(
    manual = manual,
    fastMarket = fastMarket,
    scheduled = scheduled,
    default = default
)

fun expectedTradeSideEntry(
    orderPlacedEvent: OrderPlacedEvent,
    eventId: EventId,
    sizes: EntrySizes,
    status: EntryStatus
): TradeSideEntry {
    return TradeSideEntry(
        requestId = orderPlacedEvent.requestId,
        whoRequested = orderPlacedEvent.whoRequested,
        entryType = orderPlacedEvent.entryType,
        sizes = sizes,
        side = orderPlacedEvent.side,
        price = orderPlacedEvent.price,
        timeInForce = orderPlacedEvent.timeInForce,
        whenSubmitted = orderPlacedEvent.whenHappened,
        eventId = eventId,
        status = status
    )
}

fun expectedTradeSideEntry(
    bookEntry: BookEntry,
    eventId: EventId,
    size: EntrySizes,
    status: EntryStatus
): TradeSideEntry {
    return TradeSideEntry(
        requestId = bookEntry.requestId,
        whoRequested = bookEntry.whoRequested,
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

fun countEventsByClass(events: List<Event<BookId, Books>>) =
    events.groupBy { it.javaClass.simpleName }.mapValues {it.size()}

