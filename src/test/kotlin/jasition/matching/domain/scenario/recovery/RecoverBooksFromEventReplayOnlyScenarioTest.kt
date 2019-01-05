package jasition.matching.domain.scenario.recovery

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import jasition.cqrs.Event
import jasition.cqrs.Transaction
import jasition.cqrs.recovery.replay
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.command.CreateBooksCommand
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

    var orderCommandCount = 0
    var massQuoteCommandCount = 0

    for (i in 0 until Random.nextInt(1000, 2000)) {
        if (Random.nextBoolean()) {
            orderCommandCount++
            randomPlaceOrderCommand(bookId = bookId, size = randomSize(from = -5, until = 30))
                .validate(latest.aggregate)
                .fold({ rejected ->
                    latest = append(latest, rejected)
                }, { placed ->
                    latest = append(latest, placed)
                })
        } else {
            massQuoteCommandCount++
            randomPlaceMassQuoteCommand(bookId = bookId)
                .validate(latest.aggregate)
                .fold({ rejected ->
                    latest = append(latest, rejected)
                }, { placed ->
                    latest = append(latest, placed)
                })
        }
    }
    feature("Recover from event re-playing") {
        scenario("Recover the books that was created by $orderCommandCount orders and $massQuoteCommandCount mass quotes with random values (Placed, Rejected and Trade)") {

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

private fun append(
    latest: Transaction<BookId, Books>, event: Event<BookId, Books>
) = latest.append(event).append(event.play(latest.aggregate))



