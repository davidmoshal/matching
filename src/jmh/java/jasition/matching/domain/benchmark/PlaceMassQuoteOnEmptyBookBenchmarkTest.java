/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jasition.matching.domain.benchmark;

import arrow.core.Either;
import io.vavr.collection.List;
import jasition.matching.domain.book.BookId;
import jasition.matching.domain.book.Books;
import jasition.matching.domain.book.entry.Price;
import jasition.matching.domain.book.entry.PriceWithSize;
import jasition.matching.domain.client.Client;
import jasition.matching.domain.quote.QuoteEntry;
import jasition.matching.domain.quote.command.PlaceMassQuoteCommand;
import jasition.matching.domain.quote.event.MassQuotePlacedEvent;
import jasition.matching.domain.quote.event.MassQuoteRejectedEvent;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static jasition.matching.domain.TestEntities.*;
import static jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL;
import static jasition.matching.domain.quote.QuoteModelType.QUOTE_ENTRY;

public class PlaceMassQuoteOnEmptyBookBenchmarkTest {
    @State(Scope.Benchmark)
    public static class Precondition {
        BookId bookId = aBookId("book");
        Books book = aBooks(bookId, List.empty());
        Client client = aFirmWithClient("firmId", "firmClientId");
        QuoteEntry entry1 = new QuoteEntry("1", "1", new PriceWithSize(new Price(10), 10), new PriceWithSize(new Price(11), 10));
        QuoteEntry entry2 = new QuoteEntry("2", "1", new PriceWithSize(new Price(9), 10), new PriceWithSize(new Price(12), 10));

        QuoteEntry entry3 = new QuoteEntry("3", "1", new PriceWithSize(new Price(8), 10), new PriceWithSize(new Price(13), 10));
        QuoteEntry entry4 = new QuoteEntry("4", "1", new PriceWithSize(new Price(7), 10), new PriceWithSize(new Price(14), 10));
        QuoteEntry entry5 = new QuoteEntry("5", "1", new PriceWithSize(new Price(6), 10), new PriceWithSize(new Price(15), 10));
        PlaceMassQuoteCommand commandOneLevel = new PlaceMassQuoteCommand("quoteId",  client, bookId, QUOTE_ENTRY, GOOD_TILL_CANCEL,
                List.of(entry1),
                Instant.now());
        PlaceMassQuoteCommand commandThreeLevel = new PlaceMassQuoteCommand("quoteId", client, bookId, QUOTE_ENTRY, GOOD_TILL_CANCEL,
                List.of(entry1, entry2, entry3),
                Instant.now());
        PlaceMassQuoteCommand commandFiveLevel = new PlaceMassQuoteCommand("quoteId", client, bookId, QUOTE_ENTRY, GOOD_TILL_CANCEL,
                List.of(entry1, entry2, entry3, entry4, entry5),
                Instant.now());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void commandValidatedAndMassQuotePlacedEventOfOneLevelPlayed(Precondition state) {
        Either<MassQuoteRejectedEvent, MassQuotePlacedEvent> result = state.commandOneLevel.validate(state.book);

        if (result.isLeft()) {
            throw new IllegalStateException("Mass Quote should be placed but was rejected");
        }

        result.fold(rejected -> rejected.play(state.book), placed -> placed.play(state.book));
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void commandValidatedAndMassQuotePlacedEventOfThreeLevelsPlayed(Precondition state) {
        Either<MassQuoteRejectedEvent, MassQuotePlacedEvent> result = state.commandThreeLevel.validate(state.book);

        if (result.isLeft()) {
            throw new IllegalStateException("Mass Quote should be placed but was rejected");
        }

        result.fold(rejected -> rejected.play(state.book), placed -> placed.play(state.book));
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void commandValidatedAndMassQuotePlacedEventOfFiveLevelsPlayed(Precondition state) {
        Either<MassQuoteRejectedEvent, MassQuotePlacedEvent> result = state.commandFiveLevel.validate(state.book);

        if (result.isLeft()) {
            throw new IllegalStateException("Mass Quote should be placed but was rejected");
        }

        result.fold(rejected -> rejected.play(state.book), placed -> placed.play(state.book));
    }
}
