package jasition.cqrs

import io.kotlintest.specs.StringSpec
import io.mockk.*

internal class CreateOrUpdateFunctionTest : StringSpec({
    val aggregate = TestAggregate()

    "CreateOrUpdateFunction invokes createOrUpdateFunction" {
        val repository = spyk<Repository<Int, TestAggregate>>()
        CreateOrUpdateFunction.update(aggregate, repository)

        verify { repository.createOrUpdate(aggregate) }
        confirmVerified(repository)
    }
    "CreateIfAbsentFunction invokes createIfAbsent" {
        val repository = mockk<Repository<Int, TestAggregate>>(relaxed = true)
        CreateIfAbsentFunction.update(aggregate, repository)

        every { repository.createIfAbsent(aggregate) } returns true

        verify { repository.createIfAbsent(aggregate) }
        confirmVerified(repository)
    }
    "UpdateIfPresentFunction invokes updateIfPresent" {
        val repository = mockk<Repository<Int, TestAggregate>>(relaxed = true)
        UpdateIfPresentFunction.update(aggregate, repository)

        every { repository.updateIfPresent(aggregate) } returns true

        verify { repository.updateIfPresent(aggregate) }
        confirmVerified(repository)
    }
    "DeleteFunction invokes delete" {
        val repository = spyk<Repository<Int, TestAggregate>>()
        DeleteFunction.update(aggregate, repository)

        every { repository.delete(aggregate.aggregateId) } returns aggregate

        verify { repository.delete(aggregate.aggregateId) }
        confirmVerified(repository)
    }
})