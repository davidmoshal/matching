package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import io.kotlintest.specs.DescribeSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.trade.event.TradeSideEntry
import java.time.Instant

internal class BookEntryTest : DescribeSpec() {
    init {
        describe("Book Entry") {
            it("converts to Entry Added to Book Event") {
                val entry = BookEntry(
                    key = BookEntryKey(price = Price(10), whenSubmitted = Instant.now(), eventId = EventId(1)),
                    clientRequestId = ClientRequestId("req"),
                    client = Client(firmId = "firm", firmClientId = "firmClientId"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    size = EntryQuantity(availableSize = 23, tradedSize = 2, cancelledSize = 0),
                    status = EntryStatus.PARTIAL_FILL
                )

                val bookId = BookId("bookId")

                entry.toEntryAddedToBookEvent(bookId) shouldBe EntryAddedToBookEvent(
                    eventId = entry.key.eventId,
                    bookId = bookId,
                    entry = entry,
                    whenHappened = entry.key.whenSubmitted
                )
            }
            it("converts to Trade Side Entry") {
                val entry = BookEntry(
                    key = BookEntryKey(price = Price(10), whenSubmitted = Instant.now(), eventId = EventId(1)),
                    clientRequestId = ClientRequestId("req"),
                    client = Client(firmId = "firm", firmClientId = "firmClientId"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    size = EntryQuantity(availableSize = 23, tradedSize = 2, cancelledSize = 0),
                    status = EntryStatus.PARTIAL_FILL
                )

                entry.toTradeSideEntry(8) shouldBe TradeSideEntry(
                    requestId = entry.clientRequestId,
                    whoRequested = entry.client,
                    entryType = entry.entryType,
                    side = entry.side,
                    size = EntryQuantity(availableSize = 15, tradedSize = 10, cancelledSize = 0),
                    price = entry.key.price,
                    timeInForce = entry.timeInForce,
                    whenSubmitted = entry.key.whenSubmitted,
                    entryEventId = entry.key.eventId,
                    entryStatus = EntryStatus.PARTIAL_FILL
                )
            }
        }
    }
}