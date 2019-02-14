package jasition.matching.domain.scenario.trading

import arrow.core.Tuple3
import arrow.core.Tuple4
import arrow.core.Tuple5
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.EntryStatus
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.entry.TimeInForce.IMMEDIATE_OR_CANCEL
import jasition.matching.domain.client.Client
import jasition.matching.domain.trade.event.TradeEvent

internal class `Aggressor order filled and passive quote partial filled` : StringSpec({
    val bookId = aBookId()

    forall(
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, GOOD_TILL_CANCEL, 5, 12L),
            1, 5, 12L, 1
        ),
        row(
            List.of(Tuple4(6, 11L, 6, 12L), Tuple4(7, 10L, 7, 13L)),
            Tuple5(BUY, LIMIT, IMMEDIATE_OR_CANCEL, 5, 12L),
            1, 5, 12L, 1
        )
    ) { oldEntries, new, expectedCounterpartEntryIndex, expectedTradeSize, expectedTradePrice, expectedAvailableSize ->
        "Given a book has existing quote entries of (${entriesAsString(oldEntries)}) of the same firm, when a ${new.a} ${new.b} ${new.c.code} order ${new.d} at ${new.e} is placed,  then the trade is executed $expectedTradeSize at $expectedTradePrice" {
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
            }.update(expectedCounterpartEntryIndex) {
                it.copy(
                    sizes = EntrySizes(
                        available = expectedAvailableSize,
                        traded = expectedTradeSize,
                        cancelled = 0
                    ),
                    status = EntryStatus.PARTIAL_FILL
                )
            }

            val newBookEntry = expectedBookEntry(command, EventId(4))

            with(result) {
                events shouldBe List.of(
                    expectedOrderPlacedEvent(command, EventId(6)),
                    TradeEvent(
                        bookId = command.bookId,
                        eventId = EventId(7),
                        size = expectedTradeSize,
                        price = Price(expectedTradePrice),
                        whenHappened = command.whenRequested,
                        aggressor = expectedTradeSideEntry(
                            eventId = EventId(7),
                            bookEntry = newBookEntry,
                            sizes = EntrySizes(available = 0, traded = expectedTradeSize, cancelled = 0),
                            status = EntryStatus.FILLED
                        ),
                        passive = expectedTradeSideEntry(bookEntry = oldBookEntries[expectedCounterpartEntryIndex])
                    )
                )
            }

            repo.read(bookId).let {
                it.buyLimitBook.entries.values() shouldBe List.of(
                    oldBookEntries[0],
                    oldBookEntries[2]
                )
                it.sellLimitBook.entries.values() shouldBe List.of(
                    oldBookEntries[1],
                    oldBookEntries[3]
                )
            }
        }
    }
})

