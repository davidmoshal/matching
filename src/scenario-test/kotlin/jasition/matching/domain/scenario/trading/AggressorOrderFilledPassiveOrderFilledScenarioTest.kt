package jasition.matching.domain.scenario.trading

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
import jasition.matching.domain.book.entry.EntryStatus
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.entry.TimeInForce.IMMEDIATE_OR_CANCEL
import jasition.matching.domain.trade.event.TradeEvent


internal class `Aggressor order filled and passive order filled` : StringSpec({
    val bookId = aBookId()

    forall(
        row(BUY, Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 8L), 17, 9L),
        row(BUY, Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), 17, 9L),
        row(BUY, Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), Tuple4(LIMIT, IMMEDIATE_OR_CANCEL, 17, 8L), 17, 9L),
        row(BUY, Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), Tuple4(LIMIT, IMMEDIATE_OR_CANCEL, 17, 9L), 17, 9L),
        row(SELL, Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 10L), 17, 9L),
        row(SELL, Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), 17, 9L),
        row(SELL, Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), Tuple4(LIMIT, IMMEDIATE_OR_CANCEL, 17, 10L), 17, 9L),
        row(SELL, Tuple4(LIMIT, GOOD_TILL_CANCEL, 17, 9L), Tuple4(LIMIT, IMMEDIATE_OR_CANCEL, 17, 9L), 17, 9L)
    ) { oldSide, old, new, expectedTradeSize, expectedTradePrice ->
        "Given the book has a $oldSide ${old.a} ${old.b.code} order ${old.c} at ${old.d}, when a ${oldSide.oppositeSide()} ${new.a} ${new.b.code} order ${new.c} at ${new.d} is placed, then the trade is executed $expectedTradeSize at $expectedTradePrice" {
            val oldCommand = randomPlaceOrderCommand(
                bookId = bookId,
                side = oldSide,
                entryType = old.a,
                timeInForce = old.b,
                size = old.c,
                price = Price(old.d)
            )
            val repo = aRepoWithABooks(bookId = bookId, commands = List.of(oldCommand))
            val command = randomPlaceOrderCommand(
                bookId = bookId,
                side = oldSide.oppositeSide(),
                entryType = new.a,
                timeInForce = new.b,
                size = new.c,
                price = Price(new.d)
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val oldBookEntry = expectedBookEntry(oldCommand, EventId(2))
            val newBookEntry = expectedBookEntry(command, EventId(4))

            with(result) {
                events shouldBe List.of(
                    expectedOrderPlacedEvent(command, EventId(3)),
                    TradeEvent(
                        bookId = command.bookId,
                        eventId = EventId(4),
                        size = expectedTradeSize,
                        price = Price(expectedTradePrice),
                        whenHappened = command.whenRequested,
                        aggressor = expectedTradeSideEntry(
                            bookEntry = newBookEntry,
                            sizes = EntrySizes(available = 0, traded = expectedTradeSize, cancelled = 0),
                            status = EntryStatus.FILLED
                        ),
                        passive = expectedTradeSideEntry(
                            bookEntry = oldBookEntry,
                            sizes = EntrySizes(
                                available = 0,
                                traded = expectedTradeSize,
                                cancelled = 0
                            ),
                            status = EntryStatus.FILLED
                        )
                    )
                )
            }
            repo.read(bookId).let {
                with(command.side) {
                    sameSideBook(it).entries.size() shouldBe 0
                    oppositeSideBook(it).entries.size() shouldBe 0
                }
            }
        }
    }
})

