package jasition.matching.domain

import io.kotlintest.shouldBe
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.EntryStatus
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId

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

