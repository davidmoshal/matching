package jasition.matching.domain.order.command

import arrow.core.Either
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.OrderRejectReason
import jasition.matching.domain.order.event.OrderRejectedEvent
import java.time.Instant

internal class PlaceOrderCommandTest : StringSpec({
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
        command.validate(books) shouldBe Either.right(
            OrderPlacedEvent(
                eventId = books.lastEventId.inc(),
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
        )
    }
    "When the wrong book ID is used, then the order is rejected" {
        val wrongBookId = "Wrong ID"
        command.copy(bookId = BookId(wrongBookId)).validate(books) shouldBe Either.left(
            OrderRejectedEvent(
                eventId = books.lastEventId.inc(),
                requestId = command.requestId,
                whoRequested = command.whoRequested,
                bookId = BookId(wrongBookId),
                entryType = command.entryType,
                side = command.side,
                size = command.size,
                price = command.price,
                timeInForce = command.timeInForce,
                whenHappened = command.whenRequested,
                rejectReason = OrderRejectReason.UNKNOWN_SYMBOL,
                rejectText = "Unknown book ID : $wrongBookId"
            )
        )
    }
    "When the request uses negative sizes, then the order is rejected" {
        command.copy(size = -1).validate(books) shouldBe Either.left(
            OrderRejectedEvent(
                eventId = books.lastEventId.inc(),
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
                rejectText = "Order sizes must be positive : -1"
            )
        )
    }
    "When the effective trading status disallows placing order, then the order is rejected" {
        command.validate(books.copy(tradingStatuses = TradingStatuses(TradingStatus.NOT_AVAILABLE_FOR_TRADING))) shouldBe Either.left(
            OrderRejectedEvent(
                eventId = books.lastEventId.inc(),
                requestId = command.requestId,
                whoRequested = command.whoRequested,
                bookId = command.bookId,
                entryType = command.entryType,
                side = command.side,
                size = command.size,
                price = command.price,
                timeInForce = command.timeInForce,
                whenHappened = command.whenRequested,
                rejectReason = OrderRejectReason.EXCHANGE_CLOSED,
                rejectText = "Placing orders is currently not allowed : ${TradingStatus.NOT_AVAILABLE_FOR_TRADING.name}"
            )
        )
    }
})
