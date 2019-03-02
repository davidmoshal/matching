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
import jasition.cqrs.Transaction;
import jasition.matching.domain.book.BookId;
import jasition.matching.domain.book.Books;
import jasition.matching.domain.order.command.PlaceOrderCommand;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

import static jasition.matching.domain.PreconditionSetup.*;
import static jasition.matching.domain.book.entry.Side.BUY;

public class PlaceOrderOnBookWithEntriesBenchmarkTest {
    // Can verify the test correctness here
    public static void main(String[] args) {
        Precondition precondition = new Precondition();
        Either<Exception, Transaction<BookId, Books>> transaction = precondition.commandFiveMatchesAndAddedToBook.execute(precondition.book);
        System.out.println(precondition.book);
    }

    @State(Scope.Benchmark)
    public static class Precondition {
        Books book = aBooksWithEntries(List.of(
                aPriceWithSize(9, 10),
                aPriceWithSize(8, 10),
                aPriceWithSize(7, 10),
                aPriceWithSize(6, 10),
                aPriceWithSize(5, 10)
        ), List.of(
                aPriceWithSize(11, 10),
                aPriceWithSize(12, 10),
                aPriceWithSize(13, 10),
                aPriceWithSize(14, 10),
                aPriceWithSize(15, 10)
        ));
        PlaceOrderCommand commandNoMatch = aPlaceOrderCommand(BUY, 10, 10);
        PlaceOrderCommand commandOneMatch = aPlaceOrderCommand(BUY, 11, 10);
        PlaceOrderCommand commandThreeMatches = aPlaceOrderCommand(BUY, 13, 30);
        PlaceOrderCommand commandFiveMatches = aPlaceOrderCommand(BUY, 15, 50);
        PlaceOrderCommand commandFiveMatchesAndAddedToBook = aPlaceOrderCommand(BUY, 16, 60);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void orderPlacedOnBookOfFiveEntriesNoMatch(Precondition precondition) {
        precondition.commandNoMatch.execute(precondition.book);

    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void orderPlacedOnBookOfFiveEntriesOneMatch(Precondition precondition) {
        precondition.commandOneMatch.execute(precondition.book);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void orderPlacedOnBookOfFiveEntriesThreeMatches(Precondition precondition) {
        precondition.commandThreeMatches.execute(precondition.book);

    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void orderPlacedOnBookOfFiveEntriesFiveMatches(Precondition precondition) {
        precondition.commandFiveMatches.execute(precondition.book);
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void orderPlacedOnBookOfFiveEntriesThreeMatchesAndAddedToBook(Precondition precondition) {
        precondition.commandFiveMatchesAndAddedToBook.execute(precondition.book);
    }
}
