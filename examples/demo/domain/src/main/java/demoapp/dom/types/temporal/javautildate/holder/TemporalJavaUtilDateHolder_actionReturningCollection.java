/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package demoapp.dom.types.temporal.javautildate.holder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.apache.isis.applib.annotation.Action;
import org.apache.isis.applib.annotation.SemanticsOf;

import lombok.RequiredArgsConstructor;
import lombok.val;


//tag::class[]
@Action(semantics = SemanticsOf.SAFE)
@RequiredArgsConstructor
public class TemporalJavaUtilDateHolder_actionReturningCollection {

    private final TemporalJavaUtilDateHolder holder;

    public Collection<java.util.Date> act() {
        final Collection<java.util.Date> javaSqlDates = new ArrayList<>();
        val initial = holder.getReadOnlyProperty();
        Stream.of(1, 2, 3)
                .map(x -> new java.util.Date(120,x,x))  // 1900 is the epoch
                .forEach(javaSqlDates::add);
        return javaSqlDates;
    }
}
//end::class[]
