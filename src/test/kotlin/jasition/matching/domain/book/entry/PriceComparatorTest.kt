package jasition.matching.domain.book.entry

import io.kotlintest.matchers.beGreaterThan
import io.kotlintest.matchers.beLessThan
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class PriceComparatorTest : StringSpec({
    "Price Comparator with default multiplier evaluates lower value before higher"{
        PriceComparator().compare(Price(10), Price(9)) should beGreaterThan(0)
    }
    "Price Comparator with default multiplier evaluates higher value after lower"{
        PriceComparator().compare(Price(10), Price(11)) should beLessThan(0)
    }
    "Price Comparator with default multiplier evaluates same value as the same"{
        PriceComparator().compare(Price(10), Price(10)) shouldBe 0
    }
    "Price Comparator with -1 multiplier evaluates higher value before lower"{
        PriceComparator(-1).compare(Price(10), Price(11)) should beGreaterThan(0)
    }
    "Price Comparator with -1 multiplier evaluates lower value after higher"{
        PriceComparator(-1).compare(Price(10), Price(9)) should beLessThan(0)
    }
    "Price Comparator with -1 multiplier evaluates same value as the same"{
        PriceComparator(-1).compare(Price(10), Price(10)) shouldBe 0
    }
})