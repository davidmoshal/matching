package jasition.matching.domain.order.event

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
        val books = Books(BookId("book"))
        on("an Order is placed") {
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

            val results = playOrderPlacedEvent(event, books)

            it("should contain the order") {
                results.b.buyLimitBook.entries.size() shouldBe 1
                results.b.sellLimitBook.entries.size() shouldBe 0

                val entry = results.b.buyLimitBook.entries.values().get(0)

                entry.clientRequestId shouldBe event.requestId
                entry.client shouldBe event.whoRequested
                entry.entryType shouldBe event.entryType
                entry.side shouldBe event.side
                entry.key.price shouldBe event.price
                entry.timeInForce shouldBe event.timeInForce
                entry.key.whenSubmitted shouldBe event.whenHappened
                entry.key.eventId shouldBe event.eventId
                entry.size.availableSize shouldBe event.size.availableSize
                entry.size.tradedSize shouldBe 0
                entry.size.cancelledSize shouldBe 0
                entry.status shouldBe EntryStatus.NEW
            }
        }
    }
})

