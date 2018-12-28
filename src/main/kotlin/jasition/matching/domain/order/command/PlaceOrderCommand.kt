package jasition.matching.domain.order.command

import arrow.core.Either
import jasition.matching.domain.Command
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.OrderRejectReason
import jasition.matching.domain.order.event.OrderRejectedEvent
import java.time.Instant

data class PlaceOrderCommand(
    val requestId: ClientRequestId,
    val whoRequested: Client,
    val bookId: BookId,
    val entryType: EntryType,
    val side: Side,
    val size: Int,
    val price: Price?,
    val timeInForce: TimeInForce,
    val whenRequested: Instant
) : Command {

    fun validate(
        books: Books
    ): Either<OrderRejectedEvent, OrderPlacedEvent> {
        if (size <= 0) {
            return Either.left(
                toRejectedEvent(
                    books = books,
                    currentTime = whenRequested,
                    rejectReason = OrderRejectReason.INCORRECT_QUANTITY,
                    rejectText = "Order sizes must be positive : $size"
                )
            )
        }

        if (!books.tradingStatuses.effectiveStatus().allows(this)) {
            return Either.left(
                toRejectedEvent(
                    books = books,
                    currentTime = whenRequested,
                    rejectReason = OrderRejectReason.BROKER_EXCHANGE_OPTION,
                    rejectText = "Placing orders is currently not allowed : ${books.tradingStatuses.effectiveStatus()}"
                )
            )
        }
        return Either.right(toPlacedEvent(books = books, currentTime = whenRequested))
    }

    private fun toPlacedEvent(
        books: Books,
        currentTime: Instant = Instant.now()
    ): OrderPlacedEvent = OrderPlacedEvent(
        eventId = books.lastEventId.next(),
        requestId = requestId,
        whoRequested = whoRequested,
        bookId = bookId,
        entryType = entryType,
        side = side,
        sizes = EntrySizes(
            available = size,
            traded = 0,
            cancelled = 0
        ),
        price = price,
        timeInForce = timeInForce,
        whenHappened = currentTime
    )

    private fun toRejectedEvent(
        books: Books,
        currentTime: Instant = Instant.now(),
        rejectReason: OrderRejectReason = OrderRejectReason.OTHER,
        rejectText: String?
    ): OrderRejectedEvent = OrderRejectedEvent(
        eventId = books.lastEventId.next(),
        requestId = requestId,
        whoRequested = whoRequested,
        bookId = bookId,
        entryType = entryType,
        side = side,
        sizes = size,
        price = price,
        timeInForce = timeInForce,
        whenHappened = currentTime,
        rejectReason = rejectReason,
        rejectText = rejectText
    )
}
