package jasition.matching.domain.scenario

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.*
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.trade.event.TradeEvent
import java.time.Instant

internal class `Given the book has a SELL Limit GTC Order 4 at 10` : StringSpec({
    val now = Instant.now()
    val existingEntry = aBookEntry(
        requestId = anotherClientRequestId(),
        whoRequested = anotherFirmWithClient(),
        price = Price(10),
        whenSubmitted = now,
        eventId = EventId(1),
        entryType = EntryType.LIMIT,
        side = Side.SELL,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val bookId = aBookId()
    val books = existingEntry.toEntryAddedToBookEvent(bookId).play(Books(bookId)).aggregate

    "[1 - Lower SELL over higher] When a SELL Limit GTC Order 5 at 9 is placed, then the new entry is added above the existing" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(9),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry, existingEntry)
    }
    "[1 - Lower SELL over higher] When a SELL Limit GTC Order 5 at 11 is placed, then the new entry is added below the existing" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(11),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntry, expectedBookEntry)
    }
    "[2 - Earlier over later] When a SELL Limit GTC Order 5 at 10 is placed at a later time, then the new entry is added below the existing" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now.plusMillis(1),
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntry, expectedBookEntry)
    }

    "[3 - Smaller Event ID over bigger] When a SELL Limit GTC Order 5 at 10 is placed at the same instant, then the new entry is added below the existing" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = existingEntry.key.whenSubmitted,
            eventId = EventId(2),
            sizes = EntrySizes(5)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntry, expectedBookEntry)
    }
    "[4 - No trade if prices do not cross] When a BUY Limit GTC Order 2 at 9 is placed, then the new entry is added to the BUY side" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(9),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(2)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntry)
    }
    "[5 - No wash trade] When a BUY Limit GTC Order 4 at 10 is placed by the same firm and same firm client, then the new entry is added to the BUY side" {
        val orderPlacedEvent = anOrderPlacedEvent(
            requestId = anotherClientRequestId(),
            whoRequested = anotherFirmWithClient(),
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(4)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntry)
    }
    "[5 - No wash trade] When a BUY Limit GTC Order 4 at 10 is placed by the same firm, one with but another without firm client, then the new entry is added to the BUY side" {
        val orderPlacedEvent = anOrderPlacedEvent(
            requestId = anotherClientRequestId(),
            whoRequested = anotherFirmWithoutClient(),
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(4)
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntry)
    }
    "[5 - No wash trade] When a BUY Limit GTC Order 4 at 10 is placed by the same firm, both without firm client, then the new entry is added to the BUY side" {
        val orderPlacedEvent = anOrderPlacedEvent(
            requestId = anotherClientRequestId(),
            whoRequested = anotherFirmWithoutClient(),
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(4)
        )
        val existingEntryWithoutFirmClient = existingEntry.copy(whoRequested = anotherFirmWithoutClient())
        val result = orderPlacedEvent.play(
            existingEntryWithoutFirmClient
                .toEntryAddedToBookEvent(bookId)
                .play(Books(bookId))
                .aggregate
        )

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntryWithoutFirmClient)
    }
    "[6 - Aggressor takes better execution price] When a BUY Limit GTC Order 4 at 11 is placed, then 4 at 10 traded and the SELL entry removed" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(11),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(4)
        )
        val result = orderPlacedEvent.play(books)

        result.events shouldBe List.of(
            TradeEvent(
                eventId = EventId(3),
                bookId = bookId,
                size = 4,
                price = Price(10),
                whenHappened = now,
                aggressor = expectedTradeSideEntry(
                    orderPlacedEvent = orderPlacedEvent,
                    eventId = orderPlacedEvent.eventId,
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
    "[7 - Aggressor filled passive partial-filled] When a BUY Limit GTC Order 3 at 10 is placed, then 3 at 10 traded and the SELL entry remains 1 at 10" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
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
                    eventId = orderPlacedEvent.eventId,
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
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
    }
    "[8 - Aggressor filled passive filled] When a BUY Limit GTC Order 4 at 10 is placed, then 4 at 10 traded and the SELL entry removed" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = now,
            eventId = EventId(2),
            sizes = EntrySizes(4)
        )
        val result = orderPlacedEvent.play(books)

        result.events shouldBe List.of(
            TradeEvent(
                eventId = EventId(3),
                bookId = bookId,
                size = 4,
                price = Price(10),
                whenHappened = now,
                aggressor = expectedTradeSideEntry(
                    orderPlacedEvent = orderPlacedEvent,
                    eventId = orderPlacedEvent.eventId,
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
    "[9 - Aggressor partial-filled passive filled] When a BUY Limit GTC Order 5 at 10 is placed, then 4 at 10 traded and the SELL entry removed and a BUY entry 1 at 10 added" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
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
                    eventId = orderPlacedEvent.eventId,
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
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
})

