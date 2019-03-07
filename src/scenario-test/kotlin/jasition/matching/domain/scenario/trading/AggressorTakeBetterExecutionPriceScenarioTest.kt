package jasition.matching.domain.scenario.trading

import arrow.core.Tuple3
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.kotlin.list
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntrySizes
import jasition.matching.domain.book.entry.EntryStatus.FILLED
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.trade.event.TradeEvent


internal class `Aggressor takes better execution price` : StringSpec({
    val bookId = aBookId()

    forall(
        row(BUY, Tuple3(LIMIT, GOOD_TILL_CANCEL, 10L), Tuple3(LIMIT, GOOD_TILL_CANCEL, 9L)),
        row(SELL, Tuple3(LIMIT, GOOD_TILL_CANCEL, 10L), Tuple3(LIMIT, GOOD_TILL_CANCEL, 11L))
    ) { oldSide, old, new ->
        "Given the book has a $oldSide ${old.a} ${old.b.code} order at ${old.c}, when a ${oldSide.oppositeSide()} ${new.a} ${new.b.code} order at ${new.c} is placed, then the trade is executed at ${old.c}" {
            val oldCommand = randomPlaceOrderCommand(
                bookId = bookId,
                side = oldSide,
                entryType = old.a,
                timeInForce = old.b,
                price = Price(old.c)
            )
            val repo = aRepoWithABooks(bookId = bookId, commands = list(oldCommand))
            val command = randomPlaceOrderCommand(
                bookId = bookId,
                side = oldSide.oppositeSide(),
                entryType = new.a,
                timeInForce = new.b,
                size = oldCommand.size,
                price = Price(new.c)
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val oldBookEntry = expectedBookEntry(
                command = oldCommand,
                eventId = EventId(1),
                sizes = EntrySizes(available = 0, traded = oldCommand.size, cancelled = 0),
                status = FILLED
            )
            val newBookEntry = expectedBookEntry(
                command = command,
                eventId = EventId(3),
                sizes = EntrySizes(available = 0, traded = command.size, cancelled = 0),
                status = FILLED
            )

            with(result) {
                events shouldBe list(
                    expectedOrderPlacedEvent(command, EventId(3)),
                    TradeEvent(
                        bookId = command.bookId,
                        eventId = EventId(4),
                        size = command.size,
                        price = oldCommand.price ?: Price(0),
                        whenHappened = command.whenRequested,
                        aggressor = expectedTradeSideEntry(bookEntry = newBookEntry),
                        passive = expectedTradeSideEntry(bookEntry = oldBookEntry)
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

