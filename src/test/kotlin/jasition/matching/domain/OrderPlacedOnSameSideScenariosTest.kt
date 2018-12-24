package jasition.matching.domain

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

internal object OrderPlacedOnSameSideScenariosTest : Spek({
    describe(": Book Entry Priority : BUY Price descending then submitted time ascending then Event ID ascending") {
        given("The book has a BUY Limit GTC order 4@10") {
            val existingEntry = BookEntry(
                key = BookEntryKey(
                    price = Price(10),
                    whenSubmitted = Instant.now(),
                    eventId = EventId(1)
                ),
                clientRequestId = ClientRequestId("oldReq1"),
                client = Client(firmId = "firm1", firmClientId = "client1"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                size = EntryQuantity(4),
                status = EntryStatus.NEW
            )
            val bookId = BookId("book")
            val books = existingEntry.toEntryAddedToBookEvent(bookId).play(Books(BookId("book"))).aggregate

            on("a BUY Limit GTC order 5@11 placed") {
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
                it("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                it("adds the entry on the BUY side with expected order data above the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.buyLimitBook.entries.size() shouldBe 2
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe expectedBookEntry(orderPlacedEvent)
                    result.aggregate.buyLimitBook.entries.values().get(1) shouldBe existingEntry

                }
            }
            on("a BUY Limit GTC order 5@10 is placed at a later time") {
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
                it("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                it("adds the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.buyLimitBook.entries.size() shouldBe 2
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.buyLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
            on("a BUY Limit GTC order 5@10 is placed at the same instant") {
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
                it("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                it("adds the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.buyLimitBook.entries.size() shouldBe 2
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.buyLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
            on("a BUY Limit GTC order 5@9 is placed") {
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
                it("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                it("adds the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.buyLimitBook.entries.size() shouldBe 2
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.buyLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
        }
    }

    describe(": Book Entry Priority : SELL Price ascending then submitted time ascending then Event ID ascending") {
        given("The book has a SELL Limit GTC order 4@10") {
            val existingEntry = BookEntry(
                key = BookEntryKey(
                    price = Price(10),
                    whenSubmitted = Instant.now(),
                    eventId = EventId(1)
                ),
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
            on("a SELL Limit GTC order 5@11 is placed") {
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
                it("has no entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                }
                it("adds the entry on the SELL side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.sellLimitBook.entries.size() shouldBe 2
                    result.aggregate.sellLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.sellLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
            on("a SELL Limit GTC order 5@10 is placed at a later time") {
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
                it("has no entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                }
                it("adds the entry on the SELL side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.sellLimitBook.entries.size() shouldBe 2
                    result.aggregate.sellLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.sellLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
            on("a SELL Limit GTC order 5@10 is placed at the same instant") {
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
                it("has no entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                }
                it("adds the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.sellLimitBook.entries.size() shouldBe 2
                    result.aggregate.sellLimitBook.entries.values().get(0) shouldBe existingEntry
                    result.aggregate.sellLimitBook.entries.values().get(1) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
            on("a SELL Limit GTC order 5@9 is placed") {
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
                it("has no entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                }
                it("adds the entry on the BUY side with expected order data above the existing") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.sellLimitBook.entries.size() shouldBe 2
                    result.aggregate.sellLimitBook.entries.values().get(0) shouldBe expectedBookEntry(orderPlacedEvent)
                    result.aggregate.sellLimitBook.entries.values().get(1) shouldBe existingEntry
                }
            }
        }
    }
})



