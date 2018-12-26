package jasition.matching.domain

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
    price: Price? = Price(10),
    whenSubmitted: Instant = Instant.now(),
    eventId: EventId = EventId(1)
): BookEntryKey = BookEntryKey(
    price = price,
    whenSubmitted = whenSubmitted,
    eventId = eventId
)

fun aBookEntry(
    price: Price? = Price(10),
    whenSubmitted: Instant = Instant.now(),
    eventId: EventId = EventId(1),
    clientRequestId: ClientRequestId = ClientRequestId("req1"),
    client: Client = aFirmWithClient(),
    entryType: EntryType = EntryType.LIMIT,
    side: Side = Side.BUY,
    timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    size: EntryQuantity = EntryQuantity(20),
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

