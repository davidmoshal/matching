package jasition.matching.domain.order.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import java.time.Instant

internal class OrderPlacedEventPropertyTest : StringSpec({
    val eventId = anEventId()
    val bookId = aBookId()
    val event = OrderPlacedEvent(
        requestId = aClientRequestId(),
        whoRequested = aFirmWithClient(),
        bookId = bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = aPrice(),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenHappened = Instant.now(),
        eventId = eventId,
        sizes = anEntrySizes()
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
    "Converts to BookEntry" {
        event.toBookEntry() shouldBe BookEntry(
            price = event.price,
            whenSubmitted = event.whenHappened,
            eventId = eventId,
            requestId = event.requestId,
            whoRequested = event.whoRequested,
            isQuote = false,
            entryType = event.entryType,
            side = event.side,
            timeInForce = event.timeInForce,
            sizes = event.sizes,
            status = event.status
        )
    }
})

internal class `Given an order is placed on an empty book` : StringSpec({
    val books = aBooks(aBookId())
    val event = OrderPlacedEvent(
        requestId = aClientRequestId(),
        whoRequested = aFirmWithClient(),
        bookId = books.bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = aPrice(),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenHappened = Instant.now(),
        eventId = anEventId(),
        sizes = anEntrySizes()
    )

    val actual = event.play(books)

    "The opposite-side book is not affected" {
        actual.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "No side-effect has happened" {
        actual.events.size() shouldBe 0
    }
    "The entry exists in the same-side book" {
        actual.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry(event))
    }
})

