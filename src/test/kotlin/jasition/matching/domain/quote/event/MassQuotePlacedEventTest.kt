package jasition.matching.domain.quote.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.aBookId
import jasition.matching.domain.aQuoteEntry
import jasition.matching.domain.anotherFirmWithoutClient
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.quote.command.QuoteEntry
import jasition.matching.domain.quote.command.QuoteModelType
import jasition.matching.domain.randomId
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
            expectedBookEntry(event.entries.get(0), Side.BUY, event),
            expectedBookEntry(event.entries.get(0), Side.SELL, event),
            expectedBookEntry(event.entries.get(1), Side.BUY, event),
            expectedBookEntry(event.entries.get(1), Side.SELL, event)
        )
    }
})

fun expectedBookEntry(entry: QuoteEntry, side: Side, event: MassQuotePlacedEvent): BookEntry = BookEntry(
    price = side.price(entry),
    whoRequested = event.whoRequested,
    whenSubmitted = event.whenHappened,
    eventId = event.eventId,
    requestId = ClientRequestId(
        current = entry.quoteEntryId,
        collectionId = entry.quoteSetId,
        parentId = event.quoteId
    ),
    isQuote = true,
    entryType = entry.entryType,
    side = side,
    timeInForce = event.timeInForce,
    sizes = EntrySizes(side.size(entry)!!),
    status = EntryStatus.NEW

)
