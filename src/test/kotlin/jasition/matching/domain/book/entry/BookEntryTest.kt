package jasition.matching.domain.book.entry

import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.beLessThan
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.DescribeSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.trade.event.TradeSideEntry
import java.time.Instant

internal class BookEntryTest : DescribeSpec() {
    init {
        val entry = aBookEntry(
            size = EntryQuantity(availableSize = 23, tradedSize = 2, cancelledSize = 0)
        )
        val bookId = BookId("bookId")
        describe("Book Entry") {
            it("converts to Entry Added to Book Event") {
                entry.toEntryAddedToBookEvent(bookId) shouldBe EntryAddedToBookEvent(
                    eventId = entry.key.eventId,
                    bookId = bookId,
                    entry = entry,
                    whenHappened = entry.key.whenSubmitted
                )
            }
            it("converts to Trade Side Entry") {
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
        val entryKey = BookEntryKey(
            price = Price(10),
            whenSubmitted = Instant.now(),
            eventId = EventId(9)
        )
        val comparator = EarliestSubmittedTimeFirst()
        describe("Earliest Submitted Time First") {
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

internal class SmallestEventIdFirstTest : DescribeSpec() {
    init {
        val entryKey = BookEntryKey(
            price = Price(10),
            whenSubmitted = Instant.now(),
            eventId = EventId(9)
        )
        val comparator = SmallestEventIdFirst()
        describe("Smallest Event ID First") {
            it("evaluates smaller Event ID as before bigger") {
                comparator.compare(
                    entryKey, entryKey.copy(eventId = EventId(8))
                ) should beGreaterThan(0)
            }
            it("evaluates bigger Event ID as after smaller") {
                comparator.compare(
                    entryKey, entryKey.copy(eventId = EventId(10))
                ) should beLessThan(0)
            }
            it("evaluates same Event ID as the same") {
                comparator.compare(
                    entryKey, entryKey.copy(price = Price(15), whenSubmitted = entryKey.whenSubmitted.plusMillis(3))
                ) shouldBe 0
            }
        }
    }
}

internal class HighestBuyOrLowestSellPriceFirstTest : DescribeSpec() {
    init {
        val entryKey = BookEntryKey(
            price = Price(10),
            whenSubmitted = Instant.now(),
            eventId = EventId(9)
        )

        describe("Higher BUY First") {
            val comparator = HighestBuyOrLowestSellPriceFirst(Side.BUY)
            it("evaluates null BUY price as before non-null") {
                comparator.compare(
                    entryKey, entryKey.copy(price = null)
                ) should beGreaterThan(0)
            }
            it("evaluates higher BUY price as before lower") {
                comparator.compare(
                    entryKey, entryKey.copy(price = Price(11))
                ) should beGreaterThan(0)
            }
            it("evaluates lower BUY price as after higher") {
                comparator.compare(
                    entryKey, entryKey.copy(price = Price(9))
                ) should beLessThan(0)
            }
            it("evaluates same BUY prices as the same") {
                comparator.compare(
                    entryKey, entryKey.copy(eventId = EventId(8), whenSubmitted = entryKey.whenSubmitted.plusMillis(3))
                ) shouldBe 0
            }
        }
        describe("Lower SELL First") {
            val comparator = HighestBuyOrLowestSellPriceFirst(Side.SELL)
            it("evaluates null SELL price as before non-null") {
                comparator.compare(
                    entryKey, entryKey.copy(price = null)
                ) should beGreaterThan(0)
            }
            it("evaluates lower SELL price as before higher") {
                comparator.compare(
                    entryKey, entryKey.copy(price = Price(9))
                ) should beGreaterThan(0)
            }
            it("evaluates higher SELL price as after lower") {
                comparator.compare(
                    entryKey, entryKey.copy(price = Price(11))
                ) should beLessThan(0)
            }
            it("evaluates same SELL prices as the same") {
                comparator.compare(
                    entryKey, entryKey.copy(eventId = EventId(8), whenSubmitted = entryKey.whenSubmitted.plusMillis(3))
                ) shouldBe 0
            }
        }
    }
}