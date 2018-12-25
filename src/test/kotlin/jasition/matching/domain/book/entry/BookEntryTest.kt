package jasition.matching.domain.book.entry

import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.beLessThan
import io.kotlintest.should
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
            it("updates sizes and status when traded") {
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

                entry.traded(23) shouldBe entry.copy(
                    size = EntryQuantity(availableSize = 0, tradedSize = 25, cancelledSize = 0),
                    status = EntryStatus.FILLED
                )
            }
        }
    }
}

internal class EarliestSubmittedTimeFirstTest : DescribeSpec() {
    init {
        describe("Earliest Submitted Time First") {
            val comparator = EarliestSubmittedTimeFirst()
            val entryKey = BookEntryKey(
                price = Price(10),
                whenSubmitted = Instant.now(),
                eventId = EventId(9)
            )
            it("evaluate earlier submitted time as before later") {
                comparator.compare(
                    entryKey, entryKey.copy(whenSubmitted = entryKey.whenSubmitted.minusMillis(3))
                ) should beGreaterThan(0)
            }
            it("evaluate later submitted time as after earlier") {
                comparator.compare(
                    entryKey, entryKey.copy(whenSubmitted = entryKey.whenSubmitted.plusMillis(3))
                ) should beLessThan(0)
            }
            it("evaluate same submitted time as the same") {
                comparator.compare(
                    entryKey, entryKey.copy(price = Price(15), eventId = EventId(22))
                ) shouldBe 0
            }
        }
    }
}