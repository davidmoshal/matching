package jasition.matching.domain.order.validator

import io.kotlintest.shouldBe
import jasition.matching.domain.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.playOrderPlacedEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

object PlayOrderPlacedEventTest : Spek({
    given("The book is empty") {
        var books = Books(BookId(bookId = "book"))
        on("a BUY Limit GTC Order is placed") {
            var event = OrderPlacedEvent(
                requestId = ClientRequestId(current = "req1"),
                whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                bookId = BookId("book"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(15),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenHappened = Instant.now(),
                eventId = EventId(1),
                size = EntryQuantity(
                    availableSize = 10,
                    tradedSize = 0,
                    cancelledSize = 0
                )
            )

            val results = playOrderPlacedEvent(event, books)

            it("should contain the order") {
                results.b.buyLimitBook.entries.size() shouldBe 1
                results.b.sellLimitBook.entries.size() shouldBe 0

                val entry = results.b.buyLimitBook.entries.values().get(0)

                shouldHaveCorrectContent(entry, event)
            }
        }
        on("a SELL Limit GTC Order is placed") {
            var event = OrderPlacedEvent(
                requestId = ClientRequestId(current = "req1"),
                whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                bookId = BookId("book"),
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(15),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenHappened = Instant.now(),
                eventId = EventId(1),
                size = EntryQuantity(
                    availableSize = 10,
                    tradedSize = 0,
                    cancelledSize = 0
                )
            )

            val results = playOrderPlacedEvent(event, books)

            it("should contain the order") {
                results.b.buyLimitBook.entries.size() shouldBe 0
                results.b.sellLimitBook.entries.size() shouldBe 1

                val entry = results.b.sellLimitBook.entries.values().get(0)

                shouldHaveCorrectContent(entry, event)
            }
        }
    }

    given("The book has a Buy Limit GTC Order 4@10") {
        on("a BUY Limit GTC Order 5@11is placed") {
            it("places the new order") {

            }
        }
    }
})

private fun shouldHaveCorrectContent(
    entry: BookEntry,
    event: OrderPlacedEvent
) {
    entry.clientRequestId shouldBe event.requestId
    entry.client shouldBe event.whoRequested
    entry.key.price shouldBe event.price
    entry.key.whenSubmitted shouldBe event.whenHappened
    entry.key.eventId shouldBe event.eventId
    entry.entryType shouldBe event.entryType
    entry.side shouldBe event.side
    entry.size shouldBe event.size
    entry.timeInForce shouldBe event.timeInForce
    entry.status shouldBe EntryStatus.NEW
}

