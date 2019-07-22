# Phosphor Modification Documentation

The following is a description of all the modifications made to Phosphor in order to obtain the desired functionality:

- Ability to mark functions as sources, sinks, and additionally, sanitizers that will partially endorse tainted data that passes through it.
- Introduction of 'maybe tainted' data (rather than simply 'tainted' data) that is partially trusted due to its passing through of a marked sanitizer function.
- Ability to write tainted and maybe tainted data that reaches a marked sink to a log file.

The modified fork of Phosphor can be found on [this Github.](https://github.com/uvm-plaid/phosphor-mod) The entire list of source code changes can be found [here.](https://github.com/uvm-plaid/phosphor-mod/compare/80b78c848aa5220db1e121f4d15ac3884064c4e3..master)


## Introduction of Taint Level

Originally, Phosphor distinguished between two types of data: tainted and untainted. Some data was tainted if a Taint object was placed upon the object (in the form an additional object parameter in the case that the data was an object, or in the form of additional method arguments in the case the data was a primitive). Some data was untainted if no such Taint object was placed upon the data.

To accommodate the requirement of 'maybe tainted' data, this system was altered to allow a Taint object to have one of many different taint levels. In this design, taint levels of 'not tainted', 'maybe tainted', and 'tainted' were used (in that linear order), though there is theoretically no limit to the number of levels or complexity of ordering with this implementation.

As per the functional requirements listed, OpenMRS functions were to be marked as either sources, sinks, or sanitizers. As such, the introduction of a 'tainted' taint level could only come from a marked source. To accommodate this, an additional taint level of 'unknown' was introduced and is given to every newly instantiated Taint object. And then, at every source, the taint level is set to 'tainted'. This ensures that only tainted data from marked sources reach sinks, and also ensures that taint level data is propagating properly.

### Lattice Implementation

The taint level was implemented in Phosphor with a lattice data structure, itself implemented using graphs. A lattice works nicely with the specified taint levels, since the greatest lower bound (GLB) and the least upper bound (LUB) can be used in the propagation and checking of taints. A lattice is also beneficial in that it allows more taint levels to be easily added and ordered with arbitrary complexity.

The lattice is used by adding the desired lattice elements and then adding orderings between the elements. In the case of the taint levels, all four are added and then the ordering between each are added to create a linear relationship between the four. The GLB and LUB can then be calculated on a collection of lattice elements. For taint levels, the LUB of a two given taints is used to propagate the taint level. The GLB of a given taint level and 'maybe tainted' is used in sanitizers to indicate that the data is partially endorsed.

### Runtime Complexity

The runtime complexity for each of the lattice methods are as follows.

Method             | Complexity
------------------ | -------------
addElement         | O(1)
addOrdering        | O(1)
leastUpperBound    | O(N * M^2) *
greatestLowerBound | O(N * M^2) *
compareElements    | O(M^2)

For N = the number of elements passed to the function, and M = the number of elements in the lattice.

\* For our purposes, a maximum of two elements are ever LUBed or GLBed, reducing the O(N * M^2) complexity to simply O(M^2).

If the number of lattice elements and orderings were to grow significantly large, further optimizations to the lattice methods may be desired. One way to optimize these would be to calculate all of the possible LUB and GLB results ahead of time, and look them up in constant time. This is especially practical in this situation due to only needing LUB and GLB results between pairs of elements, meaning the space complexity would be O(N^2) rather than O(N^N). For a lattice of four elements, however, the overhead imposed by lattice operations was not enough to worry about.


## Sources, Sinks, and Sanitizers

The list of sources, sinks, and sanitizers can be found on the [the Phosphor Modification Github.](https://github.com/uvm-plaid/phosphor-mod)

On the backend, OpenMRS uses a library called [Hibernate](https://en.wikipedia.org/wiki/Hibernate_(framework)) to save and search for data in a database. There are two ways of interacting with a database in Hibernate: persistent relationally mapped object saving/loading, and queries.

For persistent data, it seems that the only potential vulnerability would be an XSS attack, as no HTML encoding is performed by Hibernate itself. However, it was determined that OpenMRS does its HTML encoding after data is retrieved from the database, not before it is entered, and as such, is not compatible with this method of taint tracking as the taint would not be able to propagate  through a database. As such, the sanitizers and sinks found relate to database querying.

While Hibernate provides developers with sanitization functions for query executes, it is still possible for tainted data to be executed in a query if the sanitization function is misused or not used at all. Hibernate's sanitization methods work by using a keyword in the query string and swapping out the keyword with a variable before the query is executed. The sanitization is performed when the variable is swapped with the keyword. However, if the developer were to simply append a tainted variable onto their query string without using the keyword and swapping, they would then sending potentially malicious data into a query execute call.

As such, all of the query execute functions were found and marked as sinks, and all of the functions that allow developers to swap their variables with keywords were marked as sanitizers.

In this case, however, the arguments for the sinks were not the tainted variables themselves, but rather an object containing tainted member variables. Therefore, Phosphor was modified to not only check the arguments for a taint, but also that of the arguments member variables. This change allowed for Phosphor to detect tainted data when it passed through a sink and log it successfully.

The list of source functions are all the functions that pass data to hibernate from a web request, such as getQueryString or getAttribute/getValue (referring to values from a POST request). 


## Logging

Finally, Phosphor was modified to optionally output some of the information it obtains to a log file. The information included in the log is as follows:

- Information regarding found sources/sinks/sanitizers to ensure the desired functions are being tracked.
- The function name of any sink that had a tainted variable pass through it.
- The taint level of any sunken tainted variable.
- The value of sunken variables.
- A stack trace of the thread when a tainted variable was sunk.

Much of this information was already being collected in the unmodified Phosphor, so much of the modification was simply outputting it to a file rather than standard output when it crashed due to a tainted variable flowing to a sink.

The information written to this log file was intended to be useful to web administrators and developers looking to find potential vulnerabilities in their systems.


