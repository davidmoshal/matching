package jasition.matching.domain.order.validator

import arrow.core.getOrHandle
import io.kotlintest.shouldBe
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

            var result = validatePlaceOrderCommand(command = command, books = books)

            it("should place the order on the book") {
                result.isRight() shouldBe true

                val event = result.getOrHandle { (d) -> throw IllegalArgumentException(d) }

                event.requestId shouldBe command.requestId
                event.whoRequested shouldBe command.whoRequested
                event.bookId shouldBe command.bookId
                event.entryType shouldBe command.entryType
                event.side shouldBe command.side
                event.price shouldBe command.price
                event.size.availableSize shouldBe command.size
                event.size.tradedSize shouldBe 0
                event.size.cancelledSize shouldBe 0
                event.timeInForce shouldBe command.timeInForce
                (event.whenHappened >= command.whenRequested) shouldBe true
            }
        }
    }
})

