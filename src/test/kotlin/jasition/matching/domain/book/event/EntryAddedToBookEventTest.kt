package jasition.matching.domain.book.event

import io.kotlintest.shouldBe
import io.kotlintest.specs.BehaviorSpec
import io.kotlintest.specs.DescribeSpec
import jasition.matching.domain.EventId
import jasition.matching.domain.EventType
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import java.time.Instant

internal class EntryAddedToBookEventPropertyTest : DescribeSpec() {
    init {
        val eventId = EventId(1)
        val bookId = BookId("book")
        val event = EntryAddedToBookEvent(
            bookId = bookId,
            whenHappened = Instant.now(),
            eventId = eventId,
            entry = BookEntry(
                key = BookEntryKey(
                    price = Price(15),
                    whenSubmitted = Instant.now(),
                    eventId = EventId(1)
                ),

                clientRequestId = ClientRequestId("req1"),
                client = Client(firmId = "firm1", firmClientId = "client1"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                size = EntryQuantity(10),
                status = EntryStatus.NEW
            )
        )
        describe("EntryAddedToBookEvent") {
            it("has Book ID as Aggregate ID") {
                event.aggregateId() shouldBe bookId
            }
            it("has Event ID as Event ID") {
                event.eventId() shouldBe eventId
            }
            it("is a Side-effect event") {
                event.eventType() shouldBe EventType.SIDE_EFFECT
            }
        }
    }
}

internal class EntryAddedToBookEventBehaviourTest : BehaviorSpec() {
    init {
        given("the book is empty") {
            val books = Books(BookId(bookId = "book"))

            `when`("an entry is added to the book") {
                val eventId = EventId(1)
                val event = EntryAddedToBookEvent(
                    bookId = books.bookId,
                    whenHappened = Instant.now(),
                    eventId = eventId,
                    entry = BookEntry(
                        key = BookEntryKey(
                            price = Price(15),
                            whenSubmitted = Instant.now(),
                            eventId = EventId(1)
                        ),

                        clientRequestId = ClientRequestId("req1"),
                        client = Client(firmId = "firm1", firmClientId = "client1"),
                        entryType = EntryType.LIMIT,
                        side = Side.BUY,
                        timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                        size = EntryQuantity(10),
                        status = EntryStatus.NEW
                    )
                )
                then("the entry is added to the aggregate") {
                    val result = event.play(books)
                }
            }
        }
    }
}