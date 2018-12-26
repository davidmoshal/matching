package jasition.matching.domain.order.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

internal class OrderRejectedEventPropertyTest : StringSpec({
    val eventId = EventId(1)
    val bookId = BookId("book")
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
        size = EntryQuantity(10),
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