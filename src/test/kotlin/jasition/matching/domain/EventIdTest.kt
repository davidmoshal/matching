package jasition.matching.domain

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

internal class EventIdTest : StringSpec({
    "Cannot be negative"{
        shouldThrow<IllegalArgumentException> {
            EventId(-1)
        }
    }
    "Increments the value by one for the next Event ID"{
        EventId(1).next() shouldBe EventId(2)
    }
    "Rotates the next value to zero if the current Event ID is the maximum Long value"{
        EventId(Long.MAX_VALUE).next() shouldBe EventId(0)
    }
    "Recognises that Event ID n is not the next value of n -2"{
        EventId(3).isNextOf(EventId(1)) shouldBe false
    }
    "Recognises that Event ID n is the next value of n -1"{
        EventId(3).isNextOf(EventId(2)) shouldBe true
    }
    "Recognises that Event ID n is not the next value of n"{
        EventId(3).isNextOf(EventId(3)) shouldBe false
    }
    "Recognises that Event ID n is not the next value of n + 1"{
        EventId(3).isNextOf(EventId(4)) shouldBe false
    }
    "Recognises that Event ID n is not the next value of n + 2"{
        EventId(3).isNextOf(EventId(5)) shouldBe false
    }
    "Recognises that Event ID maximum Long value is the next value of the maximum Long value - 1"{
        EventId(Long.MAX_VALUE).isNextOf(EventId(Long.MAX_VALUE - 1)) shouldBe true
    }
    "Recognises that Event ID 0 is the next value of the maximum Long value"{
        EventId(0).isNextOf(EventId(Long.MAX_VALUE)) shouldBe true
    }
    "Recognises that Event ID 1 is the next value of 0"{
        EventId(1).isNextOf(EventId(0)) shouldBe true
    }
    "Recognises that Event ID 1 is not the next value of the maximum Long value"{
        EventId(1).isNextOf(EventId(Long.MAX_VALUE)) shouldBe false
    }
    "Evaluates that Event ID n is after n - 2"{
        EventId(8).compareTo(EventId(6)) shouldBe 1
    }
    "Evaluates that Event ID n is after n - 1"{
        EventId(8).compareTo(EventId(7)) shouldBe 1
    }
    "Evaluates that Event ID n is the same as n"{
        EventId(8).compareTo(EventId(8)) shouldBe 0
    }
    "Evaluates that Event ID n is before n + 1"{
        EventId(8).compareTo(EventId(9)) shouldBe -1
    }
    "Evaluates that Event ID n is before n + 2"{
        EventId(8).compareTo(EventId(10)) shouldBe -1
    }
    "Evaluates that Event ID Long.MAX_VALUE is before 0"{
        EventId(Long.MAX_VALUE).compareTo(EventId(0)) shouldBe -1
    }
    "Evaluates that Event ID 0 is after Long.MAX_VALUE"{
        EventId(0).compareTo(EventId(Long.MAX_VALUE)) shouldBe 1
    }
})