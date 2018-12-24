package jasition.matching.domain

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.EntryStatus
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent

fun assertEntry(
    entry: BookEntry,
    clientRequestId: ClientRequestId,
    availableSize: Int,
    tradedSize: Int = 0,
    cancelledSize: Int = 0,
    price: Price?,
    client: Client,
    status: EntryStatus = EntryStatus.NEW
) {
    entry.clientRequestId shouldBe clientRequestId
    entry.size.availableSize shouldBe availableSize
    entry.size.tradedSize shouldBe tradedSize
    entry.size.cancelledSize shouldBe cancelledSize
    entry.key.price shouldBe price
    entry.client shouldBe client
    entry.status shouldBe status
}

fun assertOrderPlacedAndEntryAddedToBookEquals(
    entryAddedToBookEvent: Event?,
    event: OrderPlacedEvent
) {
    entryAddedToBookEvent should beOfType<EntryAddedToBookEvent>()
    if (entryAddedToBookEvent is EntryAddedToBookEvent) {

        assertEntry(
            entry = entryAddedToBookEvent.entry,
            clientRequestId = event.requestId,
            availableSize = event.size.availableSize,
            price = event.price,
            client = event.whoRequested
        )
    }
}