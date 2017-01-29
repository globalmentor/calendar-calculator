/*
 * Copyright © 2013-2014 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

package com.globalmentor.calendar.calculator;

import static com.globalmentor.calendar.calculator.ISODates.*;
import static com.globalmentor.java.Conditions.*;

import java.io.*;
import java.time.LocalDate;
import java.util.*;

import com.globalmentor.io.BOMInputStreamReader;
import com.globalmentor.model.*;

/**
 * A console application to print the totals of days overlapping some ranges. This is useful, for example, in calculating the number of days in a country for
 * compliance with visa restrictions.
 * 
 * <p>
 * <code>PrintDayTotals <var>date</var> <var>windowSize</var> [<var>maxDays</var> [<var>historyCount</var>]]</code>
 * </p>
 * 
 * <p>
 * Ranges are in the form <code><var>from</var>,<var>to</var></code>, e.g.:
 * </p>
 * 
 * <blockquote><code>2010-01-02,2010-01-05<br>2010-03-10,2010-04-04</code></blockquote>
 * 
 * <p>
 * Output has five or six columns, depending on whether a maximum number of days was indicated:
 * <code><var>positive-count-flag</var>,<var>date</var>,<var>count</var>,<var>run-total</var>,<var>window-total</var>,<var>difference</var></code>, e.g.:
 * </p>
 * <blockquote><code>*,2013-02-18,1,1,136,44<br>*,2013-02-19,1,2,136,44<br>*,2013-02-20,,,135,45</code></blockquote>
 * 
 * <p>
 * If no <var>historyCount</var> is given, the value will default to <var>windowSize</var>.
 * </p>
 * 
 * <h2>Examples:</h2>
 * <p>
 * Print totals from 2010-02-04 to 2011-02-03 from the file <code>ranges.txt</code>:
 * </p>
 * <blockquote><code>PrintDayTotals <var>2011-02-03</var> <var>365</var> &lt; ranges.txt</code></blockquote> <blockquote>
 * <p>
 * Print totals from 2010-02-04 to 2011-02-03 from the file <code>ranges.txt</code>, indicating the difference of each from 180:
 * </p>
 * <code>PrintDayTotals <var>2011-02-03</var> <var>365</var> <var>180</var> &lt; ranges.txt</code></blockquote>
 * 
 * @author Garret Wilson
 * 
 */
public class PrintDayTotals {

	public static void main(final String[] args) throws UnsupportedEncodingException, IOException {
		checkArgument(args.length >= 2 && args.length <= 4,
				"Must have at least a date and number of days with an optional window size and history count: PrintDayTotals date windowSize [maxDays [historyCount]].");
		//parse the parameters
		final LocalDate date = LocalDate.parse(args[0]);
		final int windowSize = Integer.parseInt(args[1]);
		final long maxDays;
		if(args.length >= 3) {
			maxDays = Long.parseLong(args[2]);
		} else {
			maxDays = -1;
		}
		final int historyCount;
		if(args.length >= 4) {
			historyCount = Integer.parseInt(args[3]);
		} else {
			historyCount = windowSize;
		}
		//parse the ranges form System.in
		final Set<Range<LocalDate>> ranges = new HashSet<Range<LocalDate>>();
		@SuppressWarnings("resource")
		//we shouldn't close the input stream
		final LineNumberReader reader = new LineNumberReader(new BOMInputStreamReader(System.in));
		String line;
		while((line = reader.readLine()) != null) {
			final String[] lineComponents = line.split(",");
			checkArgument(lineComponents.length == 2, "Expected two components on line %d: %s", reader.getLineNumber(), line);
			ranges.add(new Range<LocalDate>(LocalDate.parse(lineComponents[0]), LocalDate.parse(lineComponents[1]))); //parse and store the range
		}
		//count the days
		final Map<LocalDate, Count> dayCounts = getDayCounts(ranges);
		//calculate the totals
		final Map<LocalDate, Long> dayTotals = getDayTotals(date, windowSize, historyCount, dayCounts);
		//print the results, calculating run totals on the fly
		long runTotal = 0;
		for(final Map.Entry<LocalDate, Long> dayTotal : dayTotals.entrySet()) {
			final LocalDate day = dayTotal.getKey();
			final Count count = dayCounts.get(day);
			if(count != null && count.getCount() > 0) { //if we have a positive count
				System.out.print('*'); //positive count indicator
				runTotal += count.getCount(); //update our run count
			} else { //if we don't have a positive count
				runTotal = 0; //reset our run total
			}
			System.out.print(',');
			final long windowTotal = dayTotal.getValue().longValue();
			System.out.print(day + ","); //e.g. *,2011-02-03
			if(count != null) {
				System.out.print(count); //e.g. *,2011-02-03,1
			}
			System.out.print(',');
			if(runTotal != 0) {
				System.out.print(runTotal); //e.g. *,2011-02-03,1,5
			}
			System.out.print(',');
			System.out.print(windowTotal); //e.g. ,2011-02-03,1,5,170
			if(maxDays >= 0) { //if we know the maximum number of days, include the days remaining
				System.out.print("," + (maxDays - windowTotal)); //e.g. *,2011-02-03,1,5,170,10
			}
			System.out.println();
		}
	}

}
