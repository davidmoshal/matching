package jasition.cqrs

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

internal class ConcurrentRepositoryTest : StringSpec({
    "Returns null if the aggregate is not found" {
        val repository = ConcurrentRepository<Int, TestAggregate>()

        repository.find(3) shouldBe null
    }
    "Returns aggregate if it is found" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val aggregate = TestAggregate(1)
        repository.createOrUpdate(aggregate)

        repository.find(1) shouldBe aggregate
    }

})