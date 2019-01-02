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


fun randomId(prefix: String = "req", from: Int = 1, to: Int = 1000): String = prefix + Random.nextInt(from, to)

fun randomSize(from: Int = 1 , until: Int = 30): Int = Random.nextInt(from, until)

fun randomPrice(): Price = Price(Random.nextLong(20, 30))

fun randomFirmWithClient(): Client = Client(
    firmId = randomId(prefix = "firm", from = 1, to = 5),
    firmClientId = randomId(prefix = "client", from = 1, to = 5)
)

fun randomPlaceOrderCommand(
    requestId: ClientRequestId = aClientRequestId(current = randomId()),
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
