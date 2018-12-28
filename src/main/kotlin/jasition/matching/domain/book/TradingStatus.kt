package jasition.matching.domain.book

import jasition.matching.domain.order.command.PlaceOrderCommand

data class TradingStatuses(
    val default: TradingStatus,
    val scheduled: TradingStatus? = null,
    val fastMarket: TradingStatus? = null,
    val manual: TradingStatus? = null
) {
    fun effectiveStatus(): TradingStatus = manual ?: fastMarket ?: scheduled ?: default
}

enum class TradingStatus {
    OPEN_FOR_TRADING {
        override fun allows(command: PlaceOrderCommand): Boolean = true
    },
    HALTED {
        override fun allows(command: PlaceOrderCommand): Boolean = false
    },
    NOT_AVAILABLE_FOR_TRADING {
        override fun allows(command: PlaceOrderCommand): Boolean = false
    },
    PRE_OPEN {
        override fun allows(command: PlaceOrderCommand): Boolean = false
    };

    abstract fun allows(command: PlaceOrderCommand): Boolean
}