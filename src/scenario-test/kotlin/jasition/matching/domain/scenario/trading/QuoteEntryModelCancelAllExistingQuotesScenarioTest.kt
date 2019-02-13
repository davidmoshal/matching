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
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.EntryStatus
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.quote.event.MassQuoteCancelledEvent

internal class `Quote entry model cancels all existing quote of the same firm` : FeatureSpec({
    val bookId = aBookId()

    feature("Quote entry model cancels all existing quote of the same firm") {
        forall(
            row(
                List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
                List.of(Tuple4(8, 12L, 8, 13L), Tuple4(9, 11L, 9, 14L))
            ),
            row(
                List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
                List.of(Tuple4(8, 10L, 8, 11L), Tuple4(9, 9L, 9, 12L))
            )
        ) { oldEntries, newEntries ->
            scenario(
                "Given a book has existing quote entries of (${entriesAsString(oldEntries)}) of the same firm, when a mass quote of (${entriesAsString(
                    newEntries
                )}) of the same firm is placed, then all existing quote entries are cancelled and all new quote entries are added"
            ) {
                val oldCommand = randomPlaceMassQuoteCommand(bookId = bookId, entries = oldEntries)
                val repo = aRepoWithABooks(bookId = bookId, commands = List.of(oldCommand))
                val command = randomPlaceMassQuoteCommand(
                    bookId = bookId, entries = newEntries,
                    whoRequested = oldCommand.whoRequested
                )

                val result = command.execute(repo.read(bookId)) commitOrThrow repo

                val oldBookEntries = List.of(
                    Tuple3(0, EventId(2), BUY),
                    Tuple3(1, EventId(4), BUY),
                    Tuple3(0, EventId(3), SELL),
                    Tuple3(1, EventId(5), SELL)
                ).map {
                    expectedBookEntry(
                        command = oldCommand,
                        entryIndex = it.a,
                        eventId = it.b,
                        side = it.c,
                        sizes = EntrySizes(
                            available = 0,
                            traded = 0,
                            cancelled = it.c.priceWithSize(oldCommand.entries[it.a])?.size ?: 0
                        ),
                        status = EntryStatus.CANCELLED
                    )
                }
                val newBookEntries = List.of(
                    Tuple3(0, EventId(8), BUY),
                    Tuple3(0, EventId(9), SELL),
                    Tuple3(1, EventId(10), BUY),
                    Tuple3(1, EventId(11), SELL)
                ).map { expectedBookEntry(command = command, entryIndex = it.a, eventId = it.b, side = it.c) }

                with(result) {
                    events shouldBe List.of(
                        MassQuoteCancelledEvent(
                            bookId = bookId,
                            eventId = EventId(6),
                            entries = oldBookEntries,
                            primary = false,
                            whoRequested = oldCommand.whoRequested,
                            whenHappened = command.whenRequested
                        ),
                        expectedMassQuotePlacedEvent(command, EventId(7)),
                        EntryAddedToBookEvent(bookId, EventId(8), newBookEntries[0]),
                        EntryAddedToBookEvent(bookId, EventId(9), newBookEntries[1]),
                        EntryAddedToBookEvent(bookId, EventId(10), newBookEntries[2]),
                        EntryAddedToBookEvent(bookId, EventId(11), newBookEntries[3])
                    )
                }

                repo.read(bookId).let {
                    it.buyLimitBook.entries.values() shouldBe List.of(newBookEntries[0], newBookEntries[2])
                    it.sellLimitBook.entries.values() shouldBe List.of(newBookEntries[1], newBookEntries[3])
                }
            }
        }
    }


    feature("Quote entry model does not cancel existing quote of different firms") {
        forall(
            row(
                List.of(Tuple4(6, 13L, 6, 14L), Tuple4(7, 12L, 7, 15L)),
                List.of(Tuple4(8, 11L, 8, 16L), Tuple4(9, 10L, 9, 17L))
            ),
            row(
                List.of(Tuple4(6, 13L, 6, 14L), Tuple4(7, 12L, 7, 15L)),
                List.of(Tuple4(8, 12L, 8, 15L), Tuple4(9, 11L, 9, 16L))
            )
        ) { oldEntries, newEntries ->
            scenario(
                "Given a book has existing quote entries of (${entriesAsString(oldEntries)}) of the different firm, when a mass quote of (${entriesAsString(
                    newEntries
                )}) of the same firm is placed, then all existing quote entries are retained and all new quote entries are added"
            ) {
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
                    Tuple3(0, EventId(2), BUY),
                    Tuple3(0, EventId(3), SELL),
                    Tuple3(1, EventId(4), BUY),
                    Tuple3(1, EventId(5), SELL)
                ).map { expectedBookEntry(command = oldCommand, entryIndex = it.a, eventId = it.b, side = it.c) }
                val newBookEntries = List.of(
                    Tuple3(0, EventId(7), BUY),
                    Tuple3(0, EventId(8), SELL),
                    Tuple3(1, EventId(9), BUY),
                    Tuple3(1, EventId(10), SELL)
                ).map { expectedBookEntry(command = command, entryIndex = it.a, eventId = it.b, side = it.c) }

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
                        oldBookEntries[0],
                        oldBookEntries[2],
                        newBookEntries[0],
                        newBookEntries[2]
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
    }
})

