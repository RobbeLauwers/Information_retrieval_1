# Information Retrievel Assignment 1

## Installation

Make sure the [small dataset](https://drive.google.com/file/d/1_8np3rVc8WIknYLr9G2e1gSr2lRPVT60/view) 's text files are
in [./Datasets/Small](./Datasets/Small)
, [large dataset](https://drive.google.com/file/d/17Fyu7y7fx6Z6RptxiUbioqmjBhXOp2HN/view)'s text files should be
in [./Datasets/Large/full_docs/full_docs](./Datasets/Large/full_docs/full_docs) (Other directories are fine if we change them to not be hardcoded)  (not included in the git repository, not sure if it has to be included here? Large dataset will be 1.4 GB compressed,
seemed too large to include)

Make sure Lucene is added to the project before running. Version
[7_3_1](https://archive.apache.org/dist/lucene/java/7.3.1/) is used. The jar files needed are core, queryParser and
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

