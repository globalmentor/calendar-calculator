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

import org.junit.*;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import com.globalmentor.calendar.calculator.PrintDayTotals.CommandLineOptions;

import static org.junit.Assert.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.hamcrest.Matchers.*;

/**
 * Unit tests to the arguments used in the {@link PrintDayTotals} program.
 * 
 * @author Magno Nascimento
 */
public class PrintDayTotalsTest {

	/**
	 * Tests if the option {@code help} is working when provided alone to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionHelpAlone() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = parseArguments("--help");
		assertThat(parsedCommandLineOptions.help(), is(true));

		assertThat(new CommandLineOptions().help(), is(false));
	}

	/**
	 * Tests if the option {@code help} is working when provided with other arguments to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionHelpWithOtherArguments() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions;

		parsedCommandLineOptions = parseArguments("--help", "--date", "2017-01-30");
		assertThat(parsedCommandLineOptions.help(), is(true));

		parsedCommandLineOptions = parseArguments("--help", "--from", "2016-02-16");
		assertThat(parsedCommandLineOptions.help(), is(true));

		parsedCommandLineOptions = parseArguments("--help", "--window", "10");
		assertThat(parsedCommandLineOptions.help(), is(true));

		parsedCommandLineOptions = parseArguments("--help", "--max", "10");
		assertThat(parsedCommandLineOptions.help(), is(true));

		parsedCommandLineOptions = parseArguments("--help", "--history", "10");
		assertThat(parsedCommandLineOptions.help(), is(true));

		parsedCommandLineOptions = parseArguments("--help", "--date", "2017-01-30", "--window", "10", "--max", "10", "--history", "10");
		assertThat(parsedCommandLineOptions.help(), is(true));

		parsedCommandLineOptions = parseArguments("--date", "2017-01-30");
		assertThat(parsedCommandLineOptions.help(), is(false));

		parsedCommandLineOptions = parseArguments("--from", "2016-02-16");
		assertThat(parsedCommandLineOptions.help(), is(false));

		parsedCommandLineOptions = parseArguments("--window", "10");
		assertThat(parsedCommandLineOptions.help(), is(false));

		parsedCommandLineOptions = parseArguments("--max", "10");
		assertThat(parsedCommandLineOptions.help(), is(false));

		parsedCommandLineOptions = parseArguments("--history", "10");
		assertThat(parsedCommandLineOptions.help(), is(false));

		parsedCommandLineOptions = parseArguments("--date", "2017-01-30", "--window", "10", "--max", "10", "--history", "10");
		assertThat(parsedCommandLineOptions.help(), is(false));
	}

	/**
	 * Tests if the option {@code date} is working when provided with its metaVar to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionDate() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions;

		assertThat(parseArguments().getDate(), equalTo(LocalDate.now()));

		parsedCommandLineOptions = parseArguments("--date", "2017-01-30");
		assertThat(parsedCommandLineOptions.getDate(), equalTo(LocalDate.of(2017, 1, 30)));

		parsedCommandLineOptions = parseArguments("--date", "03-06");
		assertThat(parsedCommandLineOptions.getDate(),
				equalTo(LocalDate.parse(CommandLineOptions.ISO_LOCAL_DATE_WITHOUT_YEAR_TEMPLATE.apply(LocalDate.now().getYear(), "03-06"))));
	}

	/**
	 * Tests if the option {@code date} is working when provided as an alias with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionDateAlias() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = parseArguments("-d", "2017-01-30");
		assertThat(parsedCommandLineOptions.getDate(), equalTo(LocalDate.of(2017, 1, 30)));
	}

	/**
	 * Tests if the option {@code date} is throwing an exception when its metaVar is in an invalid date format.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = DateTimeParseException.class)
	public void testCommandLineOptionDateWithWrongFormat() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = parseArguments("--date", "30-01-2017");
		parsedCommandLineOptions.getDate();
	}

	/**
	 * Tests if the option {@code date} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = CmdLineException.class)
	public void testCommandLineOptionDateWithNoMetaVar() throws CmdLineException {
		this.parseArguments("--date");
	}

	/**
	 * Tests if the option {@code history} is working when provided with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionFromDate() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions;

		//tests if <fromDate> is starting exactly one year before LocalDate.now()
		parsedCommandLineOptions = parseArguments();
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo((int)ChronoUnit.DAYS.between(LocalDate.now().minusYears(1), LocalDate.now())));

		//tests if <fromDate> is starting exactly one year before a given <date>
		parsedCommandLineOptions = parseArguments("--date", "2017-03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(),
				equalTo((int)ChronoUnit.DAYS.between(LocalDate.parse("2017-03-06").minusYears(1), LocalDate.parse("2017-03-06"))));

		//tests if the period between <fromDate> and LocalDate.now() is being correctly calculated
		parsedCommandLineOptions = parseArguments("--from", "2017-03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo((int)ChronoUnit.DAYS.between(LocalDate.parse("2017-03-06"), LocalDate.now())));

		//tests if the period between <fromDate> and LocalDate.now() is being correctly calculated without explicit use of a year
		parsedCommandLineOptions = parseArguments("--from", "03-06");
		assertThat(parsedCommandLineOptions.getWindowSize(),
				equalTo((int)ChronoUnit.DAYS.between(LocalDate.parse(CommandLineOptions.ISO_LOCAL_DATE_WITHOUT_YEAR_TEMPLATE.apply(LocalDate.now().getYear(), "03-06")),
						LocalDate.now())));

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
	 * Tests if the option {@code history} is throwing an exception if the initial date provided comes after the final date.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCommandLineOptionFromDateInFuture() throws CmdLineException {
		parseArguments("--from", LocalDate.now().plusDays(1).toString()).getWindowSize();
	}

	/**
	 * Tests if the option {@code history} is working when provided as an alias with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionFromDateWithAlias() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = parseArguments("-f", "2017-01-31", "--date", "2017-01-31");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(0));
	}

	/**
	 * Tests if the option {@code history} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = CmdLineException.class)
	public void testCommandLineOptionFromDateWithNoMetaVar() throws CmdLineException {
		this.parseArguments("--from");
	}

	/**
	 * Tests if the option {@code from} is throwing an exception when its metaVar is in an invalid date format.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = DateTimeParseException.class)
	public void testCommandLineOptionFromDateWithWrongFormat() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = parseArguments("--from", "30-01-2017");
		parsedCommandLineOptions.getWindowSize();
	}

	/**
	 * Tests if the option {@code window} is working when provided with its metaVar to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionWindow() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = new CommandLineOptions();
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo((int)ChronoUnit.DAYS.between(LocalDate.now().minusYears(1), LocalDate.now())));

		parsedCommandLineOptions = parseArguments("--window", "1");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(1));

		parsedCommandLineOptions = parseArguments("--window", "0");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(0));

		parsedCommandLineOptions = parseArguments("--window", "-1");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(-1));
	}

	/**
	 * Tests if the option {@code window} is working when provided as an alias with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionWindowWithAlias() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = parseArguments("-w", "1");
		assertThat(parsedCommandLineOptions.getWindowSize(), equalTo(1));
	}

	/**
	 * Tests if the option {@code window} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = CmdLineException.class)
	public void testCommandLineOptionWindowWithNoMetaVar() throws CmdLineException {
		this.parseArguments("--window");
	}

	/**
	 * Tests if the option {@code max} is working when provided with its metaVar to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionMax() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = new CommandLineOptions();
		assertThat(parsedCommandLineOptions.getMaxDays(), equalTo(Optional.empty()));

		parsedCommandLineOptions = parseArguments("--max", "1");
		assertThat(parsedCommandLineOptions.getMaxDays().get(), equalTo(1));

		parsedCommandLineOptions = parseArguments("--max", "0");
		assertThat(parsedCommandLineOptions.getMaxDays().get(), equalTo(0));
	}

	/**
	 * Tests if the option {@code max} is throwing an exception when provided an negative value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testCommandLineOptionMaxWithNegativaValue() throws CmdLineException {
		parseArguments("--max", "-1").getMaxDays();
	}

	/**
	 * Tests if the option {@code max} is working when provided as an alias with its metaVar to the parser.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionMaxWithAlias() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = parseArguments("-x", "1");
		assertThat(parsedCommandLineOptions.getMaxDays().get(), equalTo(1));
	}

	/**
	 * Tests if the option {@code max} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = CmdLineException.class)
	public void testCommandLineOptionMaxWithNoMetaVar() throws CmdLineException {
		this.parseArguments("--max");
	}

	/**
	 * Tests if the option {@code history} is working when provided with its metaVar to the parser and its default value.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test
	public void testCommandLineOptionHistory() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = new CommandLineOptions();
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
	public void testCommandLineOptionHistoryWithAlias() throws CmdLineException {
		CommandLineOptions parsedCommandLineOptions = parseArguments("-h", "1");
		assertThat(parsedCommandLineOptions.getHistoryCount(), equalTo(1));
	}

	/**
	 * Tests if the option {@code history} is throwing an exception when it's provided with no metaVar.
	 * 
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	@Test(expected = CmdLineException.class)
	public void testCommandLineOptionHistoryWithNoMetaVar() throws CmdLineException {
		this.parseArguments("--history");
	}

	/**
	 * Parses the given arguments into a {@link CommandLineOptions} to encapsulate the logic of parsing the arguments in each tests.
	 * 
	 * @param args The arguments to be parsed.
	 * @return The {@link CommandLineOptions} with the parsed arguments.
	 * @throws CmdLineException if an error occurs while parsing the arguments.
	 */
	private CommandLineOptions parseArguments(final String... args) throws CmdLineException {
		final CommandLineOptions commandLineOptions = new CommandLineOptions();

		new CmdLineParser(commandLineOptions).parseArgument(args);

		return commandLineOptions;
	}

}
