package jasition.matching.domain.book

data class TradingStatusData(val scheduledTradingStatus: TradingStatus)

enum class TradingStatus {
    OPEN_FOR_TRADING,
    CLOSED
}