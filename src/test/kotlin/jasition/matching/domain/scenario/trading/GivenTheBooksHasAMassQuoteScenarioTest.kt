package jasition.matching.domain.scenario.trading

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntriesRemovedFromBookEvent
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import jasition.matching.domain.trade.event.TradeEvent
import java.time.Instant

internal class `Given the book has quotes of BUY 4 at 9 SELL 4 at 10 and BUY 5 at 8 SELL 5 at 11` : FeatureSpec({
    val quoteEntryModel = "[1 - Quote entry model] "
    val cannotMatchOtherMarketMaker = "[2 -  Cannot match other market maker] "

    val aggressorPartialFilledByQuotes = "[3 -  Aggressor order partial-filled by quotes] "
    val aggressorFilledByQuotes = "[4 -  Aggressor order filled by quotes] "

    val now = Instant.now()
    val bookId = aBookId()
    val originalMassQuotePlacedEvent = MassQuotePlacedEvent(
        bookId = bookId,
        eventId = EventId(1),
        whenHappened = now,
        quoteId = randomId(),
        whoRequested = anotherFirmWithoutClient(),
        quoteModelType = QuoteModelType.QUOTE_ENTRY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        entries = List.of(
            aQuoteEntry(
                bid = PriceWithSize(size = 4, price = Price(9)),
                offer = PriceWithSize(size = 4, price = Price(10))
            ), aQuoteEntry(
                bid = PriceWithSize(size = 5, price = Price(8)),
                offer = PriceWithSize(size = 5, price = Price(11))
            )
        )
    )
    val books = originalMassQuotePlacedEvent.play(aBooks(bookId)).aggregate

    feature(quoteEntryModel) {
        scenario(quoteEntryModel + "When a Mass Quote with Quote Entry mode of the same firm is placed, then all existing quotes of the same firm are cancelled and all new quote entries are added") {
            val massQuotePlacedEvent = MassQuotePlacedEvent(
                bookId = bookId,
                eventId = EventId(6),
                whenHappened = now,
                quoteId = randomId(),
                whoRequested = anotherFirmWithoutClient(),
                quoteModelType = QuoteModelType.QUOTE_ENTRY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                entries = List.of(
                    aQuoteEntry(
                        bid = PriceWithSize(size = 5, price = Price(8)),
                        offer = PriceWithSize(size = 5, price = Price(9))
                    ), aQuoteEntry(
                        bid = PriceWithSize(size = 6, price = Price(7)),
                        offer = PriceWithSize(size = 6, price = Price(10))
                    )
                )
            )
            val result = massQuotePlacedEvent.play(books)

            val expectedBuyEntry1 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(8),
                entry = massQuotePlacedEvent.entries.get(0),
                side = Side.BUY,
                sizes = EntrySizes(5),
                status = EntryStatus.NEW
            )
            val expectedSellEntry1 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(9),
                entry = massQuotePlacedEvent.entries.get(0),
                side = Side.SELL,
                sizes = EntrySizes(5),
                status = EntryStatus.NEW
            )
            val expectedBuyEntry2 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(10),
                entry = massQuotePlacedEvent.entries.get(1),
                side = Side.BUY,
                sizes = EntrySizes(6),
                status = EntryStatus.NEW
            )
            val expectedSellEntry2 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(11),
                entry = massQuotePlacedEvent.entries.get(1),
                side = Side.SELL,
                sizes = EntrySizes(6),
                status = EntryStatus.NEW
            )

            result.events shouldBe List.of(
                EntriesRemovedFromBookEvent(
                    eventId = EventId(7),
                    bookId = bookId,
                    whenHappened = now,
                    entries = List.of(
                        expectedBookEntry(
                            event = originalMassQuotePlacedEvent,
                            eventId = EventId(2),
                            entry = originalMassQuotePlacedEvent.entries.get(0),
                            side = Side.BUY,
                            sizes = EntrySizes(available = 0, traded = 0, cancelled = 4),
                            status = EntryStatus.CANCELLED
                        )
                        ,
                        expectedBookEntry(
                            event = originalMassQuotePlacedEvent,
                            eventId = EventId(4),
                            entry = originalMassQuotePlacedEvent.entries.get(1),
                            side = Side.BUY,
                            sizes = EntrySizes(available = 0, traded = 0, cancelled = 5),
                            status = EntryStatus.CANCELLED
                        )
                        ,
                        expectedBookEntry(
                            event = originalMassQuotePlacedEvent,
                            eventId = EventId(3),
                            entry = originalMassQuotePlacedEvent.entries.get(0),
                            side = Side.SELL,
                            sizes = EntrySizes(available = 0, traded = 0, cancelled = 4),
                            status = EntryStatus.CANCELLED
                        )
                        ,
                        expectedBookEntry(
                            event = originalMassQuotePlacedEvent,
                            eventId = EventId(5),
                            entry = originalMassQuotePlacedEvent.entries.get(1),
                            side = Side.SELL,
                            sizes = EntrySizes(available = 0, traded = 0, cancelled = 5),
                            status = EntryStatus.CANCELLED
                        )
                    )
                ),
                expectedBuyEntry1.toEntryAddedToBookEvent(bookId),
                expectedSellEntry1.toEntryAddedToBookEvent(bookId),
                expectedBuyEntry2.toEntryAddedToBookEvent(bookId),
                expectedSellEntry2.toEntryAddedToBookEvent(bookId)
            )
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBuyEntry1, expectedBuyEntry2)
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedSellEntry1, expectedSellEntry2)
        }
    }

    feature(cannotMatchOtherMarketMaker) {
        scenario(cannotMatchOtherMarketMaker + "When a Mass Quote from another firm is placed, then all existing quotes and new quotes are on the book even prices have crossed") {
            val massQuotePlacedEvent = MassQuotePlacedEvent(
                bookId = bookId,
                eventId = EventId(6),
                whenHappened = now,
                quoteId = randomId(),
                whoRequested = aFirmWithoutClient(),
                quoteModelType = QuoteModelType.QUOTE_ENTRY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                entries = List.of(
                    aQuoteEntry(
                        bid = PriceWithSize(size = 5, price = Price(8)),
                        offer = PriceWithSize(size = 5, price = Price(9))
                    ), aQuoteEntry(
                        bid = PriceWithSize(size = 6, price = Price(7)),
                        offer = PriceWithSize(size = 6, price = Price(10))
                    )
                )
            )
            val result = massQuotePlacedEvent.play(books)

            val expectedBuyEntry1 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(7),
                entry = massQuotePlacedEvent.entries.get(0),
                side = Side.BUY,
                sizes = EntrySizes(5),
                status = EntryStatus.NEW
            )
            val expectedSellEntry1 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(8),
                entry = massQuotePlacedEvent.entries.get(0),
                side = Side.SELL,
                sizes = EntrySizes(5),
                status = EntryStatus.NEW
            )
            val expectedBuyEntry2 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(9),
                entry = massQuotePlacedEvent.entries.get(1),
                side = Side.BUY,
                sizes = EntrySizes(6),
                status = EntryStatus.NEW
            )
            val expectedSellEntry2 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(10),
                entry = massQuotePlacedEvent.entries.get(1),
                side = Side.SELL,
                sizes = EntrySizes(6),
                status = EntryStatus.NEW
            )

            val existingEntries = originalMassQuotePlacedEvent.toBookEntries()
            val existingBuyEntry1 = existingEntries.get(0).withKey(eventId = EventId(2))
            val existingBuyEntry2 = existingEntries.get(2).withKey(eventId = EventId(4))
            val existingSellEntry1 = existingEntries.get(1).withKey(eventId = EventId(3))
            val existingSellEntry2 = existingEntries.get(3).withKey(eventId = EventId(5))

            result.events shouldBe List.of(
                expectedBuyEntry1.toEntryAddedToBookEvent(bookId),
                expectedSellEntry1.toEntryAddedToBookEvent(bookId),
                expectedBuyEntry2.toEntryAddedToBookEvent(bookId),
                expectedSellEntry2.toEntryAddedToBookEvent(bookId)
            )
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingBuyEntry1,
                existingBuyEntry2,
                expectedBuyEntry1,
                expectedBuyEntry2
            )
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
                expectedSellEntry1,
                existingSellEntry1,
                expectedSellEntry2,
                existingSellEntry2
            )
        }
    }
    feature(aggressorPartialFilledByQuotes) {
        scenario(aggressorPartialFilledByQuotes + "When a BUY Limit GTC Order 10 at 11 is placed, then 4 at 10 traded and 5 at 11 traded and a new BUY entry 1 at 11 added and all the SELL entries removed") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(11),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(6),
                sizes = EntrySizes(10)
            )
            val result = orderPlacedEvent.play(books)

            val expectedBuyEntry = expectedBookEntry(
                event = orderPlacedEvent, eventId = EventId(9),
                status = EntryStatus.PARTIAL_FILL,
                sizes = EntrySizes(available = 1, traded = 9, cancelled = 0)
            )

            val existingBuyEntry1 = expectedBookEntry(
                event = originalMassQuotePlacedEvent,
                eventId = EventId(2),
                entry = originalMassQuotePlacedEvent.entries.get(0),
                side = Side.BUY,
                sizes = EntrySizes(4),
                status = EntryStatus.NEW
            )
            val existingBuyEntry2 = expectedBookEntry(
                event = originalMassQuotePlacedEvent,
                eventId = EventId(4),
                entry = originalMassQuotePlacedEvent.entries.get(1),
                side = Side.BUY,
                sizes = EntrySizes(5),
                status = EntryStatus.NEW
            )

            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                expectedBuyEntry,
                existingBuyEntry1,
                existingBuyEntry2
            )

            result.aggregate.sellLimitBook.entries.size() shouldBe 0

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(7),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        eventId = EventId(6),
                        sizes = EntrySizes(available = 6, traded = 4, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        event = originalMassQuotePlacedEvent,
                        entry = originalMassQuotePlacedEvent.entries.get(0),
                        eventId = EventId(3),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                ),
                TradeEvent(
                    eventId = EventId(8),
                    bookId = bookId,
                    size = 5,
                    price = Price(11),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        eventId = EventId(6),
                        sizes = EntrySizes(available = 1, traded = 9, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        event = originalMassQuotePlacedEvent,
                        eventId = EventId(5),
                        entry = originalMassQuotePlacedEvent.entries.get(1),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 5, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                ),
                expectedBuyEntry.toEntryAddedToBookEvent(bookId = bookId, eventId = EventId(9))
            )
        }
    }
    feature(aggressorFilledByQuotes) {
        scenario(aggressorFilledByQuotes + "When a BUY Limit GTC Order 9 at 11 is placed, then 4 at 10 traded and 5 at 11 traded and the SELL entry 1 at 11 remains") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(11),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = now,
                eventId = EventId(6),
                sizes = EntrySizes(9)
            )
            val result = orderPlacedEvent.play(books)

            val existingBuyEntry1 = expectedBookEntry(
                event = originalMassQuotePlacedEvent,
                eventId = EventId(2),
                entry = originalMassQuotePlacedEvent.entries.get(0),
                side = Side.BUY,
                sizes = EntrySizes(4),
                status = EntryStatus.NEW
            )
            val existingBuyEntry2 = expectedBookEntry(
                event = originalMassQuotePlacedEvent,
                eventId = EventId(4),
                entry = originalMassQuotePlacedEvent.entries.get(1),
                side = Side.BUY,
                sizes = EntrySizes(5),
                status = EntryStatus.NEW
            )

            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
                existingBuyEntry1,
                existingBuyEntry2
            )

            result.aggregate.sellLimitBook.entries.size() shouldBe 0

            result.events shouldBe List.of(
                TradeEvent(
                    eventId = EventId(7),
                    bookId = bookId,
                    size = 4,
                    price = Price(10),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        eventId = EventId(6),
                        sizes = EntrySizes(available = 5, traded = 4, cancelled = 0),
                        status = EntryStatus.PARTIAL_FILL
                    ),
                    passive = expectedTradeSideEntry(
                        event = originalMassQuotePlacedEvent,
                        entry = originalMassQuotePlacedEvent.entries.get(0),
                        eventId = EventId(3),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 4, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                ),
                TradeEvent(
                    eventId = EventId(8),
                    bookId = bookId,
                    size = 5,
                    price = Price(11),
                    whenHappened = now,
                    aggressor = expectedTradeSideEntry(
                        event = orderPlacedEvent,
                        eventId = EventId(6),
                        sizes = EntrySizes(available = 0, traded = 9, cancelled = 0),
                        status = EntryStatus.FILLED
                    ),
                    passive = expectedTradeSideEntry(
                        event = originalMassQuotePlacedEvent,
                        eventId = EventId(5),
                        entry = originalMassQuotePlacedEvent.entries.get(1),
                        side = Side.SELL,
                        sizes = EntrySizes(available = 0, traded = 5, cancelled = 0),
                        status = EntryStatus.FILLED
                    )
                )
            )
        }
    }
})

