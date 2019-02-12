package jasition.matching.domain.scenario.trading

import arrow.core.Tuple3
import arrow.core.Tuple4
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.event.EntryAddedToBookEvent

internal class `Place mass quote on empty book` : FeatureSpec({
    val bookId = aBookId()

    forall(
        row(List.of(Tuple4(4, 10L, 4, 11L), Tuple4(5, 9L, 5, 12L)))
    ) { entries ->
        feature("Quote entries added to empty book") {
            scenario("Given an empty book, when a mass quote of (${entriesAsString(entries)}) is placed, then all quote entries are added") {
                val repo = aRepoWithABooks(bookId = bookId)
                val command = randomPlaceMassQuoteCommand(bookId = bookId, entries = entries)

                val result = command.execute(repo.read(bookId)) commitOrThrow repo

                val expectedBookEntries = List.of(
                    Tuple3(0, EventId(2), BUY),
                    Tuple3(0, EventId(3), SELL),
                    Tuple3(1, EventId(4), BUY),
                    Tuple3(1, EventId(5), SELL)
                ).map { expectedBookEntry(command = command, entryIndex = it.a, eventId = it.b, side = it.c) }

                with(result) {
                    events shouldBe List.of(
                        expectedMassQuotePlacedEvent(command, EventId(1)),
                        EntryAddedToBookEvent(bookId, EventId(2), expectedBookEntries[0]),
                        EntryAddedToBookEvent(bookId, EventId(3), expectedBookEntries[1]),
                        EntryAddedToBookEvent(bookId, EventId(4), expectedBookEntries[2]),
                        EntryAddedToBookEvent(bookId, EventId(5), expectedBookEntries[3])
                    )
                }

                repo.read(bookId).let {
                    it.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntries[0], expectedBookEntries[2])
                    it.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntries[1], expectedBookEntries[3])
                }
            }
        }
    }
})

