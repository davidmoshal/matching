package jasition.matching.domain.book.command

import arrow.core.Either
import io.vavr.collection.List
import jasition.cqrs.Command
import jasition.cqrs.Command_2_
import jasition.cqrs.Transaction_2_
import jasition.matching.domain.book.*
import jasition.matching.domain.book.event.BooksCreatedEvent
import java.time.LocalDate

data class CreateBooksCommand(
    val bookId: BookId,
    val businessDate: LocalDate = LocalDate.now(),
    val defaultTradingStatus: TradingStatus
) : Command, Command_2_<BookId, Books> {
    override fun execute(aggregate: Books?): Either<Exception, Transaction_2_<BookId, Books>> {
        if (aggregate != null) return Either.left(BooksAlreadyExistsException("Books ${bookId} already exists"))

        val event = BooksCreatedEvent(
            bookId = bookId,
            businessDate = businessDate,
            tradingStatuses = TradingStatuses(default = defaultTradingStatus)
        )
        return Either.right(Transaction_2_<BookId, Books>(
            aggregate = event.play_2_(Books(bookId)),
            events = List.of(event)
        ))
    }

    fun validate(): BooksCreatedEvent = BooksCreatedEvent(
        bookId = bookId,
        businessDate = businessDate,
        tradingStatuses = TradingStatuses(default = defaultTradingStatus)
    )
}