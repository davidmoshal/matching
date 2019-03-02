package jasition.matching.domain.trade.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.cqrs.EventId
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

internal class TradeEventPropertyTest : StringSpec({
    val eventId = EventId(3)
    val bookId = aBookId()
    val event = TradeEvent(
        bookId = bookId,
        whenHappened = Instant.now(),
        eventId = eventId,
        size = 10,
        price = Price(20),
        aggressor = TradeSideEntry(
            requestId = aClientRequestId(),
            whoRequested = aFirmWithClient(),
            isQuote = false,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            sizes = anEntrySizes(),
            price = aPrice(),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            status = EntryStatus.PARTIAL_FILL,
            eventId = EventId(2),
            whenSubmitted = Instant.now()
        ),
        passive = TradeSideEntry(
            requestId = anotherClientRequestId(),
            whoRequested = anotherFirmWithClient(),
            isQuote = true,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            sizes = anEntrySizes(),
            price = aPrice(),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            status = EntryStatus.FILLED,
            eventId = EventId(1),
            whenSubmitted = Instant.now()
        )
    )
    "Has Book ID as Aggregate ID" {
        event.aggregateId() shouldBe bookId
    }
    "Has Event ID as Event ID" {
        event.eventId() shouldBe eventId
    }
})

internal class TradeSideEntryTest : StringSpec({
    "Converts to BookEntryKey" {
        val entry = TradeSideEntry(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            isQuote = false,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            sizes = EntrySizes(10, traded = 10, cancelled = 0),
            price = Price(20),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            status = EntryStatus.PARTIAL_FILL,
            eventId = EventId(2),
            whenSubmitted = Instant.now()
        )

        entry.toBookEntryKey() shouldBe BookEntryKey(
            price = entry.price,
            whenSubmitted = entry.whenSubmitted,
            eventId = entry.eventId
        )
    }
})
