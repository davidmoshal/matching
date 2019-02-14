package jasition.matching.domain.scenario.trading

import arrow.core.Tuple5
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client


internal class `Wash trades betweeen sides identified as the same firm or same client are prevented` : StringSpec
    ({
    val bookId = aBookId()

    forall(
        row(
            BUY,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1"),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 9L, "firm1", "client1")
        ),
        row(
            BUY,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", null),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 9L, "firm1", "client1")
        ),
        row(
            BUY,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1"),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 9L, "firm1", null)
        ),
        row(
            BUY,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1"),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1")
        ),
        row(
            BUY,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", null),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1")
        ),
        row(
            BUY,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1"),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", null)
        ),
        row(
            SELL,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1"),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 11L, "firm1", "client1")
        ),
        row(
            SELL,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", null),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 11L, "firm1", "client1")
        ),
        row(
            SELL,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1"),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 11L, "firm1", null)
        ),
        row(
            SELL,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1"),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1")
        ),
        row(
            SELL,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", null),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1")
        ),
        row(
            SELL,
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", "client1"),
            Tuple5(LIMIT, GOOD_TILL_CANCEL, 10L, "firm1", null)
        )
    ) { oldSide, old, new ->
        "Given the book has a $oldSide ${old.a} ${old.b.code} order at ${old.c} by ${old.d}/${old.e}, when a ${oldSide.oppositeSide()} ${new.a} ${new.b.code} order at ${new.c} is placed by ${new.d}/${new.e}, then the new entry is added and there is no trade." {
            val oldCommand = randomPlaceOrderCommand(
                bookId = bookId,
                side = oldSide,
                entryType = old.a,
                timeInForce = old.b,
                price = Price(old.c),
                whoRequested = Client(firmId = old.d, firmClientId = old.e)
            )
            val repo = aRepoWithABooks(bookId = bookId, commands = List.of(oldCommand))
            val command = randomPlaceOrderCommand(
                bookId = bookId,
                side = oldSide.oppositeSide(),
                entryType = new.a,
                timeInForce = new.b,
                price = Price(new.c),
                whoRequested = Client(firmId = new.d, firmClientId = new.e)
            )

            val result = command.execute(repo.read(bookId)) commitOrThrow repo

            val oldBookEntry = expectedBookEntry(oldCommand, EventId(2))
            val newBookEntry = expectedBookEntry(command, EventId(4))

            with(result) {
                events shouldBe List.of(
                    expectedOrderPlacedEvent(command, EventId(3)),
                    EntryAddedToBookEvent(bookId = bookId, eventId = EventId(4), entry = newBookEntry)
                )
            }
            repo.read(bookId).let {
                with(command.side) {
                    sameSideBook(it).entries.values() shouldBe List.of(newBookEntry)
                    oppositeSideBook(it).entries.values() shouldBe List.of(oldBookEntry)
                }
            }
        }
    }
})

