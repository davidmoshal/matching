package jasition.matching.domain.book.entry

import jasition.matching.domain.EventId
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.TradeSideEntry
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

