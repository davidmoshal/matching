package jasition.matching.domain.order.command

import arrow.core.Either
import jasition.matching.domain.Command
import jasition.matching.domain.book.Books
import jasition.matching.domain.order.*
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.OrderRejectedEvent
import java.time.Instant

data class PlaceOrderCommand(
    val requestId: String,
    val whoRequested: Client,
    val bookId: String,
    val orderType: OrderType,
    val side: Side,
    val size: Int,
    val price: Price?,
    val timeInForce: TimeInForce,
    val whenRequested: Instant
) : Command

fun validatePlaceOrderCommand(
    command: PlaceOrderCommand,
    books: Books,
    currentTime: Instant = Instant.now()
): Either<OrderRejectedEvent, OrderPlacedEvent> {

    return Either.right(
        OrderPlacedEvent(
            eventId = books.lastEventId.next(),
            requestId = command.requestId,
            whoRequested = command.whoRequested,
            bookId = command.bookId,
            orderType = command.orderType,
            side = command.side,
            size = OrderQuantity(
                availableSize = command.size,
                tradedSize = 0,
                cancelledSize = 0
            ),
            price = command.price,
            timeInForce = command.timeInForce,
            whenHappened = currentTime
        )
    )
}