/*
 * Copyright © 2013-2014 GlobalMentor, Inc. <https://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.calendar.calculator;

import static com.globalmentor.calendar.calculator.CalendarCalculator.*;
import static com.globalmentor.java.Conditions.*;
import static java.util.Objects.*;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javax.annotation.*;

import com.globalmentor.application.*;
import com.globalmentor.io.BOMInputStreamReader;
import com.globalmentor.model.*;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * A console application to print the totals of days overlapping some ranges. This is useful, for example, in calculating the number of days in a country for
 * compliance with visa restrictions.
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
@Command(name = "print-day-totals", description = "A console application to print the totals of days overlapping some ranges.")
public class PrintDayTotals extends BaseCliApplication {

	/**
	 * Constructor.
	 * @param args The command line arguments.
	 */
	public PrintDayTotals(@Nonnull final String[] args) {
		super(args);
	}

	/**
	 * Main program entry method.
	 * @param args Program arguments.
	 */
	public static void main(@Nonnull final String[] args) {
		Application.start(new PrintDayTotals(args));
	}

	@Override
	public void run() {

		//parse the parameters
		LocalDate date = null;
		LocalDate resetDate = null;

		try {
			date = getDate();
			resetDate = findInitialDate().orElse(null);
		} catch(final DateTimeParseException dateTimeParseException) {
			System.err
					.println("The provided date was in an invalid format. Please, make sure that the given date is correctly in the ISO-8601 format. i.e.: YYYY-MM-DD.");
			System.exit(1);
		}

		assert date != null : "<date> should not be null at this point of the program";

		final int windowSize = getWindowSize();
		final int historyCount = getHistoryCount();

		//parse the ranges from System.in
		final Set<Range<LocalDate>> ranges = new HashSet<Range<LocalDate>>();

		try {
			//we shouldn't close the input stream
			final LineNumberReader reader = new LineNumberReader(new BOMInputStreamReader(System.in));

			String line;

			while((line = reader.readLine()) != null) {
				final String[] lineComponents = line.split(",");
				checkArgument(lineComponents.length == 2, "Expected two components on line %d: %s", reader.getLineNumber(), line);
				ranges.add(new Range<LocalDate>(LocalDate.parse(lineComponents[0]), LocalDate.parse(lineComponents[1]))); //parse and store the range
			}
		} catch(final IOException ioException) {
			throw new UncheckedIOException(ioException);
		}

		//count the days
		final boolean isRangeLowerInclusive = rangeLowerBound == RangeBoundType.inclusive;
		final Map<LocalDate, Count> dayCounts = getDayCounts(ranges, isRangeLowerInclusive);

		//calculate the totals
		final Map<LocalDate, Long> dayTotals = getDayTotals(date, resetDate, windowSize, historyCount, dayCounts);

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

			System.out.print(windowTotal); //e.g. *,2011-02-03,1,5,170

			findMaxDays().ifPresent(maxDays -> System.out.print("," + (maxDays - windowTotal)));//if we know the maximum number of days, include the days remaining. e.g. *,2011-02-03,1,5,170,10

			System.out.println();
		}

	}

	@Option(names = "--date", paramLabel = "<date>", description = "The ending date that the program will use for the calculations. If no date is provided, the current local date will be used. If no year is provided (i.e., if a date on the format [MM-dd] is provided), it will default to the current year.")
	private String date;

	@Option(names = {"--from",
			"-f"}, paramLabel = "<fromDate>", description = "The initial date to be used for the calculations. This will set up the window size automatically. If no year is provided (i.e., if a date on the format [MM-dd] is provided), it will default to the last occurrence of the provided date.")
	private String fromDate;

	@Option(names = {"--window",
			"-w"}, paramLabel = "<windowSize>", description = "The number of days back to include in each total. If no window size is provided, the number of days between the given date and the same date a year before will be used.")
	private Integer windowSize;

	@Option(names = {"--max",
			"-x"}, paramLabel = "<maxDays>", description = "The maximum number of days to be included. If no maximum number is provided, all the days will be included.")
	private Integer maxDays;

	@Option(names = {"--history",
			"-c"}, paramLabel = "<historyCount>", description = "The number of day totals to include. If no history count is provided, the window size will be used.")
	private Integer historyCount;

	/** Whether a range bound should be included in the totals. */
	public enum RangeBoundType {
		/** The range bound should be included. */
		inclusive,
		/** The range bound should not be included. */
		exclusive
	}

	@Option(names = "--range-lower-bound", description = "Whether the first date in each range should be included in the totals.%nValid values: ${COMPLETION-CANDIDATES}.%nDefaults to @|bold ${DEFAULT-VALUE}|@.", defaultValue = "inclusive", arity = "0..1")
	private RangeBoundType rangeLowerBound;

	/**
	 * The current local date for default calculations. Can be overridden in tests to prevent reliance on the true local date. Defaults to
	 * {@link LocalDate#now()}.
	 */
	private LocalDate now = LocalDate.now();

	/**
	 * Overrides the local date used to represent "now".
	 * @apiNote This method is used mostly for testing to avoid using the true current date in unit tests.
	 * @implSpec Defaults to {@link LocalDate#now()}.
	 * @param now
	 */
	void setNow(@Nonnull final LocalDate now) {
		this.now = requireNonNull(now);
	}

	/**
	 * Returns the ending date that the program will use for the calculations. If no date is provided, the current local date will be used. If no year is provided
	 * (i.e., if a date on the format <code>MM-dd</code> is provided), it will default to the current year.
	 * 
	 * @return The date that the program should start. It defaults to the current date if no date is provided.
	 * @throws DateTimeParseException if the given date was in an invalid format.
	 */
	LocalDate getDate() throws DateTimeParseException {

		if(this.date != null) {

			try {
				return LocalDate.parse(this.date); //if we cannot parse this date, an exception is thrown.
			} catch(final DateTimeParseException dateTimeParseExceptionLocalISO) {
				return LocalDate.parse(String.format("%d-%s", now.getYear(), this.date));
			}

		} else {
			return now; //if the date is null, we default it to the current date.
		}

	}

	/**
	 * Retrieves the initial date that the program must use as reset date.
	 * 
	 * @return The initial date that the program should use as reset date.
	 * @throws DateTimeParseException if the given date was in an invalid format.
	 */
	Optional<LocalDate> findInitialDate() throws DateTimeParseException {

		LocalDate initialDate = null;

		if(this.fromDate != null) {

			try {
				initialDate = LocalDate.parse(this.fromDate); //if windowSize is null and fromDate not, the windowSize will be the amount of days between the initial date (exclusive) and the provided date (inclusive).
			} catch(final DateTimeParseException dateTimeParseExceptionLocalISO) {
				final LocalDate date = getDate();
				int year = date.getYear();

				final MonthDay fromDateWithoutYear = MonthDay.parse(String.format("--%s", this.fromDate));

				if(fromDateWithoutYear.isAfter(MonthDay.from(date))) { //if the provided initial date is after the current date, then we use its last occurrence i.e., the same date on the last year.
					year--;
				}

				initialDate = fromDateWithoutYear.atYear(year); //if initialDate could not be parsed with ISO_LOCAL_DATE, we try to parse it using MonthDay at the year of the last occurrence of the provided date.
			}

			return Optional.of(initialDate);
		} else {
			return Optional.empty();
		}

	}

	/**
	 * @return The window size that the program must use. If an initial date is provided, window size will be the amount of days between the initial date and the
	 *         date provided for calculations. If both window size and initial date are not provided it defaults to the amount of days between the current date
	 *         (inclusive) and the date of one year before (exclusive).
	 * @throws IllegalArgumentException if the given window size is a negative value.
	 * @throws DateTimeParseException if the given date was in an invalid format.
	 */
	int getWindowSize() {
		int windowSize;

		if(this.windowSize != null) { //safe auto-unboxing after checking windowSize for null. 
			checkArgument(this.windowSize >= 0, "<windowSize> cannot be after less than 0");

			windowSize = this.windowSize;
		} else {

			final LocalDate finalDate = getDate();
			final LocalDate initialDate = findInitialDate().orElse(finalDate.minusYears(1)); //if windowSize and fromDate is null, we default it to the amount of days between the provided date and exactly one year before.

			checkArgument(finalDate.compareTo(initialDate) >= 0, "<fromDate> cannot be after <date>");

			windowSize = (int)ChronoUnit.DAYS.between(initialDate, finalDate);
		}

		return windowSize;
	}

	/** @return The history count that must be used by the program. It defaults to the window size if no history count is provided. */
	int getHistoryCount() {

		if(this.historyCount != null) {
			return this.historyCount;
		} else {
			return getWindowSize();
		}

	}

	/**
	 * @return The amount of days that must be printed to the user.
	 * 
	 * @throws IllegalArgumentException if the given max number is negative.
	 */
	Optional<Integer> findMaxDays() {

		if(maxDays != null) {
			return Optional.of(checkArgumentNotNegative(this.maxDays));
		} else {
			return Optional.empty();
		}

	}

}
