package jasition.matching.domain.quote.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntriesRemovedFromBookEvent
import jasition.matching.domain.quote.QuoteModelType
import java.time.Instant

internal class MassQuotePlacedEventPropertyTest : StringSpec({
    val bookId = aBookId()
    val eventId = EventId(1)
    val event = MassQuotePlacedEvent(
        bookId = bookId,
        eventId = eventId,
        whenHappened = Instant.now(),
        quoteId = randomId(),
        whoRequested = anotherFirmWithoutClient(),
        quoteModelType = QuoteModelType.QUOTE_ENTRY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        entries = List.of(
            aQuoteEntry(
                quoteEntryId = "qe1",
                bid = PriceWithSize(size = 4, price = Price(9)),
                offer = PriceWithSize(size = 4, price = Price(10))
            ), aQuoteEntry(
                quoteEntryId = "qe2",
                bid = PriceWithSize(size = 5, price = Price(8)),
                offer = PriceWithSize(size = 5, price = Price(11))
            )
        )
    )
    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
    "Is a Primary event" {
        event.isPrimary() shouldBe true
    }
    "Converts to BookEntries" {
        event.toBookEntries() shouldBe List.of(
            expectedBookEntry(entry = event.entries.get(0), side = Side.BUY, event = event),
            expectedBookEntry(entry = event.entries.get(0), side = Side.SELL, event = event),
            expectedBookEntry(entry = event.entries.get(1), side = Side.BUY, event = event),
            expectedBookEntry(entry = event.entries.get(1), side = Side.SELL, event = event)
        )
    }
})

internal class `Given a mass quote is placed on an empty book` : StringSpec({
    val books = aBooks(aBookId())
    val event = MassQuotePlacedEvent(
        bookId = books.bookId,
        eventId = EventId(1),
        whenHappened = Instant.now(),
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
    val result = event.play(books)
    val expectedBidEntry1 = expectedBookEntry(
        event = event,
        eventId = EventId(2),
        entry = event.entries.get(0),
        side = Side.BUY,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val expectedBidEntry2 = expectedBookEntry(
        event = event,
        eventId = EventId(4),
        entry = event.entries.get(1),
        side = Side.BUY,
        sizes = EntrySizes(5),
        status = EntryStatus.NEW
    )
    val expectedOfferEntry1 = expectedBookEntry(
        event = event,
        eventId = EventId(3),
        entry = event.entries.get(0),
        side = Side.SELL,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val expectedOfferEntry2 = expectedBookEntry(
        event = event,
        eventId = EventId(5),
        entry = event.entries.get(1),
        side = Side.SELL,
        sizes = EntrySizes(5),
        status = EntryStatus.NEW
    )
    "Then the BID entries exist in the BUY book" {
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
            expectedBidEntry1,
            expectedBidEntry2
        )
    }
    "Then the OFFER entries exist in the SELL book" {
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
            expectedOfferEntry1,
            expectedOfferEntry2
        )
    }
    "Then all entries were added to the books" {
        result.events shouldBe List.of(
            expectedBidEntry1.toEntryAddedToBookEvent(books.bookId),
            expectedOfferEntry1.toEntryAddedToBookEvent(books.bookId),
            expectedBidEntry2.toEntryAddedToBookEvent(books.bookId),
            expectedOfferEntry2.toEntryAddedToBookEvent(books.bookId)
        )
    }
})

internal class `Given a mass quote is placed on a book with existing entries` : StringSpec({
    val existingEntry = aBookEntry(
        eventId = EventId(0),
        side = Side.SELL,
        price = Price(Long.MAX_VALUE),
        whoRequested = aFirmWithoutClient(),
        isQuote = false
    )
    val removedEntry = aBookEntry(
        eventId = EventId(0),
        side = Side.SELL,
        whoRequested = aFirmWithoutClient(),
        isQuote = true
    )
    val books = aBooks(aBookId()).addBookEntry(existingEntry).addBookEntry(
        removedEntry
    )
    val event = MassQuotePlacedEvent(
        bookId = books.bookId,
        eventId = EventId(1),
        whenHappened = Instant.now(),
        quoteId = randomId(),
        whoRequested = aFirmWithoutClient(),
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
    val result = event.play(books)
    val expectedBidEntry1 = expectedBookEntry(
        event = event,
        eventId = EventId(3),
        entry = event.entries.get(0),
        side = Side.BUY,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val expectedBidEntry2 = expectedBookEntry(
        event = event,
        eventId = EventId(5),
        entry = event.entries.get(1),
        side = Side.BUY,
        sizes = EntrySizes(5),
        status = EntryStatus.NEW
    )
    val expectedOfferEntry1 = expectedBookEntry(
        event = event,
        eventId = EventId(4),
        entry = event.entries.get(0),
        side = Side.SELL,
        sizes = EntrySizes(4),
        status = EntryStatus.NEW
    )
    val expectedOfferEntry2 = expectedBookEntry(
        event = event,
        eventId = EventId(6),
        entry = event.entries.get(1),
        side = Side.SELL,
        sizes = EntrySizes(5),
        status = EntryStatus.NEW
    )
    "Then the BID entries exist in the BUY book" {
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(
            expectedBidEntry1,
            expectedBidEntry2
        )
    }
    "Then the OFFER entries and the previous order exist in the SELL book" {
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(
            expectedOfferEntry1,
            expectedOfferEntry2,
            existingEntry
        )
    }
    "Then all entries from the mass quote were added to the books" {
        result.events shouldBe List.of(
            EntriesRemovedFromBookEvent(
                eventId = EventId(2),
                entries = List.of(
                    removedEntry.copy(
                        status = EntryStatus.CANCELLED,
                        sizes = removedEntry.sizes.copy(
                            available = 0,
                            traded = 0,
                            cancelled = removedEntry.sizes.available
                        )
                    )
                ),
                whenHappened = event.whenHappened,
                bookId = books.bookId
            ),
            expectedBidEntry1.toEntryAddedToBookEvent(books.bookId),
            expectedOfferEntry1.toEntryAddedToBookEvent(books.bookId),
            expectedBidEntry2.toEntryAddedToBookEvent(books.bookId),
            expectedOfferEntry2.toEntryAddedToBookEvent(books.bookId)
        )
    }
})
