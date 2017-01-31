package com.globalmentor.calendar.calculator;
/*
 * Copyright © 2013 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import static com.globalmentor.collections.Sets.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.time.LocalDate;
import java.util.*;

import org.junit.Test;

import com.globalmentor.iso.datetime.ISODate;
import com.globalmentor.model.*;

/**
 * Tests of ISO date utilities.
 * 
 * @author Garret Wilson
 * @see CalendarCalculator
 * 
 */
public class ISODatesTest {

	/** @see CalendarCalculator#getDayCounts(Set) */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetDayCounts() {
		Map<LocalDate, Count> dayCounts;
		//no ranges
		dayCounts = CalendarCalculator.getDayCounts(Collections.<Range<LocalDate>>emptySet());
		assertTrue(dayCounts.isEmpty());
		//a single day
		dayCounts = CalendarCalculator.getDayCounts(immutableSetOf(new Range<LocalDate>(LocalDate.of(2002, 03, 02), LocalDate.of(2002, 03, 02))));
		assertFalse(dayCounts.containsKey(LocalDate.of(2002, 03, 01)));
		assertThat(dayCounts.get(LocalDate.of(2002, 03, 02)).getCount(), is(1L));
		assertFalse(dayCounts.containsKey(LocalDate.of(2002, 03, 03)));
		//a whole year, one range
		dayCounts = CalendarCalculator.getDayCounts(immutableSetOf(new Range<LocalDate>(LocalDate.of(2001, 03, 05), LocalDate.of(2002, 03, 04))));
		assertThat(dayCounts.get(LocalDate.of(2001, 03, 05)).getCount(), is(1L));
		assertThat(dayCounts.get(LocalDate.of(2001, 03, 12)).getCount(), is(1L));
		assertThat(dayCounts.get(LocalDate.of(2002, 03, 04)).getCount(), is(1L));
	}

	/** @see CalendarCalculator#getDayTotals(ISODate, int, Map) */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetDayTotals() {
		Map<LocalDate, Count> dayCounts;
		Map<LocalDate, Long> dayTotals;
		//no ranges
		dayCounts = CalendarCalculator.getDayCounts(Collections.<Range<LocalDate>>emptySet());
		dayTotals = CalendarCalculator.getDayTotals(LocalDate.of(2002, 03, 04), 365, dayCounts);
		assertThat(dayTotals.size(), is(365));
		for(final Map.Entry<LocalDate, Long> dayTotalEntry : dayTotals.entrySet()) {
			assertThat(dayTotalEntry.getValue(), is(0L));
		}
		//a single day
		dayCounts = CalendarCalculator.getDayCounts(immutableSetOf(new Range<LocalDate>(LocalDate.of(2002, 03, 02), LocalDate.of(2002, 03, 02))));
		dayTotals = CalendarCalculator.getDayTotals(LocalDate.of(2002, 03, 04), 365, dayCounts);
		assertThat(dayTotals.size(), is(365));
		assertThat(dayTotals.get(LocalDate.of(2002, 03, 01)), is(0L));
		assertThat(dayTotals.get(LocalDate.of(2002, 03, 02)), is(1L));
		assertThat(dayTotals.get(LocalDate.of(2002, 03, 03)), is(1L));
		//a whole year, one range
		dayCounts = CalendarCalculator.getDayCounts(immutableSetOf(new Range<LocalDate>(LocalDate.of(2001, 03, 05), LocalDate.of(2002, 03, 04))));
		dayTotals = CalendarCalculator.getDayTotals(LocalDate.of(2002, 03, 04), 365, dayCounts);
		assertThat(dayTotals.size(), is(365));
		assertThat(dayTotals.get(LocalDate.of(2001, 03, 05)), is(1L));
		assertThat(dayTotals.get(LocalDate.of(2001, 03, 12)), is(8L));
		assertThat(dayTotals.get(LocalDate.of(2002, 03, 04)), is(365L));
	}

}
