package jasition.matching.domain.order.command

import jasition.matching.domain.order.OrderType
import jasition.matching.domain.order.Price
import jasition.matching.domain.order.Side
import jasition.matching.domain.order.TimeInForce
import jasition.matching.domain.order.Client
import java.time.Instant

data class PlaceOrderCommand(
        val requestId: String,
        val whoRequested: Client,
        val bookId: String,
        val orderType : OrderType,
        val side : Side,
        val size : Int,
        val price : Price?,
        val timeInForce: TimeInForce,
        val whenRequested : Instant
        )
