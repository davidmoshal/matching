package jasition.matching.domain

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.DescribeSpec

internal class EventIdTest : DescribeSpec() {
    init {
        describe("Event ID") {
            it("cannot be negative") {
                shouldThrow<IllegalArgumentException> {
                    EventId(-1)
                }
            }
            it ("increments the value by one for the next Event ID"){
                EventId(1).next() shouldBe EventId(2)
            }
            it ("rotates the next value to zero if the current Event ID is the maximum Long value"){
                EventId(Long.MAX_VALUE).next() shouldBe EventId(0)
            }
            it ("recognises that Event ID n is not the next value of n -2"){
                EventId(3).isNextOf(EventId(1)) shouldBe false
            }
            it ("recognises that Event ID n is the next value of n -1"){
                EventId(3).isNextOf(EventId(2)) shouldBe true
            }
            it ("recognises that Event ID n is not the next value of n"){
                EventId(3).isNextOf(EventId(3)) shouldBe false
            }
            it ("recognises that Event ID n is not the next value of n + 1"){
                EventId(3).isNextOf(EventId(4)) shouldBe false
            }
            it ("recognises that Event ID n is not the next value of n + 2"){
                EventId(3).isNextOf(EventId(5)) shouldBe false
            }
            it ("recognises that Event ID maximum Long value is the next value of the maximum Long value - 1"){
                EventId(Long.MAX_VALUE).isNextOf(EventId(Long.MAX_VALUE - 1)) shouldBe true
            }
            it ("recognises that Event ID 0 is the next value of the maximum Long value"){
                EventId(0).isNextOf(EventId(Long.MAX_VALUE)) shouldBe true
            }
            it ("recognises that Event ID 1 is the next value of 0"){
                EventId(1).isNextOf(EventId(0)) shouldBe true
            }
            it ("recognises that Event ID 1 is not the next value of the maximum Long value"){
                EventId(1).isNextOf(EventId(Long.MAX_VALUE)) shouldBe false
            }
            it ("evaluates that Event ID n is after n - 2"){
                EventId(8).compareTo(EventId(6)) shouldBe 1
            }
            it ("evaluates that Event ID n is after n - 1"){
                EventId(8).compareTo(EventId(7)) shouldBe 1
            }
            it ("evaluates that Event ID n is the same as n"){
                EventId(8).compareTo(EventId(8)) shouldBe 0
            }
            it ("evaluates that Event ID n is before n + 1"){
                EventId(8).compareTo(EventId(9)) shouldBe -1
            }
            it ("evaluates that Event ID n is before n + 2"){
                EventId(8).compareTo(EventId(10)) shouldBe -1
            }
            it ("evaluates that Event ID Long.MAX_VALUE is before 0"){
                EventId(Long.MAX_VALUE).compareTo(EventId(0)) shouldBe -1
            }
            it ("evaluates that Event ID 0 is after Long.MAX_VALUE"){
                EventId(0).compareTo(EventId(Long.MAX_VALUE)) shouldBe 1
            }
        }
    }
}