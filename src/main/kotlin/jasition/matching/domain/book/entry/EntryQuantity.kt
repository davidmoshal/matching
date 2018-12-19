package jasition.matching.domain.book.entry

data class EntryQuantity(
    val availableSize: Int,
    val tradedSize: Int,
    val cancelledSize: Int
) {
    init {
        if (availableSize < 0 || tradedSize < 0 || cancelledSize < 0) {
            throw IllegalStateException("Order sizes cannot be negative: available=$availableSize, traded=$tradedSize, cancelled=$cancelledSize")
        }
    }
}