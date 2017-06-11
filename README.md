# Random People Generator

This package is just Scala-based command-line tool to create fake people
records, using first and last names taken at random from United
States Census Bureau data that's captured in local files. By default, it splits
the generated names so that half are female and half are male, but that can be
changed via command line options.

The tool can generate CSV or JSON output.

As is probably obvious, I use this script to generate test data.

# Installation

Clone this repo in the usual way. Then, read on.

`peoplegen` is built with [SBT](http://scala-sbt.org), but if you're installing
on Mac OS X or a Unix-like system, you can just use `bin/activator` in this
repo.

```
bin/activator install
```

will build a fat jar and install it in `$HOME/local/libexec`, by default.
It'll then install a wrapper `peoplegen` script in `$HOME/local/bin`. You
can change the prefix from `$HOME/local` to something else by setting the
`INSTALL_HOME` environment variable. For example:

```
INSTALL_HOME=/usr/local bin/activator install
```

Windows users, you'll have to install SBT manually. Then you can just
run `sbt install`, instead.

**NOTE**: I don't run this thing on Windows, so I'm probably not going to go 
out of my way to support it there. Caveat (Windows) user.

# Usage

At any time, you can run `peoplegen --help` for a usage summary. It
currently supports the following options:

* `--help`: Generate the usage message and exit.

* `-f <percent>` or `--female <percent>`: Percent of female records to
  generate.

* `-m <percent>` or `--male <percent>`: Percent of male records to generate.
  
**NOTE**: If you specify neither male nor female percentages, both default
to 50. If you specify only one percentage, the other is set to the remainder.
(e.g., If you specify only `--male 60`, the female percentage is set to 40.)
If you set _both_ percentages, they _must_ add up to 100, or `peoplegen`
will abort.

* `--ssn`: Generate Social Security Number values. Note that the generated
  SSNs are deliberately invalid, as described at 
  <https://stackoverflow.com/a/2313726/53495>.
  
* `--salaries`: Generate salary data. Salaries are generated as a normal 
  distribution of integers, around a mean of 72,641 (the U.S. mean salary in 
  2014), with a sigma (i.e., a spread, or standard deviation) of 20,000. To 
  change these values, use --salary-mean and
                                 --salary-sigma.

peoplegen, 1.0.0 (built 2017/06/11 00:19:14)

Usage: peoplegen [options] <total> [<outputfile>]

  --help
        This usage message.
  -f <percent> | --female <percent>
        Percentage of female names. Defaults to 50.
  -m <percent> | --male <percent>
        Percentage of male names. Defaults to 50.
  --ssn
        Whether or not to generate (fake) SSNs.
  --salaries
        Generate salary data. Salaries are generated as a normal distribution
        around a mean of 72641 (the U.S. mean salary in 2014), with a sigma
        (spread) of 20000. To changes these values, use --salary-mean and
        --salary-sigma.
  --salary-mean <value>
        Change the salary generation mean. Note: Changing this value can
        result in negative salaries, so check your final data.
  --salary-sigma <value>
        Change the salary generation sigma (i.e., standard deviation). Note:
        Changing this value can result in negative salaries, so check your
        final data.
  --delim <c>
        Delimiter to use in CSV mode. Use "\t" for tab.
  --header
        Generate a header for CSV output. Default: no header
  -F <format> | --format <format>
        File format to generate. Allowable values: csv, json
  --camel
        Use camelCase for column names.
  --english
        Use English (space-separated) names for column names.
  -j <format> | --json-format <format>
        If generating JSON, specify how the JSON is generated. Ignored if
        --pretty is specified. Legal values:
        "array": write one JSON array of records.
        "rows": write one JSON object per line.
        Default: rows
  --pretty
        Pretty-print JSON, instead of printing it all on one line. honored if
        --format is "json".
  --snake
        Use snake_case for column names.
  -v | --verbose
        Emit (some) verbose messages
  <total>
        Total number of names to generate
  <outputfile>
        Output path.

