package jasition.matching.domain.scenario.trading

import arrow.core.Tuple2
import arrow.core.Tuple4
import arrow.core.Tuple5
import arrow.core.Tuple9
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import io.vavr.kotlin.list
import jasition.cqrs.Command
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
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.order.command.PlaceOrderCommand
import jasition.matching.domain.trade.event.TradeEvent

internal class `Multiple level quote entries executed in natural order` : StringSpec({
    val bookId = aBookId()

    forall(
        /**
         * 1. Passive  : side, type, time in force, size, price
         * 2. Aggressor: bid size, bid price, offer size, offer price
         * 3. Trade    : aggressor entry index (even = buy(0, 2), odd = sell(1, 3)), passive entry index,
         *               size, price,
         *               aggressor status, aggressor available size, aggressor traded size,
         *               passive status, passive available size
         */

        row(
            list(
                Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 6, 12L),
                Tuple5(SELL, LIMIT, GOOD_TILL_CANCEL, 6, 13L)
            ),
            list(
                Tuple4(10, 13L, 10, 15L),
                Tuple4(10, 14L, 10, 16L)
            ),
            list(Tuple9(0, 1, 6, 13L, PARTIAL_FILL, 4, 6, FILLED, 0))
        )
    ) { oldEntries, newEntries, expectedTrades ->
        "Given a book has existing orders of (${orderEntriesAsString(
            oldEntries
        )}) , when a mass quote of (${quoteEntriesAsString(
            newEntries
        )} is placed, then the trade is executed ${tradesAsString(
            expectedTrades.map { Tuple2(it.c, it.d) }
        )} and the rest of the quotes added to the book in natural order" {
            val oldCommands = oldEntries.map {
                randomPlaceOrderCommand(
                    bookId = bookId,
                    side = it.a,
                    entryType = it.b,
                    timeInForce = it.c,
                    size = it.d,
                    price = Price(it.e),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client1")
                ) as Command<BookId, Books>
            }

            val repo = aRepoWithABooks(
                bookId = bookId,
                commands = oldCommands as List<Command<BookId, Books>>
            )
            val command = randomPlaceMassQuoteCommand(
                bookId = bookId, entries = newEntries,
                whoRequested = Client(firmId = "firm2", firmClientId = null)
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            var oldBookEventId = 0L
            val oldBookEntries = oldCommands.map {
                expectedBookEntry(
                    command = it as PlaceOrderCommand,
                    eventId = EventId((oldBookEventId++ * 2) + 1L)
                )
            }

            val massQuotePlacedEventId = EventId(5)
            var newBookEntries = list(
                Tuple2(0, BUY),
                Tuple2(0, SELL),
                Tuple2(1, BUY),
                Tuple2(1, SELL)
            ).map {
                expectedBookEntry(command = command, entryIndex = it.a, eventId = massQuotePlacedEventId, side = it.b)
            }

            expectedTrades.forEach { t ->
                newBookEntries = newBookEntries.update(t.a) { e ->
                    e.copy(
                        sizes = EntrySizes(
                            available = t.f,
                            traded = t.g,
                            cancelled = 0
                        ),
                        status = t.e
                    )
                }
            }


            var eventId = massQuotePlacedEventId
            with(result) {
                events shouldBe List.of<Event<BookId, Books>>(
                    expectedMassQuotePlacedEvent(command, massQuotePlacedEventId)
                ).appendAll(expectedTrades.map { trade ->
                    TradeEvent(
                        bookId = command.bookId,
                        eventId = ++eventId,
                        size = trade.c,
                        price = Price(trade.d),
                        whenHappened = command.whenRequested,
                        aggressor = expectedTradeSideEntry(
                            bookEntry = expectedBookEntry(
                                command = command,
                                entryIndex = 0,
                                side = BUY,
                                eventId = massQuotePlacedEventId,
                                sizes = EntrySizes(available = trade.f, traded = trade.g, cancelled = 0),
                                status = trade.e
                            )
                        ),
                        passive = expectedTradeSideEntry(
                            bookEntry = oldBookEntries[trade.b].copy(
                                sizes = EntrySizes(
                                    available = trade.i,
                                    traded = trade.c,
                                    cancelled = 0
                                ),
                                status = trade.h
                            )
                        )
                    )
                }).appendAll(
                    list(
                        EntryAddedToBookEvent(bookId = bookId, eventId = EventId(7), entry = newBookEntries[0]),
                        EntryAddedToBookEvent(bookId = bookId, eventId = EventId(8), entry = newBookEntries[1]),
                        EntryAddedToBookEvent(bookId = bookId, eventId = EventId(9), entry = newBookEntries[2]),
                        EntryAddedToBookEvent(bookId = bookId, eventId = EventId(10), entry = newBookEntries[3])
                    )
                )
            }

            repo.read(bookId).let {
                it.buyLimitBook.entries.values() shouldBe list(newBookEntries[2], newBookEntries[0], oldBookEntries[0])
                it.sellLimitBook.entries.values() shouldBe list(newBookEntries[1], newBookEntries[3])
            }
        }
    }
})
