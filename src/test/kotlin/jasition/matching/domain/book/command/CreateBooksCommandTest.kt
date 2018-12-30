package jasition.matching.domain.book.command

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import jasition.cqrs.EventId
import jasition.matching.domain.aBookId
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.TradingStatuses
import jasition.matching.domain.book.event.BooksCreatedEvent

internal class `Given there is a request to create a Books` : StringSpec({
    val command = CreateBooksCommand(
        bookId = aBookId(),
        defaultTradingStatus = TradingStatus.NOT_AVAILABLE_FOR_TRADING
    )
    "Then the books is created" {
        command.validate() shouldBe BooksCreatedEvent(
            eventId = EventId(0),
            bookId = command.bookId,
            businessDate = command.businessDate,
            tradingStatuses = TradingStatuses(default = command.defaultTradingStatus)
        )
    }
})