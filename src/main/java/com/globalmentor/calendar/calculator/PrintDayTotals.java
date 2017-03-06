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

import static com.globalmentor.calendar.calculator.CalendarCalculator.*;
import static com.globalmentor.java.Conditions.*;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.kohsuke.args4j.*;

import com.globalmentor.io.BOMInputStreamReader;
import com.globalmentor.model.*;
import com.globalmentor.util.StringTemplate;

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
public class PrintDayTotals {

	/**
	 * The main logic of the program.
	 * 
	 * @param args The arguments to be used on its execution.
	 * @throws UnsupportedEncodingException if an input stream with an invalid BOM is read.
	 * @throws IOException if an I/O error occurs.
	 * 
	 * @see CommandLineOptions
	 */
	public static void main(final String[] args) throws UnsupportedEncodingException, IOException {

		final CommandLineOptions commandLineOptions = new CommandLineOptions();
		final CmdLineParser commandLineParser = new CmdLineParser(commandLineOptions);

		try {
			commandLineParser.parseArgument(args);
		} catch(CmdLineException cmdLineException) {
			System.err.println(String.format("One of the arguments of the program weren't type correctly: %s", cmdLineException.getMessage()));
			commandLineParser.printUsage(System.out);
			System.exit(1);
		}

		if(commandLineOptions.help()) { //if the command help was called, we print the usage and finish the execution of the program.
			commandLineParser.printUsage(System.out);
			System.exit(0);
		}

		//parse the parameters
		LocalDate date = null;

		try {
			date = commandLineOptions.getDate();
		} catch(final DateTimeParseException dateTimeParseException) {
			System.err
					.println("The provided date was in an invalid format. Please, make sure that the given date is correctly in the ISO-8601 format. i.e.: YYYY-MM-DD.");
			System.exit(1);
		}

		assert date != null : "<date> should not be null at this point of the program";

		final int windowSize = commandLineOptions.getWindowSize();
		final int historyCount = commandLineOptions.getHistoryCount();

		//parse the ranges from System.in
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

			System.out.print(windowTotal); //e.g. *,2011-02-03,1,5,170

			commandLineOptions.getMaxDays().ifPresent(maxDays -> System.out.print("," + (maxDays - windowTotal)));//if we know the maximum number of days, include the days remaining. e.g. *,2011-02-03,1,5,170,10

			System.out.println();
		}

	}

	/**
	 * Command Line Option Handler to the {@link PrintDayTotals} program.
	 * 
	 * @author Magno Nascimento
	 */
	static class CommandLineOptions {

		static final StringTemplate ISO_LOCAL_DATE_WITHOUT_YEAR_TEMPLATE = new StringTemplate(StringTemplate.STRING_PARAMETER, "-",
				StringTemplate.STRING_PARAMETER);

		@Option(name = "--date", aliases = "-d", metaVar = "<date>", usage = "The date that the program will use for the calculations. If no date is provided, the current local date will be used. If no year is provided, it will default to the current year.")
		private String date;

		@Option(name = "--from", aliases = "-f", metaVar = "<fromDate>", forbids = {
				"--window"}, usage = "The initial date to be used for the calculations. This will set up the window size automatically. If no year is provided, it will default to the current year.")
		private String fromDate;

		@Option(name = "--window", aliases = "-w", metaVar = "<windowSize>", forbids = {
				"--from"}, usage = "The number of days back to include in each total. If no window size is provided, the number of days between the given date and the same date a year before will be used.")
		private Integer windowSize;

		@Option(name = "--max", aliases = "-x", metaVar = "<maxDays>", usage = "The maximum number of days to be included. If no maximum number is provided, all the days will be included.")
		private Integer maxDays;

		@Option(name = "--history", aliases = "-h", metaVar = "<historyCount>", usage = "The number of day totals to include. If no history count is provided, the window size will be used.")
		private Integer historyCount;

		@Option(name = "--help", help = true, usage = "Presents the information of the command-line options usage. If this option is enabled, all the other arguments will be ignored.")
		private boolean help;

		/**
		 * Retrieves the date that the program must start.
		 * 
		 * @return The date that the program should start. It defaults to the current date if no date is provided.
		 * @throws DateTimeParseException if the given date was in an invalid format.
		 */
		public LocalDate getDate() throws DateTimeParseException {

			if(this.date != null) {
				try {
				return LocalDate.parse(this.date); //if we cannot parse this date, an exception is thrown.
				} catch(final DateTimeParseException dateTimeParseExceptionLocalISO) {
					return LocalDate.parse(ISO_LOCAL_DATE_WITHOUT_YEAR_TEMPLATE.apply(LocalDate.now().getYear(), this.date));
				}
			} else {
				return LocalDate.now(); //if the date is null, we default it to the current date.
			}

		}

		/**
		 * @return The window size that the program must use. If an initial date is provided, window size will be the amount of days between the initial date and
		 *         the date provided for calculations. If both window size and initial date are not provided it defaults to the amount of days between the current
		 *         date (inclusive) and the date of one year before (exclusive).
		 * @throws DateTimeParseException if the given date was in an invalid format.
		 */
		public int getWindowSize() {
			int windowSize;

			if(this.windowSize != null) {
				windowSize = this.windowSize; //safe auto-unboxing, we already checked windowSize for null. 
			} else {

				LocalDate initialDate = null;
				final LocalDate finalDate = getDate();

				if(this.fromDate != null) {

					try {
						initialDate = LocalDate.parse(this.fromDate); //if windowSize is null and fromDate not, the windowSize will be the amount of days between the initial date (exclusive) and the provided date (inclusive).
					} catch(final DateTimeParseException dateTimeParseExceptionLocalISO) {
						initialDate = LocalDate.parse(ISO_LOCAL_DATE_WITHOUT_YEAR_TEMPLATE.apply(finalDate.getYear(), this.fromDate)); //if initialDate could not be parsed with ISO_LOCAL_DATE, we try to parse it using ISO_LOCAL_DATE_WITHOUT_YEAR
					}

				} else {
					initialDate = finalDate.minusYears(1); //if windowSize and fromDate is null, we default it to the amount of days between the provided date and exactly one year before.
				}

				assert initialDate != null : "<initialDate> should not be null at this point of the program";

				checkArgument(finalDate.compareTo(initialDate) >= 0, "<fromDate> cannot be after <date>");

				windowSize = (int)ChronoUnit.DAYS.between(initialDate, finalDate);
			}

			return windowSize;
		}

		/** @return The history count that must be used by the program. It defaults to the window size if no history count is provided. */
		public int getHistoryCount() {

			if(this.historyCount != null) {
				return this.historyCount;
			} else {
				return getWindowSize(); //if historyCount is null, we default it to windowSize.
			}

		}

		/**
		 * @return The amount of days that must be printed to the user.
		 * 
		 * @throws IllegalArgumentException if the given max number is negative.
		 */
		public Optional<Integer> getMaxDays() {

			if(maxDays != null) {
				return Optional.of(checkArgumentNotNegative(this.maxDays));
			} else {
				return Optional.empty();
			}

		}

		/** @return {@code true} if the command help was called. */
		public boolean help() {
			return this.help;
		}

	}

}
