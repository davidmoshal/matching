package jasition.matching.domain.order.event

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.BookEntry
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.book.event.EntryAddedToBookEvent
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
        size = anEntryQuantity()
    )
    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
    "Is a Primary event" {
        event.eventType() shouldBe EventType.PRIMARY
    }
    "Converts to BookEntry" {
        event.toBookEntry() shouldBe BookEntry(
            price = event.price,
            whenSubmitted = event.whenHappened,
            eventId = eventId,
            clientRequestId = event.requestId,
            client = event.whoRequested,
            entryType = event.entryType,
            side = event.side,
            timeInForce = event.timeInForce,
            size = event.size,
            status = event.entryStatus
        )
    }
})

internal class `Given an order is placed on an empty book` : StringSpec({
    val books = Books(BookId("book"))
    val orderPlacedEvent = OrderPlacedEvent(
        requestId = aClientRequestId(),
        whoRequested = aFirmWithClient(),
        bookId = books.bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = aPrice(),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenHappened = Instant.now(),
        eventId = anEventId(),
        size = anEntryQuantity()
    )

    val result = orderPlacedEvent.play(books)

    "The opposite-side book is not affected" {
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "The order is added to the same-side book" {
        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)

        result.events.size() shouldBe 1
        val entryAddedToBookEvent = result.events.get(0)
        entryAddedToBookEvent should beOfType<EntryAddedToBookEvent>()
        if (entryAddedToBookEvent is EntryAddedToBookEvent) {
            entryAddedToBookEvent shouldBe expectedEntryAddedToBookEvent(
                orderPlacedEvent, books, expectedBookEntry
            )
        }

        result.aggregate.buyLimitBook.entries.size() shouldBe 1
        result.aggregate.buyLimitBook.entries.values().get(0) shouldBe expectedBookEntry
    }
})

