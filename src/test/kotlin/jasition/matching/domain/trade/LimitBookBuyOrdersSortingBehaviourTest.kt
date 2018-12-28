package jasition.matching.domain.trade

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.anotherClientRequestId
import jasition.matching.domain.anotherFirmWithClient
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.expectedBookEntry
import jasition.matching.domain.order.event.OrderPlacedEvent
import java.time.Instant

internal class `Given the book has a BUY Limit GTC Order 4 at 10` : BehaviorSpec() {
    init {
        given("the book has a BUY Limit GTC Order 4@10") {
            val existingEntry = BookEntry(
                price = Price(10),
                whenSubmitted = Instant.now(),
                eventId = EventId(1),
                clientRequestId = anotherClientRequestId(),
                client = anotherFirmWithClient(),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                size = EntryQuantity(4),
                status = EntryStatus.NEW
            )
            val bookId = BookId("book")
            val books = existingEntry.toEntryAddedToBookEvent(bookId).play(Books(BookId("book"))).aggregate

            `when`("a BUY Limit GTC Order 5@11 placed") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.BUY,
                    price = Price(11),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = orderPlacedEvent.play(books)
                then("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                then("adds the entry on the BUY side with expected order data above the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.buyLimitBook.entries.size() shouldBe 2
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe expectedBookEntry(orderPlacedEvent)
                    result.aggregate.buyLimitBook.entries.values().get(1) shouldBe existingEntry

                }
            }
            `when`("a BUY Limit GTC Order 5@10 is placed at a later time") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.BUY,
                    price = Price(10),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now().plusMillis(1),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = orderPlacedEvent.play(books)
                then("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                then("adds the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.buyLimitBook.entries.size() shouldBe 2
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.buyLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
            `when`("a BUY Limit GTC Order 5@10 is placed at the same instant") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.BUY,
                    price = Price(10),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = existingEntry.key.whenSubmitted,
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = orderPlacedEvent.play(books)
                then("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                then("adds the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.buyLimitBook.entries.size() shouldBe 2
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.buyLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
            `when`("a BUY Limit GTC Order 5@9 is placed") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.BUY,
                    price = Price(9),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = orderPlacedEvent.play(books)
                then("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                then("adds the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.buyLimitBook.entries.size() shouldBe 2
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.buyLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
        }
    }
}



