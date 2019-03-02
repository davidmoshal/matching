package jasition.matching.domain.scenario.recovery

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import jasition.cqrs.ConcurrentRepository
import jasition.cqrs.append
import jasition.cqrs.commitOrThrow
import jasition.cqrs.play
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.TradingStatus.OPEN_FOR_TRADING
import jasition.matching.domain.book.command.CreateBooksCommand
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

internal class `Recover books from replaying events only` : FeatureSpec({
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
            )
        } else {
            massQuoteCommandCount++
            randomPlaceMassQuoteCommand(
                bookId = bookId,
                whoRequested = aFirmWithoutClient()
            )
        }

        latest = latest.append(command.execute(repository.read(bookId)) commitOrThrow repository)
    }
    println("Book state created. noOfEvents=${latest.events.size()},  eventCountByType=${countEventsByClass(latest.events)}\n")

    feature("Recover from event re-playing") {
        scenario("Recover the books that was created by $orderCommandCount orders and $massQuoteCommandCount mass quotes with random values (Placed, Rejected and Trade)") {

            printBooksOverview("Current books", latest.aggregate)

            val start = Instant.now()

            val recovered = latest.events play initial.aggregate

            val end = Instant.now()
            printBooksOverview("Recovered books", recovered)
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



