Matching
==========

This is a lightweight matching engine written in Kotlin.

## The farmer's market
If you are not familiar with matching engine, read this example. You are an apple farmers and you have 100 apples ripe to be sold. You can sell them in three ways. You can either let the customers knock your door, or you travel around in the town and knock each potential customer's door, or you could bring them to the farmer's market. You are not the only apple farmers in the town. Waiting to have customers turning up to your door and asking to buy your apples, you either are an apple farmer celebrity, or you are insane, or just plain lazy. You may be super hardworking that you would knock everyone's door to sell your apples, but I presume you would be arrested for the crime of disturbing the peace very soon. Statistically you have a better chance to sell your apples in the farmer's market, where the farmers and customers both have strong intentions to trade, and they are more committed to settle trades (exchange of goods and services). So assume you go to the farmer's market. There are tens of apple farmers, and thousands of customers. You could set up your booth and wait for the customers, which is better than waiting at home, but you are still passively selling. Or you could active go around and ask every stranger to buy your apples, probably with desperation on your face.

The farmer's market owner does not like farmers and customers running in the market like headless chickens, because it is simply dangerous and chaotic. The market owner announces that farmers and customers can now put their intention to either buying or selling in a centralised book anonymously, and the market owner will match the buying and selling in a fair and orderly manner. However, the market owner requires both buyers and sellers to commit to honouring the trades, that is, if you said you wanted to sell 50 apples for 1 pound each, you need to be ready to hand over 50 apples and get 50 pounds back in your pocket, and pay transaction fees to the market owner for helping you out. So selling and buying are more than intentions, they are obligations. So we call them Orders. The centralised book is called the Order Book. 

Then some genius businessmen come and say, "I can buy the apples for my customers on their behalf, and I could deliver the apples to their doors, and of course, for a fee". So customers no longer need to even spend time out in the market. Brokers are the names of these businessmen. Fair enough, so customers can wait just sit home and enjoy the apples, knowing they have paid a fee to the Brokers for each apple they purchased. 

There are also opportunists that watch the market and are willing to buy and sell at the same time. They aim to gain profit by arbitraging (Buy low, sell high). The market owner is very happy with these activities because it makes the market looks very busy and active, even when there are no real customers and farmers around, so the market owner called these opportunists "Market Makers". Market Makers provide Quotes that are usually in pairs (Buy and Sell). Some Market Makers prefer quoting in multiple levels (multiple pairs) at the same time, and this aggregation is called a Mass Quote. 

The operation of the Order Book is the Matching Engine. And the farmer's market is the Exchange.

The Orders and Quotes sitting in the Order Book are passively waiting to be traded, so they are passive entries. While the new coming Orders and Quotes are called the aggressors.

