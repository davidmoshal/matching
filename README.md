Matching
==========

This is a lightweight matching engine written in Kotlin.

## Application Semantics
### Functional programming
There are two basic constructs:

* Persistent data structure as immutable state. Every state is immutable, though the state could provide some functions: 
    * Transformation of representation - e.g. `toString`
    * Mutation - Mutate the current state by creating a new one
    * Derived property getter
* Stateless function. Stateless function makes all the business decisions. 

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

## The Domain
The domain borrows a lot of languages and terms from the [FIX Protocol](http://fiximate.fixtrading.org/latestEP/), meanwhile abstract away from the verbose and vintage style of naming.  
![Domain](/doc/images/domain.png) 

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
