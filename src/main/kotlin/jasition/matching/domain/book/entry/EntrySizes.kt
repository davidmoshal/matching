package jasition.matching.domain.book.entry

data class EntrySizes(
    val available: Int,
    val traded: Int = 0,
    val cancelled: Int = 0
) {
    init {
        if (available < 0 || traded < 0 || cancelled < 0) {
            throw IllegalArgumentException("Order sizes cannot be negative: available=$available, traded=$traded, cancelled=$cancelled")
        }
    }

    fun traded(size: Int): EntrySizes = copy(
        available = available - size,
        traded = traded + size
    )

    fun amended(newOrderSize: Int): EntrySizes = copy(
        available =
        if (newOrderSize == traded + cancelled)
            throw IllegalArgumentException("Cannot amend to zero available sizes: $newOrderSize")
        else newOrderSize - traded - cancelled
    )

    fun cancelled(): EntrySizes = copy(
        available = 0,
        cancelled = cancelled + available
    )
}