package jasition.matching.domain.book.command

import io.kotlintest.specs.StringSpec
import jasition.matching.domain.aBookId
import jasition.matching.domain.book.TradingStatus

internal class `Given there is a request to create a Books` : StringSpec({
    val command = CreateBooksCommand(
        bookId = aBookId(),
        defaultTradingStatus = TradingStatus.NOT_AVAILABLE_FOR_TRADING
    )

})