package jasition.matching.domain.scenario.trading

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.trade.event.TradeEvent
import java.time.Instant

internal class `Given the book has one BUY Limit GTC Order 5 at 10 and one 3 at 8` : FeatureSpec({
    val higherBuyOverLowerFeature = "[1 - Higher BUY over lower] "
    val stopMatchingWhenPricesDoNotCrossFeature = "[2 - Stop matching when prices do not cross] "
    val aggressorMatchMultiplePassives = "[3 - Aggressor matches multiple passives] "

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
        sizes = EntrySizes(5),
        status = EntryStatus.NEW
    )
    val existingEntry2 = aBookEntry(
        requestId = anotherClientRequestId(),
        whoRequested = anotherFirmWithClient(),
        price = Price(8),
        whenSubmitted = now,
        eventId = EventId(2),
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        sizes = EntrySizes(3),
        status = EntryStatus.NEW
    )
    val bookId = aBookId()
    val books = aBooks(bookId, List.of(existingEntry, existingEntry2))

    feature(higherBuyOverLowerFeature) {
        scenario(higherBuyOverLowerFeature + "When a BUY Limit GTC Order 5 at 9 is placed, then the new entry is between the two existing entries") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(9),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(3),
                sizes = EntrySizes(5)
            )
            val result = orderPlacedEvent.play(books)

            result.events.size() shouldBe 0
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingEntry,
                expectedBookEntry(orderPlacedEvent),
                existingEntry2
            )
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
    }

    feature(stopMatchingWhenPricesDoNotCrossFeature) {
        scenario(stopMatchingWhenPricesDoNotCrossFeature + "When a SELL Limit GTC Order 7 at 9 is placed, then 5 at 10 is traded and the SELL entry 2 at 9 is added and the first BUY entry removed and the second BUY entry remains 3 at 8") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(9),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(3),
                sizes = EntrySizes(7)
            )

            val result = orderPlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(4),
                    bookId = bookId,
                    size = 5,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        sizes = EntrySizes(available = 2, traded = 5, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        bookEntry = existingEntry,
                        sizes = EntrySizes(available = 0, traded = 5, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                )
            )
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(existingEntry2)
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = orderPlacedEvent,
                    status = EntryStatus.PARTIAL_FILL,
                    sizes = EntrySizes(available = 2, traded = 5, cancelled = 0)
                )
            )
        }
    }

    feature(aggressorMatchMultiplePassives) {
        scenario(aggressorMatchMultiplePassives + "When a SELL Limit GTC Order 7 at 8 is placed, then 5 at 10 is traded and 2 at 8 is traded and the first BUY entry removed and the second BUY entry remains 1 at 8") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(8),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(3),
                sizes = EntrySizes(7)
            )

            val result = orderPlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(4),
                    bookId = bookId,
                    size = 5,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        eventId = orderPlacedEvent.eventId,
                        sizes = EntrySizes(available = 2, traded = 5, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        existingEntry,
                        existingEntry.key.eventId,
                        EntrySizes(available = 0, traded = 5, cancelled = 0),
                        EntryStatus.FILLED
                    )
                )
                , TradeEvent(
                    eventId = EventId(5),
                    bookId = bookId,
                    size = 2,
                    price = Price(8),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        eventId = orderPlacedEvent.eventId,
                        sizes = EntrySizes(available = 0, traded = 7, cancelled = 0),
                        status = EntryStatus.FILLED
                    ),
                    passive = expectedTradeSideEntry(
                        existingEntry2,
                        existingEntry2.key.eventId,
                        EntrySizes(available = 1, traded = 2, cancelled = 0),
                        EntryStatus.PARTIAL_FILL
                    )
                )
            )
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingEntry2.copy(
                    status = EntryStatus.PARTIAL_FILL,
                    sizes = EntrySizes(available = 1, traded = 2, cancelled = 0)
                )
            )
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
        scenario(aggressorMatchMultiplePassives + "When a SELL Limit GTC Order 11 at 8 is placed, then 5 at 10 is traded and 3 at 8 is traded and the SELL entry 3 at 8 is added and the all BUY entries removed") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(8),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(3),
                sizes = EntrySizes(11)
            )

            val result = orderPlacedEvent.play(books)

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(4),
                    bookId = bookId,
                    size = 5,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        eventId = orderPlacedEvent.eventId,
                        sizes = EntrySizes(available = 6, traded = 5, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        existingEntry,
                        existingEntry.key.eventId,
                        EntrySizes(available = 0, traded = 5, cancelled = 0),
                        EntryStatus.FILLED
                    )
                ), TradeEvent(
                    eventId = EventId(5),
                    bookId = bookId,
                    size = 3,
                    price = Price(8),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        eventId = orderPlacedEvent.eventId,
                        sizes = EntrySizes(available = 3, traded = 8, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        existingEntry2,
                        existingEntry2.key.eventId,
                        EntrySizes(available = 0, traded = 3, cancelled = 0),
                        EntryStatus.FILLED
                    )
                )
            )
            result.aggregate.buyLimitBook.entries.size() shouldBe 0
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedBookEntry(
                    event = orderPlacedEvent,
                    status = EntryStatus.PARTIAL_FILL,
                    sizes = EntrySizes(available = 3, traded = 8, cancelled = 0)
                )
            )
        }
    }
})

