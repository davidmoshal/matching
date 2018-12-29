package jasition.matching.domain.scenario.recovery

import io.kotlintest.shouldBe
import io.kotlintest.specs.FeatureSpec
import jasition.matching.domain.*
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.recovery.replay
import kotlin.random.Random

internal class `Recover books from replaying events only` : FeatureSpec({
    val bookId = aBookId()
    val books = aBooks(bookId)

    feature("Recover from event re-playing") {
        val commandCount = Random.nextInt(10, 50)
        scenario("Recover the books that was created by $commandCount PlaceOrderCommands with random values (Placed, Rejected and Trade)") {
            var latest = Transaction<BookId, Books>(books)

            for (i in 0 until commandCount) {
                randomPlaceOrderCommand(bookId = bookId)
                    .validate(latest.aggregate)
                    .fold({ rejected ->
                        latest = latest.append(rejected).append(rejected.play(latest.aggregate))
                    }, { placed ->
                        latest = latest.append(placed).append(placed.play(latest.aggregate))
                    })
            }

            val (aggregate, events) = latest

            println("Books to recover: lastEventId=${aggregate.lastEventId}, eventCountByType=${countEventsByClass(events)}")

            replay(initial = books, events = events) shouldBe aggregate
        }
    }
})



