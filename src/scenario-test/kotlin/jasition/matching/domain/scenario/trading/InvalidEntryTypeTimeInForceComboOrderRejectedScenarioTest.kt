package jasition.matching.domain.scenario.trading

import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.kotlin.list
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.aBookId
import jasition.matching.domain.aRepoWithABooks
import jasition.matching.domain.book.entry.EntryType.MARKET
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.order.event.OrderRejectReason
import jasition.matching.domain.order.event.OrderRejectedEvent
import jasition.matching.domain.randomPlaceOrderCommand

internal class `Invalid entry type and time in force Combo Order rejected` : StringSpec({
    val bookId = aBookId()

    forall(
        row(BUY, MARKET, GOOD_TILL_CANCEL, null),
        row(SELL, MARKET, GOOD_TILL_CANCEL, null)
    ) { side, entryType, timeInForce, price ->
        "When a $side $entryType ${timeInForce.code} order is requested, then the command is requested" {
            val repo = aRepoWithABooks(bookId = bookId)
            val command = randomPlaceOrderCommand(
                bookId = bookId,
                side = side,
                entryType = entryType,
                timeInForce = timeInForce,
                price = price
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val expectedEventId = EventId(1)

            with(result) {
                events shouldBe list(
                    OrderRejectedEvent(
                        eventId = expectedEventId,
                        requestId = command.requestId,
                        whoRequested = command.whoRequested,
                        bookId = bookId,
                        entryType = command.entryType,
                        side = command.side,
                        size = command.size,
                        price = command.price,
                        timeInForce = command.timeInForce,
                        whenHappened = command.whenRequested,
                        rejectReason = OrderRejectReason.UNSUPPORTED_ORDER_CHARACTERISTIC,
                        rejectText = "${command.entryType} ${command.timeInForce.code} is not supported"
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