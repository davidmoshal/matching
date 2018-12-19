package jasition.matching.domain.order.validator

import jasition.matching.domain.book.Books
import jasition.matching.domain.order.Client
import jasition.matching.domain.order.OrderType
import jasition.matching.domain.order.Side
import jasition.matching.domain.order.TimeInForce
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
        var books = Books(id = "book")
        on("a BUY Limit GTC Order is placed") {
            var event = OrderPlacedEvent(
                requestId = "req1",
                whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                bookId = "book",
                orderType = OrderType.LIMIT,
                side = Side.BUY,
                price = 15,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenHappened = Instant.now(),
                id = 1,
                availableSize = 10,
                tradedSize = 0,
                cancelledSize = 0
            )

            val results = playOrderPlacedEvent(event, books)

            it("should contain the order") {
                expectThat(results.b.buyLimitBook.entries.size()).isEqualTo(1)
                expectThat(results.b.buyLimitBook.entries.values().get(0).clientEntryId).isEqualTo(event.requestId)
            }
        }
    }
})

