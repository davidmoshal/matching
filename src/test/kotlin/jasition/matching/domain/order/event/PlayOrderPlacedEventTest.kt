package jasition.matching.domain.order.event

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import jasition.matching.domain.EventId
import jasition.matching.domain.assertEntry
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

object PlayOrderPlacedEventTest : Spek({
    given("The book is empty") {
        val books = Books(BookId("book"))
        on("an order placed") {
            val orderPlacedEvent = OrderPlacedEvent(
                requestId = ClientRequestId("req1"),
                whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                bookId = BookId("book"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(15),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenHappened = Instant.now(),
                eventId = EventId(1),
                size = EntryQuantity(10)
            )

            val result = orderPlacedEvent.play(books)

            it("has no effect on the opposite side") {
                result.aggregate.sellLimitBook.entries.size() shouldBe 0
            }
            it("adds the order to the book") {
                result.events.size() shouldBe 1
                val entryAddedToBookEvent = result.events.get(0)
                entryAddedToBookEvent should beOfType<EntryAddedToBookEvent>()
                if (entryAddedToBookEvent is EntryAddedToBookEvent) {

                    assertEntry(
                        entry = entryAddedToBookEvent.entry,
                        clientRequestId = orderPlacedEvent.requestId,
                        availableSize = orderPlacedEvent.size.availableSize,
                        price = orderPlacedEvent.price,
                        client = orderPlacedEvent.whoRequested
                    )
                }

                result.aggregate.buyLimitBook.entries.size() shouldBe 1

                val entry = result.aggregate.buyLimitBook.entries.values().get(0)

                entry.clientRequestId shouldBe orderPlacedEvent.requestId
                entry.client shouldBe orderPlacedEvent.whoRequested
                entry.entryType shouldBe orderPlacedEvent.entryType
                entry.side shouldBe orderPlacedEvent.side
                entry.key.price shouldBe orderPlacedEvent.price
                entry.timeInForce shouldBe orderPlacedEvent.timeInForce
                entry.key.whenSubmitted shouldBe orderPlacedEvent.whenHappened
                entry.key.eventId shouldBe entryAddedToBookEvent.eventId()
                entry.size.availableSize shouldBe orderPlacedEvent.size.availableSize
                entry.size.tradedSize shouldBe 0
                entry.size.cancelledSize shouldBe 0
                entry.status shouldBe EntryStatus.NEW
            }
        }
    }
})

