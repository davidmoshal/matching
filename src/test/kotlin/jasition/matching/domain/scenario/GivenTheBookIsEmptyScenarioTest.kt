package jasition.matching.domain.scenario

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.vavr.collection.List
import jasition.matching.domain.anOrderPlacedEvent
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.expectedBookEntry

internal class `Given the book is empty` : StringSpec({
    val bookId = BookId("book")
    val books = Books(bookId)

    "When a BUY Limit GTC Order is placed, then the new entry is added to the BUY side" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            timeInForce = TimeInForce.GOOD_TILL_CANCEL
        )

        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntry(orderPlacedEvent))
        result.aggregate.sellLimitBook.entries.size() shouldBe 0
    }
    "When a SELL Limit GTC order is placed, then the new entry is added to the SELL side" {
        val orderPlacedEvent = anOrderPlacedEvent(
            bookId = bookId,
            entryType = EntryType.LIMIT,
            side = Side.SELL,
            timeInForce = TimeInForce.GOOD_TILL_CANCEL
        )
        val result = orderPlacedEvent.play(books)

        val expectedBookEntry = expectedBookEntry(orderPlacedEvent)
        result.events shouldBe List.of(expectedBookEntry.toEntryAddedToBookEvent(bookId))
        result.aggregate.buyLimitBook.entries.size() shouldBe 0
        result.aggregate.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntry)
    }
})

