# Name Generator

`namegen.rb` is just a quick-and-dirty Ruby script that generates a set of fake
person names, using first and last names taken, at random from United States
Census Bureau data that's captured in local files. By default, it splits the
generated names so that half are female and half are male, but that can be
changed via command line options.

Output is in CSV format, by default, though the column separator can be
changed.

# Usage

    Usage: namegen.rb [options] total
        -f                           Generate only female names.
        --female PERCENT             Percentage of female names. Default: 50
        -m                           Generate only male names.
        --male PERCENT               Percentage of male names. Default: 50
        -s, --sep STR                Output field separator. Default: ,

# Examples

## Generate 10 names, half male, half female

    $ ruby namegen.rb 10          
    Wanda,Sydney,F
    Despina,Wiede,F
    Nereida,Jamesson,F
    Ludivina,Ailey,F
    Windy,Hooser,F
    Cody,Courseault,M
    Augustus,Zwolski,M
    Maximo,Jovel,M
    Andre,Gravis,M
    Mohamed,Drivers,M

## Generate 20 names, all female

    $ ruby namegen.rb -f 20
    Maude,Korpela,F
    Casimira,Eade,F
    Emmaline,Latz,F
    Harriett,Ranni,F
    Kecia,Trame,F
    Danielle,Hudgens,F
    Modesta,Avila,F
    Hollie,Paddock,F
    Amada,Molinski,F
    Molly,Buccellato,F
    Fernanda,Solle,F
    Melinda,Dienes,F
    Toni,Speilman,F
    Marline,Laboissonnier,F
    Gilda,Mcquarrie,F
    Ernestina,Kinart,F
    Son,Wisor,F
    Chiquita,Volpe,F
    Dionne,Hamil,F
    Ehtel,Fagle,F

## Generate 20 names, 30% of them male, and use a space as the separator

    $ ruby namegen.rb --male 30 -s ' ' 20
    Dorthy Ogans F
    Micheline Atwill F
    Edie Bellmore F
    Karyl Maree F
    Dianna Westcoat F
    Ja Elletson F
    Priscila Embery F
    Layla Dewolff F
    Randi Tanski F
    Magaly Fontaine F
    Cherryl Mcclammy F
    Minh Schrum F
    Tiesha Bochicchio F
    Lynell Carabajal F
    Norberto Chari M
    Eduardo Olide M
    Rusty Regester M
    Cameron Novencido M
    Damon Efrati M
    Chuck Donges M

# License

This trivial thing is released under a BSD license. See `LICENSE.md`.
