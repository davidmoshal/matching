package jasition.matching.domain.book.entry

enum class EntryType {
    LIMIT {
        override fun isPriceRequiredOrMustBeNull(): Boolean = true
    },
    MARKET {
        override fun isPriceRequiredOrMustBeNull(): Boolean = false
    };

    /**
     * Returns true if the price required; false if the price must be null
     */
    abstract fun isPriceRequiredOrMustBeNull(): Boolean
}