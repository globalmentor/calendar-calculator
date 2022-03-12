/*
 * Copyright © 2017 GlobalMentor, Inc. <http://www.globalmentor.com/>
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

import org.junit.jupiter.api.Test;

import picocli.CommandLine;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import javax.annotation.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests to the arguments used in the {@link PrintDayTotals} program.
 * 
 * @author Magno Nascimento
 */
public class PrintDayTotalsTest {

	/**
	 * Tests if the option {@code date} is working when provided with its metaVar to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionDate() {
		PrintDayTotals parsedCommandLineOptions;

		//Normally we would don't want to have unit tests depend on the date, but
		//an exception is made here to ensure that the arguments default to the current
		//date, without caring what that date is.
		//(Technically even this usage is treacherous; it as a tiny chance of failing
		//at midnight because of the race condition.)
		assertThat(new PrintDayTotals(new String[0]).getDate(), equalTo(LocalDate.now()));

		parsedCommandLineOptions = parseArguments("--date", "2017-01-30");
		assertThat(parsedCommandLineOptions.getDate(), equalTo(LocalDate.of(2017, 1, 30)));

		parsedCommandLineOptions = parseArguments(LocalDate.of(2019, 2, 3), "--date", "03-06");
		assertThat(parsedCommandLineOptions.getDate(), equalTo(LocalDate.of(2019, 3, 6)));
	}

	/**
	 * Tests if the option {@code date} is throwing an exception when its metaVar is in an invalid date format.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionDateWithWrongFormat() {
		PrintDayTotals parsedCommandLineOptions = parseArguments("--date", "30-01-2017");
		assertThrows(DateTimeParseException.class, () -> parsedCommandLineOptions.getDate());
	}

	/**
	 * Tests if the option {@code date} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionDateWithNoMetaVar() {
		assertThrows(CommandLine.MissingParameterException.class, () -> this.parseArguments("--date"));
	}

	/**
	 * Tests if the option {@code history} is working when provided with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionFromDate() {
		PrintDayTotals parsedCommandLineOptions;

		//tests if <fromDate> is starting exactly one year before LocalDate.now()
		parsedCommandLineOptions = parseArguments(LocalDate.of(2019, 2, 3));
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(365));

		//tests if <fromDate> is starting exactly one year before a given <date>
		parsedCommandLineOptions = parseArguments("--date", "2017-03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(),
				equalTo((int)ChronoUnit.DAYS.between(LocalDate.parse("2017-03-06").minusYears(1), LocalDate.parse("2017-03-06"))));

		//tests if the period between <fromDate> and LocalDate.now() is being correctly calculated
		parsedCommandLineOptions = parseArguments(LocalDate.of(2019, 2, 3), "--from", "2017-03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo((int)ChronoUnit.DAYS.between(LocalDate.parse("2017-03-06"), LocalDate.of(2019, 2, 3))));

		//tests if the period between <fromDate> and LocalDate.now() is being correctly calculated without explicit use of a year, if <fromDate> is earlier than now
		parsedCommandLineOptions = parseArguments(LocalDate.of(2019, 7, 8), "--from", "03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo((int)ChronoUnit.DAYS.between(LocalDate.of(2019, 3, 6), LocalDate.of(2019, 7, 8))));

		//tests if the period between <fromDate> and LocalDate.now() is being correctly calculated without explicit use of a year, if <fromDate> is the same as now
		parsedCommandLineOptions = parseArguments(LocalDate.of(2019, 3, 6), "--from", "03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(0));

		//tests if the period between <fromDate> and LocalDate.now() is being correctly calculated without explicit use of a year, if <fromDate> is later than now
		parsedCommandLineOptions = parseArguments(LocalDate.of(2019, 2, 3), "--from", "03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo((int)ChronoUnit.DAYS.between(LocalDate.of(2018, 3, 6), LocalDate.of(2019, 2, 3))));

		//tests if the period between <fromDate> and <date> is being correctly calculated without explicit use of an year
		parsedCommandLineOptions = parseArguments("--from", "03-06", "--date", "2017-03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(0));

		//tests if the year of the given <fromDate> is the same as the one of <date>
		parsedCommandLineOptions = parseArguments("--from", "01-01", "--date", "2000-01-01");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(0));

		//tests if the window size is correct when providing the same <fromDate> as <date>.
		parsedCommandLineOptions = parseArguments("--from", "2017-01-31", "--date", "2017-01-31");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(0));

		//tests if the window size is correct when providing a Period of a non-leap year.
		parsedCommandLineOptions = parseArguments("--from", "2015-01-31", "--date", "2016-01-31");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(365));

		//tests if the window size is correct when providing a Period of a leap year.
		parsedCommandLineOptions = parseArguments("--from", "2016-01-31", "--date", "2017-01-31");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(366));
	}

	/**
	 * Tests if the option {@code window} is throwing an exception if the initial date provided comes after the final date.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	public void testCommandLineOptionFromDateInFuture() {
		assertThrows(IllegalArgumentException.class,
				() -> parseArguments(LocalDate.of(2019, 2, 3), "--from", LocalDate.of(2019, 2, 3).plusDays(1).toString()).getWindowSize());
	}

	/**
	 * Tests if the option {@code window} is working if the initial date without year provided comes after the final date.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionFromDateInFutureWithoutYear() {
		final LocalDate testNow = LocalDate.of(2019, 2, 3);

		final PrintDayTotals parsedCommandLineOptions = parseArguments(testNow, "--from", testNow.plusDays(1).toString().substring(5));
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo((int)ChronoUnit.DAYS.between(testNow.plusDays(1).minusYears(1), testNow)));
	}

	/**
	 * Tests if the option {@code window} is working when provided as an alias with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionFromDateWithAlias() {
		PrintDayTotals parsedCommandLineOptions = parseArguments("-f", "2017-01-31", "--date", "2017-01-31");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(0));
	}

	/**
	 * Tests if the command line handler is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionFromDateWithNoMetaVar() {
		assertThrows(CommandLine.MissingParameterException.class, () -> this.parseArguments("--from"));
	}

	/**
	 * Tests if the option {@code from} is throwing an exception when its metaVar is in an invalid date format.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionFromDateWithWrongFormat() {
		PrintDayTotals parsedCommandLineOptions = parseArguments("--from", "30-01-2017");
		assertThrows(DateTimeParseException.class, () -> parsedCommandLineOptions.getWindowSize());
	}

	/**
	 * Tests if the option {@code window} is working when provided with its metaVar to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionWindow() {
		final LocalDate testNow = LocalDate.of(2019, 2, 3);
		PrintDayTotals parsedCommandLineOptions = parseArguments();
		parsedCommandLineOptions.setNow(testNow);
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo((int)ChronoUnit.DAYS.between(testNow.minusYears(1), testNow)));

		parsedCommandLineOptions = parseArguments("--window", "1");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(1));

		parsedCommandLineOptions = parseArguments("--window", "0");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(0));
	}

	/**
	 * Tests if the option {@code window} is throwing an exception when it's provided with a negative value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	public void testCommandLineOptionWindowWithNegativeValue() {
		assertThrows(IllegalArgumentException.class, () -> parseArguments("--window", "-1").getWindowSize());
	}

	/**
	 * Tests if the option {@code window} is working when provided as an alias with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionWindowWithAlias() {
		PrintDayTotals parsedCommandLineOptions = parseArguments("-w", "1");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(1));
	}

	/**
	 * Tests if the option {@code window} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionWindowWithNoMetaVar() {
		assertThrows(CommandLine.MissingParameterException.class, () -> this.parseArguments("--window"));
	}

	/**
	 * Tests if the option {@code max} is working when provided with its metaVar to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionMax() {
		PrintDayTotals parsedCommandLineOptions = parseArguments();
		assertThat(parsedCommandLineOptions.findMaxDays(), equalTo(Optional.empty()));

		parsedCommandLineOptions = parseArguments("--max", "1");
		assertThat(parsedCommandLineOptions.findMaxDays().get(), equalTo(1));

		parsedCommandLineOptions = parseArguments("--max", "0");
		assertThat(parsedCommandLineOptions.findMaxDays().get(), equalTo(0));
	}

	/**
	 * Tests if the option {@code max} is throwing an exception when provided an negative value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	public void testCommandLineOptionMaxWithNegativaValue() {
		assertThrows(IllegalArgumentException.class, () -> parseArguments("--max", "-1").findMaxDays());
	}

	/**
	 * Tests if the option {@code max} is working when provided as an alias with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionMaxWithAlias() {
		PrintDayTotals parsedCommandLineOptions = parseArguments("-x", "1");
		assertThat(parsedCommandLineOptions.findMaxDays().get(), equalTo(1));
	}

	/**
	 * Tests if the option {@code max} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionMaxWithNoMetaVar() {
		assertThrows(CommandLine.MissingParameterException.class, () -> this.parseArguments("--max"));
	}

	/**
	 * Tests if the option {@code history} is working when provided with its metaVar to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionHistory() {
		PrintDayTotals parsedCommandLineOptions = parseArguments();
		assertThat(parsedCommandLineOptions.getHistoryCount(), equalTo(parsedCommandLineOptions.getWindowSize()));

		parsedCommandLineOptions = parseArguments("--history", "1");
		assertThat(parsedCommandLineOptions.getHistoryCount(), equalTo(1));

		parsedCommandLineOptions = parseArguments("--history", "0");
		assertThat(parsedCommandLineOptions.getHistoryCount(), equalTo(0));

		parsedCommandLineOptions = parseArguments("--history", "-1");
		assertThat(parsedCommandLineOptions.getHistoryCount(), equalTo(-1));
	}

	/**
	 * Tests if the option {@code history} is working when provided as an alias with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionHistoryWithAlias() {
		PrintDayTotals parsedCommandLineOptions = parseArguments("-c", "1");
		assertThat(parsedCommandLineOptions.getHistoryCount(), equalTo(1));
	}

	/**
	 * Tests if the option {@code history} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionHistoryWithNoMetaVar() {
		assertThrows(CommandLine.MissingParameterException.class, () -> this.parseArguments("--history"));
	}

	/**
	 * Parses the given arguments into a {@link CommandLineOptions} to encapsulate the logic of parsing the arguments in each tests.
	 * 
	 * @implSpec This implementation delegates to {@link #parseArguments(LocalDate, String...)} with the current date from {@link LocalDate#now()}.
	 * @param args The arguments to be parsed.
	 * @return The {@link CommandLineOptions} with the parsed arguments.
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	private PrintDayTotals parseArguments(final String... args) {
		return parseArguments(LocalDate.now(), args);
	}

	/**
	 * Parses the given arguments into a {@link CommandLineOptions} to encapsulate the logic of parsing the arguments in each tests. An explicit local date is
	 * given to be considered "now".
	 * 
	 * @param now The current local date to use as the current date+time when processing the command line arguments.
	 * @param args The arguments to be parsed.
	 * @return The {@link CommandLineOptions} with the parsed arguments.
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	private PrintDayTotals parseArguments(@Nonnull final LocalDate now, final String... args) {
		final PrintDayTotals printDayTotals = new PrintDayTotals(args);
		new CommandLine(printDayTotals).parseArgs(args);
		printDayTotals.setNow(now);
		return printDayTotals;
	}

}
