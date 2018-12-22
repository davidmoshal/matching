package jasition.matching.domain.book.entry

import jasition.matching.domain.EventId
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
    val status: EntryStatus = EntryStatus.NEW
) {
    fun toTradeSideEntry(tradeSize: Int) : TradeSideEntry {
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

    fun traded(tradeSize: Int, tradePrice: Price): BookEntry {
        val newQuantity = size.traded(tradeSize)

        return BookEntry(
            key = key,
            clientRequestId = clientRequestId,
            client = client,
            side = side,
            timeInForce = timeInForce,
            entryType = entryType,
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
    private val priceComparator = nullsFirst(PriceComparator())

    override fun compare(o1: BookEntryKey, o2: BookEntryKey): Int =
        side.comparatorMultipler() * priceComparator.compare(o1.price, o2.price)
}

fun entryComparator(side: Side): Comparator<BookEntryKey> {
    return HighestBuyOrLowestSellPriceFirst(side) then EarliestSubmittedTimeFirst() then SmallestEventIdFirst()
}