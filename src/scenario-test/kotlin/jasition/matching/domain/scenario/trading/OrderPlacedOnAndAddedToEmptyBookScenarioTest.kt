package jasition.matching.domain.scenario.trading

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.event.EntryAddedToBookEvent

internal class `Order placed on and added to empty book` : StringSpec({
    val bookId = aBookId()

    forall(
        row(BUY, LIMIT, GOOD_TILL_CANCEL),
        row(SELL, LIMIT, GOOD_TILL_CANCEL)
    ) { side, entryType, timeInForce ->
        "Given an empty book, when a $side $entryType ${timeInForce.code} order is placed, then the entry is added to the $side limit book" {
            val repo = aRepoWithABooks(bookId = bookId)
            val command = randomPlaceOrderCommand(
                bookId = bookId,
                side = side,
                entryType = entryType,
                timeInForce = timeInForce
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val expectedBookEntry = expectedBookEntry(command, EventId(1))

            with(result) {
                events shouldBe List.of(
                    expectedOrderPlacedEvent(command, EventId(1)),
                    EntryAddedToBookEvent(bookId = bookId, eventId = EventId(2), entry = expectedBookEntry)
                )
            }
            repo.read(bookId).let {
                with(command.side) {
                    sameSideBook(it).entries.values() shouldBe List.of(expectedBookEntry)
                    oppositeSideBook(it).entries.size() shouldBe 0
                }
            }
        }
    }
})