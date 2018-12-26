package jasition.matching.domain.book

import io.kotlintest.shouldBe
import io.kotlintest.specs.DescribeSpec
import io.mockk.mockk

internal class TradingStatusesTest : DescribeSpec() {
    init {
        describe("TradingStatuses") {
            it("prioritises manual status over fast market") {
                TradingStatuses(
                    default = TradingStatus.NOT_AVAILABLE_FOR_TRADING,
                    scheduled = TradingStatus.PRE_OPEN,
                    fastMarket = TradingStatus.HALTED,
                    manual = TradingStatus.OPEN_FOR_TRADING
                ).effectiveStatus() shouldBe TradingStatus.OPEN_FOR_TRADING
            }
            it("prioritises fast market status over scheduled") {
                TradingStatuses(
                    default = TradingStatus.NOT_AVAILABLE_FOR_TRADING,
                    scheduled = TradingStatus.PRE_OPEN,
                    fastMarket = TradingStatus.HALTED
                ).effectiveStatus() shouldBe TradingStatus.HALTED
            }
            it("prioritises scheduled status over default") {
                TradingStatuses(
                    default = TradingStatus.NOT_AVAILABLE_FOR_TRADING,
                    scheduled = TradingStatus.PRE_OPEN
                ).effectiveStatus() shouldBe TradingStatus.PRE_OPEN
            }
            it("uses default status when all else is absent") {
                TradingStatuses(
                    default = TradingStatus.NOT_AVAILABLE_FOR_TRADING
                ).effectiveStatus() shouldBe TradingStatus.NOT_AVAILABLE_FOR_TRADING
            }
        }
    }
}

internal class TradingStatusTest : DescribeSpec() {
    init {
        describe("TradingStatus") {
            it("'Open for Trading' allows PlaceOrderCommand") {
                TradingStatus.OPEN_FOR_TRADING.allows(mockk()) shouldBe true
            }
            it("'Halted' disallows PlaceOrderCommand") {
                TradingStatus.HALTED.allows(mockk()) shouldBe false
            }
            it("'Not available for Trading' disallows PlaceOrderCommand") {
                TradingStatus.NOT_AVAILABLE_FOR_TRADING.allows(mockk()) shouldBe false
            }
            it("'Pre-open' disallows PlaceOrderCommand") {
                TradingStatus.PRE_OPEN.allows(mockk()) shouldBe false
            }
        }
    }
}