package jasition.matching.domain.order

import io.kotlintest.shouldBe
import jasition.matching.domain.EventId
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.order.event.playOrderPlacedEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

object OrderPlacedScenariosTest : Spek({
    given("The book is empty") {
        val books = Books(BookId("book"))
        on("a BUY Limit GTC Order is placed") {
            val event = OrderPlacedEvent(
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

            val results = playOrderPlacedEvent(event, books)

            it("should contain the order") {
                results.b.buyLimitBook.entries.size() shouldBe 1
                results.b.sellLimitBook.entries.size() shouldBe 0

                val entry = results.b.buyLimitBook.entries.values().get(0)

                assertEntry(
                    entry = entry,
                    clientRequestId = event.requestId,
                    availableSize = event.size.availableSize,
                    price = event.price,
                    client = event.whoRequested
                )
            }
        }
        on("a SELL Limit GTC Order is placed") {
            val event = OrderPlacedEvent(
                requestId = ClientRequestId("req1"),
                whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                bookId = BookId("book"),
                entryType = EntryType.LIMIT,
                side = Side.SELL,
                price = Price(15),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenHappened = Instant.now(),
                eventId = EventId(1),
                size = EntryQuantity(10)
            )

            val result = playOrderPlacedEvent(event, books)

            it("should contain the order") {
                result.b.buyLimitBook.entries.size() shouldBe 0
                result.b.sellLimitBook.entries.size() shouldBe 1

                val entry = result.b.sellLimitBook.entries.values().get(0)

                assertEntry(
                    entry = entry,
                    clientRequestId = event.requestId,
                    availableSize = event.size.availableSize,
                    price = event.price,
                    client = event.whoRequested
                )
            }
        }
    }

    given("The book has a Buy Limit GTC Order 4@10") {
        val existingEntry = BookEntry(
            key = BookEntryKey(
                price = Price(10),
                whenSubmitted = Instant.now(),
                eventId = EventId(1)
            ),
            clientRequestId = ClientRequestId("oldReq1"),
            client = Client(firmId = "firm1", firmClientId = "client1"),
            entryType = EntryType.LIMIT,
            side = Side.BUY,
            timeInForce = TimeInForce.GOOD_TILL_CANCEL,
            size = EntryQuantity(4),
            status = EntryStatus.NEW
        )
        val books = Books(BookId("book"))
            .addBookEntry(
                existingEntry
            )
        on("a BUY Limit GTC Order 5@11 is placed") {
            val event = OrderPlacedEvent(
                requestId = ClientRequestId("req1"),
                whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                bookId = BookId("book"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(11),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenHappened = Instant.now(),
                eventId = EventId(2),
                size = EntryQuantity(5)
            )
            it("places the new order above the existing") {
                val results = playOrderPlacedEvent(event, books)

                it("should contain the order") {
                    results.b.buyLimitBook.entries.size() shouldBe 2
                    results.b.sellLimitBook.entries.size() shouldBe 0

                    assertEntry(
                        entry = results.b.buyLimitBook.entries.values().get(0),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                    assertEntry(
                        entry = results.b.buyLimitBook.entries.values().get(1),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                }
            }
        }
        on("a BUY Limit GTC Order 5@9 is placed") {
            val event = OrderPlacedEvent(
                requestId = ClientRequestId("req1"),
                whoRequested = Client(firmId = "firm1", firmClientId = "client1"),
                bookId = BookId("book"),
                entryType = EntryType.LIMIT,
                side = Side.BUY,
                price = Price(11),
                timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                whenHappened = Instant.now(),
                eventId = EventId(2),
                size = EntryQuantity(5)
            )
            it("places the new order below of the existing") {
                val results = playOrderPlacedEvent(event, books)

                it("should contain the order") {
                    results.b.buyLimitBook.entries.size() shouldBe 2
                    results.b.sellLimitBook.entries.size() shouldBe 0

                    assertEntry(
                        entry = results.b.buyLimitBook.entries.values().get(0),
                        clientRequestId = existingEntry.clientRequestId,
                        availableSize = existingEntry.size.availableSize,
                        price = existingEntry.key.price,
                        client = existingEntry.client
                    )
                    assertEntry(
                        entry = results.b.buyLimitBook.entries.values().get(1),
                        clientRequestId = event.requestId,
                        availableSize = event.size.availableSize,
                        price = event.price,
                        client = event.whoRequested
                    )
                }
            }
        }
    }
})

private fun assertEntry(
    entry: BookEntry,
    clientRequestId: ClientRequestId,
    availableSize: Int,
    tradedSize: Int = 0,
    cancelledSize: Int = 0,
    price: Price?,
    client: Client,
    status: EntryStatus = EntryStatus.NEW
) {
    entry.clientRequestId shouldBe clientRequestId
    entry.size.availableSize shouldBe availableSize
    entry.size.tradedSize shouldBe tradedSize
    entry.size.cancelledSize shouldBe cancelledSize
    entry.key.price shouldBe price
    entry.client shouldBe client
    entry.status shouldBe status
}

