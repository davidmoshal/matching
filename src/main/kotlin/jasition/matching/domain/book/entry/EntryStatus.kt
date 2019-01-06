package jasition.matching.domain.book.entry

enum class EntryStatus {
    NEW, PARTIAL_FILL, FILLED, CANCELLED;

    fun traded(newSizes: EntrySizes): EntryStatus =
        if (newSizes.available == 0) FILLED else PARTIAL_FILL

}