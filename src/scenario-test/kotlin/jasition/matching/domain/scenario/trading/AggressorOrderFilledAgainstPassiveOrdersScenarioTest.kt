package jasition.matching.domain.scenario.trading

import arrow.core.Tuple2
import arrow.core.Tuple5
import arrow.core.Tuple8
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.Command_2_
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
import jasition.matching.domain.order.command.PlaceOrderCommand
import jasition.matching.domain.trade.event.TradeEvent

internal class `Aggressor order filled against passive orders` : StringSpec({
    val bookId = aBookId()

    forall(
        /**
         * 1. Passive  : side, type, time in force, size, price
         * 2. Aggressor: side, type, time in force, size, price
         * 3. Trade    : passive entry index (even = buy(0, 2), odd = sell(1, 3)), size, price,
         *               aggressor status, aggressor available size, aggressor traded size,
         *               passive status, passive available size
         *
         * Parameter dimensions
         * 1. Buy / Sell of aggressor order
         * 2. Fill / Partial-fill of passive orders
         * 3. GTC / IOC of aggressor order
         * 4. Single / Multiple fills
         * 5. Exact / Better price executions (embedded in multiple fill cases)
         */

        row(
            List.of(
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 7, 13L)
            ),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
            List.of(Tuple8(0, 6, 12L, FILLED, 0, 6, FILLED, 0))
        ),
        row(
            List.of(
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 7, 13L)
            ),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 6, 12L),
            List.of(Tuple8(0, 6, 12L, FILLED, 0, 6, FILLED, 0))
        ),
        row(
            List.of(
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 7, 13L)
            ),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 2, 12L),
            List.of(Tuple8(0, 2, 12L, FILLED, 0, 2, PARTIAL_FILL, 4))
        ),
        row(
            List.of(
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 7, 13L)
            ),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 2, 12L),
            List.of(Tuple8(0, 2, 12L, FILLED, 0, 2, PARTIAL_FILL, 4))
        ),
        row(
            List.of(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 7, 10L)
            ),
            Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
            List.of(Tuple8(0, 6, 11L, FILLED, 0, 6, FILLED, 0))
        ),
        row(
            List.of(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 7, 10L)
            ),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 6, 11L),
            List.of(Tuple8(0, 6, 11L, FILLED, 0, 6, FILLED, 0))
        ),
        row(
            List.of(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 7, 10L)
            ),
            Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 4, 11L),
            List.of(Tuple8(0, 4, 11L, FILLED, 0, 4, PARTIAL_FILL, 2))
        ),
        row(
            List.of(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 7, 10L)
            ),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 4, 11L),
            List.of(Tuple8(0, 4, 11L, FILLED, 0, 4, PARTIAL_FILL, 2))
        ),
        row(
            List.of(
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 7, 13L)
            ),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 13, 13L),
            List.of(
                Tuple8(0, 6, 12L, PARTIAL_FILL, 7, 6, FILLED, 0),
                Tuple8(1, 7, 13L, FILLED, 0, 13, FILLED, 0)
            )
        ),
        row(
            List.of(
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 7, 13L)
            ),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 13, 13L),
            List.of(
                Tuple8(0, 6, 12L, PARTIAL_FILL, 7, 6, FILLED, 0),
                Tuple8(1, 7, 13L, FILLED, 0, 13, FILLED, 0)
            )
        ),
        row(
            List.of(
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 7, 13L)
            ),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 10, 13L),
            List.of(
                Tuple8(0, 6, 12L, PARTIAL_FILL, 4, 6, FILLED, 0),
                Tuple8(1, 4, 13L, FILLED, 0, 10, PARTIAL_FILL, 3)
            )
        ),
        row(
            List.of(
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 7, 13L)
            ),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 10, 13L),
            List.of(
                Tuple8(0, 6, 12L, PARTIAL_FILL, 4, 6, FILLED, 0),
                Tuple8(1, 4, 13L, FILLED, 0, 10, PARTIAL_FILL, 3)
            )
        ),
        row(
            List.of(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 7, 10L)
            ),
            Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 13, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 7, 6, FILLED, 0),
                Tuple8(1, 7, 10L, FILLED, 0, 13, FILLED, 0)
            )
        ),
        row(
            List.of(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 7, 10L)
            ),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 13, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 7, 6, FILLED, 0),
                Tuple8(1, 7, 10L, FILLED, 0, 13, FILLED, 0)
            )
        ),
        row(
            List.of(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 7, 10L)
            ),
            Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 10, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 4, 6, FILLED, 0),
                Tuple8(1, 4, 10L, FILLED, 0, 10, PARTIAL_FILL, 3)
            )
        ),
        row(
            List.of(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 11L),
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 7, 10L)
            ),
            Tuple5(SELL, LIMIT, IMMEDIATE_OR_CANCEL, 10, 10L),
            List.of(
                Tuple8(0, 6, 11L, PARTIAL_FILL, 4, 6, FILLED, 0),
                Tuple8(1, 4, 10L, FILLED, 0, 10, PARTIAL_FILL, 3)
            )
        )
    ) { oldEntries, new, expectedTrades ->
        "Given a book has existing orders of (${orderEntriesAsString(
            oldEntries
        )}) , when a ${new.a} ${new.b} ${new.c.code} order ${new.d} at ${new.e} is placed, then the trade is executed ${tradesAsString(
            expectedTrades.map { Tuple2(it.b, it.c) }
        )}" {
            val oldCommands = oldEntries.map {
                randomPlaceOrderCommand(
                    bookId = bookId,
                    side = it.a,
                    entryType = it.b,
                    timeInForce = it.c,
                    size = it.d,
                    price = Price(it.e),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1")
                ) as Command_2_<BookId, Books>
            }

            val repo = aRepoWithABooks(
                bookId = bookId,
                commands = oldCommands as List<Command_2_<BookId, Books>>
            )
            val command = randomPlaceOrderCommand(
                bookId = bookId,
                side = new.a,
                entryType = new.b,
                timeInForce = new.c,
                size = new.d,
                price = Price(new.e),
                whoRequested = Client(firmId = "firm2", firmClientId = "client2")
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            var oldBookEventId = 1L
            val oldBookEntries = oldCommands.map {
                expectedBookEntry(command = it as PlaceOrderCommand,
                    eventId = EventId(oldBookEventId++ * oldEntries.size()))
            }

            var tradeEventId = 5L
            with(result) {
                events shouldBe List.of<Event<BookId, Books>>(
                    expectedOrderPlacedEvent(command, EventId(5))
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
                    side.sameSideBook(it).entries.size() shouldBe 0
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
