package jasition.matching.domain.book.command

import arrow.core.Either
import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.kotlin.list
import jasition.cqrs.EventId
import jasition.cqrs.Transaction
import jasition.matching.domain.aBookId
import jasition.matching.domain.aBooks
import jasition.matching.domain.book.*
import jasition.matching.domain.book.event.BooksCreatedEvent

internal class CreateBooksCommandTest : StringSpec({
    val bookId = aBookId()
    val command = CreateBooksCommand(
        bookId = bookId,
        defaultTradingStatus = TradingStatus.NOT_AVAILABLE_FOR_TRADING
    )

    "Exception if the books already existed" {
        command.execute(aBooks(bookId = bookId))
            .swap().toOption().orNull() should beOfType<BooksAlreadyExistsException>()
    }
    "Books created if the books did not exist" {
        command.execute(null) shouldBe Either.right(
            Transaction<BookId, Books>(
                aggregate = Books(
                    bookId = bookId,
                    tradingStatuses = TradingStatuses(default = command.defaultTradingStatus)
                ),
                events = list(
                    BooksCreatedEvent(
                        eventId = EventId(0),
                        bookId = bookId,
                        businessDate = command.businessDate,
                        tradingStatuses = TradingStatuses(default = command.defaultTradingStatus)
                    )
                )
            )
        )
    }
})