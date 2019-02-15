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
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.EntryStatus.FILLED
import jasition.matching.domain.book.entry.EntryStatus.PARTIAL_FILL
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.IMMEDIATE_OR_CANCEL
import jasition.matching.domain.client.Client
import jasition.matching.domain.trade.event.TradeEvent

internal class `Aggressor order partial filled against passive quotes then cancelled` : StringSpec({
    val bookId = aBookId()

    forall(
        /**
         * 1. Passive  : bid size, bid price, offer size, offer price
         * 2. Aggressor: side, type, time in force, size, price
         * 3. Trade    : passive entry index (even = buy(0, 2), odd = sell(1, 3)), size, price,
         *               aggressor status, aggressor available size, aggressor traded size,
         *               passive status, passive available size
         *
         * Parameter dimensions
         * 1. Buy / Sell of aggressor order
         * 2. Single / Multiple fills
         * 3. Exact / Better price executions (embedded in multiple fill cases)
         * 4. Stop matching when prices do not match (embedded in single fill cases)
         */

        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 16, 12L),
            List.of(
                Tuple8(1, 6, 12L, PARTIAL_FILL, 10, 6, FILLED, 0)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 16, 11L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 10, 6, FILLED, 0)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 16, 13L),
            List.of(
                Tuple8(1, 6, 12L, PARTIAL_FILL, 10, 6, FILLED, 0),
                Tuple8(3, 7, 13L, PARTIAL_FILL, 3, 13, FILLED, 0)
            )
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 16, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 10, 6, FILLED, 0),
                Tuple8(2, 7, 10L, PARTIAL_FILL, 3, 13, FILLED, 0)
            )
        )

    ) { oldEntries, new, expectedTrades ->
        "Given a book has existing quote entries of (${quoteEntriesAsString(
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
            val entryAddedToBookEventId = tradeEventId  + expectedTrades.size() + 1
            val lastNewBookEntry = expectedTrades.last().let {
                expectedBookEntry(
                    command = command,
                    eventId = EventId(entryAddedToBookEventId),
                    sizes = EntrySizes(available = it.e, traded = it.f, cancelled = 0),
                    status = it.d
                )
            }

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
                }).append(
                    expectedOrderCancelledByExchangeEvent(
                        bookId = bookId,
                        eventId = EventId(entryAddedToBookEventId),
                        entry = lastNewBookEntry
                    )
                )
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