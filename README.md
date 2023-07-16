# Lossless-Compression

This project is part of **CSE471: Multimidea And Information Theory** which aims at implmenation and compare different lossless compression (Huffman, LZW, Arithmetic).

To run the algorithms, go to the main class and insert the path of the file to be tested.

## Huffman Algorithm

The general idea for Huffman’s algorithm is to replace original text
with binary text by using short binary code for frequently used N
characters and longer codes for less frequent ones, where N is the
number of characters to consider as a unit. Larger N usually leads to
faster compression/decompression but results in larger sized
compressed files.

We begin by counting the frequency of each unit and use them to
create a tree by repeatedly combining the least two frequent units
into a new tree node, then placing the new node instead of these two
units and repeating the operation until we create a binary Huffman
Tree. Whereas the further we go down the tree this means that this
unit is less frequently used so to represent it we would need more
bits than units that are further up in the tree. To get the binary code for a given unit we traverse the tree and each time we go to the left node we add a 0 to the code and each time we
go right we add a 1 to the code.

Compression operation begins by writing the metadata in the
beginning which includes the Huffman Tree created and any vital
information for the decompression to be possible then we add the
binary codes for each unit.

Decompression operation begins by reading the metadata for
compressed file which includes the Huffman Tree used for
compression, then reading byte by byte from the binary code
representation of the characters, then using the Huffman Tree we
can find out which code resembles which unit so that we can
reconstruct the original file.

### Main Functions:

* **generateFreqTable**: generates the frequency table by counting each instance of N size units.
* **generateNodes**: generates a list of nodes using the frequency table where each node contains the unit value and its frequency.
* **generateTree**: generates the Huffman Tree by getting the least two frequent nodes then creating a new node to be their parent node repeatedly until we create the tree.
* **generateCode**: gets the code for each unit and creates a map out of these values where the key is the unit and the binary code is the value.
* **getFirstLine**: generates the first line of metadata to be added to the compressed file by adding the N then the number of units then the size of the last unit if the total size of the file wasn’t divisible by N.
* **getBfsTree**: generates the text representation in form of BFS for the Huffman Tree to be added as metadata in the compressed file to be used in the decompression.
* **writeCompressed**: write the compressed file by firstly adding the metadata using getFirstLine and getBfsTree then secondly begins writing the binary code for each unit in the input file as generated by the Huffman Tree.
* **insertNode**: used in the decompression to recreate the Huffman Tree by inserting nodes retrieved from the metadata in the compressed file into a tree.
* **generateCodeReversed**: which is similar to generateCode mentioned above but instead of creating a map whose key and value are the unit and code respectively, we use the code as key and unit as value as in decompression we want to find out what unit does each code represent.
* **compress**: calls and combines the outputs of functions mentioned above and calculates the time and compression rate.
* **decompress**: calls and combines the outputs of functions mentioned above and calculates the time of decompression.

## LZW Algorithm

The general idea behind LZW is to replace a sequence of characters
in a message with a shorter code that represents that sequence.
This reduces the overall size of the message and makes it easier to
transmit or store.

The LZW algorithm works by building a dictionary of all possible
sequences of characters that appear in the message. It starts with a
dictionary that contains all single characters as entries, and then
adds entries for longer sequences as it encounters them in the
message. Each entry in the dictionary is assigned a unique code that
is used to represent that sequence in the compressed message.
As the algorithm scans the message, it looks for the longest
sequence of characters that has already been added to the
dictionary. When it finds such a sequence, it replaces it with a code
that represents that sequence, and then adds a new entry to the
dictionary for the next sequence of characters that hasn't been seen
before.

So LZW compression is very effective when there are long
duplicated sequences in the text as all these sequences are replaced
by an integer code so it’s used in compressing text data, also and it
has been widely used in many different applications, including image
and audio compression, as well as in various file formats such as
GIF and TIFF.

### Main Functions:

* **compress**: : LZW compression is done using the following steps:
    1) Initialize the dictionary with all possible single bytes.
    2)  Read in the input message.
    3)  While there are still bytes in the input message:
        1) Find the longest sequence of bytes in the dictionary that matches the current input.
        2) Output the code for that sequence.
        3) Add a new entry to the dictionary for the sequence of characters that includes the current input.
    4)  Output the end-of-file marker.
* **decompress**:  LZW decompression is done using the following steps:
    1)  Initialize the dictionary with all possible single bytes.
    2)  Read in the compressed message.
    3)   Initialize a variable to hold the previous code (set to the first code in the compressed message).
    4)   Output the corresponding bytes for the first code.
    5)  While there are still bytes in the compressed message:
        1) Read in the next code.
        2) If the code is not in the dictionary:
            1) Add an entry to the dictionary for the previous code followed by the first character of the previous code.
            2) Output the corresponding character(s) for the previous code followed by the first character of the previous code.
            3) Set the previous code to the current code.
        4) If the code is in the dictionary:
            1) Output the corresponding character(s) for the current code.
            2) Add an entry to the dictionary for the previous code followed by the corresponding character(s) for the current code.
            3)  Set the previous code to the current code.
    6)  Output the end-of-file marker.

