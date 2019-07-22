# OpenMRS Fuzzer

The OpenMRS Fuzzer is a program that logs into OpenMRS and systematically visits every page it can find, enters all the data in all the places it can, and then submits that data for every submit button discovered.

For information about the Fuzzer and how to use it, continue reading this README. For other information, see the following:

- [Phosphor Modification Documentation](./documentation/Phosphor%20Modification%20Documentation.md)
- [Fuzzer Results Explanation](./documentation/Fuzzer%20Results%20Explanation.md)
- [Reproducing Our Results](./documentation/Fuzzer%20Results%20Reproduction%20Instructions.md)


## Overview

### Purpose

The purpose of the OpenMRS Fuzzer is twofold. First and foremost, the Fuzzer attempts to submit as much data to the OpenMRS web server as it can in order to demonstrate the implementation of taint-tracking software that enables administrators to mark sources, sanitizers, and sinks that tainted data could pass through. After running the Fuzzer, the data that actually makes it any marked sanitizers and sinks can be observed.

Second, the Fuzzer generates reports regarding the time it takes to complete the actions it takes. This includes both form submit times as well as page loading times when traversing \<a\> tags. These reports are useful to compare the overhead imposed by the taint-tracking software.

Fortuitously, the Fuzzer also works as a nice way of reproducing data obtained in our experiment.

### How it works

The OpenMRS Fuzzer works by using a browser automation package called Selenium. The Fuzzer starts by logging in to OpenMRS. It then looks for and maintains a collection of all the \<a\> tags on the page. The same search is performed on tags that can be altered by the user, such as input tags and checkboxes. This is done so that the Fuzzer can enter arbitrary data to the page everywhere it can. 

The important data fields are likely that of text boxes, since very strict validation can be performed on other inputs such as checkboxes and radio buttons. However, the Fuzzer will interact with fields other than text boxes since this is often necessary in order for validation to pass on the submitted form (and thus have the data be sent to any potential database query). 

There are some cases where the form does not pass validation, despite the Fuzzer entering data and interacting with all of the form's elements. In this case, the Fuzzer prompts the user to change what's necessary on the form in order for it to be submitted. This is due to validation checks such as proper date formatting or requiring the name of a previously created person. The user can then tell the Fuzzer to repeat the form submission with the data entered, or skip the form submission if even the user is unable to pass validation (which could happen, for example, if the form expects some data that needs to be entered on another form that has not been visited yet).

After a page is checked for \<a\> tags, populated with arbitrary data, and all the forms have been submitted, these same steps are taken recursively on one of the linked pages that has not yet been visited. This continues until all the page's links have been exhausted. 

### Relation to OpenMRS

There are several instances where this Fuzzer relies on the fuzzed page to be OpenMRS specifically. For example, when determining if a form has passed validation the HTML is searched for specific class names. These class names would not be used in other web apps, and therefore, for reasons such as this one, this Fuzzer is meant only to fuzz OpenMRS.


## Usage

The Fuzzer is used in the following manner.

First, ensure Python is installed. The Fuzzer was written for and tested in Python 3. Also, install Selenium via `pip3 install selenium`.

Then, clone or download the source code from this repository. Import the Fuzzer class from Fuzzer, and create a new Fuzzer class. The first argument is the output spreadsheet of timing data for the form submits and the second argument is the output spreadsheet of timing data for the page loads. Next, call `fuzz()` on the Fuzzer object and pass it the root URL of OpenMRS. This will likely be: `http://localhost:8080/openmrs/`.

Then, for the most part, the Fuzzer will run on its own, traversing pages and submitting data. When the Fuzzer submits a form that does not pass validation, it will stop activity and prompt you to submit the form again, or continue to the next form. If you wish to resubmit the form, fill out the form so that it will pass validation and then enter `s` into the Fuzzer. If you wish to skip, enter `c` into the Fuzzer. You may attempt to pass validation on the same form as many times as you want.

When the Fuzzer has exhausted all of the links it has seen from all of the pages, it will end.
