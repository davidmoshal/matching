package jasition.cqrs

import java.util.concurrent.ConcurrentHashMap

/**
 * This is probably the only CRUD-style interface as the aggregate roots still need a repository to host them.
 */
interface Repository<KEY, AGG : Aggregate<KEY>> {
    /**
     * Finds the aggregate by the given [aggregateId] and it may be present or absent.
     */
    fun find(aggregateId : KEY) : AGG?

    /**
     * Returns the aggregate by the given [aggregateId] and expects it to be present.
     */
    fun read(aggregateId : KEY) : AGG

    /**
     * Stores the [aggregate] if the aggregate did not exist in the repository. Returns true if the given [aggregate] was
     * stored in the repository.
     */
    fun createIfAbsent(aggregate : AGG) : Boolean

    /**
     * Stores the given [aggregate] and potentially overwrites the existing aggregate of the same aggregateId.
     */
    fun createOrUpdate(aggregate : AGG)

    /**
     * Overwrites the existing aggregate of the same aggregateId by the given [aggregate] if present. Returns true if
     * the existing aggregate was overwritten.
     */
    fun updateIfPresent(aggregate : AGG) : Boolean

    /**
     * Removes the aggregate by the given [aggregateId]. Returns the existing aggregate in the repository if present.
     */
    fun delete(aggregateId : KEY) : AGG?
}

class ConcurrentRepository<KEY, AGG : Aggregate<KEY>> (initialSize : Int) : Repository<KEY, AGG> {
    constructor() : this (256)

    private val delegate = ConcurrentHashMap<KEY, AGG>(initialSize)

    override fun find(aggregateId: KEY): AGG? = delegate[aggregateId]

    override fun read(aggregateId: KEY): AGG =
        delegate[aggregateId] ?: throw NoSuchElementException("Cannot find the aggregate $aggregateId")

    override fun createIfAbsent(aggregate: AGG): Boolean =
        delegate.putIfAbsent(aggregate.aggregateId(), aggregate) == null

    override fun createOrUpdate(aggregate: AGG) {
        delegate[aggregate.aggregateId()] = aggregate
    }

    override fun updateIfPresent(aggregate: AGG): Boolean =
        delegate.computeIfPresent(aggregate.aggregateId()) { _, _ -> aggregate } != null

    override fun delete(aggregateId: KEY): AGG? = delegate.remove(aggregateId)
}

interface RepositoryUpdateFunction {
    fun <KEY, AGG : Aggregate<KEY>> update(aggregate: AGG, repository: Repository<KEY, AGG>)
}

object CreateOrUpdateFunction : RepositoryUpdateFunction {
    override fun <KEY, AGG : Aggregate<KEY>> update(aggregate: AGG, repository: Repository<KEY, AGG>) {
        repository.createOrUpdate(aggregate)
    }
}

object CreateIfAbsentFunction : RepositoryUpdateFunction {
    override fun <KEY, AGG : Aggregate<KEY>> update(aggregate: AGG, repository: Repository<KEY, AGG>) {
        repository.createIfAbsent(aggregate)
    }
}

object UpdateIfPresentFunction : RepositoryUpdateFunction {
    override fun <KEY, AGG : Aggregate<KEY>> update(aggregate: AGG, repository: Repository<KEY, AGG>) {
        repository.updateIfPresent(aggregate)
    }
}

object DeleteFunction : RepositoryUpdateFunction {
    override fun <KEY, AGG : Aggregate<KEY>> update(aggregate: AGG, repository: Repository<KEY, AGG>) {
        repository.delete(aggregate.aggregateId())
    }
}