# Fuzzer Results Explanation

The following is a description of the results obtained by running the Fuzzer program on an OpenMRS instance rewritten using the modified Phosphor program. For a description of the steps taken to produce this data, see the [OpenMRS Fuzzer Results Reproduction Instructions](./OpenMRS Fuzzer Results Reproduction Instructions.md) document.

## Overview

The following steps were taken to produce the results. First, OpenMRS was instrumented using our modified Phosphor and run in Tomcat. Demo data was then added to the database to create more opportunities for submitting data (such as searches). The Fuzzer was then run on the instrumented OpenMRS, submitting data wherever possible. Submits were timed by the Fuzzer, as well as page traversals and written to a spreadsheet. The Fuzzer was then run once more on the instrumented OpenMRS before clearing the database and running the Fuzzer on an uninstrumented OpenMRS. The goal with these tests was ultimately to compare the difference between OpenMRS unmodified, and OpenMRS instrumented with Phosphor.


## Fuzzer Tests
The reasoning for these runs specifically are as follows. Given the goal of comparing OpenMRS unmodified and OpenMRS instrumented with Phosphor, the reasoning for the first and third runs are self-evident. However, a second test run was performed on the instrumented OpenMRS due to how Phosphor instruments source code.

When instrumenting Java classes with Phosphor, one can either run the software manually, specifying all of the source files to the program; or, Phosphor can automatically detect and instrument uninstrumented code as the program runs. The latter option was chosen for this project due to the nature of OpenMRS. By design, the functionality of OpenMRS is split into many modules, meaning the Java classes are split up into many difference places, making manual pre-instrumentation less practical. This dynamic instrumentation process is noted on [Phosphor's Github README](https://github.com/gmu-swe/phosphor):

> Note: It is not 100% necessary to instrument your application/library code in advance - the javaagent will detect any uninstrumented class files as they are being loaded into the JVM and instrument them as necessary.

As a consequence of instrumenting Java classes dynamically, an instrumentation overhead occurs as new uninstrumented source code is discovered. Thus, it was predicted that the first run through a Phosphor-modified OpenMRS would be slower than consecutive runs, which was indeed observed by further testing. As such, two instrumented OpenMRS Fuzzer results were made: one after a fresh launch of Tomcat, and the other directly following the resolution of the first without terminating the server, in order to keep the instrumented code in memory.


## Fuzzer Data Analysis

### Action Data

The first category obtained from running the Fuzzer is the action duration data. This data comes from submitting a form containing arbitrary data and logging how long it takes for the subsequent page is loaded, indicating that the data was processed and potentially sent through a sanitizer and/or a sink.

After visiting every page the Fuzzer could find, around 50 forms were submitted and logged for each run. Slight differences for specific forms submitted exist between runs, however. Around 5 forms submitted are different between the runs. This likely occurs due to a couple of reasons.

First, the Fuzzer does not traverse the web app exactly the same each time. This is mainly due to variations in user input, as the user is prompted to fill out a form whenever the Fuzzer is not able to pass validation automatically. As such, it is possible that user submitted data changed the course of the web app traversal, slightly altering which forms were submitted. 

Second, differences between the uninitialized Phosphor run and the pre-initialized Phosphor run can be explained by the fact that the database was not reset between runs. This could impact the consistency of the traversal due to differences in the information/possible actions presented to the user based on more data existing in the database. The data was not reset between these runs because the dynamically instrumented source code needed to be kept in memory, and OpenMRS would have needed to be restarted after reseting its database.

In any case, the three runs of the Fuzzer submitted data to almost entirely the same forms so the durations logged can be reliably compared. The presented data was obtained by combining the results of the three runs and ordering them by the duration of the uninitialized Phosphor run. As predicted, the average duration for submits during the uninitialized Phosphor run (1.4761) was higher than the pre-initialized Phosphor run (0.9560). Additionally, both Phosphor runs averaged longer than the unmodified run (0.3344), explained by the overhead incurred by tracking tainted variables.

One unexpected result did occur, however, in that while the average of submits for the uninitialized Phosphor run decreased on the subsequent run, a few individual submits took longer in the pre-initialized run than the exact same submit in the uninitialized run. This may be explained by the fact that on the second run, more data existed in the database, and thus, more data may have been requested by the browser, resulting in an increase in execution duration.


### Sunken Data

While the Fuzzer was logging data on an instrumented OpenMRS run, the Phosphor log file (implemented in our modified Phosphor) was monitored. If the Fuzzer submitted data that was sunken at one of the marked sinks, it was recorded. Forms from which data made its way to a sink are marked in the Sunken column in the OpenMRS Actions table. A total of 6 out of ~50 forms submitted data that went through a marked sanitizer and a marked sink. The passing through of a sanitizer was not labeled as it was never the case, in these runs, that any tainted data passed into a sink that had not already passed through one of the marked sanitizers. For reasons discussed in the [Phosphor Modification Document](/TODO), only a subset of submitted data passed into any sinks due to persistent data functions not being marked as sinks.


### Page Load Data

While the focus of this analysis pertains to the results obtained from submitting data to OpenMRS, the overhead occurred by tracking tainted data can also be observed in the time it takes to traverse between different pages, not just when traversing from a submit button. As such, the Fuzzer was implemented to additionally record the time it takes to navigate between pages. These page loading results and the averages/ratios are thus presented for each of the three runs.

For reasons similar to the differences in forms submitted for the Actions data, it can be observed that the Fuzzer took slightly different routes through the web app between runs. The majority of pages visited, however, are the same, and thus comparing averages is reasonable.

Similar to the results obtained from the submitted form data, there was around a 4.6x slowdown in the uninitialized OpenMRS run compared to an unmodified OpenMRS run. However, a more realistic subsequent run had a slowdown of around 2.4x. 












