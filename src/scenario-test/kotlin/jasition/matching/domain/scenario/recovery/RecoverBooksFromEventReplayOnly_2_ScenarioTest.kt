package jasition.matching.domain.scenario.recovery

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import jasition.cqrs.Command_2_
import jasition.cqrs.ConcurrentRepository
import jasition.cqrs.commitOrThrow
import jasition.cqrs.recovery.replay_2_
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.TradingStatus.OPEN_FOR_TRADING
import jasition.matching.domain.book.command.CreateBooksCommand
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

internal class `Recover books from replaying events only_2_` : FeatureSpec({
    val bookId = aBookId()

    val repository = ConcurrentRepository<BookId, Books>()

    val initial = CreateBooksCommand(bookId = bookId, defaultTradingStatus = OPEN_FOR_TRADING)
        .execute(null) commitOrThrow repository

    var orderCommandCount = 0
    var massQuoteCommandCount = 0

    println("About to start validating the commands and playing the events to create the book state\n")

    var latest = initial
    for (i in 0 until 10000) {
        val orderOrQuote = Random.nextBoolean()

        val command = if (orderOrQuote) {
            orderCommandCount++
            randomPlaceOrderCommand(
                bookId = bookId,
                size = randomSize(from = -5, until = 30)
            ) as Command_2_<BookId, Books>
        } else {
            massQuoteCommandCount++
            randomPlaceMassQuoteCommand(
                bookId = bookId,
                whoRequested = aFirmWithoutClient()
            ) as Command_2_<BookId, Books>
        }

        latest = latest.append(command.execute(repository.read(bookId)) commitOrThrow repository)
    }
    println("Book state created. noOfEvents=${latest.events.size()},  eventCountByType=${countEventsByClass(latest.events)}\n")

    feature("Recover from event re-playing") {
        scenario("Recover the books that was created by $orderCommandCount orders and $massQuoteCommandCount mass quotes with random values (Placed, Rejected and Trade)") {

            val (aggregate, events) = latest

            printBooksOverview("Current books", aggregate)

            val start = Instant.now()

            val recovered = replay_2_(initial.aggregate, latest.events)

            val end = Instant.now()
            printBooksOverview("Recovered books", aggregate)
            println("Time spent = ${Duration.between(start, end).toMillis()} millis")
            recovered shouldBe latest.aggregate
        }
    }
})

fun printBooksOverview(name: String, aggregate: Books) {
    println(
        name +
                ": lastEventId=${aggregate.lastEventId}" +
                ", buyBookDepth=${aggregate.buyLimitBook.entries.size()}" +
                ", sellBookDepth=${aggregate.sellLimitBook.entries.size()}\n"
    )
}



