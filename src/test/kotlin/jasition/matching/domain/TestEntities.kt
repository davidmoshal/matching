package jasition.matching.domain

import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import java.time.Instant

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
    clientRequestId = orderPlacedEvent.requestId,
    client = orderPlacedEvent.whoRequested,
    entryType = orderPlacedEvent.entryType,
    side = orderPlacedEvent.side,
    timeInForce = orderPlacedEvent.timeInForce,
    size = orderPlacedEvent.size,
    status = orderPlacedEvent.entryStatus
)

fun aBookId(bookId: String = "book"): BookId = BookId(bookId = bookId)

fun aFirmWithClient(
    firmId: String = "firm1",
    firmClientId: String = "firm1Client1"
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
    clientRequestId: ClientRequestId = aClientRequestId(),
    client: Client = aFirmWithClient(),
    entryType: EntryType = EntryType.LIMIT,
    side: Side = Side.BUY,
    timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    size: EntryQuantity = anEntryQuantity(),
    status: EntryStatus = EntryStatus.NEW
) = BookEntry(
    key = aBookEntryKey(price, whenSubmitted, eventId),
    clientRequestId = clientRequestId,
    client = client,
    entryType = entryType,
    side = side,
    timeInForce = timeInForce,
    size = size,
    status = status
)

fun anEntryQuantity(i: Int = 20) = EntryQuantity(i)

fun anEventId(value: Long = 1) = EventId(value)

fun aPrice(value: Long = 10) = Price(value = value)

fun aClientRequestId(
    current: String = "req1",
    original: String? = null,
    listId: String? = null
) = ClientRequestId(current, original, listId)

