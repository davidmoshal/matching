package jasition.matching.domain.order.validator

import arrow.core.getOrHandle
import io.kotlintest.fail
import jasition.matching.domain.book.Books
import jasition.matching.domain.order.Client
import jasition.matching.domain.order.OrderType
import jasition.matching.domain.order.Side
import jasition.matching.domain.order.TimeInForce
import jasition.matching.domain.order.command.PlaceOrderCommand
import jasition.matching.domain.order.command.validatePlaceOrderCommand
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isTrue
import java.time.Instant

object ValidatePlaceOrderCommandTest : Spek({
    given("The book is empty") {
        var books = Books(id = "book")
        on("Submit a Limit Good-till-date Order") {
            var command = PlaceOrderCommand(
                    requestId = "req1",
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = "book",
                    orderType = OrderType.LIMIT,
                    side = Side.BUY,
                    price = 15,
                    size = 10,
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenRequested = Instant.now()
                    )

            var event = validatePlaceOrderCommand(command = command, books = books)

            it("should place the order on the book") {
                expectThat(event.isRight()).isTrue()

                val event = event.getOrHandle { (d) -> fail("Expect OrderPlacedEvent") }

                expectThat(event.requestId).isEqualTo(command.requestId)
                expectThat(event.whoRequested).isEqualTo(command.whoRequested)
                expectThat(event.bookId).isEqualTo(command.bookId)
                expectThat(event.orderType).isEqualTo(command.orderType)
                expectThat(event.side).isEqualTo(command.side)
                expectThat(event.price).isEqualTo(command.price)
                expectThat(event.availableSize).isEqualTo(command.size)
                expectThat(event.tradedSize).isEqualTo(0)
                expectThat(event.cancelledSize).isEqualTo(0)
                expectThat(event.timeInForce).isEqualTo(command.timeInForce)
                expectThat(event.whenHappened).isGreaterThanOrEqualTo(command.whenRequested)
            }
        }
    }
})

