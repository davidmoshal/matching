package jasition.matching.domain.scenario

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.expectedBookEntry
import jasition.matching.domain.order.event.OrderPlacedEvent
import java.time.Instant

internal class `Given the book is empty` : StringSpec({
    val books = Books(BookId("book"))
    "When a BUY Limit GTC Order is placed, then the new entry is added to the BUY side" {
        val orderPlacedEvent = OrderPlacedEvent(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            bookId = books.bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            price = Price(15),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = Instant.now(),
            eventId = EventId(1),
            size = EntryQuantity(10)
        )
        val result = orderPlacedEvent.play(books)
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
        result.events.size() shouldBe 1
        result.events.get(0) should beOfType<EntryAddedToBookEvent>()
        result.aggregate.buyLimitBook.entries.size() shouldBe 1
        result.aggregate.buyLimitBook.entries.values().get(0) shouldBe expectedBookEntry(
            orderPlacedEvent
        )
    }
    "When a SELL Limit GTC order is placed, then the new entry is added to the SELL side" {
        val orderPlacedEvent = OrderPlacedEvent(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            bookId = BookId("book"),
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(15),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = Instant.now(),
            eventId = EventId(1),
            size = EntryQuantity(10)
        )
        val result = orderPlacedEvent.play(books)
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.events.size() shouldBe 1
        result.events.get(0) should beOfType<EntryAddedToBookEvent>()
        result.aggregate.sellLimitBook.entries.size() shouldBe 1
        result.aggregate.sellLimitBook.entries.values().get(0) shouldBe expectedBookEntry(
            orderPlacedEvent
        )
    }
})

