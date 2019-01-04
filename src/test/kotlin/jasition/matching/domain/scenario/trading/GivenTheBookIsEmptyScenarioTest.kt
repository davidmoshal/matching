package jasition.matching.domain.scenario.trading

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.quote.QuoteModelType
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import java.time.Instant

internal class `Given the book is empty` : FeatureSpec({
    val addOrderToEmptyBookFeature = "[1 - Add order to empty book] "
    val addQuotesToEmptyBookFeature = "[2 - Add mass quote to empty book] "

    val bookId = aBookId()
    val books = aBooks(bookId)

    feature(addOrderToEmptyBookFeature) {
        scenario(addOrderToEmptyBookFeature + "When a BUY Limit GTC Order is placed, then the BUY entry is added") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL
            )

            val result = orderPlacedEvent.play(books)

            val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
            result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry(orderPlacedEvent))
            result.aggregate.sellLimitBook.entries.size() shouldBe 0
        }
        scenario(addOrderToEmptyBookFeature + "When a SELL Limit GTC order is placed, then the new SELL entry is added") {
            val orderPlacedEvent = anOrderPlacedEvent(
                bookId = bookId,
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL
            )
            val result = orderPlacedEvent.play(books)

            val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
            result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
            result.aggregate.buyLimitBook.entries.size() shouldBe 0
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
        }
    }
    feature(addQuotesToEmptyBookFeature) {
        scenario(addQuotesToEmptyBookFeature + "When a Mass Quote of ((BUY 4 at 10 SELL 4 at 11), (BUY 5 at 9 SELL 5 at 12)) is placed, and all quote entries are added") {
            val massQuotePlacedEvent = MassQuotePlacedEvent(
                bookId = bookId,
                eventId = EventId(1),
                whenHappened = Instant.now(),
                quoteId = randomId(),
                whoRequested = aFirmWithoutClient(),
                quoteModelType = QuoteModelType.QUOTE_ENTRY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                entries = List.of(
                    aQuoteEntry(
                        bid = PriceWithSize(size = 4, price = Price(10)),
                        offer = PriceWithSize(size = 4, price = Price(11))
                    ), aQuoteEntry(
                        bid = PriceWithSize(size = 5, price = Price(9)),
                        offer = PriceWithSize(size = 5, price = Price(12))
                    )
                )
            )
            val result = massQuotePlacedEvent.play(books)

            val expectedBuyEntry1 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(2),
                entry = massQuotePlacedEvent.entries.get(0),
                side = Side.BUY,
                sizes = EntrySizes(4),
                status = EntryStatus.NEW
            )
            val expectedSellEntry1 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(3),
                entry = massQuotePlacedEvent.entries.get(0),
                side = Side.SELL,
                sizes = EntrySizes(4),
                status = EntryStatus.NEW
            )
            val expectedBuyEntry2 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(4),
                entry = massQuotePlacedEvent.entries.get(1),
                side = Side.BUY,
                sizes = EntrySizes(5),
                status = EntryStatus.NEW
            )
            val expectedSellEntry2 = expectedBookEntry(
                event = massQuotePlacedEvent,
                eventId = EventId(5),
                entry = massQuotePlacedEvent.entries.get(1),
                side = Side.SELL,
                sizes = EntrySizes(5),
                status = EntryStatus.NEW
            )

            result.events shouldBe List.of(
                expectedBuyEntry1.toEntryAddedToBookEvent(bookId),
                expectedSellEntry1.toEntryAddedToBookEvent(bookId),
                expectedBuyEntry2.toEntryAddedToBookEvent(bookId),
                expectedSellEntry2.toEntryAddedToBookEvent(bookId)
            )
            result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBuyEntry1, expectedBuyEntry2)
            result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedSellEntry1, expectedSellEntry2)
        }
    }
})
