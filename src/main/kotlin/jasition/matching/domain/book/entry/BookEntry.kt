package jasition.matching.domain.book.entry

import jasition.matching.domain.EventId
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
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
)

data class BookEntryKey(
    val price: Price?,
    val whenSubmitted: Instant,
    val eventId: EventId
)

