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
replace `bin/activator` with `sbt` in the above commands.

I don't run this thing on Windows, so I'm probably not going to go out of
my way to support it there. Caveat (Windows) user.

# Usage

Run `peoplegen --help` for a usage summary.
