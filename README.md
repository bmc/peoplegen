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

At any time, you can run `peoplegen --help` for a usage summary. The command
line looks like:

```
Usage: peoplegen [options] <total> [<outputfile>]
```

* `<total>` is the total number of people records to generate.
* `<outputfile>` is the file to which to write the records; if not supplied,
  the output goes to standard output.

## Options

`peoplegen` currently supports the following options:

* `--help`: Generate the usage message and exit.

* `-f <percent>` or `--female <percent>`: Percent of female records to
  generate.

* `-m <percent>` or `--male <percent>`: Percent of male records to generate.
  
**NOTE**: If you specify neither male nor female percentages, both default
to 50. If you specify only one percentage, the other is set to the remainder.
(e.g., If you specify only `--male 60`, the female percentage is set to 40.)
If you set _both_ percentages, they _must_ add up to 100, or `peoplegen`
will abort.

* `--id`: Generate unique per-row ID values.

* `--ssn`: Generate Social Security Number values. Note that the generated
  SSNs are deliberately invalid, as described at 
  <https://stackoverflow.com/a/2313726/53495>.
  
* `--salaries`: Generate salary data. Salaries are generated as a normal 
  distribution of integers, around a mean of 72,641 (the U.S. mean salary in 
  2014), with a sigma (i.e., a spread, or standard deviation) of 20,000. To 
  change these values, use --salary-mean and
                                 --salary-sigma.

* `--salary-mean <value>`: You can use this option to change the mean salary
  for the salary distribution. **Note**: Changing this value can result
  in negative salaries, so check your final data.

* `--salary-sigma <value>`: You can use this option to change the salary
  generation sigma—the spread, if you prefer. A smaller number means more
  salaries will cluster around the mean. A larger number means the distribution
  will be more "spread out". The distribution will still be a normal one (a
  bell curve), but the mean and the sigma control the _shape_ of the curve.

* `--year-min <value>`: Specify the starting year for birth dates. Defaults to 
  65 years ago from this year.
  
* `--year-max <value>`: Specify the ending year for birth dates. Defaults to 
  18 years ago from this year. This year cannot _precede_ the `year-min` value.

* `--delim <c>`: (CSV only) The delimiter to use between columns. The default
  is a comma (","). Any single character is fine. For tab, use the 2-character
  sequence "\t".

* `--header`: (CSV only) Generate a header for CSV output. Default: no header

* `-F <format>` or `--format <format>`: The file format to generate. Allowable
  values: "csv" or "json"

* `--camel`: Use camel case for CSV column names or JSON field names. For
  example: `firstName`, `lastName`

* `--english`: Use English (space-separated) names for column names. For
  example: `first name`, `last name`
  
* `--snake`: Use "snake case" (underscores) names for column names. For
  example: `first_name`, `last_name`

* `-j <format>` or `--json-format <format>`: (JSON only) Specify how the
  JSON should be generated. Legal values:
    * "rows" (default): generate individual rows of 1-line JSON people records.
      This format is useful with [Apache Spark](https://spark.apache.org).
    * "array": generate a JSON array with the JSON people records, all on
      one line        
    * "pretty": generate pretty-printed JSON.

* `-v` or `--verbose`: Emit (some) verbose processing messages.
