package jasition.matching

import jasition.matching.domain.book.Books
import jasition.matching.domain.order.*
import jasition.matching.domain.order.command.PlaceOrderCommand
import jasition.matching.domain.order.validator.PlaceOrderCommandValidator
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import strikt.api.expectThat
import strikt.assertions.isTrue
import java.time.Instant

object PlaceOrderCommandValidatorTest : Spek({
    given("The book is empty") {
        var books = Books(id = "book")
        on("Submit a Limit Good-till-date Order") {
            var command = PlaceOrderCommand(
                    requestId = "req1",
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                    bookId = "book",
                    orderType = OrderType.LIMIT,
                    side = Side.BUY,
                    price = Price(value = 15),
                    size = 10,
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenRequested = Instant.now()
                    )

            var event = PlaceOrderCommandValidator(command = command, books = books)

            it("should place the order on the book") {
                expectThat(event.isRight()).isTrue()

            }
        }
    }
})

