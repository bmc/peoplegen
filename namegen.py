# Quick and dirty script to generate the specified number of random people,
# using U.S. census data for first names, last names, and genders.
#
# Usage:
#
# ruby namegen.rb [options] total
#
# run with no arguments for a usage summary.
#
# Displays generated names in CSV format. The separator can be specified on
# the command line.

# ---------------------------------------------------------------------------
# Includes
# ---------------------------------------------------------------------------

from argparse import ArgumentParser, ArgumentDefaultsHelpFormatter
import csv
import string
import random
import sys

# ---------------------------------------------------------------------------
# Functions
# ---------------------------------------------------------------------------

def load_names_file(filename):
    # Read the lines in the file, strip off the newlines, and capitalize
    # each of the resulting names.
    return map(lambda x: string.capitalize(x.rstrip()), open(filename))

def generate(total, file, last_names):
  first_names = load_names_file(file)

  def generate_one():
    # Choose a random first name and a random last name.
    first_name = first_names[random.randint(0, len(first_names) - 1)]
    last_name  = last_names[random.randint(0, len(last_names) - 1)]
    return first_name, last_name

  return map(lambda i: generate_one(), range(0, total))

def generate_names(total_male, total_female, options):
    last_names = load_names_file("last_names.txt")
    separator = options.field_sep
    format = options.format

    params = [[total_female, "female_first_names.txt", "F"],
            [total_male,   "male_first_names.txt",   "M"]]
    buf = []
    for total, filename, gender in params:
        if total > 0:
            for first, last in generate(total, filename, last_names):
                buf.append([first, last, gender])
    
    if format is 'text':
        for first, last, gender in buf:
            print "%s %s (%s)" % (first, last, gender)
    elif format is 'csv':
        pass
    else:
        die ("OOOPS")

def die(msg):
    sys.stderr.write(msg + "\n")
    sys.exit(1)

# ---------------------------------------------------------------------------
# Command line options
# ---------------------------------------------------------------------------

argparser = ArgumentParser(formatter_class=ArgumentDefaultsHelpFormatter)
argparser.add_argument('-f', dest="female", help="Generate only female names",
                       action='store_true')
argparser.add_argument('-m', dest="male", help="Generate only male names",
                       action='store_true')
argparser.add_argument('--female', dest="female_percent", type=int,
                       help="Percentage of names that should be female.",
                       metavar="PERCENT")
argparser.add_argument('--male', dest="male_percent", type=int,
                       help="Percentage of names that should be male.",
                       metavar="PERCENT")
argparser.add_argument('-s', '--sep', help="CSV field separator.", metavar="SEP",
                       default=',', dest='field_sep')
argparser.add_argument('-F', '--format', help="Output format: text, csv",
                       metavar="format", default="text")
argparser.add_argument("total", help="Total number of names to generate",
                       type=int)

options = argparser.parse_args()

if options.female:
    options.male_percent = 0
    options.female_percent = 100

if options.male:
    options.male_percent = 100
    options.female_percent = 0

if options.female_percent is None and options.male_percent is None:
    options.female_percent = options.male_percent = 50
elif options.female_percent is not None and options.male_percent is None:
    options.male_percent = 100 - options.female_percent
elif options.female_percent is None and options.male_percent is not None:
    options.female_percent = 100 - options.male_percent

if options.female_percent + options.male_percent != 100:
    die("--male and --female percentage values don't add up to 100")

# ---------------------------------------------------------------------------
# Main logic
# ---------------------------------------------------------------------------

totals = {
   'male':   (options.total * options.male_percent) / 100,
   'female': (options.total * options.female_percent) / 100
 }

# Handle division slop.
i = 0
while (totals['male'] + totals['female']) < options.total:
    which = 'male' if ((i % 2) == 0) else 'female'
    totals[which] += 1

if totals['female'] + totals['male'] != options.total:
    raise "(BUG) Fix me."

generate_names(totals['male'], totals['female'], options)
