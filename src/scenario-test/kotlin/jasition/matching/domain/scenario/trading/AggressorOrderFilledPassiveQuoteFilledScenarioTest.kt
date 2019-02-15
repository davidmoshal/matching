package jasition.matching.domain.scenario.trading

import arrow.core.Tuple3
import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple6
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.Event
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.EntryStatus.FILLED
import jasition.matching.domain.book.entry.EntryStatus.PARTIAL_FILL
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.entry.TimeInForce.IMMEDIATE_OR_CANCEL
import jasition.matching.domain.client.Client
import jasition.matching.domain.trade.event.TradeEvent

internal class `Aggressor order filled and passive quote filled` : StringSpec({
    val bookId = aBookId()

    forall(
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
            List.of(
                Tuple6(1, 6, 12L, FILLED, 0, 6)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 6, 12L),
            List.of(
                Tuple6(1, 6, 12L, FILLED, 0, 6)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 13, 13L),
            List.of(
                Tuple6(1, 6, 12L, PARTIAL_FILL, 7, 6),
                Tuple6(3, 7, 13L, FILLED, 0, 13)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 13, 13L),
            List.of(
                Tuple6(1, 6, 12L, PARTIAL_FILL, 7, 6),
                Tuple6(3, 7, 13L, FILLED, 0, 13)
            )
        )
    ) { oldEntries, new, expectedTrades ->

        "Given a book has existing quote entries of (${entriesAsString(
            oldEntries
        )}) of the same firm, when a ${new.a} ${new.b} ${new.c.code} order ${new.d} at ${new.e} is placed, then the trade is executed ${tradesAsString(
            expectedTrades
        )}" {
            val oldCommand = randomPlaceMassQuoteCommand(
                bookId = bookId, entries = oldEntries,
                whoRequested = Client(firmId = "firm1", firmClientId = null)
            )
            val repo = aRepoWithABooks(bookId = bookId, commands = List.of(oldCommand))
            val command = randomPlaceOrderCommand(
                bookId = bookId,
                side = new.a,
                entryType = new.b,
                timeInForce = new.c,
                size = new.d,
                price = Price(new.e)
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val oldBookEntries = List.of(
                Tuple3(0, EventId(2), BUY),
                Tuple3(0, EventId(3), SELL),
                Tuple3(1, EventId(4), BUY),
                Tuple3(1, EventId(5), SELL)
            ).map {
                expectedBookEntry(command = oldCommand, entryIndex = it.a, eventId = it.b, side = it.c)
            }

            var tradeEventId = 6L

            val tradeEvents = expectedTrades.map { trade ->
                tradeEventId++

                val passive = oldBookEntries[trade.a].copy(
                    sizes = EntrySizes(
                        available = 0,
                        traded = trade.b,
                        cancelled = 0
                    ),
                    status = FILLED
                )

                val aggressor = expectedBookEntry(
                    command = command,
                    eventId = EventId(tradeEventId),
                    sizes = EntrySizes(available = trade.e, traded = trade.f, cancelled = 0),
                    status = trade.d
                )

                TradeEvent(
                    bookId = command.bookId,
                    eventId = EventId(tradeEventId),
                    size = trade.b,
                    price = Price(trade.c),
                    whenHappened = command.whenRequested,
                    aggressor = expectedTradeSideEntry(bookEntry = aggressor),
                    passive = expectedTradeSideEntry(bookEntry = passive)
                )
            }
            with(result) {
                events shouldBe List.of<Event<BookId, Books>>(
                    expectedOrderPlacedEvent(command, EventId(6))
                ).appendAll(tradeEvents)
            }

            repo.read(bookId).let {
                with(command) {
                    side.sameSideBook(it).entries.values() shouldBe oldBookEntries.filter { entry ->
                        entry.side == side
                    }

                    side.oppositeSideBook(it).entries.values() shouldBe oldBookEntries.filter { entry ->
                        entry.side == side.oppositeSide()
                    }.removeAll(expectedTrades.map { trade -> oldBookEntries[trade.a] })
                }
            }
        }
    }
})