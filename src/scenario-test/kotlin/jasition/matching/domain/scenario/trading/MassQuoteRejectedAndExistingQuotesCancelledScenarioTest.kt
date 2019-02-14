package jasition.matching.domain.scenario.trading

import arrow.core.Tuple3
import arrow.core.Tuple4
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.EntryStatus.CANCELLED
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.quote.event.MassQuoteCancelledEvent
import jasition.matching.domain.quote.event.QuoteRejectReason.INVALID_BID_ASK_SPREAD

internal class `Mass quote rejected and existing quotes cancelled` : StringSpec({
    val bookId = aBookId()

    forall(
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            List.of(Tuple4(8, 10L, 8, 11L), Tuple4(9, 11L, 9, 12L)),
            INVALID_BID_ASK_SPREAD,
            "Quote prices must not cross within a mass quote: lowestSellPrice=11, highestBuyPrice=11"
        )
    ) { oldEntries, newEntries, expectedQuoteRejectReason, expectedQuoteRejectText ->
        "Given a book has existing quote entries of (${entriesAsString(
            oldEntries
        )}) of the same firm, when a mass quote of (${entriesAsString(
            newEntries
        )}) of the same firm is placed, then all existing quote entries are cancelled and the mass quote is rejected and no new quote entries are added" {
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
                    status = CANCELLED
                )
            }

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
                    expectedMassQuoteRejectedEvent(
                        bookId = bookId,
                        eventId = EventId(7),
                        command = command,
                        expectedQuoteRejectReason = expectedQuoteRejectReason,
                        expectedQuoteRejectText = expectedQuoteRejectText
                    )
                )
            }

            repo.read(bookId).let {
                it.buyLimitBook.entries.size() shouldBe 0
                it.sellLimitBook.entries.size() shouldBe 0
            }
        }
    }
})