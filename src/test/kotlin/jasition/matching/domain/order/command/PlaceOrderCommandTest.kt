package jasition.matching.domain.order.command

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.*
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.OrderRejectReason
import jasition.matching.domain.order.event.OrderRejectedEvent
import java.time.Instant

internal class `Given there is a request to place an order` : StringSpec({
    val bookId = aBookId()
    val books = aBooks(bookId)
    val command = PlaceOrderCommand(
        requestId = aClientRequestId(),
        whoRequested = aFirmWithClient(),
        bookId = bookId,
        entryType = EntryType.LIMIT,
        side = Side.BUY,
        price = aPrice(),
        size = 10,
        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
        whenRequested = Instant.now()
    )

    "When the request is valid, then the order is placed" {
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
                sizes = EntrySizes(available = command.size, traded = 0, cancelled = 0),
                price = command.price,
                timeInForce = command.timeInForce,
                whenHappened = command.whenRequested,
                status = EntryStatus.NEW
            )
        }
    }

    "When the request uses negative sizes, then the order is rejected" {
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
                sizes = -1,
                price = command.price,
                timeInForce = command.timeInForce,
                whenHappened = command.whenRequested,
                rejectReason = OrderRejectReason.INCORRECT_QUANTITY,
                rejectText = "Order sizes must be positive : -1"
            )
        }
    }
    "When the effective trading status disallows placing order, then the order is rejected" {
        val result =
            command.validate(books.copy(tradingStatuses = TradingStatuses(TradingStatus.NOT_AVAILABLE_FOR_TRADING)))

        result.isLeft() shouldBe true
        result.mapLeft { orderRejectedEvent ->
            orderRejectedEvent shouldBe OrderRejectedEvent(
                eventId = books.lastEventId.next(),
                requestId = command.requestId,
                whoRequested = command.whoRequested,
                bookId = command.bookId,
                entryType = command.entryType,
                side = command.side,
                sizes = command.size,
                price = command.price,
                timeInForce = command.timeInForce,
                whenHappened = command.whenRequested,
                rejectReason = OrderRejectReason.EXCHANGE_CLOSED,
                rejectText = "Placing orders is currently not allowed : NOT_AVAILABLE_FOR_TRADING"
            )
        }
    }
})
