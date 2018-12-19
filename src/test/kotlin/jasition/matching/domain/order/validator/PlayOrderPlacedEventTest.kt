package jasition.matching.domain.order.validator

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
import strikt.api.expectThat
import strikt.assertions.isEqualTo
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
                expectThat(results.b.buyLimitBook.entries.size()).isEqualTo(1)
                expectThat(results.b.buyLimitBook.entries.values().get(0).clientRequestId).isEqualTo(event.requestId)
                expectThat(results.b.buyLimitBook.entries.values().get(0).client).isEqualTo(event.whoRequested)
                expectThat(results.b.buyLimitBook.entries.values().get(0).key.price).isEqualTo(event.price)
                expectThat(results.b.buyLimitBook.entries.values().get(0).key.whenSubmitted).isEqualTo(event.whenHappened)
                expectThat(results.b.buyLimitBook.entries.values().get(0).key.eventId).isEqualTo(event.eventId)
                expectThat(results.b.buyLimitBook.entries.values().get(0).entryType).isEqualTo(event.entryType)
                expectThat(results.b.buyLimitBook.entries.values().get(0).side).isEqualTo(event.side)
                expectThat(results.b.buyLimitBook.entries.values().get(0).size).isEqualTo(event.size)
                expectThat(results.b.buyLimitBook.entries.values().get(0).timeInForce).isEqualTo(event.timeInForce)
                expectThat(results.b.buyLimitBook.entries.values().get(0).status).isEqualTo(EntryStatus.NEW)
            }
        }
    }
})

