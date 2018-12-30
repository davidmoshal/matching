package jasition.matching.domain.book.command

import jasition.cqrs.Command
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.event.BooksCreatedEvent
import java.time.LocalDate

data class CreateBooksCommand(val bookId: BookId,
                              val businessDate: LocalDate = LocalDate.now(),
                              val defaultTradingStatus : TradingStatus
) : Command {
    fun validate() : BooksCreatedEvent = BooksCreatedEvent(
        bookId = bookId,
        businessDate = businessDate,
        tradingStatuses = TradingStatuses(default = defaultTradingStatus)
    )
}