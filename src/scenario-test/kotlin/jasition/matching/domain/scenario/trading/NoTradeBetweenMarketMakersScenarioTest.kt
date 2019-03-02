package jasition.matching.domain.scenario.trading

import arrow.core.Tuple2
import arrow.core.Tuple4
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client

internal class `No trade between market makers` : StringSpec({
    val bookId = aBookId()

    forall(
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            List.of(Tuple4(8, 12L, 8, 13L), Tuple4(9, 11L, 9, 14L))
        )
    ) { oldEntries, newEntries ->
        "Given a book has existing quote entries of (${quoteEntriesAsString(oldEntries)}) of the same firm, when a mass quote of (${quoteEntriesAsString(
            newEntries
        )}) of the different firm is placed, then all existing quote entries are retained and all new quote entries are added and there is no trade" {
            val oldCommand = randomPlaceMassQuoteCommand(
                bookId = bookId, entries = oldEntries,
                whoRequested = Client(firmId = "firm1", firmClientId = null)
            )
            val repo = aRepoWithABooks(bookId = bookId, commands = List.of(oldCommand))
            val command = randomPlaceMassQuoteCommand(
                bookId = bookId, entries = newEntries,
                whoRequested = Client(firmId = "firm2", firmClientId = null)
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val oldBookEntries = List.of(
                Tuple2(0, BUY),
                Tuple2(0, SELL),
                Tuple2(1, BUY),
                Tuple2(1, SELL)
            ).map { expectedBookEntry(command = oldCommand, entryIndex = it.a, eventId = EventId(1), side = it.b) }
            val newBookEntries = List.of(
                Tuple2(0, BUY),
                Tuple2(0, SELL),
                Tuple2(1, BUY),
                Tuple2(1, SELL)
            ).map { expectedBookEntry(command = command, entryIndex = it.a, eventId = EventId(6), side = it.b) }

            with(result) {
                events shouldBe List.of(
                    expectedMassQuotePlacedEvent(command, EventId(6)),
                    EntryAddedToBookEvent(bookId, EventId(7), newBookEntries[0]),
                    EntryAddedToBookEvent(bookId, EventId(8), newBookEntries[1]),
                    EntryAddedToBookEvent(bookId, EventId(9), newBookEntries[2]),
                    EntryAddedToBookEvent(bookId, EventId(10), newBookEntries[3])
                )
            }

            repo.read(bookId).let {
                it.buyLimitBook.entries.values() shouldBe List.of(
                    newBookEntries[0],
                    oldBookEntries[0],
                    newBookEntries[2],
                    oldBookEntries[2]
                )
                it.sellLimitBook.entries.values() shouldBe List.of(
                    oldBookEntries[1],
                    oldBookEntries[3],
                    newBookEntries[1],
                    newBookEntries[3]
                )
            }
        }
    }
})

