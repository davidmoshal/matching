package jasition.matching.domain.book.entry

import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.beLessThan
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.DescribeSpec

internal class PriceComparatorTest : DescribeSpec() {
    init {
        describe("Price Comparator with default multiplier") {
            val comparator = PriceComparator()
            it("evaluates lower value before higher") {
                comparator.compare(Price(10), Price(9)) should beGreaterThan(0)
            }
            it("evaluates higher value after lower") {
                comparator.compare(Price(10), Price(11)) should beLessThan(0)
            }
            it("evaluates same value as the same") {
                comparator.compare(Price(10), Price(10)) shouldBe 0
            }
        }
        describe("Price Comparator with -1 multiplier") {
            val comparator = PriceComparator(-1)
            it("evaluates higher value before lower") {
                comparator.compare(Price(10), Price(11)) should beGreaterThan(0)
            }
            it("evaluates lower value after higher") {
                comparator.compare(Price(10), Price(9)) should beLessThan(0)
            }
            it("evaluates same value as the same") {
                comparator.compare(Price(10), Price(10)) shouldBe 0
            }
        }
    }
}