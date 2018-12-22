package jasition.matching.domain

import io.kotlintest.shouldBe
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.play
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

object OrderPlacedOnEmptyBookScenariosTest : Spek({
    context(": Able to place an order on an empty book") {
        given("The book is empty") {
            val books = Books(BookId("book"))
            on("a BUY Limit GTC Order is placed") {
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

                val result = play(event, books)

                it("should contain the order") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 1
                    result.aggregate.sellLimitBook.entries.size() shouldBe 0
                    result.events.size() shouldBe 0

                    val entry = result.aggregate.buyLimitBook.entries.values().get(0)

                    assertEntry(
                        entry = entry,
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
            on("a SELL Limit GTC Order is placed") {
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

                val result = play(event, books)

                it("should contain the order") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                    result.aggregate.sellLimitBook.entries.size() shouldBe 1
                    result.events.size() shouldBe 0

                    val entry = result.aggregate.sellLimitBook.entries.values().get(0)

                    assertEntry(
                        entry = entry,
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

