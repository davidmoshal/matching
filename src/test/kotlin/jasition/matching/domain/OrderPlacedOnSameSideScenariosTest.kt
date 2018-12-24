package jasition.matching.domain

import io.kotlintest.shouldBe
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

object OrderPlacedOnSameSideScenariosTest : Spek({
    describe(": Book Entry Priority : Buy Price descending then submitted time ascending then Event ID ascending") {
        given("The book has a Buy Limit GTC order 4@10") {
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
            val books = Books(BookId("book")).addBookEntry(existingEntry).withEventId(existingEntry.key.eventId)
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
                it("places the entry on the BUY side with expected order data above the existing") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    assertOrderPlacedAndEntryAddedToBookEquals(entryAddedToBookEvent, orderPlacedEvent)

                    result.aggregate.buyLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )
                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(1),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
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
                it("places the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    assertOrderPlacedAndEntryAddedToBookEquals(entryAddedToBookEvent, orderPlacedEvent)

                    result.aggregate.buyLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(1),
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )
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
                it("places the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    assertOrderPlacedAndEntryAddedToBookEquals(entryAddedToBookEvent, orderPlacedEvent)

                    result.aggregate.buyLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(1),
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )
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
                it("places the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    assertOrderPlacedAndEntryAddedToBookEquals(entryAddedToBookEvent, orderPlacedEvent)

                    result.aggregate.buyLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(1),
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )

                }
            }
        }
    }

    describe(": Book Entry Priority : Sell Price ascending then submitted time ascending then Event ID ascending") {
        given("The book has a Sell Limit GTC order 4@10") {
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
            val books = Books(BookId("book")).addBookEntry(existingEntry).withEventId(existingEntry.key.eventId)
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
                it("places the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    assertOrderPlacedAndEntryAddedToBookEquals(entryAddedToBookEvent, orderPlacedEvent)

                    result.aggregate.sellLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(1),
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )
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
                it("places the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    assertOrderPlacedAndEntryAddedToBookEquals(entryAddedToBookEvent, orderPlacedEvent)

                    result.aggregate.sellLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(1),
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )
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
                it("places the entry on the BUY side with expected order data below the existing") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    assertOrderPlacedAndEntryAddedToBookEquals(entryAddedToBookEvent, orderPlacedEvent)

                    result.aggregate.sellLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(1),
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )
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
                it("places the entry on the BUY side with expected order data above the existing") {
                    result.events.size() shouldBe 1
                    assertOrderPlacedAndEntryAddedToBookEquals(result.events.get(0), orderPlacedEvent)

                    result.aggregate.sellLimitBook.entries.size() shouldBe 2

                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(0),
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )
                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(1),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                }
            }
        }
    }
})



