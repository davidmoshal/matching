package jasition.matching.domain.order

data class OrderQuantity(
    val availableSize: Int,
    val tradedSize: Int,
    val cancelledSize: Int
) {

    init {
        if (availableSize or tradedSize or cancelledSize < 0) {
            throw IllegalStateException("Order sizes cannot be negative: available=$availableSize, traded=$tradedSize, cancelled=$cancelledSize")
        }
    }
}