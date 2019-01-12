import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class SampleTest : StringSpec({
    "one is one" {
        1 shouldBe 1
    }
})