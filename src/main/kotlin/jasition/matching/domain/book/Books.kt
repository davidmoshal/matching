package jasition.matching.domain.book

data class Books (val id : String,
                  val buyLimitBook : LimitBook = LimitBook(),
                  val sellLimitBook : LimitBook = LimitBook())