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
import jasition.matching.domain.order.event.orderPlaced
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

object OrderPlacedOnEmptyBookScenariosTest : Spek({
    describe(": Behaviour : Able to place an order on an empty book") {
        given("The book is empty") {
            val books = Books(BookId("book"))
            on("a BUY Limit GTC order placed") {
                val event = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.BUY,
                    price = Price(15),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(1),
                    size = EntryQuantity(10)
                )
                val result = orderPlaced(event, books)
                it("has no entry on the SELL side") {
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                }
                it("places the entry on the BUY side with expected order data") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    entryAddedToBookEvent should beOfType<EntryAddedToBookEvent>()
                    if (entryAddedToBookEvent is EntryAddedToBookEvent) {

                        assertEntry(
                            entry = entryAddedToBookEvent.entry,
                            clientRequestId = event.requestId,
                            availableSize = event.size.availableSize,
                            price = event.price,
                            client = event.whoRequested
                        )
                    }

                    result.aggregate.buyLimitBook.entries.size() shouldBe 1

                    assertEntry(
                        entry = result.aggregate.buyLimitBook.entries.values().get(0),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
            on("a SELL Limit GTC order placed") {
                val event = OrderPlacedEvent(
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
                val result = orderPlaced(event, books)
                it("has no entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                }
                it("places the entry on the SELL side with expected order data") {
                    result.events.size() shouldBe 1
                    val entryAddedToBookEvent = result.events.get(0)
                    entryAddedToBookEvent should beOfType<EntryAddedToBookEvent>()
                    if (entryAddedToBookEvent is EntryAddedToBookEvent) {

                        assertEntry(
                            entry = entryAddedToBookEvent.entry,
                            clientRequestId = event.requestId,
                            availableSize = event.size.availableSize,
                            price = event.price,
                            client = event.whoRequested
                        )
                    }

                    result.aggregate.sellLimitBook.entries.size() shouldBe 1

                    assertEntry(
                        entry = result.aggregate.sellLimitBook.entries.values().get(0),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
        }
    }
})

