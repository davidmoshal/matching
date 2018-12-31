package jasition.matching.domain.scenario.recovery

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import jasition.cqrs.Transaction
import jasition.cqrs.recovery.replay
import jasition.matching.domain.aBookId
import jasition.matching.domain.aBooks
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.command.CreateBooksCommand
import jasition.matching.domain.countEventsByClass
import jasition.matching.domain.randomPlaceOrderCommand
import kotlin.random.Random

internal class `Recover books from replaying events only` : FeatureSpec({
    val bookId = aBookId()
    val initial = Transaction(aBooks(bookId))
    val initialBooks = initial.aggregate

    val booksCreatedEvent = CreateBooksCommand(
        bookId = bookId,
        defaultTradingStatus = TradingStatus.OPEN_FOR_TRADING
    ).validate()

    var latest = initial
        .append(booksCreatedEvent)
        .append(booksCreatedEvent.play(initialBooks))

    val commandCount = Random.nextInt(1000, 2000)
    for (i in 0 until commandCount) {
        randomPlaceOrderCommand(bookId = bookId)
            .validate(latest.aggregate)
            .fold({ rejected ->
                latest = latest.append(rejected).append(rejected.play(latest.aggregate))
            }, { placed ->
                latest = latest.append(placed).append(placed.play(latest.aggregate))
            })
    }
    feature("Recover from event re-playing") {
        scenario("Recover the books that was created by $commandCount PlaceOrderCommands with random values (Placed, Rejected and Trade)") {

            val (aggregate, events) = latest

            println(
                "Books to recover: lastEventId=${aggregate.lastEventId}" +
                        ", buyBookDepth=${aggregate.buyLimitBook.entries.size()}" +
                        ", sellBookDepth=${aggregate.sellLimitBook.entries.size()}" +
                        ", eventCountByType=${countEventsByClass(events)}"
            )

            replay(initial = initialBooks, events = events) shouldBe aggregate
        }
    }
})



