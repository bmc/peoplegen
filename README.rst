Random People Generator
=======================

This package is just a quick-and-dirty Python script that generates a
set of fake person names, using first and last names taken, at random
from United States Census Bureau data that's captured in local files. By
default, it splits the generated names so that half are female and half
are male, but that can be changed via command line options.

The tool can generate CSV or JSON output.

As is probably obvious, I use this script to generate test data.

Installation
============

``peoplegen`` isn't in `PyPI <https://pypi.python.org/pypi>`__. If you
want it, you need to get the source and build it.

You can get the source one of two ways:

**Way 1**. Clone this repo:

::

    git clone https://github.com/bmc/peoplegen.git

**Way 2**. Download the tarball and unpack it:

::

    wget -O peoplegen.tar.gz https://github.com/bmc/peoplegen/tarball/master
    tar xf peoplegen.tar.gz

Then, ``cd`` into the resulting directory and type:

::

    python setup.py install

Usage
=====

::

    peoplegen — Generate random people data.

    Usage:
      peoplegen --help
      peoplegen [options] <total> <outputfile>

    Options:
       -f PERCENT, --female PERCENT  Percentage of female names. [default: 50]
       -m PERCENT, --male PERCENT    Percentage of male names. [default: 50]
       --ssn                         Generate social security numbers
       --header                      Generate header record if output is CSV.
       --salary RANGE                Generate salaries. Range is: <lower>-<upper>
       --yearmin YEAR                Minimum birth year. Defaults to 65 years ago.
       --yearmax YEAR                Maximum birth year. Defaults to 18 years ago.
       -s SEP, --sep SEP             Output field separator. [default: ,]

    The output format is determined from the output file extension. Use '.json'
    for JSON output and '.csv' for CSV output.

    If you specify one of -m or -f, but not the other, the other will be calculated.
    For instance, -f 25 means "25% females, 75% males". You can specify both.

Examples
========

**Generate 10 names, half male, half female**

::

    $ peoplegen 10 out.csv
    $ cat out.csv
    Andrea,Tillie,Angwin,F,1989-11-12
    Riva,Genie,Bushner,F,1985-11-23
    Svetlana,Shandi,Difeo,F,1991-03-16
    Sharda,Conchita,Peleg,F,1995-07-16
    In,Mai,Benischek,F,1957-07-09
    Rudolph,Dominique,Kurtz,M,1963-10-18
    Winston,Ollie,Yasui,M,1951-02-13
    Milan,Glenn,Amati,M,1998-07-23
    Rob,Britt,Kar,M,1958-05-22
    Dustin,Brad,Metenosky,M,1971-01-07

**Generate 5 names, all female, with fake Social Security Numbers and a
header**

::

    $ peoplegen --header --ssn -f 100 5 out.csv
    firstName,middleName,lastName,gender,birthDate,ssn
    Naida,Conception,Dowse,F,1987-02-27,982-49-3226
    Gilberte,Bernice,Huckle,F,1983-02-09,907-93-7146
    Allie,Hilda,Brem,F,1994-02-14,924-78-7354
    Danyell,Theresa,Wickson,F,1952-05-15,903-77-4317
    Jeane,Susanne,Hoffarth,F,1973-12-12,913-23-9300

**Generate 10 names, 30% of them male, with fake Social Security
Numbers, salaries between 10,000 and 200,000, and a header, using a ":"
delimiter**

::

    $ peoplegen --header --sep : --male 30 --ssn --salary 10000-200000 10 out.csv
    firstName:middleName:lastName:gender:birthDate:salary:ssn
    Lindsy:Natacha:Michealson:F:1981-12-25:130016:927-73-4816
    Kiyoko:Alfreda:Matthew:F:1990-02-25:78339:996-58-7457
    Reva:Jerica:Tiede:F:1958-05-11:29695:998-52-6279
    Troy:Alecia:Czachorowski:F:1990-11-23:65992:997-77-2400
    Myrtle:Jenette:Granfield:F:1988-06-09:71149:924-55-8164
    Rocio:Eugenie:Dalka:F:1962-06-19:78364:948-28-6392
    Ilene:Trudi:Conerly:F:1960-10-05:72132:925-57-9109
    Porfirio:Rafael:Herke:M:1997-10-30:129596:908-87-2184
    Eric:Bret:Fontaine:M:1994-07-06:61913:904-23-7909
    Curtis:Solomon:Caoagdan:M:1969-07-13:134306:986-19-2614

License
=======

This trivial thing is released under a BSD license. See ``LICENSE.md``.