##  Arithmetic Encoding Algorithm

Arithmetic encoding is a data compression technique that
represents a message or a data stream as a single real number in
the interval [0,1]. The idea behind arithmetic encoding is to assign a
probability range to each symbol in the message, and then encode
the message as a single number that falls within the probability
range of the entire message.

During the encoding process, the interval [0,1] is divided into
sub-intervals, with each sub-interval corresponding to a symbol in
the message. The size of each subinterval is proportional to the
probability of the corresponding symbol. The entire message is then
represented by the sub-interval that corresponds to the entire
message.

Decoding the compressed message involves dividing the interval
[0,1] into sub-intervals that correspond to the individual symbols in
the message, and then determining which sub-interval contains the
encoded number. The symbol corresponding to that sub-interval is
then outputted, and the process is repeated until the entire message
has been decoded.

The basic idea of arithmetic encoding is to represent a message as
a single real number in the interval [0,1]. To do this, the message is
first mapped to a sequence of probabilities, where each symbol in
the message is assigned a probability based on its frequency of
occurrence.

Next, we use these probabilities to divide the interval [0,1] into
sub-intervals, where the size of each sub-interval corresponds to the
probability of the symbol it represents. Specifically, we start with the
interval [0,1], and then divide it into sub-intervals.
The final encoded number falls within the interval that represents the
entire message. To decode the message, we start with the original
interval [0,1], and successively narrow it down to the sub-interval that
contains the encoded number. We then output the symbol
represented by that sub-interval, and repeat the process with the
remaining portion of the interval until we have decoded the entire
message.

### Main Functions & Classes:

* **Compress**: Takes the file path to be compressed and writes the bits in the compressed file.
* **Decompress**: Takes as input the file to be decompressed and decompresses the given file till all the symbols are decompressed.
* **getFrequencies**: Counts all the frequencies of the symbols of the given file path. Used in compressing.
* **readFrequencies**: Reads the encoded frequency table in the header of the file when decompressing.
* **writeFrequencies**: writes the frequency table in the compressed file header when compressing the file.
* **ArithmeticEncoder**: Implementation of the arithmetic encoder.
* **ArithmeticEncoder.update**: updates the ArithmeticEncoder’s state.
* **ArithmeticDecoder**: Implementation of the arithmetic decoder.
* **ArithmeticDecoder.update**: updates the ArithmeticDecoder’s state.
* **ArithmeticDecoder.nextSymbol**: Gets the next symbol of the compressed text.
* **FrequencyTable**: Represents the probabilities' table which holds the cumulative probability and the occurrences of each character. It contains functions which get the occurrences and the high & low values for each symbol.
* **BitOutputStream**: Used to write bits using output stream.
* **BitInputStream**: Used to read bits given InputStream.


## Evaluations

### Size Evaluation 
Compression ratios for the three algorithms:

| **File Size** | **Huffman** | **LZW** | **Arithmetic** |
|-----------|---------|-----|------------|
| 10 KB (Lorem Ipsum) | 1.8841 | 0.43083 | 1.6295 |
| 5 MB (same character) | 7.9846 | 66.812 | 341.19 |
| 250 MB (Lorem Ipsum) | 1.9294 | 11.829 | 1.9484 |
| 500 MB (Lorem Ipsum) | 1.9153 | 16.403 | 1.9371 |

### Time Evaluation:

*  10KB File (Lorem Ipsum):

| **Algorithm** | **Compression Time** | **Decompression Time** | **Compression Ratio** |
|-----------|-----------------|--------------------|-------------------|
| **Huffman** | 42 ms | 31 ms | 1.8841 |
| **LZW** | 46 ms | 63 ms | 0.43083 |
| **Arithmetic** | 15 ms | 31 ms | 1.6295 |

*  5MB File of the same character:

| **Algorithm** | **Compression Time** | **Decompression Time** | **Compression Ratio** |
|-----------|-----------------|--------------------|-------------------|
| **Huffman** | 717 ms | 360 ms | 7.9846 |
| **LZW** | 2,134 ms | 93 ms | 66.812 |
| **Arithmetic** | 223 ms | 234 ms | 341.19 |

*  250MB File (Lorem Ipsum):

| **Algorithm** | **Compression Time** | **Decompression Time** | **Compression Ratio** |
|-----------|-----------------|--------------------|-------------------|
| **Huffman** | 37,101 ms | 39,854 ms | 1.9294 |
| **LZW** | 88,150 ms | 23,640 ms | 11.829 |
| **Arithmetic** | 20,140 ms | 23,321 ms | 1.9484 |

*  500MB File (Lorem Ipsum):

| **Algorithm** | **Compression Time** | **Decompression Time** | **Compression Ratio** |
|-----------|-----------------|--------------------|-------------------|
| **Huffman** | 74,324 ms | 76,663 ms | 1.9153 |
| **LZW** | 189,720 ms | 35,893 ms | 16.403 |
| **Arithmetic** | 38,627 ms | 45,745 ms | 1.9371 |
