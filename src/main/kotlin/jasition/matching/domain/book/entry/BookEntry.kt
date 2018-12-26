package jasition.matching.domain.book.entry

import jasition.matching.domain.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.trade.event.TradeSideEntry
import java.time.Instant

data class BookEntry(
    val key: BookEntryKey,
    val clientRequestId: ClientRequestId,
    val client: Client,
    val entryType: EntryType,
    val side: Side,
    val timeInForce: TimeInForce,
    val size: EntryQuantity,
    val status: EntryStatus
) {
    constructor(
        price: Price?,
        whenSubmitted: Instant,
        eventId: EventId,
        clientRequestId: ClientRequestId,
        client: Client,
        entryType: EntryType,
        side: Side,
        timeInForce: TimeInForce,
        size: EntryQuantity,
        status: EntryStatus
    ) : this(
        key = BookEntryKey(price = price, whenSubmitted = whenSubmitted, eventId = eventId),
        clientRequestId = clientRequestId,
        client = client,
        entryType = entryType,
        side = side,
        timeInForce = timeInForce,
        size = size,
        status = status
    )

    fun toEntryAddedToBookEvent(bookId: BookId): EntryAddedToBookEvent =
        EntryAddedToBookEvent(
            eventId = key.eventId,
            bookId = bookId,
            entry = this,
            whenHappened = key.whenSubmitted
        )

    fun toTradeSideEntry(tradeSize: Int): TradeSideEntry {
        val newQuantity = size.traded(tradeSize)

        return TradeSideEntry(
            requestId = clientRequestId,
            whoRequested = client,
            entryType = entryType,
            side = side,
            size = newQuantity,
            price = key.price,
            timeInForce = timeInForce,
            whenSubmitted = key.whenSubmitted,
            entryEventId = key.eventId,
            entryStatus = status.traded(newQuantity)
        )
    }

    fun traded(tradeSize: Int): BookEntry {
        val newQuantity = size.traded(tradeSize)

        return copy(
            size = newQuantity,
            status = status.traded(newQuantity)
        )
    }
}

data class BookEntryKey(
    val price: Price?,
    val whenSubmitted: Instant,
    val eventId: EventId
)

class EarliestSubmittedTimeFirst : Comparator<BookEntryKey> {
    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int = o1.whenSubmitted.compareTo(o2.whenSubmitted)
}

class SmallestEventIdFirst : Comparator<BookEntryKey> {
    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int = o1.eventId.compareTo(o2.eventId)
}

class HighestBuyOrLowestSellPriceFirst(val side: Side) : Comparator<BookEntryKey> {
    private val priceComparator = nullsFirst(PriceComparator(side.comparatorMultipler()))

    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int =
        priceComparator.compare(o1.price, o2.price)
}

