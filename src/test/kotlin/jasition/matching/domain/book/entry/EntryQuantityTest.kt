package jasition.matching.domain.book.entry

import io.kotlintest.shouldBe
import org.junit.jupiter.api.Test

internal class EntryQuantityTest {

    @Test
    fun addTradedSizeAndReduceAvailableWhenTraded() {
        val given = EntryQuantity(availableSize = 100, tradedSize = 25, cancelledSize = 33)

        given.traded(25) shouldBe EntryQuantity(availableSize = 75, tradedSize = 50, cancelledSize = 33)
    }

    @Test
    fun amended() {
    }

    @Test
    fun cancelled() {
    }
}