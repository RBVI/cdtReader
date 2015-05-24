This app reads generalized CDT files (output from Cluster 3.0) or Matrix
files, depending on the specific headers provided.  In the general
case, the app reads in the first line of the file and searches for the
GWEIGHT column.  All data before that column will be read in as node
table columns.  All data after that column *may* be read in as node table
columns depending on the row headers.  If a GWEIGHT column is not found,
it is assumed that this is a general matrix file (see matrix file below).
If it is found, than all of the potential column headers are saved and the
app will procede to read in the next rows until it finds an "EWEIGHT" row,
which indicates the end of the row headers.  The next row contains the
first data row.  If the row GID is the same as one of the column names,
it is assumed that the data values represent edge weights.  Otherwise,
all of the columns are created based on the names after the GWEIGHT
column and the row data is assigned to those columns.
