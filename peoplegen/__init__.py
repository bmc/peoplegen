# -*- coding: utf-8 -*-
'''
peoplegen — Generate random people data.

Usage:
  peoplegen --help
  peoplegen [options] <total> <outputfile>

Options:
   -f PERCENT, --female PERCENT  Percentage of female names. Defaults to 50%.
   -m PERCENT, --male PERCENT    Percentage of male names. Defaults to 50%.
   --ssn                         Generate social security numbers.
   --header                      Generate header record if output is CSV.
   --salary RANGE                Generate salaries. Range is: LOWER-UPPER
   --yearmin YEAR                Minimum birth year. Defaults to 65 years ago.
   --yearmax YEAR                Maximum birth year. Defaults to 18 years ago.
   -s SEP, --sep SEP             Output field separator. [default: ,]

The output format is determined from the output file extension. Use '.json'
for JSON output and '.csv' for CSV output.

If you specify one of -m or -f, but not the other, the other will be calculated.
For instance, -f 25 means "25% females, 75% males". You can specify both.
'''

from __future__ import (absolute_import, division, print_function,
                        unicode_literals)

from builtins import (bytes, dict, int, list, object, range, str, ascii,
                      chr, hex, input, next, oct, open, pow, round, super,
                      filter, map, zip)
from future.standard_library import install_aliases
install_aliases()

import sys
import re
import docopt
import random
import os
from pkg_resources import resource_stream

__version__ = '2.0'
USAGE = __doc__

SSN_PREFIXES = list(range(900, 1000)) + [666]


def _load_data_file(name):
    here=os.path.dirname(os.path.abspath(__file__))
    files_to_check = (
        os.path.join(here, 'peoplegen', 'data', name),
        os.path.join(here, 'data, name')
    )

    res = None
    for path in files_to_check:
        if os.path.exists(path):
            with open(path) as f:
                res = [s.strip() for s in f.readlines()]
            break
    else:
        f = resource_stream('peoplegen.data', name)
        res = [s.strip() for s in f.readlines()]

    return res


def _load_data():
    last_names = _load_data_file('last_names.txt')
    male_first_names = _load_data_file('male_first_names.txt')
    female_first_names = _load_data_file('female_first_names.txt')
    return (last_names,
            [s.capitalize() for s in female_first_names],
            [s.capitalize() for s in male_first_names])


def _generate_salaries(total, low, high):
    # This is a hack to approximate a normal (bell curve) distribution while
    # eliminating values that fall outside the [lower, upper] range.
    mean = (low + high) / 2
    sigma = min((high - low) / 5, 30000)

    nums = []
    while len(nums) < total:
        n = int(random.normalvariate(mean, sigma))
        if (n >= low) and (n <= high):
            nums.append(n)

    return nums


def _make_fake_ssn():
    first = random.choice(SSN_PREFIXES)
    second = random.randint(10, 99)
    third = random.randint(1000, 9999)
    return '{0}-{1}-{2}'.format(first, second, third)


def _generate_name(first_names, last_names):
    first = random.choice(first_names)
    return (first,
            random.choice(first_names),
            random.choice(last_names))


def _generate_birth_date(min_year, max_year):
    from datetime import datetime, date
    import time
    import calendar

    today = datetime.today()
    if not min_year:
        min_year = today.year - 65
    if not max_year:
        max_year = today.year - 18

    min_date = date(year=min_year, month=1, day=1)
    max_date = date(year=max_year,month=12, day=31)

    # Convert to unix times.
    max_epoch = calendar.timegm(max_date.timetuple())
    min_epoch = calendar.timegm(min_date.timetuple())

    rand_epoch = random.randint(min_epoch, max_epoch)
    return unicode(time.strftime('%Y-%m-%d', time.localtime(rand_epoch)))

def _write_csv(file, delim, people, header, salaries=False, ssn=False):
    import csv
    import unicodedata
    sep = unicodedata.normalize('NFKD', delim).encode('ascii', 'ignore')

    with open(file, 'wb') as csvfile:
        w = csv.writer(csvfile, delimiter=sep)
        if header:
            headers = ['firstName', 'middleName', 'lastName', 'gender',
                       'birthDate']
            if salaries:
                headers.append('salary')
            if ssn:
                headers.append('ssn')
            w.writerow(headers)

        for person in people:
            data = [
                person['firstName'], person['middleName'], person['lastName'],
                person['gender'], person['birthDate']
            ]
            if salaries:
                data.append(person['salary'])
            if ssn:
                data.append(person['ssn'])
            w.writerow(data)

