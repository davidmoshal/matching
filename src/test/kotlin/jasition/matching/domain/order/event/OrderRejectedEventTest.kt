package jasition.matching.domain.order.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
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
        event.eventType() shouldBe EventType.PRIMARY
    }
})

internal class `Given an order is rejected` : StringSpec({
    val eventId = EventId(1)
    val bookId = BookId("book")
    val books = Books(bookId)
    val event = OrderRejectedEvent(
        requestId = ClientRequestId("req1"),
        whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
        bookId = bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = Price(15),
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenHappened = Instant.now(),
        eventId = eventId,
        size = 10,
        rejectReason = OrderRejectReason.BROKER_EXCHANGE_OPTION,
        rejectText = "Not allowed"
    )

    "The books only have the last event ID updated and no other side effects" {
        event.play(books) shouldBe Transaction<BookId, Books>(
            aggregate = books.copy(lastEventId = eventId),
            events = List.empty()
        )
    }
})