package jasition.matching.domain.order.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import java.time.Instant

internal class OrderRejectedEventPropertyTest : StringSpec({
    val eventId = anEventId()
    val bookId = aBookId()
    val event = OrderRejectedEvent(
        requestId = aClientRequestId(),
        whoRequested = aFirmWithClient(),
        bookId = bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = aPrice(),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenHappened = Instant.now(),
        eventId = eventId,
        size = 10,
        rejectReason = OrderRejectReason.BROKER_EXCHANGE_OPTION,
        rejectText = "Not allowed"
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

internal class `Given an order is rejected` : StringSpec({
    val eventId = EventId(1)
    val bookId = aBookId()
    val books = aBooks(bookId)
    val event = OrderRejectedEvent(
        requestId = aClientRequestId(),
        whoRequested = aFirmWithClient(),
        bookId = bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = aPrice(),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenHappened = Instant.now(),
        eventId = eventId,
        size = 10,
        rejectReason = OrderRejectReason.BROKER_EXCHANGE_OPTION,
        rejectText = "Not allowed"
    )

    val actual = event.play(books)

    "The books only have the last event ID updated" {
        actual.aggregate shouldBe books.copy(lastEventId = eventId)
    }
    "There is no side effect" {
        actual.events shouldBe List.empty()
    }
})