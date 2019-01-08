package jasition.matching.domain.quote

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.quote.event.MassQuoteCancelledEvent
import jasition.matching.domain.quote.event.MassQuotePlacedEvent
import java.time.Instant

internal class CancelExistingQuotesTest : StringSpec({
    val event = MassQuotePlacedEvent(
        bookId = aBookId(),
        eventId = EventId(1),
        whenHappened = Instant.now(),
        quoteId = randomId(),
        whoRequested = aFirmWithoutClient(),
        quoteModelType = QuoteModelType.QUOTE_ENTRY,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        entries = List.of(
            aQuoteEntry(
                bid = PriceWithSize(size = 4, price = Price(9)),
                offer = PriceWithSize(size = 5, price = Price(10))
            ), aQuoteEntry(
                bid = PriceWithSize(size = 6, price = Price(8)),
                offer = PriceWithSize(size = 7, price = Price(11))
            )
        )
    )
    val books = event.play(aBooks(aBookId())).aggregate

    "Cancels existing quotes of the same client if there are any" {
        cancelExistingQuotes(
            books = books,
            eventId = event.eventId,
            whoRequested = event.whoRequested,
            whenHappened = event.whenHappened,
            primary = true
        ) shouldBe MassQuoteCancelledEvent(
            eventId = EventId(2),
            bookId = event.bookId,
            entries = List.of(
                expectedBookEntry(
                    event = event, quoteEntry = event.entries.get(0), side = Side.BUY,
                    sizes = EntrySizes(available = 0, traded = 0, cancelled = 4), status = EntryStatus.CANCELLED
                ),
                expectedBookEntry(
                    event = event, quoteEntry = event.entries.get(1), side = Side.BUY,
                    sizes = EntrySizes(available = 0, traded = 0, cancelled = 6), status = EntryStatus.CANCELLED
                ),
                expectedBookEntry(
                    event = event, quoteEntry = event.entries.get(0), side = Side.SELL,
                    sizes = EntrySizes(available = 0, traded = 0, cancelled = 5), status = EntryStatus.CANCELLED
                ),
                expectedBookEntry(
                    event = event, quoteEntry = event.entries.get(1), side = Side.SELL,
                    sizes = EntrySizes(available = 0, traded = 0, cancelled = 7), status = EntryStatus.CANCELLED
                )
            ),
            primary = true,
            whoRequested = event.whoRequested,
            whenHappened = event.whenHappened
        )
    }
    "Does not cancel any order of the same client" {
        cancelExistingQuotes(
            books = books.addBookEntry(aBookEntry(whoRequested = event.whoRequested)),
            eventId = event.eventId,
            whoRequested = anotherFirmWithoutClient(),
            whenHappened = event.whenHappened,
            primary = true
        ) shouldBe null
    }
    "Does not cancel any quotes if there are none" {
        cancelExistingQuotes(
            books = books,
            eventId = event.eventId,
            whoRequested = anotherFirmWithoutClient(),
            whenHappened = event.whenHappened,
            primary = true
        ) shouldBe null
    }
})