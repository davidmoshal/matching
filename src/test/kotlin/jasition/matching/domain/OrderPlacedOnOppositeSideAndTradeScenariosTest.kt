package jasition.matching.domain

import io.kotlintest.matchers.beOfType
import io.kotlintest.should
import io.kotlintest.shouldBe
import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.Books
import jasition.matching.domain.book.entry.*
import jasition.matching.domain.book.event.EntryAddedToBookEvent
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.event.OrderPlacedEvent
import jasition.matching.domain.trade.event.TradeEvent
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.context
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Instant

internal object OrderPlacedOnOppositeSideAndTradeScenariosTest : Spek({
    context(": Matching Rule : Match if aggressor price is same or better than passive") {
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
            val bookId = BookId("book")
            val books = existingEntry.toEntryAddedToBookEvent(bookId).play(Books(BookId("book"))).aggregate
            on("a SELL Limit GTC Order 5@11 placed") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client2"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(11),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = orderPlacedEvent.play(books)
                it("has the existing entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 1
                    result.aggregate.buyLimitBook.entries.values().get(0) shouldBe existingEntry
                }
                it("adds the entry on the SELL side with expected order data") {
                    result.events.size() shouldBe 1
                    result.events.get(0) should beOfType<EntryAddedToBookEvent>()
                    result.aggregate.sellLimitBook.entries.size() shouldBe 1
                    result.aggregate.sellLimitBook.entries.values().get(0) shouldBe expectedBookEntry(orderPlacedEvent)
                }
            }
            on("a SELL Limit GTC Order 5@10 placed") {
                val orderPlacedEvent = OrderPlacedEvent(
                    requestId = ClientRequestId("req1"),
                    whoRequested = Client(firmId = "firm1", firmClientId = "client2"),
                    bookId = BookId("book"),
                    entryType = EntryType.LIMIT,
                    side = Side.SELL,
                    price = Price(10),
                    timeInForce = TimeInForce.GOOD_TILL_CANCEL,
                    whenHappened = Instant.now(),
                    eventId = EventId(2),
                    size = EntryQuantity(5)
                )
                val result = orderPlacedEvent.play(books)
                it("generates a Trade 4@10 and no more side-effect event") {
                    result.events.size() shouldBe 2

                    val sideEffectEvent = result.events.get(0)
                    sideEffectEvent should beOfType<TradeEvent>()

                    if (sideEffectEvent is TradeEvent) {
                        sideEffectEvent.size shouldBe 4
                        sideEffectEvent.price shouldBe Price(10)
                    }
                }
                it("remove the existing entry on the BUY side") {
                    result.aggregate.buyLimitBook.entries.size() shouldBe 0
                }
                it("adds a Sell 1@10 on the SELL side") {
                    result.events.size() shouldBe 2
                    result.events.get(1) should beOfType<EntryAddedToBookEvent>()

                    val entryAddedToBookEvent = result.events.get(1)
                    if (entryAddedToBookEvent is EntryAddedToBookEvent) {
                        result.aggregate.sellLimitBook.entries.size() shouldBe 1
                        result.aggregate.sellLimitBook.entries.values().get(0) shouldBe expectedBookEntry(
                            orderPlacedEvent
                        ).withEventId(entryAddedToBookEvent.eventId).withStatus(EntryStatus.PARTIAL_FILL).withSize(
                            EntryQuantity(
                                availableSize = 1,
                                tradedSize = 4,
                                cancelledSize = 0
                            )
                        )
                    }
                }
            }
        }
    }
})
