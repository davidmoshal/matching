package jasition.matching.domain

import jasition.matching.domain.book.BookId
import jasition.matching.domain.book.entry.EntryType
import jasition.matching.domain.book.entry.Price
import jasition.matching.domain.book.entry.Side
import jasition.matching.domain.book.entry.TimeInForce
import jasition.matching.domain.client.Client
import jasition.matching.domain.client.ClientRequestId
import jasition.matching.domain.order.command.PlaceOrderCommand
import java.time.Instant
import kotlin.random.Random

fun randomSize(): Int = Random.nextInt(-5, 30)

fun randomPrice(): Price = Price(Random.nextLong(20, 30))

fun randomFirmWithClient(): Client = Client(
    firmId = "firm" + Random.nextInt(1, 5),
    firmClientId = "client" + Random.nextInt(1, 5)
)

fun randomPlaceOrderCommand(
    requestId: ClientRequestId = aClientRequestId(current = "req" + Random.nextInt()),
    bookId: BookId = aBookId(),
    entryType: EntryType = EntryType.LIMIT,
    side: Side = if (Random.nextBoolean()) Side.BUY else Side.SELL,
    price: Price? = randomPrice(),
    timeInForce: TimeInForce = TimeInForce.GOOD_TILL_CANCEL,
    whenRequested: Instant = Instant.now(),
    whoRequested: Client = randomFirmWithClient(),
    size: Int = randomSize()
) = PlaceOrderCommand(
    requestId = requestId,
    bookId = bookId,
    entryType = entryType,
    side = side,
    price = price,
    timeInForce = timeInForce,
    whenRequested = whenRequested,
    whoRequested = whoRequested,
    size = size
)
