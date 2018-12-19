package jasition.matching.domain.order.validator

import arrow.core.getOrHandle
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
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
        var books = Books(BookId(bookId = "book"))
        on("Submit a Limit Good-till-date Order") {
            var command = PlaceOrderCommand(
                requestId = ClientRequestId(current = "req1"),
                whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                bookId = BookId("book"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(15),
                size = 10,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenRequested = Instant.now()
            )

            var event = validatePlaceOrderCommand(command = command, books = books)

            it("should place the order on the book") {
                expectThat(event.isRight()).isTrue()

                val event = event.getOrHandle { (d) -> throw IllegalArgumentException(d) }

                expectThat(event.requestId).isEqualTo(command.requestId)
                expectThat(event.whoRequested).isEqualTo(command.whoRequested)
                expectThat(event.bookId).isEqualTo(command.bookId)
                expectThat(event.entryType).isEqualTo(command.entryType)
                expectThat(event.side).isEqualTo(command.side)
                expectThat(event.price).isEqualTo(command.price)
                expectThat(event.size.availableSize).isEqualTo(command.size)
                expectThat(event.size.tradedSize).isEqualTo(0)
                expectThat(event.size.cancelledSize).isEqualTo(0)
                expectThat(event.timeInForce).isEqualTo(command.timeInForce)
                expectThat(event.whenHappened).isGreaterThanOrEqualTo(command.whenRequested)
            }
        }
    }
})

