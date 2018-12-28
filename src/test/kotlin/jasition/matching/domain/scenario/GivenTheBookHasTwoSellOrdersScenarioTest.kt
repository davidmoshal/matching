package jasition.matching.domain.scenario

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.*
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.trade.event.TradeEvent
import java.time.Instant

internal class `Given the book has one SELL Limit GTC Order 4 at 8 and one 4 at 10` : StringSpec({
    val now = Instant.now()
    val existingEntry = aBookEntry(
        requestId = anotherClientRequestId(),
        whoRequested = anotherFirmWithClient(),
        price = Price(8),
        whenSubmitted = now,
        eventId = EventId(1),
        entryType = EntryType.LIMIT,
        side = Side.SELL,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val existingEntry2 = aBookEntry(
        requestId = anotherClientRequestId(),
        whoRequested = anotherFirmWithClient(),
        price = Price(10),
        whenSubmitted = now,
        eventId = EventId(2),
        entryType = EntryType.LIMIT,
        side = Side.SELL,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val bookId = aBookId()
    val books = existingEntry2.toEntryAddedToBookEvent(bookId).play(
        existingEntry.toEntryAddedToBookEvent(bookId).play(Books(aBookId())).aggregate
    ).aggregate

    "[1 - Lower SELL over higher] When a SELL Limit GTC Order 5 at 9 is placed, then the new entry is between the two existing entries" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(9),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(3),
            sizes = EntrySizes(5)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
            existingEntry,
            expectedBookEntry,
            existingEntry2
        )
    }
    "[2 - No trade if prices do not cross] When a BUY Limit GTC Order 6 at 9 is placed, then 4 at 8 is traded and the entry 2 at 9 is added to the BUY side and the second SELL entry remains 4 at 10" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(9),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(3),
            sizes = EntrySizes(6)
        )

        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(
            orderPlacedEvent = orderPlacedEvent,
            eventId = EventId(5),
            status = EntryStatus.PARTIAL_FILL,
            sizes = EntrySizes(available = 2, traded = 4, cancelled = 0)
        )

        result.events shouldBe List.of(
            TradeEvent(
                eventId = EventId(4),
                bookId = bookId,
                size = 4,
                price = Price(8),
                whenHappened = now,
                aggressor = expectedTradeSideEntry(
                    orderPlacedEvent = orderPlacedEvent,
                    eventId = orderPlacedEvent.eventId,
                    sizes = EntrySizes(available = 2, traded = 4, cancelled = 0),
                    status = EntryStatus.PARTIAL_FILL
                ),
                passive = expectedTradeSideEntry(
                    existingEntry,
                    existingEntry.key.eventId,
                    EntrySizes(available = 0, traded = 4, cancelled = 0),
                    EntryStatus.FILLED
                )
            )
            , expectedBookEntry.toEntryAddedToBookEvent(bookId)
        )
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntry2)
    }
})

