package jasition.matching.domain.trade.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

internal class TradeEventPropertyTest : StringSpec({
    val eventId = EventId(3)
    val bookId = BookId("book")
    val event = TradeEvent(
        bookId = bookId,
        whenHappened = Instant.now(),
        eventId = eventId,
        size = 10,
        price = Price(20),
        aggressor = TradeSideEntry(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            size = EntryQuantity(10, tradedSize = 10, cancelledSize = 0),
            price = Price(20),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            entryStatus = EntryStatus.PARTIAL_FILL,
            entryEventId = EventId(2),
            whenSubmitted = Instant.now()
        ),
        passive = TradeSideEntry(
            requestId = ClientRequestId("req2"),
            whoRequested = Client(firmId = "firm2", firmClientId = "client2"),
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            size = EntryQuantity(0, tradedSize = 10, cancelledSize = 0),
            price = Price(20),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            entryStatus = EntryStatus.FILLED,
            entryEventId = EventId(1),
            whenSubmitted = Instant.now()
        )
    )
    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
    "Is a Side-effect event" {
        event.eventType() shouldBe EventType.SIDE_EFFECT
    }
})