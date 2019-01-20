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

import io.vavr.collection.List;
import jasition.matching.domain.book.Books;
import jasition.matching.domain.quote.QuoteEntry;
import jasition.matching.domain.quote.command.PlaceMassQuoteCommand;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static jasition.matching.domain.PreconditionSetup.aPlaceMassQuoteCommand;
import static jasition.matching.domain.PreconditionSetup.anEmptyBooks;
import static jasition.matching.domain.TestEntities.aQuoteEntry;
import static jasition.matching.domain.TestExecutor.validateAndPlay;

public class PlaceMassQuoteOnEmptyBookBenchmarkTest {
    @State(Scope.Benchmark)
    public static class Precondition {
        Books book = anEmptyBooks();
        QuoteEntry entry1 = aQuoteEntry(10, 10, 11, 10);
        QuoteEntry entry2 = aQuoteEntry(9, 10, 12, 10);
        QuoteEntry entry3 = aQuoteEntry(8, 10, 13, 10);
        QuoteEntry entry4 = aQuoteEntry(7, 10, 14, 10);
        QuoteEntry entry5 = aQuoteEntry(6, 10, 15, 10);

        PlaceMassQuoteCommand commandOneLevel = aPlaceMassQuoteCommand(List.of(entry1));
        PlaceMassQuoteCommand commandThreeLevel = aPlaceMassQuoteCommand(List.of(entry1, entry2, entry3));
        PlaceMassQuoteCommand commandFiveLevel = aPlaceMassQuoteCommand(List.of(entry1, entry2, entry3, entry4, entry5));
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void massQuoteOfOneLevelsPlacedOnEmptyBook(Precondition precondition) {
        validateAndPlay(precondition.commandOneLevel, precondition.book);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void massQuoteOfThreeLevelsPlacedOnEmptyBook(Precondition precondition) {
        validateAndPlay(precondition.commandThreeLevel, precondition.book);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void massQuoteOfFiveLevelsPlacedOnEmptyBook(Precondition precondition) {
        validateAndPlay(precondition.commandFiveLevel, precondition.book);
    }
}
