package jasition.matching.domain.book.entry

import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.beLessThan
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.trade.event.TradeSideEntry
import java.time.Instant

internal class BookEntryTest : StringSpec({
    val entry = aBookEntry(
        size = EntryQuantity(availableSize = 23, tradedSize = 2, cancelledSize = 0)
    )
    val bookId = BookId("bookId")

    "Converts to EntryAddedToBookEvent" {
        entry.toEntryAddedToBookEvent(bookId) shouldBe EntryAddedToBookEvent(
            eventId = entry.key.eventId,
            bookId = bookId,
            entry = entry,
            whenHappened = entry.key.whenSubmitted
        )
    }
    "Converts to TradeSideEntry" {
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
    "Updates sizes and status when traded" {
        entry.traded(23) shouldBe entry.copy(
            size = EntryQuantity(availableSize = 0, tradedSize = 25, cancelledSize = 0),
            status = EntryStatus.FILLED
        )
    }
})

internal class EarliestSubmittedTimeFirstTest : StringSpec({
    val entryKey = BookEntryKey(
        price = Price(10),
        whenSubmitted = Instant.now(),
        eventId = EventId(9)
    )
    val comparator = EarliestSubmittedTimeFirst()

    "Evaluates earlier submitted time as before later" {
        comparator.compare(
            entryKey, entryKey.copy(whenSubmitted = entryKey.whenSubmitted.minusMillis(3))
        ) should beGreaterThan(0)
    }
    "Evaluates later submitted time as after earlier" {
        comparator.compare(
            entryKey, entryKey.copy(whenSubmitted = entryKey.whenSubmitted.plusMillis(3))
        ) should beLessThan(0)
    }
    "Evaluates same submitted time as the same" {
        comparator.compare(
            entryKey, entryKey.copy(price = Price(15), eventId = EventId(22))
        ) shouldBe 0
    }
})

internal class SmallestEventIdFirstTest : StringSpec({
    val entryKey = BookEntryKey(
        price = Price(10),
        whenSubmitted = Instant.now(),
        eventId = EventId(9)
    )
    val comparator = SmallestEventIdFirst()

    "Evaluates smaller Event ID as before bigger" {
        comparator.compare(
            entryKey, entryKey.copy(eventId = EventId(8))
        ) should beGreaterThan(0)
    }
    "Evaluates bigger Event ID as after smaller" {
        comparator.compare(
            entryKey, entryKey.copy(eventId = EventId(10))
        ) should beLessThan(0)
    }
    "Evaluates same Event ID as the same" {
        comparator.compare(
            entryKey, entryKey.copy(price = Price(15), whenSubmitted = entryKey.whenSubmitted.plusMillis(3))
        ) shouldBe 0
    }
})

internal class HighestBuyOrLowestSellPriceFirstTest : StringSpec({
    val entryKey = BookEntryKey(
        price = Price(10),
        whenSubmitted = Instant.now(),
        eventId = EventId(9)
    )
    "Evaluates null BUY price as before non-null" {
        HighestBuyOrLowestSellPriceFirst(Side.BUY).compare(
            entryKey, entryKey.copy(price = null)
        ) should beGreaterThan(0)
    }
    "Evaluates higher BUY price as before lower" {
        HighestBuyOrLowestSellPriceFirst(Side.BUY).compare(
            entryKey, entryKey.copy(price = Price(11))
        ) should beGreaterThan(0)
    }
    "Evaluates lower BUY price as after higher" {
        HighestBuyOrLowestSellPriceFirst(Side.BUY).compare(
            entryKey, entryKey.copy(price = Price(9))
        ) should beLessThan(0)
    }
    "Evaluates same BUY prices as the same" {
        HighestBuyOrLowestSellPriceFirst(Side.BUY).compare(
            entryKey, entryKey.copy(eventId = EventId(8), whenSubmitted = entryKey.whenSubmitted.plusMillis(3))
        ) shouldBe 0
    }
    "Evaluates null SELL price as before non-null" {
        HighestBuyOrLowestSellPriceFirst(Side.SELL).compare(
            entryKey, entryKey.copy(price = null)
        ) should beGreaterThan(0)
    }
    "Evaluates lower SELL price as before higher" {
        HighestBuyOrLowestSellPriceFirst(Side.SELL).compare(
            entryKey, entryKey.copy(price = Price(9))
        ) should beGreaterThan(0)
    }
    "Evaluates higher SELL price as after lower" {
        HighestBuyOrLowestSellPriceFirst(Side.SELL).compare(
            entryKey, entryKey.copy(price = Price(11))
        ) should beLessThan(0)
    }
    "Evaluates same SELL prices as the same" {
        HighestBuyOrLowestSellPriceFirst(Side.SELL).compare(
            entryKey, entryKey.copy(eventId = EventId(8), whenSubmitted = entryKey.whenSubmitted.plusMillis(3))
        ) shouldBe 0
    }
})