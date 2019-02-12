package jasition.cqrs

import java.util.concurrent.ConcurrentHashMap

/**
 * This is probably the only CRUD-style interface as the aggregate roots still need a repository to host them.
 */
interface Repository<K, A : Aggregate<K>> {
    /**
     * Finds the aggregate by the given [aggregateId] and it may be present or absent.
     */
    fun find(aggregateId : K) : A?

    /**
     * Returns the aggregate by the given [aggregateId] and expects it to be present.
     */
    fun read(aggregateId : K) : A

    /**
     * Stores the [aggregate] if the aggregate did not exist in the repository. Returns true if the given [aggregate] was
     * stored in the repository.
     */
    fun createIfAbsent(aggregate : A) : Boolean

    /**
     * Stores the given [aggregate] and potentially overwrites the existing aggregate of the same aggregateId.
     */
    fun createOrUpdate(aggregate : A)

    /**
     * Overwrites the existing aggregate of the same aggregateId by the given [aggregate] if present. Returns true if
     * the existing aggregate was overwritten.
     */
    fun updateIfPresent(aggregate : A) : Boolean

    /**
     * Removes the aggregate by the given [aggregateId]. Returns the existing aggregate in the repository if present.
     */
    fun delete(aggregateId : K) : A?
}

//TODO: Unit test
class ConcurrentRepository<K, A : Aggregate<K>> (initialSize : Int) : Repository<K, A> {
    constructor() : this (256)

    private val delegate = ConcurrentHashMap<K, A>(initialSize)

    override fun find(aggregateId: K): A? = delegate[aggregateId]

    override fun read(aggregateId: K): A =
        delegate[aggregateId] ?: throw NoSuchElementException("Cannot find the aggregate $aggregateId")

    override fun createIfAbsent(aggregate: A): Boolean =
        delegate.putIfAbsent(aggregate.aggregateId(), aggregate) == null

    override fun createOrUpdate(aggregate: A) {
        delegate[aggregate.aggregateId()] = aggregate
    }

    override fun updateIfPresent(aggregate: A): Boolean =
        delegate.computeIfPresent(aggregate.aggregateId()) { _, _ -> aggregate } != null

    override fun delete(aggregateId: K): A? = delegate.remove(aggregateId)
}
