package jasition.matching.domain.order.command

import arrow.core.getOrHandle
import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import java.time.Instant

internal class PlaceOrderCommandTest : BehaviorSpec(){
    init {
        given("the book is empty") {
            val books = Books(BookId(bookId = "book"))
            `when`("an order submitted") {
                val command = PlaceOrderCommand(
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

                val result = command.validate(books)

                then("places the order on the book") {
                    result.isRight() shouldBe true

                    result.getOrHandle { throw IllegalArgumentException() } shouldBe OrderPlacedEvent(
                        eventId = books.lastEventId + 1,
                        requestId = command.requestId,
                        whoRequested = command.whoRequested,
                        bookId = command.bookId,
                        entryType = command.entryType,
                        side = command.side,
                        size = EntryQuantity(availableSize = command.size, tradedSize = 0, cancelledSize = 0),
                        price = command.price,
                        timeInForce = command.timeInForce,
                        whenHappened = command.whenRequested,
                        entryStatus = EntryStatus.NEW
                    )
                }
            }
        }   
    }
}
