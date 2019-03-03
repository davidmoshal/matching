package jasition.matching.domain.book.command

import arrow.core.Either
import io.vavr.collection.List
import jasition.cqrs.Command
import jasition.cqrs.Transaction
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.event.BooksCreatedEvent
import java.time.LocalDate

data class CreateBooksCommand(
    val bookId: BookId,
    val businessDate: LocalDate = LocalDate.now(),
    val defaultTradingStatus: TradingStatus
) : Command<BookId, Books> {
    override fun execute(aggregate: Books?): Either<Exception, Transaction<BookId, Books>> {
        if (aggregate != null) return Either.left(IllegalArgumentException("Books ${bookId} already exists"))

        val event = BooksCreatedEvent(
            bookId = bookId,
            businessDate = businessDate,
            tradingStatuses = TradingStatuses(default = defaultTradingStatus)
        )
        return Either.right(Transaction<BookId, Books>(
            aggregate = event.play(Books(bookId)),
            events = List.of(event)
        ))
    }
}