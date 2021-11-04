# Information Retrievel Assignment 1

## Usage

First, go to the top of the main class and change the all-caps variables to use the correct data/directories
(dataset not included in the git repository, not sure if it has to be included here? Large dataset will be
1.4 GB compressed, seemed too large to include)



results will always be written to root folder as result.csv. If you want to keep old results, make sure to copy the old file.

Make sure Lucene is added to the project before running. Version
[7_3_1](https://archive.apache.org/dist/lucene/java/7.3.1/) is used (7.3.0 probably fine too?) The jar files needed are core, queryParser and
misc (Should the lucene jars be included on github?)

Adding jars using intelliJ:

- Top left -> File
- Project Structure
- left sidebar: Modules
- Select Lucene_code in the middle column
- On the right, select dependencies
- Look for a +
- JARs or directories
- Select needed jar files

