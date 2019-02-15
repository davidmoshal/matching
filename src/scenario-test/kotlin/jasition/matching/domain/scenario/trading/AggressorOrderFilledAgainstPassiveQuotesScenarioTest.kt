package jasition.matching.domain.scenario.trading

import arrow.core.*
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
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.entry.EntryStatus.FILLED
import jasition.matching.domain.book.entry.EntryStatus.PARTIAL_FILL
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.entry.TimeInForce.IMMEDIATE_OR_CANCEL
import jasition.matching.domain.client.Client
import jasition.matching.domain.trade.event.TradeEvent

internal class `Aggressor order filled against passive quotes` : StringSpec({
    val bookId = aBookId()

    forall(
        /**
         * 1. Quote: bid size, bid price, offer size, offer price
         * 2. Order: side, type, time in force, size, price
         * 3. Trade: passive entry index (even = buy(0, 2), odd = sell(1, 3)), size, price,
         *           aggressor status, aggressor available size, aggressor traded size,
         *           passive status, passive available size
         *
         * Parameter dimensions
         * 1. Buy / Sell of aggressor order
         * 2. Fill / Partial-fill of passive quotes
         * 3. GTC / IOC of aggressor order
         * 4. Single / Multiple fills
         * 5. Exact / Better price executions (embedded in multiple fill cases)
         */

        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
            List.of(Tuple8(1, 6, 12L, FILLED, 0, 6, FILLED, 0))
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 6, 12L),
            List.of(Tuple8(1, 6, 12L, FILLED, 0, 6, FILLED, 0))
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 2, 12L),
            List.of(Tuple8(1, 2, 12L, FILLED, 0, 2, PARTIAL_FILL, 4))
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 2, 12L),
            List.of(Tuple8(1, 2, 12L, FILLED, 0, 2, PARTIAL_FILL, 4))
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
            List.of(Tuple8(0, 6, 11L, FILLED, 0, 6, FILLED, 0))
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 6, 11L),
            List.of(Tuple8(0, 6, 11L, FILLED, 0, 6, FILLED, 0))
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 4, 11L),
            List.of(Tuple8(0, 4, 11L, FILLED, 0, 4, PARTIAL_FILL, 2))
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 4, 11L),
            List.of(Tuple8(0, 4, 11L, FILLED, 0, 4, PARTIAL_FILL, 2))
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 13, 13L),
            List.of(
                Tuple8(1, 6, 12L, PARTIAL_FILL, 7, 6, FILLED, 0),
                Tuple8(3, 7, 13L, FILLED, 0, 13, FILLED, 0)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 13, 13L),
            List.of(
                Tuple8(1, 6, 12L, PARTIAL_FILL, 7, 6, FILLED, 0),
                Tuple8(3, 7, 13L, FILLED, 0, 13, FILLED, 0)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 10, 13L),
            List.of(
                Tuple8(1, 6, 12L, PARTIAL_FILL, 4, 6, FILLED, 0),
                Tuple8(3, 4, 13L, FILLED, 0, 10, PARTIAL_FILL, 3)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 10, 13L),
            List.of(
                Tuple8(1, 6, 12L, PARTIAL_FILL, 4, 6, FILLED, 0),
                Tuple8(3, 4, 13L, FILLED, 0, 10, PARTIAL_FILL, 3)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 13, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 7, 6, FILLED, 0),
                Tuple8(2, 7, 10L, FILLED, 0, 13, FILLED, 0)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 13, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 7, 6, FILLED, 0),
                Tuple8(2, 7, 10L, FILLED, 0, 13, FILLED, 0)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 10, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 4, 6, FILLED, 0),
                Tuple8(2, 4, 10L, FILLED, 0, 10, PARTIAL_FILL, 3)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 10, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 4, 6, FILLED, 0),
                Tuple8(2, 4, 10L, FILLED, 0, 10, PARTIAL_FILL, 3)
            )
        )
    ) { oldEntries, new, expectedTrades ->
        "Given a book has existing quote entries of (${entriesAsString(
            oldEntries
        )}) of the same firm, when a ${new.a} ${new.b} ${new.c.code} order ${new.d} at ${new.e} is placed, then the trade is executed ${tradesAsString(
            expectedTrades.map { Tuple2(it.b, it.c) }
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
                price = Price(new.e),
                whoRequested = Client(firmId = "firm2", firmClientId = randomFirmClientId())
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
            with(result) {
                events shouldBe List.of<Event<BookId, Books>>(
                    expectedOrderPlacedEvent(command, EventId(6))
                ).appendAll(expectedTrades.map { trade ->
                    tradeEventId++

                    TradeEvent(
                        bookId = command.bookId,
                        eventId = EventId(tradeEventId),
                        size = trade.b,
                        price = Price(trade.c),
                        whenHappened = command.whenRequested,
                        aggressor = expectedTradeSideEntry(
                            bookEntry = expectedBookEntry(
                                command = command,
                                eventId = EventId(tradeEventId),
                                sizes = EntrySizes(available = trade.e, traded = trade.f, cancelled = 0),
                                status = trade.d
                            )
                        ),
                        passive = expectedTradeSideEntry(
                            bookEntry = oldBookEntries[trade.a].copy(
                                sizes = EntrySizes(
                                    available = trade.h,
                                    traded = trade.b,
                                    cancelled = 0
                                ),
                                status = trade.g
                            )
                        )
                    )
                })
            }

            repo.read(bookId).let {
                with(command) {
                    side.sameSideBook(it).entries.values() shouldBe oldBookEntries.filter { entry ->
                        entry.side == side
                    }

                    side.oppositeSideBook(it).entries.values() shouldBe updatedBookEntries(
                        side = side,
                        oldBookEntries = oldBookEntries,
                        expectedTrades = expectedTrades
                    )
                }
            }
        }
    }
})

fun updatedBookEntries(
    side: Side,
    oldBookEntries: List<BookEntry>,
    expectedTrades: List<Tuple8<Int, Int, Long, EntryStatus, Int, Int, EntryStatus, Int>>
): List<BookEntry> {
    var updatedBookEntries = oldBookEntries
    expectedTrades.forEach { t ->
        updatedBookEntries = updatedBookEntries.update(t.a) { e ->
            e.copy(
                sizes = EntrySizes(
                    available = t.h,
                    traded = t.b,
                    cancelled = 0
                ),
                status = t.g
            )
        }
    }
    return updatedBookEntries.filter { entry ->
        entry.side == side.oppositeSide() && !entry.status.isFinal()
    }
}