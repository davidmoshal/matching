package jasition.matching.domain.quote.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.SizeAtPrice
import jasition.matching.domain.book.entry.TimeInForce
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
                bid = SizeAtPrice(size = 4, price = Price(9)),
                offer = SizeAtPrice(size = 4, price = Price(10))
            ), aQuoteEntry(
                quoteEntryId = "qe2",
                bid = SizeAtPrice(size = 5, price = Price(8)),
                offer = SizeAtPrice(size = 5, price = Price(11))
            )
        )
    )
    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
    "Converts to BookEntries" {
        event.toBookEntries() shouldBe List.of(
            expectedBookEntry(quoteEntry = event.entries.get(0), side = Side.BUY, event = event),
            expectedBookEntry(quoteEntry = event.entries.get(0), side = Side.SELL, event = event),
            expectedBookEntry(quoteEntry = event.entries.get(1), side = Side.BUY, event = event),
            expectedBookEntry(quoteEntry = event.entries.get(1), side = Side.SELL, event = event)
        )
    }
})

