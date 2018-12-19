package jasition.matching.domain.book

data class TradingStatuses(
    val default: TradingStatus,
    val scheduled: TradingStatus? = null,
    val fastMarket: TradingStatus? = null,
    val manual: TradingStatus? = null
) {
    fun effectiveStatus(): TradingStatus = manual ?: fastMarket ?: scheduled ?: default
}

enum class TradingStatus {
    OPEN_FOR_TRADING,
    CLOSED
}