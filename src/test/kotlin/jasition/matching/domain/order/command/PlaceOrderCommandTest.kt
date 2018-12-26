package jasition.matching.domain.order.command

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.OrderRejectReason
import jasition.matching.domain.order.event.OrderRejectedEvent
import java.time.Instant

internal class PlaceOrderCommandTest : FeatureSpec({
    feature("Place Order Command") {
        val books = Books(BookId(bookId = "book"))
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
        scenario("PlaceOrderCommand validated and then OrderPlacedEvent fired") {
            val result = command.validate(books)

            result.isRight() shouldBe true

            result.map { orderPlacedEvent ->
                orderPlacedEvent shouldBe OrderPlacedEvent(
                eventId = books.lastEventId.next(),
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
            )}
        }
        scenario("PlaceOrderCommand failed validation due to negative size and then OrderRejectedEvent fired") {
            val result = command.copy(size = -1).validate(books)

            result.isLeft() shouldBe true
            result.mapLeft { orderRejectedEvent ->
                orderRejectedEvent shouldBe OrderRejectedEvent(
                    eventId = books.lastEventId.next(),
                    requestId = command.requestId,
                    whoRequested = command.whoRequested,
                    bookId = command.bookId,
                    entryType = command.entryType,
                    side = command.side,
                    size = -1,
                    price = command.price,
                    timeInForce = command.timeInForce,
                    whenHappened = command.whenRequested,
                    rejectReason = OrderRejectReason.INCORRECT_QUANTITY,
                    rejectText = "Order size must be positive : -1"
                )
            }
        }
        scenario("PlaceOrderCommand failed validation due to trading status and then OrderRejectedEvent fired") {
            val result = command.validate(books.copy(tradingStatuses = TradingStatuses(TradingStatus.NOT_AVAILABLE_FOR_TRADING)))

            result.isLeft() shouldBe true
            result.mapLeft { orderRejectedEvent ->
                orderRejectedEvent shouldBe OrderRejectedEvent(
                    eventId = books.lastEventId.next(),
                    requestId = command.requestId,
                    whoRequested = command.whoRequested,
                    bookId = command.bookId,
                    entryType = command.entryType,
                    side = command.side,
                    size = command.size,
                    price = command.price,
                    timeInForce = command.timeInForce,
                    whenHappened = command.whenRequested,
                    rejectReason = OrderRejectReason.BROKER_EXCHANGE_OPTION,
                    rejectText = "Placing orders is currently not allowed : NOT_AVAILABLE_FOR_TRADING"
                )
            }
        }
    }
})
