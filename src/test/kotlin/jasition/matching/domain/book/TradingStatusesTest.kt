package jasition.matching.domain.book

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.mockk.mockk

internal class TradingStatusesTest : StringSpec({
    "Prioritises manual status over fast market"{
        TradingStatuses(
            default = TradingStatus.NOT_AVAILABLE_FOR_TRADING,
            scheduled = TradingStatus.PRE_OPEN,
            fastMarket = TradingStatus.HALTED,
            manual = TradingStatus.OPEN_FOR_TRADING
        ).effectiveStatus() shouldBe TradingStatus.OPEN_FOR_TRADING
    }
    "Prioritises fast market status over scheduled"{
        TradingStatuses(
            default = TradingStatus.NOT_AVAILABLE_FOR_TRADING,
            scheduled = TradingStatus.PRE_OPEN,
            fastMarket = TradingStatus.HALTED
        ).effectiveStatus() shouldBe TradingStatus.HALTED
    }
    "Prioritises scheduled status over default"{
        TradingStatuses(
            default = TradingStatus.NOT_AVAILABLE_FOR_TRADING,
            scheduled = TradingStatus.PRE_OPEN
        ).effectiveStatus() shouldBe TradingStatus.PRE_OPEN
    }
    "Uses default status when all else is absent"{
        TradingStatuses(
            default = TradingStatus.NOT_AVAILABLE_FOR_TRADING
        ).effectiveStatus() shouldBe TradingStatus.NOT_AVAILABLE_FOR_TRADING
    }
})

internal class TradingStatusTest : StringSpec({
    "Open-for-trading allows PlaceOrderCommand"{
        TradingStatus.OPEN_FOR_TRADING.allows(mockk()) shouldBe true
    }
    "Halted disallows PlaceOrderCommand"{
        TradingStatus.HALTED.allows(mockk()) shouldBe false
    }
    "Not-available-for-trading disallows PlaceOrderCommand"{
        TradingStatus.NOT_AVAILABLE_FOR_TRADING.allows(mockk()) shouldBe false
    }
    "Pre-open disallows PlaceOrderCommand"{
        TradingStatus.PRE_OPEN.allows(mockk()) shouldBe false
    }
})