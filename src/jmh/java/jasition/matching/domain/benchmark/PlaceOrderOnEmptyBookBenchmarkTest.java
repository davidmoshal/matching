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
import jasition.matching.domain.order.command.PlaceOrderCommand;
import jasition.matching.domain.order.event.OrderPlacedEvent;
import jasition.matching.domain.order.event.OrderRejectedEvent;
import org.openjdk.jmh.annotations.*;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static jasition.matching.domain.TestEntities.*;
import static jasition.matching.domain.book.entry.EntryType.LIMIT;
import static jasition.matching.domain.book.entry.Side.BUY;
import static jasition.matching.domain.book.entry.TimeInForce.GOOD_TILL_CANCEL;

public class PlaceOrderOnEmptyBookBenchmarkTest {
    @State(Scope.Benchmark)
    public static class Precondition {
        BookId bookId = aBookId("book");
        Books book = aBooks(bookId, List.empty());
        PlaceOrderCommand command = new PlaceOrderCommand(
                aClientRequestId("req", null, null),
                aFirmWithClient("firmId", "firmClientId"),
                bookId, LIMIT, BUY, 10, new Price(10), GOOD_TILL_CANCEL, Instant.now());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void commandValidatedAndOrderPlacedEventPlayed(Precondition state) {
        Either<OrderRejectedEvent, OrderPlacedEvent> result = state.command.validate(state.book);

        if (result.isLeft()) {
            throw new IllegalStateException("Order should be placed but was rejected");
        }

        result.fold(rejected -> rejected.play(state.book), placed -> placed.play(state.book));
    }
}
