package jasition.matching.domain.quote.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.quote.QuoteModelType
import java.time.Instant


internal class MassQuoteRejectedEventPropertyTest : StringSpec({
    val bookId = aBookId()
    val eventId = EventId(1)
    val event = MassQuoteRejectedEvent(
        bookId = bookId,
        eventId = eventId,
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
        ),
        quoteRejectReason = QuoteRejectReason.DUPLICATE_QUOTE,
        quoteRejectText = "for some reasons"
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
})


internal class `Given a mass quote is rejected by an empty book` : StringSpec({
    val books = aBooks(aBookId())
    val event = MassQuoteRejectedEvent(
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
        ),
        quoteRejectReason = QuoteRejectReason.EXCHANGE_CLOSED,
        quoteRejectText = "Exchange closed"
    )
    val result = event.play(books)

    "Then the BUY book is still empty" {
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
    }
    "Then the SELL book is still empty" {
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "Then no entries were added" {
        result.events shouldBe List.empty()
    }
})

internal class `Given a mass quote is rejected by a book with existing entries` : StringSpec({
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
    val event = MassQuoteRejectedEvent(
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
        ),
        quoteRejectReason = QuoteRejectReason.EXCHANGE_CLOSED,
        quoteRejectText = "Exchange closed"
    )
    val result = event.play(books)

    "Then the BUY book is still empty" {
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
    }
    "Then all the previous order remains in the SELL book" {
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(existingEntry)
    }
    "Then the existing quote entry is removed and no new entries were added" {
        result.events shouldBe List.of(
            MassQuoteCancelledEvent(
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
                primary = false,
                whoRequested = event.whoRequested,
                whenHappened = event.whenHappened,
                bookId = books.bookId
            )
        )
    }
})
