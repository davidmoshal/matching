package jasition.matching.domain.order.validator

import arrow.core.Either
import jasition.matching.domain.book.Books
import jasition.matching.domain.order.command.PlaceOrderCommand
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.OrderRejectedEvent
import java.time.Instant

fun PlaceOrderCommandValidator(
        command: PlaceOrderCommand,
        books: Books,
        currentTime : Instant = Instant.now())
        : Either<OrderRejectedEvent, OrderPlacedEvent> {

    return Either.right(OrderPlacedEvent(
            requestId = command.requestId,
            whoRequested = command.whoRequested,
            bookId = command.bookId,
            orderType = command.orderType,
            side = command.side,
            availableSize = command.size,
            tradedSize = 0,
            cancelledSize = 0,
            price = command.price,
            timeInForce = command.timeInForce,
            whenHappened = currentTime
    ))
}