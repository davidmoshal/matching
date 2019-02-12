package jasition.matching.domain.scenario.trading

import arrow.core.Tuple3
import arrow.core.Tuple4
import io.kotlintest.data.forall
import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import io.kotlintest.tables.row
import io.vavr.collection.List
import jasition.cqrs.EventId
import jasition.cqrs.commitOrThrow
import jasition.matching.domain.*
import jasition.matching.domain.book.entry.EntryType.LIMIT
import jasition.matching.domain.book.entry.Side.BUY
import jasition.matching.domain.book.entry.Side.SELL
import jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL
import jasition.matching.domain.book.entry.TimeInForce.IMMEDIATE_OR_CANCEL
import jasition.matching.domain.book.event.EntryAddedToBookEvent

internal class `Given the book is empty _2_` : FeatureSpec({
    val addOrderToEmptyBookFeature = "[1 - Add order to empty book] "
    val addQuotesToEmptyBookFeature = "[2 - Add mass quote to empty book] "

    val bookId = aBookId()

    feature(addOrderToEmptyBookFeature) {
        forall(
            row(BUY, LIMIT, GOOD_TILL_CANCEL),
            row(SELL, LIMIT, GOOD_TILL_CANCEL)
        ) { side, entryType, timeInForce ->
            scenario(addOrderToEmptyBookFeature + "When a $side $entryType ${timeInForce.code} order is placed, then the entry is added to the $side limit book") {
                val repo = aRepoWithAnEmptyBook(bookId = bookId)
                val command = randomPlaceOrderCommand(
                    bookId = bookId,
                    side = side,
                    entryType = entryType,
                    timeInForce = timeInForce
                )

                val result = command.execute(repo.read(bookId)) commitOrThrow repo

                val expectedBookEntry = expectedBookEntry(command, EventId(1))

                with(result) {
                    events shouldBe List.of(
                        expectedOrderPlacedEvent(command, EventId(1)),
                        EntryAddedToBookEvent(bookId = bookId, eventId = EventId(2), entry = expectedBookEntry)
                    )
                }

                repo.read(bookId).let {
                    with(command.side) {
                        sameSideBook(it).entries.values() shouldBe List.of(expectedBookEntry)
                        oppositeSideBook(it).entries.size() shouldBe 0
                    }
                }
            }
        }

        forall(
            row(BUY, LIMIT, IMMEDIATE_OR_CANCEL),
            row(SELL, LIMIT, IMMEDIATE_OR_CANCEL)
        ) { side, entryType, timeInForce ->
            scenario(addOrderToEmptyBookFeature + "When a $side $entryType ${timeInForce.code} order is placed, then the order is cancelled") {
                val repo = aRepoWithAnEmptyBook(bookId = bookId)

                val command = randomPlaceOrderCommand(
                    bookId = bookId,
                    side = side,
                    entryType = entryType,
                    timeInForce = timeInForce
                )

                val result = command.execute(repo.read(bookId)) commitOrThrow repo

                with(result) {
                    events shouldBe List.of(
                        expectedOrderPlacedEvent(command, EventId(1)),
                        expectedOrderCancelledByExchangeEvent(command, EventId(2))
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
    }
    forall(
        row(List.of(Tuple4(4, 10L, 4, 11L), Tuple4(5, 9L, 5, 12L)))
    ) { entries ->

        val entriesAsString = entries.map { "(BUY ${it.a} at ${it.b} SELL ${it.c} at ${it.d})" }
            .intersperse(", ")
            .fold("") { s1, s2 -> s1 + s2 }

        feature(addQuotesToEmptyBookFeature) {
            scenario(addQuotesToEmptyBookFeature + "When a mass quote of ($entriesAsString) is placed, and all quote entries are added") {
                val repo = aRepoWithAnEmptyBook(bookId = bookId)
                val command = randomPlaceMassQuoteCommand(bookId = bookId, entries = entries)

                val result = command.execute(repo.read(bookId)) commitOrThrow repo

                val expectedBookEntries = List.of(
                    Tuple3(0, EventId(2), BUY),
                    Tuple3(0, EventId(3), SELL),
                    Tuple3(1, EventId(4), BUY),
                    Tuple3(1, EventId(5), SELL)
                ).map { expectedBookEntry(command = command, entryIndex = it.a, eventId = it.b, side = it.c) }

                with(result) {
                    events shouldBe List.of(
                        expectedMassQuotePlacedEvent(command, EventId(1)),
                        EntryAddedToBookEvent(bookId, EventId(2), expectedBookEntries[0]),
                        EntryAddedToBookEvent(bookId, EventId(3), expectedBookEntries[1]),
                        EntryAddedToBookEvent(bookId, EventId(4), expectedBookEntries[2]),
                        EntryAddedToBookEvent(bookId, EventId(5), expectedBookEntries[3])
                    )
                }

                repo.read(bookId).let {
                    it.buyLimitBook.entries.values() shouldBe List.of(expectedBookEntries[0], expectedBookEntries[2])
                    it.sellLimitBook.entries.values() shouldBe List.of(expectedBookEntries[1], expectedBookEntries[3])
                }
            }
        }
    }
})