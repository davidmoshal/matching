Matching
==========

This is a lightweight matching engine written in Kotlin.

## The farmer's market
If you are not familiar with matching engine, read this example. You are an apple farmers and you have 100 apples ripe to be sold. You can sell them into two ways. You can either let the customers knock your door, or you could bring them to the farmer's market. You are not the only apple farmers in the town. Waiting to have customers turning up to your door and asking to buy your apples, you either are an apple farmer celebrity, or you are insane, or just plain lazy. Statistically you have a better chance to sell your apples in the farmer's market, where the farmers and customers both have strong intentions to trade, and they are more committed to settle trades (exchange of goods and services). So assume you go to the farmer's market. There are tens of apple farmers, and thousands of customers. You could set up your booth and wait for the customers, which is better than waiting at home, but you are still passively selling. Or you could active go around and ask every stranger to buy your apples, probably with desperation on your face.

The farmer's market owner does not want farmers and customers running in the market like headless chickens, because it is simply dangerous and chaotic. The market owner announces that farmers and customers can now put their intention to either buying or selling in a centralised book anonymously, and the market will match the buying and selling in a fair and orderly manner. However, the market owner requires both buyers and sellers to commit to honouring the trades, that is, if you said you wanted to sell 50 apples for 1 pound each, you need to be ready to hand over 50 apples and get 50 pounds back in your pocket, and pay transaction fees to the market owner for helping you out. So selling and buying are more than intentions, they are obligations. So we call them Orders. The centralised book is called the Order Book. 

Then some genius businessmen come and say, "I can buy the apples for my customers on their behalf, and I could deliver the apples to their doors, and of course, with a fee". So customers no longer need to even spend time out in the market. Brokers are the names of these businessmen. Fair enough, so customers can wait just sit home and enjoy the apples, knowing they have paid a fee to the Brokers for each apple they purchased. 

There are also opportunists that watch the market and are willing to buy and sell at the same time, and aim to gain profit by arbitraging (Buy low, sell high). The market owner is very happy with these activities because it makes the market looks very busy and active, even when there are no real customers and farmers around, so the market owner called these opportunists "Market Makers". Market Makers provide Quotes that are usually in pairs (Buy and Sell). Some Market Makers prefer quoting in multiple levels (multiple pairs) at the same time, and this aggregation is called a Mass Quote. 

The operation of the Order Book is the Matching Engine. And the farmer's market is the Exchange.

The Orders and Quotes sitting in the Order Book are passively waiting to match, so they are passive entries. While the new coming Orders and Quotes are called the aggressors.

## The Domain
The domain borrows a lot of languages and terms from the [FIX Protocol](http://fiximate.fixtrading.org/latestEP/), meanwhile abstract away from the verbose and vintage style of naming.  
![Domain](/doc/images/domain.png) 

### Behaviours

#### Order Type
* Limit - willingness to buy up to or sell down to a given price
* Market - willingness to buy or sell at any price. Therefore Market Orders do not contain a price.

#### Time in Force
Determines when the Order is in effect.
* Good till cancel - Effective until cancelled (by request or by the Exchange) 
* Day - Effective until the current Business Date is over
* Good till day - Effective until the given Business Date is over
* Immediate or cancel - Immediately match Book entries and cancel the unmatched quantities
* Fill or kill - Fill the full quantity of the Order or kill the Order

#### Trade rules
* Aggressors take the better execution price. For example, if a Sell @ 15 is passive, and a Buy @ 20 is aggressor, then the Trade is executed @ 15 so the aggressor gets a better execution price.
* The Order Book is prioritised by price and then time. Firstly, better prices come first, i.e. higher buy prices and lower buy prices. For the same price, the earlier Orders or Quotes come first.
* No customer can match its own Orders.
* Market makers cannot match another Market makers. One of parties in the trade must have real products or money. 

### Entities
* Order - an instruction of Buy or Sell given quantity of the unit. It may or may not have a price.
* Mass Quote - a collection of pairs of Buy and Sell prices with given quantities of units.
* Trade - an agreed transaction between a Buy and a Sell instruction
* Book and Book Entry - *Orders* and *Mass Quotes* are transformed into *Entries* that are sorted by the matching priorties and put together as a *Book*

### Aggregate
The *Book* is the [Aggregate Root](https://martinfowler.com/bliki/DDD_Aggregate.html) that guarantees that each update is the result of one transaction.

The *Book* contains
* Book Identifier - Literally the ID
* Buy Limit Book - The collection of *Book Entries* sorted in the ascending order, that is, higher Buy prices have priority to be matched over lower.
* Sell Limit Book - The collection of *Book Entries* sorted in the descending order, that is, lower Sell prices have priority to be matched over higher.
* Business Date - A logical date without time portion.
* Trading Status - The shortcut representation of trading rule enforcement. Certain commands are not allowed under certain *Trading Status*.
* Last Event ID - The last Event ID the results in the current *Book* state. 

## Application Semantics
### Functional programming
There are two basic constructs:

* Persistent data structure as immutable state. Every state is immutable, though the state could provide some functions: 
    * Transformation of representation - e.g. `toString`
    * Mutation - Mutate the current state by creating a new one
    * Derived property getter
    * Business decisions are passed in and captured. Persistent data structures do not make any business decision.
* Stateless function or Pure function. Stateless function makes all the business decisions. The output is deterministic at all time given the same input parameters. 

### Domain-driven development
The entire library focuses on the [domain](https://airbrake.io/blog/software-design/domain-driven-design) logic of a matching engine, and therefore, will not depend on any particular technology like Kafka or REST.

### Event-sourcing
The simplest way to interpret [Event-sourcing](https://martinfowler.com/eaaDev/EventSourcing.html) is as below:

*"New State" = func (Event, State)*

So each state transition is the consequence of an Event. Events are played sequentially and therefore single-threaded. Each event has a monotonic increasing sequence number as the ID per aggregate.

### CQRS
[Command Query Responsibility Segregation (CQRS)](https://martinfowler.com/bliki/CQRS.html), literally re-categorises the traditional [CRUD](https://www.codecademy.com/articles/what-is-crud) into the followings

* Command - the intention
* Event - the validated Command that result in state change
* Query - the request for information only

Events in this domain is categorised into the followings:
* Primary event - a direct response as a result of Command validation 
* Side-effect event - an Event generated as a side effect of playing a Primary event 

Combined with Event-sourcing, the application semantics can be as the following recursion:

*"Primary event" = validate (Command, State)*

*"New State", "Side-effect events" = play (Event, State)*

The transaction is completed when all Events are played and the final new State is computed.

During recovery, only Primary events need to be re-played as the Side-effect events will be re-generated.

### Machine-time and randomisation
Machine-time and randomisation are strictly prohibited in the domain, because 
* Functions are no longer deterministic, and therefore
* States recovered from re-playing Events will be different from the previous.

Machine-time is stateful and randomisation is indeterministic. They are supplied outside of the domain.

## Dependencies
Production 
* [Arrow](https://arrow-kt.io/) - Monads
* [Vavr](http://www.vavr.io/) - Persistent collections minus its Monads

Testing
* [Spek](https://spekframework.org/) - Specifications framework
* [Kotlintest](https://github.com/kotlintest/kotlintest) - Fluent DSL assertions

## References
[My technical blog](https://jasition.github.io)