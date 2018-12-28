package jasition.matching.domain.scenario

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.trade.event.TradeEvent
import java.time.Instant

internal class `Given the book has a BUY Limit GTC Order 4 at 10` : StringSpec({
    val now = Instant.now()
    val existingEntry = aBookEntry(
        requestId = anotherClientRequestId(),
        whoRequested = anotherFirmWithClient(),
        price = Price(10),
        whenSubmitted = now,
        eventId = EventId(1),
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val bookId = BookId("book")
    val books = existingEntry.toEntryAddedToBookEvent(bookId).play(Books(BookId("book"))).aggregate

    "[1 - Higher BUY over lower] When a BUY Limit GTC Order 5 at 11 is placed, then the new entry is added above the existing" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(11),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        val result = orderPlacedEvent.play(books)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry, existingEntry)
        result.aggregate.sellLimitBook.entries.size() shouldBe 0

    }
    "[2 - Earlier over later] When a BUY Limit GTC Order 5 at 10 is placed at a later time, then the new entry is added below the existing" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now.plusMillis(1),
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        val result = orderPlacedEvent.play(books)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(existingEntry, expectedBookEntry)
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }

    "[3 - Smaller Event ID over bigger] When a BUY Limit GTC Order 5 at 10 is placed at the same instant, then the new entry is added below the existing" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = existingEntry.key.whenSubmitted,
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        val result = orderPlacedEvent.play(books)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(existingEntry, expectedBookEntry)
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "[1 - Higher BUY over lower] When a BUY Limit GTC Order 5 at 9 is placed, then the new entry is added below the existing" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(9),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        val result = orderPlacedEvent.play(books)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(existingEntry, expectedBookEntry)
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "[4 - Prices do not cross] When a SELL Limit GTC Order 2 at 11 is placed, then the new entry is added to the SELL side" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(11),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(2)
        )
        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        val result = orderPlacedEvent.play(books)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(existingEntry)
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
    }
    "[5 - Aggressor filled passive partial-filled] When a SELL Limit GTC Order 3 at 10 is placed, then 3 at 10 traded and the BUY entry remains 1 at 10" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(3)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = existingEntry.copy(
            status = EntryStatus.PARTIAL_FILL,
            sizes = EntrySizes(available = 1, traded = 3, cancelled = 0)
        )

        result.events shouldBe List.of(
            TradeEvent(
                eventId = EventId(3),
                bookId = bookId,
                size = 3,
                price = Price(10),
                whenHappened = now,
                aggressor = expectedTradeSideEntry(
                    orderPlacedEvent = orderPlacedEvent,
                    eventId = EventId(2),
                    sizes = EntrySizes(available = 0, traded = 3, cancelled = 0),
                    status = EntryStatus.FILLED
                ),
                passive = expectedTradeSideEntry(
                    existingEntry,
                    existingEntry.key.eventId,
                    EntrySizes(available = 1, traded = 3, cancelled = 0),
                    EntryStatus.PARTIAL_FILL
                )
            )
        )

        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "[6 - Aggressor filled passive filled] When a SELL Limit GTC Order 4 at 10 is placed, then 4 at 10 traded and the BUY entry removed" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(4)
        )
        val result = orderPlacedEvent.play(books)

        expectedBookEntry(
            orderPlacedEvent = orderPlacedEvent,
            eventId = EventId(4),
            status = EntryStatus.FILLED,
            sizes = EntrySizes(available = 0, traded = 4, cancelled = 0)
        )

        result.events shouldBe List.of(
            TradeEvent(
                eventId = EventId(3),
                bookId = bookId,
                size = 4,
                price = Price(10),
                whenHappened = now,
                aggressor = expectedTradeSideEntry(
                    orderPlacedEvent = orderPlacedEvent,
                    eventId = EventId(2),
                    sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                    status = EntryStatus.FILLED
                ),
                passive = expectedTradeSideEntry(
                    existingEntry,
                    existingEntry.key.eventId,
                    EntrySizes(available = 0, traded = 4, cancelled = 0),
                    EntryStatus.FILLED
                )
            )
        )

        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "[7 - Aggressor partial-filled passive filled] When a SELL Limit GTC Order 5 at 10 is placed, then 4 at 10 traded and the BUY entry removed and a SELL entry 1 at 10 added" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(
            orderPlacedEvent = orderPlacedEvent,
            eventId = EventId(4),
            status = EntryStatus.PARTIAL_FILL,
            sizes = EntrySizes(available = 1, traded = 4, cancelled = 0)
        )

        result.events shouldBe List.of(
            TradeEvent(
                eventId = EventId(3),
                bookId = bookId,
                size = 4,
                price = Price(10),
                whenHappened = now,
                aggressor = expectedTradeSideEntry(
                    orderPlacedEvent = orderPlacedEvent,
                    eventId = EventId(2),
                    sizes = EntrySizes(available = 1, traded = 4, cancelled = 0),
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

        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
    }
})

