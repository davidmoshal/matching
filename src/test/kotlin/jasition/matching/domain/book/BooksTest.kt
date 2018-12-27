package jasition.matching.domain.book

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.mockk.every
import io.mockk.spyk
import jasition.matching.domain.EventId
import jasition.matching.domain.aBookEntry
import jasition.matching.domain.aBookId
import jasition.matching.domain.aTradingStatuses
import jasition.matching.domain.book.entry.Side


internal class BooksTest : StringSpec({
    val buyEntry = aBookEntry(
        eventId = EventId(1),
        side = Side.BUY
    )
    val sellEntry = buyEntry.copy(side = Side.SELL)
    var tradeBuySideEntry = buyEntry.toTradeSideEntry(1)
    var tradeSellSideEntry = sellEntry.toTradeSideEntry(1)

    val buyLimitBook = spyk(LimitBook(Side.BUY))
    val sellLimitBook = spyk(LimitBook(Side.SELL))
    val newBuyLimitBook = spyk(LimitBook(Side.BUY))
    val newSellLimitBook = spyk(LimitBook(Side.SELL))
    val tradedBuyLimitBook = spyk(LimitBook(Side.BUY))
    val tradedSellLimitBook = spyk(LimitBook(Side.SELL))
    val books = Books(
        bookId = aBookId(),
        buyLimitBook = buyLimitBook,
        sellLimitBook = sellLimitBook,
        tradingStatuses = aTradingStatuses(),
        lastEventId = EventId(0)
    )

    every { buyLimitBook.add(buyEntry) } returns newBuyLimitBook
    every { sellLimitBook.add(sellEntry) } returns newSellLimitBook
    every { buyLimitBook.update(tradeBuySideEntry)} returns tradedBuyLimitBook
    every { sellLimitBook.update(tradeSellSideEntry)} returns tradedSellLimitBook

    "Adding BUY entry updates the BUY Limit Book and Last event ID only" {
        books.addBookEntry(buyEntry) shouldBe books.copy(
            buyLimitBook = newBuyLimitBook,
            lastEventId = buyEntry.key.eventId
        )
    }
    "Adding SELL entry updates the SELL Limit Book and Last event ID only" {
        books.addBookEntry(sellEntry) shouldBe books.copy(
            sellLimitBook = newSellLimitBook,
            lastEventId = sellEntry.key.eventId
        )
    }
    "Updating BUY trade entry updates the SELL Limit Book only" {
        books.traded(tradeBuySideEntry) shouldBe books.copy(buyLimitBook = newBuyLimitBook)
    }
    "Updating SELL trade entry updates the SELL Limit Book only" {
        books.traded(tradeSellSideEntry) shouldBe books.copy(sellLimitBook = newSellLimitBook)
    }
    "Accepts event ID (last event ID + 1)" {
        books.copy(lastEventId = EventId(4))
            .verifyEventId(eventId = EventId(5)) shouldBe EventId(5)
    }
    "Rejects event IDs other than (last event ID + 1)" {
        val newBooks = books.copy(lastEventId = EventId(4))
        forall(
            row(2L),
            row(3L),
            row(4L),
            row(6L),
            row(7L)
        ) { nextEventId ->
            shouldThrow<IllegalArgumentException> { newBooks.verifyEventId(EventId(nextEventId)) }
        }
    }
})