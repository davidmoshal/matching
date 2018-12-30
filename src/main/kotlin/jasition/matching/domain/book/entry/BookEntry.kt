package jasition.matching.domain.book.entry

import jasition.cqrs.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.trade.event.TradeSideEntry
import java.time.Instant

data class BookEntry(
    val key: BookEntryKey,
    val requestId: ClientRequestId,
    val whoRequested: Client,
    val entryType: EntryType,
    val side: Side,
    val timeInForce: TimeInForce,
    val sizes: EntrySizes,
    val status: EntryStatus
) {
    constructor(
        price: Price?,
        whenSubmitted: Instant,
        eventId: EventId,
        requestId: ClientRequestId,
        whoRequested: Client,
        entryType: EntryType,
        side: Side,
        timeInForce: TimeInForce,
        sizes: EntrySizes,
        status: EntryStatus
    ) : this(
        key = BookEntryKey(price = price, whenSubmitted = whenSubmitted, eventId = eventId),
        requestId = requestId,
        whoRequested = whoRequested,
        entryType = entryType,
        side = side,
        timeInForce = timeInForce,
        sizes = sizes,
        status = status
    )

    fun toEntryAddedToBookEvent(bookId: BookId): EntryAddedToBookEvent =
        EntryAddedToBookEvent(
            eventId = key.eventId,
            bookId = bookId,
            entry = this,
            whenHappened = key.whenSubmitted
        )

    fun toEntryAddedToBookEvent(bookId: BookId, eventId: EventId): EntryAddedToBookEvent =
        EntryAddedToBookEvent(
            eventId = eventId,
            bookId = bookId,
            entry = copy(key = key.copy(eventId = eventId)),
            whenHappened = key.whenSubmitted
        )

    fun toTradeSideEntry(): TradeSideEntry = TradeSideEntry(
        requestId = requestId,
        whoRequested = whoRequested,
        entryType = entryType,
        side = side,
        sizes = sizes,
        price = key.price,
        timeInForce = timeInForce,
        whenSubmitted = key.whenSubmitted,
        eventId = key.eventId,
        status = status.traded(sizes)
    )

    fun traded(tradeSize: Int): BookEntry {
        val newSizes = sizes.traded(tradeSize)

        return copy(
            sizes = newSizes,
            status = status.traded(newSizes)
        )
    }
}

data class BookEntryKey(
    val price: Price?,
    val whenSubmitted: Instant,
    val eventId: EventId
)

object EarliestSubmittedTimeFirst : Comparator<BookEntryKey> {
    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int = o1.whenSubmitted.compareTo(o2.whenSubmitted)
}

object SmallestEventIdFirst : Comparator<BookEntryKey> {
    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int = o1.eventId.compareTo(o2.eventId)
}

class HighestBuyOrLowestSellPriceFirst(val side: Side) : Comparator<BookEntryKey> {
    private val priceComparator = nullsFirst(PriceComparator(side.comparatorMultiplier()))

    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int =
        priceComparator.compare(o1.price, o2.price)
}

