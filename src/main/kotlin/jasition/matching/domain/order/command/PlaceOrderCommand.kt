package jasition.matching.domain.order.command

import arrow.core.Either
import io.vavr.collection.List
import jasition.cqrs.Command
import jasition.cqrs.Command_2_
import jasition.cqrs.Transaction_2_
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.BooksNotFoundException
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.OrderRejectReason
import jasition.matching.domain.order.event.OrderRejectedEvent
import jasition.matching.domain.trade.matchAndFinalise_2_
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
) : Command, Command_2_<BookId, Books> {
    override fun execute(aggregate: Books?): Either<Exception, Transaction_2_<BookId, Books>> {
        if (aggregate == null) return Either.left(BooksNotFoundException("Books $bookId not found"))

        val rejection = rejectDueToUnknownSymbol(aggregate)
            ?: rejectDueToIncorrectSize(aggregate)
            ?: rejectDueToExchangeClosed(aggregate)

        rejection?.let {
            return Either.right(Transaction_2_<BookId, Books>(it.play_2_(aggregate), List.of(it)))
        }

        val placedEvent = toPlacedEvent(books = aggregate, currentTime = whenRequested)
        val placedAggregate = placedEvent.play_2_(aggregate)

        return Either.right(
            Transaction_2_<BookId, Books>(placedAggregate, List.of(placedEvent))
                .append(matchAndFinalise_2_(placedEvent.toBookEntry_2_(), placedAggregate))
        )
    }

    fun validate(
        books: Books
    ): Either<OrderRejectedEvent, OrderPlacedEvent> {
        val rejection =
            rejectDueToUnknownSymbol(books)
                ?: rejectDueToIncorrectSize(books)
                ?: rejectDueToExchangeClosed(books)

        if (rejection != null) {
            return Either.left(rejection)
        }
        return Either.right(toPlacedEvent(books = books, currentTime = whenRequested))
    }

    private fun rejectDueToUnknownSymbol(books: Books): OrderRejectedEvent? =
        if (bookId != books.bookId)
            toRejectedEvent(
                books = books.copy(bookId = bookId),
                currentTime = whenRequested,
                rejectReason = OrderRejectReason.UNKNOWN_SYMBOL,
                rejectText = "Unknown book ID : ${bookId.bookId}"
            )
        else null

    private fun rejectDueToExchangeClosed(books: Books): OrderRejectedEvent? =
        if (!books.tradingStatuses.effectiveStatus().allows(this))
            toRejectedEvent(
                books = books,
                currentTime = whenRequested,
                rejectReason = OrderRejectReason.EXCHANGE_CLOSED,
                rejectText = "Placing orders is currently not allowed : ${books.tradingStatuses.effectiveStatus()}"
            ) else null


    private fun rejectDueToIncorrectSize(books: Books): OrderRejectedEvent? {
        return if (size <= 0)
            toRejectedEvent(
                books = books,
                currentTime = whenRequested,
                rejectReason = OrderRejectReason.INCORRECT_QUANTITY,
                rejectText = "Order sizes must be positive : $size"
            )
        else null
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
        size = size,
        price = price,
        timeInForce = timeInForce,
        whenHappened = currentTime,
        rejectReason = rejectReason,
        rejectText = rejectText
    )
}
