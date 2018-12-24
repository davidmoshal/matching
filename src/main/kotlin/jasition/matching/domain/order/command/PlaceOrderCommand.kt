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
                    rejectText = "Order size must be positive : ${size}"
                )
            )
        }

        if (!books.tradingStatuses.effectiveStatus().allows(this)) {
            return Either.left(
                toRejectedEvent(
                    books = books,
                    currentTime = whenRequested,
                    rejectReason = OrderRejectReason.EXCHANGE_CLOSED,
                    rejectText = "The Book is current not open for trading : ${books.tradingStatuses.effectiveStatus()}"
                )
            )
        }

        return Either.right(toPlacedEvent(books = books, currentTime = whenRequested))
    }

    fun toPlacedEvent(
        books: Books,
        currentTime: Instant = Instant.now()
    ): OrderPlacedEvent = OrderPlacedEvent(
        eventId = books.lastEventId.next(),
        requestId = requestId,
        whoRequested = whoRequested,
        bookId = bookId,
        entryType = entryType,
        side = side,
        size = EntryQuantity(
            availableSize = size,
            tradedSize = 0,
            cancelledSize = 0
        ),
        price = price,
        timeInForce = timeInForce,
        whenHappened = currentTime
    )

    fun toRejectedEvent(
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
        size = EntryQuantity(
            availableSize = size,
            tradedSize = 0,
            cancelledSize = 0
        ),
        price = price,
        timeInForce = timeInForce,
        whenHappened = currentTime,
        rejectReason = rejectReason,
        rejectText = rejectText
    )
}
