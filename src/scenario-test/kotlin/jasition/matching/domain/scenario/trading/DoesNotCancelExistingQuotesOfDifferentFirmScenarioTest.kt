package jasition.matching.domain.scenario.trading

import arrow.core.Tuple2
import arrow.core.Tuple4
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.kotlin.list
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client

internal class `Quote entry model does not cancel existing quote of the different firm` : StringSpec({
    val bookId = aBookId()

    forall(
        row(
            list(Tuple4(6, 13L, 6, 14L), Tuple4(7, 12L, 7, 15L)),
            list(Tuple4(8, 11L, 8, 16L), Tuple4(9, 10L, 9, 17L))
        ),
        row(
            list(Tuple4(6, 13L, 6, 14L), Tuple4(7, 12L, 7, 15L)),
            list(Tuple4(8, 12L, 8, 15L), Tuple4(9, 11L, 9, 16L))
        )
    ) { oldEntries, newEntries ->
        "Given a book has existing quote entries of (${quoteEntriesAsString(
            oldEntries
        )}) of the different firm, when a mass quote of (${quoteEntriesAsString(
            newEntries
        )}) of the same firm is placed, then all existing quote entries are retained and all new quote entries are added" {
            val oldCommand = randomPlaceMassQuoteCommand(
                bookId = bookId, entries = oldEntries,
                whoRequested = Client(firmId = "firm1", firmClientId = null)
            )
            val repo = aRepoWithABooks(bookId = bookId, commands = list(oldCommand))
            val command = randomPlaceMassQuoteCommand(
                bookId = bookId, entries = newEntries,
                whoRequested = Client(firmId = "firm2", firmClientId = null)
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val oldBookEntries = list(
                Tuple2(0, BUY),
                Tuple2(0, SELL),
                Tuple2(1, BUY),
                Tuple2(1, SELL)
            ).map { expectedBookEntry(command = oldCommand, entryIndex = it.a, eventId = EventId(1), side = it.b) }
            val newBookEntries = list(
                Tuple2(0, BUY),
                Tuple2(0, SELL),
                Tuple2(1, BUY),
                Tuple2(1, SELL)
            ).map { expectedBookEntry(command = command, entryIndex = it.a, eventId = EventId(6), side = it.b) }

            with(result) {
                events shouldBe list(
                    expectedMassQuotePlacedEvent(command, EventId(6)),
                    EntryAddedToBookEvent(bookId, EventId(7), newBookEntries[0]),
                    EntryAddedToBookEvent(bookId, EventId(8), newBookEntries[1]),
                    EntryAddedToBookEvent(bookId, EventId(9), newBookEntries[2]),
                    EntryAddedToBookEvent(bookId, EventId(10), newBookEntries[3])
                )
            }

            repo.read(bookId).let {
                it.buyLimitBook.entries.values() shouldBe list(
                    oldBookEntries[0],
                    oldBookEntries[2],
                    newBookEntries[0],
                    newBookEntries[2]
                )
                it.sellLimitBook.entries.values() shouldBe list(
                    oldBookEntries[1],
                    oldBookEntries[3],
                    newBookEntries[1],
                    newBookEntries[3]
                )
            }
        }
    }
})