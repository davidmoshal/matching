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

internal class `Given the book has a SELL Limit GTC Order 4 at 10` : StringSpec({
    val existingEntry = BookEntry(
        price = Price(10),
        whenSubmitted = Instant.now(),
        eventId = EventId(1),
        clientRequestId = ClientRequestId("oldReq1"),
        client = Client(firmId = "firm1", firmClientId = "client1"),
        entryType = EntryType.LIMIT,
        side = Side.SELL,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        size = EntryQuantity(4),
        status = EntryStatus.NEW
    )
    val bookId = BookId("book")
    val books = existingEntry.toEntryAddedToBookEvent(bookId).play(Books(BookId("book"))).aggregate
    "When a SELL Limit GTC order 5 at 11 is placed, then the new entry is added below the existing" {
        val orderPlacedEvent = OrderPlacedEvent(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            bookId = BookId("book"),
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(11),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = Instant.now(),
            eventId = EventId(2),
            size = EntryQuantity(5)
        )
        val result = orderPlacedEvent.play(books)
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.events.size() shouldBe 1
        result.events.get(0) should beOfType<EntryAddedToBookEvent>()
        result.aggregate.sellLimitBook.entries.size() shouldBe 2
        result.aggregate.sellLimitBook.entries.values().get(0) shouldBe existingEntry
        result.aggregate.sellLimitBook.entries.values().get(1) shouldBe expectedBookEntry(
            orderPlacedEvent
        )
    }
    "When a SELL Limit GTC order 5 at 10 is placed at a later time, then the new entry is added below the existing" {
        val orderPlacedEvent = OrderPlacedEvent(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            bookId = BookId("book"),
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = Instant.now().plusMillis(1),
            eventId = EventId(2),
            size = EntryQuantity(5)
        )
        val result = orderPlacedEvent.play(books)
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.events.size() shouldBe 1
        result.events.get(0) should beOfType<EntryAddedToBookEvent>()
        result.aggregate.sellLimitBook.entries.size() shouldBe 2
        result.aggregate.sellLimitBook.entries.values().get(0) shouldBe existingEntry
        result.aggregate.sellLimitBook.entries.values().get(1) shouldBe expectedBookEntry(
            orderPlacedEvent
        )
    }
    "When a SELL Limit GTC order 5 at 10 is placed at the same instant, then the new entry is added below the existing" {
        val orderPlacedEvent = OrderPlacedEvent(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            bookId = BookId("book"),
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(10),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = existingEntry.key.whenSubmitted,
            eventId = EventId(2),
            size = EntryQuantity(5)
        )
        val result = orderPlacedEvent.play(books)
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.events.size() shouldBe 1
        result.events.get(0) should beOfType<EntryAddedToBookEvent>()
        result.aggregate.sellLimitBook.entries.size() shouldBe 2
        result.aggregate.sellLimitBook.entries.values().get(0) shouldBe existingEntry
        result.aggregate.sellLimitBook.entries.values().get(1) shouldBe expectedBookEntry(
            orderPlacedEvent
        )
    }
    "When a SELL Limit GTC order 5 at 9 is placed, then the new entry is added above the existing" {
        val orderPlacedEvent = OrderPlacedEvent(
            requestId = ClientRequestId("req1"),
            whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
            bookId = BookId("book"),
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            price = Price(9),
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            whenHappened = Instant.now(),
            eventId = EventId(2),
            size = EntryQuantity(5)
        )
        val result = orderPlacedEvent.play(books)
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.events.size() shouldBe 1
        result.events.get(0) should beOfType<EntryAddedToBookEvent>()
        result.aggregate.sellLimitBook.entries.size() shouldBe 2
        result.aggregate.sellLimitBook.entries.values().get(0) shouldBe expectedBookEntry(
            orderPlacedEvent
        )
        result.aggregate.sellLimitBook.entries.values().get(1) shouldBe existingEntry
    }
})