## The Domain
The domain borrows a lot of languages and terms from the [FIX Protocol](http://fiximate.fixtrading.org/latestEP/), meanwhile abstract away from the verbose and vintage style of naming.  
![Domain](/doc/images/domain.png) 

### Behaviours

#### Order Type
* Limit - willingness to buy up to or sell down to a given price
* Market - willingness to buy or sell at any price. Therefore Market Orders do not contain a price.

#### Time in Force
Determines when the Order is in effect.
* Good till cancel (GTC) - Effective until cancelled (by request or by the Exchange) 
* Day - Effective until the current Business Date is over
* Good till day (GTD) - Effective until the given Business Date is over
* Immediate or cancel (IOC) - Immediately match Book entries and cancel the unmatched quantities
* Fill or kill (FOK) - Fill the full quantity of the Order or kill the Order

#### Trading rules
* Aggressors take the better execution price. For example, if a Sell @ 15 is passive, and a Buy @ 20 is aggressor, then the Trade is executed @ 15 so the aggressor gets a better execution price.
* The Order Book is prioritised by price and then time. Firstly, better prices come first, i.e. higher buy prices and lower buy prices. For the same price, the earlier Orders or Quotes come first.
* No customer can match its own Orders.
* Market makers cannot match another Market makers. One of parties in the trade must have real products or money.
* As there are cases where the aggressor and passive entries will not match, the prices in the book may look crossed. This is called backwardation. 

### Entities
* Order - an instruction of Buy or Sell given quantity of the unit. It may or may not have a price.
* Mass Quote - a collection of pairs of Buy and Sell prices with given quantities of units.
* Trade - an agreed transaction between a Buy and a Sell instruction
* Book and Book Entry - *Orders* and *Mass Quotes* are transformed into *Entries* that are sorted by the matching priorities and put together as a *Book*

### Aggregate
The *Book* is the [Aggregate Root](https://martinfowler.com/bliki/DDD_Aggregate.html) that guarantees that each update is the result of one transaction.

The *Book* contains
* Book Identifier - Literally the ID
* Buy Limit Book - The collection of *Book Entries* sorted in the ascending order, that is, higher Buy prices have priority to be matched over lower.
* Sell Limit Book - The collection of *Book Entries* sorted in the descending order, that is, lower Sell prices have priority to be matched over higher.
* Business Date - A logical date without time portion.
* Trading Status - The shortcut representation of trading rule enforcement. Certain commands are not allowed under certain *Trading Status*.
* Last Event ID - The last Event ID the results in the current *Book* state. 

## How-to build and test
### Build the project
The project is built with the following option:
`gradle build test`

And the test reports can be found under
* Unit tests -  `/build/reports/unit-tests/index.html`
* Scenario tests - `/build/reports/scenario-tests/index.html`
* Coverage - `/build/reports/coverage/index.html`

### Run benchmarking
Benchmarking is run under the following option:
`gradle --no-daemon clean jmh`

Somehow changing the annotation @Benchmark did not affect the generated JMS source. The issue was reported [here](https://github.com/melix/jmh-gradle-plugin/issues/132).

And the report can be found under `/build/reports/jmh/results.json`

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

So each state transition is the consequence of an event. Events are played sequentially and therefore single-threaded. Each event has a monotonic increasing sequence number as the ID per aggregate.

### CQRS
[Command Query Responsibility Segregation (CQRS)](https://martinfowler.com/bliki/CQRS.html), literally re-categorises the traditional [CRUD](https://www.codecademy.com/articles/what-is-crud) into the followings

* Command - the intention
* Event - the validated command that result in state change
* Query - the request for information only
* Report - the result of a query

Events in this domain is categorised into the followings:
* Primary event - a direct response as a result of Command validation 
* Side-effect event - an event generated as a side effect of playing a Primary event 

Combined with Event-sourcing, the application semantics can be as the following recursion:

*"Primary event" = validate (Command, State)*

*"New State", "Side-effect events" = play (Event, State)*

The transaction is completed when all events are played and the final new state is computed.

### Recovery
During recovery, only Primary events need to be re-played as the Side-effect events will be re-generated.

Each aggregate can be recovered in isolation, given the fact that each aggregate has its own sequence of events. Events can be compacted as a snapshot to reduce the ever-growing number of events to be re-played and hence the recovery time. However, the snapshot is not planned to be implemented in this project, at least currently. This is namely the [Memento](https://refactoring.guru/design-patterns/memento) pattern.

### Transaction
During to the recursive nature of playing events, aggregates are also computed recursively. The transaction will collect the generated events and merge the aggregates during the recursion. At the end of the transaction, there should be one final aggregate and a list of events in chronological order. 

If the events were to be transported externally, it is recommended to group all events in one transaction into one transport message. In this way, the transaction either has happened or not at all from the external's perspectives, and therefore it ensures state integrity and consistency. It would be extremely difficult to reason or to recover the state if only half of the events of a transaction is played.   

### Machine-time and randomisation
**Machine-time and randomisation are strictly prohibited in the domain**, because functions involving them are no longer deterministic, and therefore states recovered from re-playing events will be different from the previous.

Machine-time is stateful and randomisation is indeterministic. They are supplied outside of the domain.

### Warnings
As all instruction manuals warn their readers in the very last pages, I strongly warn anyone [**NOT TO USE CQRS AND EVENT-SOURCING AT THE SYSTEM LEVEL**](https://www.infoq.com/news/2016/04/event-sourcing-anti-pattern). They are meant to be used within the [bounded-context](https://martinfowler.com/bliki/BoundedContext.html) and the applications in your system need to be well divided by domains. Check out [Domain-driven development](https://medium.com/the-coding-matrix/ddd-101-the-5-minute-tour-7a3037cf53b8) if you want to explore more.

### Domain boundary
Boundary needs to be set up around the domain in order to ensure the domain integrity and the success application of CQRS and Event-sourcing. Here are the rules:
* Commands, events, queries, reports and aggregates are dedicated to one and only one domain. In other words, the domain owns them and does not share with other domains. 
* Domain A's commands and queries are strictly used to enter Domain A only. Other domains use commands and queries to send requests to Domain A.  
* Domain A's events and reports are strictly used to leave Domain A only. The only exception is when recovering Domain A, events from Domain A are re-played. Other domains listen and react to Domain A's events and reports.
* Domain A's aggregates are not shared with nor visible to other domains. External representations need to be used for inter-domain communication.

### Floating-point numbers or BigDecimal
I purposefully do not use floating-point numbers or `BigDecimal` as prices. Long is used to represent a price throughout the project and it is not going to change. The reason is that floating-point numbers are not accurate and `BigDecimal` is very slow. Moreover, there is no real need for matching engine to work with either of them. As long as the incoming orders or quotes have their prices normalised to a fixed decimal place and keep the precision somewhere else, the actual price can always be translated from the price as long in this domain. There is an alternative of `BigDecimal` (The [Money](http://java-performance.info/high-performance-money-class/) class) which is so much faster. But still Long will be used as the data type of price values in this project.

## Performance
I benchmark the command validation and event playing to potentially resolve any performance deficiency. My choice of tool is [JMH](https://openjdk.java.net/projects/code-tools/jmh/) as JetBrains also used it in [Kotlin Benchmarking](https://github.com/JetBrains/kotlin-benchmarks).

## Code coverage
I aim to achieve 100% code coverage because this is pure domain code and there should be no excuse of not testing all the code and branches. Also if I cannot achieve 100% I would consider the implementation or API not fluent enough in this project.

## Dependencies
I intend to use as few dependencies as possible. However, I need support for Functional Programming and a fluent assertion framework as a minimum.

Production 
* [Arrow](https://arrow-kt.io/) - Monads
* [Vavr](http://www.vavr.io/) - Persistent collections minus its monads. The monads in Vavr is more suitable for Java in my opinion. However its persistent collections are amazing.

Testing
* [Kotlintest](https://github.com/kotlintest/kotlintest) - Test framework, fluent DSL assertions. I have tried [Spek](https://spekframework.org/), [Strikt](https://strikt.io/) and [JUnit 5](https://junit.org/junit5/), but Kotlintest is far more superior in terms of flexibility, fluency and neat organisation of test cases. I used to be a fan of [AssertJ](http://joel-costigliola.github.io/assertj/) but I feel it should belong to the Java world as Kotlin-specific frameworks offer better readability.
* [MockK](https://github.com/mockk/mockk) - Mocking framework for Kotlin. [Mockito](https://site.mockito.org/) is more suitable to Java than to Kotlin, because of the default final classes and lack of usable Kotlin-specific features like backticks and infixes.
* [Jacoco](https://www.eclemma.org/jacoco/) - Code coverage tool
* [nl.fabianm.kotlin.plugin.generated](https://plugins.gradle.org/plugin/nl.fabianm.kotlin.plugin.generated) - Exclude Kotlin-generated code from the code coverage
* [JMH](https://openjdk.java.net/projects/code-tools/jmh/) - Micro-benchmarking tool

## Why am I doing this?
This project was started as a pet project for myself to learn Kotlin only. 

I have been working on Exchange systems for more or less 10 years. It was natural for me to pick the problem I was most familiar with. It is a summary or brain-dump of what I have learnt about the matching engine. I wrote new code in this project from scratch and purposefully did not even look at my old works, in order to ensure I have the knowledge in my head.

I am perfectly aware that this project may become something of commercial value. While I am sharing the source code of the matching algorithm here, I am also very happy to be consulted how to realise it as an enterprise system. Contact me if you are interested.

## References
[Change Log](/doc/change-log.md)

[Strictly Programming](https://jasition.github.io) - My technical blog 