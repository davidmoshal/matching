package jasition.matching.domain.scenario.recovery

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import jasition.cqrs.playAndAppend
import jasition.cqrs.recovery.replay
import jasition.cqrs.thenPlay
import jasition.matching.domain.*
import jasition.matching.domain.book.TradingStatus
import jasition.matching.domain.book.command.CreateBooksCommand
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

internal class `Recover books from replaying events only` : FeatureSpec({
    val bookId = aBookId()
    val initialBooks = aBooks(bookId)

    val booksCreatedEvent = CreateBooksCommand(
        bookId = bookId,
        defaultTradingStatus = TradingStatus.OPEN_FOR_TRADING
    ).validate()

    var latest = booksCreatedEvent playAndAppend initialBooks

    var orderCommandCount = 0
    var massQuoteCommandCount = 0

    println("About to start validating the commands and playing the events to create the book state")

    for (i in 0 until 10000) {
        val orderOrQuote = Random.nextBoolean()
        if (orderOrQuote) {
            orderCommandCount++
            randomPlaceOrderCommand(bookId = bookId, size = randomSize(from = -5, until = 30))
                .validate(latest.aggregate)
                .fold({ rejected ->
                    latest = latest thenPlay rejected
                }, { placed ->
                    latest = latest thenPlay placed
                })
        } else {
            massQuoteCommandCount++
            randomPlaceMassQuoteCommand(bookId = bookId, whoRequested = aFirmWithoutClient())
                .validate(latest.aggregate)
                .fold({ rejected ->
                    latest = latest thenPlay rejected
                }, { placed ->
                    latest = latest thenPlay placed
                })
        }
    }
    println("Book state created. noOfEvents=${latest.events.size()},  eventCountByType=${countEventsByClass(latest.events)}\n")

    feature("Recover from event re-playing") {
        scenario("Recover the books that was created by $orderCommandCount orders and $massQuoteCommandCount mass quotes with random values (Placed, Rejected and Trade)") {

            val (aggregate, events) = latest

            printBooksOverview("Current books", aggregate)

            val start = Instant.now()
            val recovered = replay(initial = initialBooks, events = events)
            val end = Instant.now()

            printBooksOverview("Recovered books", aggregate)
            println("Time spent = ${Duration.between(start, end).toMillis()} millis")
            recovered shouldBe aggregate
        }
    }
})



