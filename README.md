# Information Retrievel Assignment 1

## Installation

The datasets are not included on git, and their text files manually have to be placed in ./Datasets/Small for the small 
dataset and ./Datasets/Large for the large dataset. The query files from the assignment are included on git.

Make sure Lucene is added to the project before running. Version
[7_3_1](https://archive.apache.org/dist/lucene/java/7.3.1/) is used. The jar files needed are core, queries, queryParser and
misc.

The code contains several hardcoded paths,
so the program should always be used from this project root.


## Usage

Running mainClass.main() will run the main set of queries. If you want to recreate the results of the test queries, 
you should instead call testSmall() for the small dataset or run_best_of_each() for the large 
dataset. Indexing the large dataset can take up to 30 
minutes, and the large test runs 4 models, so be aware that this can take some time.

Query output will be written to result.csv (this will overwrite the existing file if one exists). 
If either of the tests with example results is used, an additional file comparisons.csv is generated (or overwritten). 
This contains two columns: the left one has the name of the model, the right one has the amount of unmatched files as 
described in 
the report.

Running the tests should not be necessary, as the test results for the small dataset are provided in SmallResultsCSV.csv
and results for the large dataset are included in the report.
