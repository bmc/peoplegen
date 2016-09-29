# Name Generator

`namegen.rb` is just a quick-and-dirty Ruby script that generates a set of fake
person names, using first and last names taken, at random from United States
Census Bureau data that's captured in local files. By default, it splits the
generated names so that half are female and half are male, but that can be
changed via command line options.

Output is in CSV format, by default, though the column separator can be
changed.

As is probably obvious, I use this script to generate test data.

# Usage

```
Usage: namegen.rb [options] total
    -f                               Generate only female names.
        --female PERCENT             Percentage of female names. Default: 50
    -m                               Generate only male names.
        --male PERCENT               Percentage of male names. Default: 50
        --ssn                        Generate social security numbers
        --header                     Generate header record if output is CSV.
        --salary RANGE               Generate salaries
    -F, --format FORMAT              Specify output format: json, csv
    -s, --sep STR                    Output field separator. Default: ,
```

# Examples

**Generate 10 names, half male, half female**

```
$ ruby namegen.rb 10          
Carla,Natashia,Eapen,F,1916-10-21
Joselyn,Debbie,Jeavons,F,1945-11-28
Elodia,Monica,Led,F,1920-04-20
Iliana,Arnita,Atcheson,F,1922-08-20
Pearle,Leonila,Armbrester,F,1921-05-26
Hector,Archie,Offer,M,1984-05-29
Maynard,Chauncey,Laursen,M,1959-10-12
Irwin,Reed,Paschel,M,1986-03-27
Fred,Ruben,Gattuso,M,1925-11-24
Clinton,Rico,Gurule,M,1971-08-16
```

**Generate 20 names, all female, with fake Social Security Numbers and a header**

```
$ ruby namegen.rb --header -f 20 
first_name,middle_name,last_name,gender,birth_date,ssn
Lorilee,Shala,Haggett,F,2004-06-05,963-82-3230
Pennie,Logan,Busuttil,F,2014-04-25,908-29-4957
Kathleen,Marlys,Wrzesien,F,1958-01-09,998-53-6173
Nathalie,Bernarda,Fenton,F,1995-08-01,904-82-8554
Norene,Delsie,Stenson,F,1932-02-20,975-55-6176
Karren,Leatrice,Beyke,F,1933-11-12,969-95-8779
Sherril,Santana,Brommer,F,1966-01-03,918-19-4624
Lavinia,Chelsea,Thoben,F,1997-10-25,912-91-6571
Sharell,Beth,Wedgworth,F,1985-05-10,957-72-5842
Tillie,Randi,Wheelus,F,2006-06-01,970-28-4095
Sondra,Daine,Mitra,F,1987-09-26,996-37-6496
Golda,Penelope,Kirchner,F,1920-10-06,976-47-1407
Cammie,Helene,Skold,F,1955-04-14,959-36-1990
Adelina,Karri,Rieth,F,1949-01-25,902-56-4440
Brandy,Kori,Botz,F,1917-04-26,991-97-6259
Dorene,Kasie,Bulfer,F,1986-08-14,978-45-5028
Judi,Helene,Hund,F,1982-12-06,922-39-8006
Emilia,Valeri,Saniger,F,2000-01-12,935-21-1193
Wynona,Bonny,Sarette,F,1947-02-03,976-54-8479
Rosanna,Martha,Tull,F,1962-09-20,988-82-1642
```

**Generate 5 names, 30% of them male, with fake Social Security Numbers,
salaries between 10,000 and 100,000, and a header, using a ":" delimiter**

```
$ ruby namegen.rb --male 30 -s : --ssn --salary 10000-100000 --header 5
first_name:middle_name:last_name:gender:birth_date:ssn:salary
Kerstin:Britteny:Cashio:F:1977-12-14:913-31-5613:48154
Veronica:Fatimah:Slee:F:1943-04-05:930-32-7212:11367
Keli:Andera:Duch:F:1943-02-21:926-61-2383:77694
Hilton:Royce:Arciga:M:2004-05-17:947-20-4330:55205
Benjamin:Ernie:Corkery:M:1941-03-29:909-24-3585:11670
```

# License

This trivial thing is released under a BSD license. See `LICENSE.md`.
