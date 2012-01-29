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

require 'optparse'
require 'csv'

# ---------------------------------------------------------------------------
# Constants
# ---------------------------------------------------------------------------

DATA_DIR = File.dirname(File.expand_path(__FILE__))

# ---------------------------------------------------------------------------
# Functions
# ---------------------------------------------------------------------------

def load_names_file(name)
  File.open(File.join(DATA_DIR, name)).readlines.map { |s| s.chomp.capitalize }
end

def generate(total, file, last_names, gender, csv)
  first_names = load_names_file(file)
  (1..total).each do
    first_name = first_names[rand(first_names.length)]
    last_name  = last_names[rand(last_names.length)]
    csv << [first_name, last_name, gender]
  end
end

def generate_names(total_male, total_female, separator=",")
  last_names = load_names_file("last_names.txt")

  CSV.generate(:col_sep => separator) do |csv|
    [[total_female, "female_first_names.txt", "F"],
     [total_male,   "male_first_names.txt",   "M"]].each do |total, file, gender|
      generate(total, file, last_names, gender, csv) if total > 0
    end
  end
end

def die(msg)
  $stderr.puts(msg)
  exit(1)
end

# ---------------------------------------------------------------------------
# Command line options
# ---------------------------------------------------------------------------

options = {
  :female_percent => nil,
  :male_percent   => nil,
  :field_sep      => ","
}

optparse = OptionParser.new do |opts|
  opts.banner = "Usage: namegen.rb [options] total"

  opts.on("-f", "Generate only female names.") do
    options[:female_percent] = 100
    options[:male_percent] = 0
  end

  def parse_percent(s, opt)
    die "Non-numeric argument to #{opt}" unless s =~ /^\d+$/
    i = s.to_i
    die "Invalid percentage value: #{s}" unless (0..100).include? i
    i
  end

  opts.on("--female PERCENT", "Percentage of female names. Default: 50") do |pct|
    options[:female_percent] = parse_percent(pct, "--female")
  end

  opts.on("-m", "Generate only male names.") do
    options[:male_percent] = 100
    options[:female_percent] = 0
  end

  opts.on("--male PERCENT", "Percentage of male names. Default: 50") do |pct|
    options[:male_percent] = parse_percent(pct, "--male")
  end

  opts.on("-s", "--sep STR", "Output field separator. Default: ,") do |s|
    options[:field_sep] = s
  end
end

optparse.parse!

die optparse.to_s unless ARGV.length == 1

# ---------------------------------------------------------------------------
# Main logic
# ---------------------------------------------------------------------------

female_percent = options[:female_percent] || 0
male_percent   = options[:male_percent] || 0

case
when options[:female_percent].nil? && options[:male_percent].nil?
  options[:female_percent] = 50
  options[:male_percent]   = 50

when options[:female_percent].nil?
  options[:female_percent] = 100 - options[:male_percent]

when options[:male_percent].nil?
  options[:male_percent] = 100 - options[:female_percent]
end

if (options[:male_percent] + options[:female_percent]) != 100
  die "Male and female percentage values don't add up to 100"
end

total = ARGV[0].to_i

totals = {
  :male   => (total * options[:male_percent]) / 100,
  :female => (total * options[:female_percent]) / 100
}

# Handle division slop.
i = 0
while (totals[:male] + totals[:female]) < total
  which = ((i % 2) == 0) ? :male : :female
  totals[which] += 1
end

raise "Assertion failed." unless (totals[:male] + totals[:female]) == total

puts generate_names(totals[:male], totals[:female], options[:field_sep])