def _write_json(file, people):
    import json
    with open(file, 'wb') as out:
        json.dump(people, out)

def _die(msg):
    sys.stderr.write(msg + '\n')
    sys.exit(1)


def _die_usage(msg):
    _die(msg + '\n' + USAGE)


def _parse_salary(salary_range):
    range_pat = re.compile('^(\d+)-(\d+)$')
    try:
        m = range_pat.match(salary_range)
        if not m:
            _die_usage('Bad range for --salary: {0}'.format(salary_range))

        lower = int(m.group(1))
        upper = int(m.group(2))
        if lower >= upper:
            _die_usage('Empty range for --salary: {0}'.format(salary_range))
        return (lower, upper)
    except ValueError:
        _die(USAGE)


def _parse_percents(male_percent_str, female_percent_str):
    if not (male_percent_str or female_percent_str):
        # Neither specified. 50% each.
        male_percent = 50
        female_percent = 50
    else:
        r = re.compile('^\d+$')
        male_percent = None
        female_percent = None
        if male_percent_str:
            if not r.match(male_percent_str):
                _die_usage('Bad male percent: {0}'.format(male_percent_str))
            male_percent = int(male_percent_str)

        if female_percent_str:
            if not r.match(female_percent_str):
                _die_usage('Bad female percent: {0}'.format(female_percent_str))
            female_percent = int(female_percent_str)


        if not female_percent:
            # We have a male value, but not a female one.
            female_percent = 100 - male_percent
        if not male_percent:
            # We have a male value, but not a female one.
            male_percent = 100 - female_percent

    if (male_percent + female_percent) != 100:
        _die_usage("Male and female percentages don't add up to 100.")

    return (male_percent, female_percent)


def _parse_args():
    opts = docopt.docopt(USAGE, version=__version__)
    if opts['--salary']:
        opts['--salary'] = _parse_salary(opts['--salary'])
    (male_percent, female_percent) = _parse_percents(opts['--male'],
                                                     opts['--female'])
    opts['--male'] = male_percent
    opts['--female'] = female_percent
    opts['--sep'] = str(opts['--sep'])

    for key in ('--yearmin', '--yearmax', '<total>'):
        try:
            if opts[key]:
                opts[key] = int(opts[key])
        except ValueError:
            _die_usage('Bad value for {0}: "{1}"'.format(key, opts[key]))

    _, ext = os.path.splitext(opts['<outputfile>'])
    if ext == '.csv':
        opts['<format>'] = 'csv'
    elif ext == '.json':
        opts['<format>'] = 'json'
    else:
        _die_usage('Unknown extension: {0}'.format(ext))

    return opts


def main():
    opts = _parse_args()
    last_names, female_first_names, male_first_names = _load_data()
    people = []
    salaries = None
    total = opts['<total>']
    if opts['--salary']:
        (low, high) = opts['--salary']
        salaries = _generate_salaries(total, low, high)

    total_females = int(total * opts['--female'] / 100)
    total_males   = int(total * opts['--male'] / 100)
    delta = abs(total - total_females - total_males)
    total_females += delta

    # Generate the invariant fields.
    for first_names, n, gender in ((female_first_names, total_females, 'F'),
                                   (male_first_names, total_males, 'M')):
      for i in range(n):
          person = {}
          first, middle, last = _generate_name(first_names, last_names)
          person['firstName'] = first
          person['middleName'] = middle
          person['lastName'] = last
          person['gender'] = gender
          person['birthDate'] = _generate_birth_date(opts['--yearmin'],
                                                     opts['--yearmax'])
          people.append(person)

    # Generate everything else
    for i, person in enumerate(people):
        if opts['--ssn']:
            person['ssn'] = _make_fake_ssn()

        if opts['--salary']:
            person['salary'] = salaries[i]

    if opts['<format>'] == 'json':
        _write_json(opts['<outputfile>'], people)
    else:
        _write_csv(file=opts['<outputfile>'],
                   people=people,
                   delim=opts['--sep'],
                   header=opts['--header'],
                   salaries=opts['--salary'] is not None,
                   ssn=opts['--ssn'])

if __name__ == '__main__':
    main()
