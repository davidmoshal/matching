package jasition.cqrs

import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

internal class ConcurrentRepositoryTest : StringSpec({
    "Find: Returns null if the aggregate was not found" {
        val repository = ConcurrentRepository<Int, TestAggregate>()

        repository.find(3) shouldBe null
    }
    "Find: Returns aggregate if it was found" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val aggregate = TestAggregate(1)
        repository.createOrUpdate(aggregate)

        repository.find(1) shouldBe aggregate
    }
    "Read: Throws exception if the aggregate did not exist" {
        val repository = ConcurrentRepository<Int, TestAggregate>()

        shouldThrow<NoSuchElementException> {
            repository.read(1)
        }
    }
    "Read: Returns if the aggregate existed" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val aggregate = TestAggregate(1)
        repository.createOrUpdate(aggregate)

        repository.read(1) shouldBe aggregate
    }
    "CreateIfAbsent: Adds aggregate if did not exist" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val aggregate = TestAggregate(1, "original")

        repository.createIfAbsent(aggregate) shouldBe true
        repository.find(1) shouldBe aggregate
    }
    "CreateIfAbsent: Does not replace aggregate if already existed" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val original = TestAggregate(1, "original")
        val new = TestAggregate(1, "new")
        repository.createOrUpdate(original)

        repository.createIfAbsent(new) shouldBe false
        repository.find(1) shouldBe original
    }
    "createOrUpdate: Adds aggregate if did not exist" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val aggregate = TestAggregate(1, "original")

        repository.createOrUpdate(aggregate)
        repository.find(1) shouldBe aggregate
    }
    "createOrUpdate: Replaces aggregate if already existed" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val original = TestAggregate(1, "original")
        val new = TestAggregate(1, "new")
        repository.createOrUpdate(original)

        repository.createOrUpdate(new)
        repository.find(1) shouldBe new
    }
    "updateIfPresent: Does not add aggregate if did not exist" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val aggregate = TestAggregate(1, "original")

        repository.updateIfPresent(aggregate) shouldBe false
        repository.find(1) shouldBe null
    }
    "updateIfPresent: Replaces aggregate if already existed" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val original = TestAggregate(1, "original")
        val new = TestAggregate(1, "new")
        repository.createOrUpdate(original)

        repository.updateIfPresent(new) shouldBe true
        repository.find(1) shouldBe new
    }
    "delete: Returns null if the aggregate did not exist" {
        val repository = ConcurrentRepository<Int, TestAggregate>()

        repository.delete(1) shouldBe null
    }
    "delete: Removes aggregate if already existed" {
        val repository = ConcurrentRepository<Int, TestAggregate>()
        val aggregate = TestAggregate(1, "original")
        repository.createOrUpdate(aggregate)

        repository.delete(1) shouldBe aggregate
        repository.find(1) shouldBe null
    }
})