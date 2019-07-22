# OpenMRS Fuzzer Results Reproduction Instructions

The following are the steps needed to reproduce the results obtained from running the OpenMRS fuzzer with the modified Phosphor.

### Computer Information

Specifications of the computer used to run the tests:

```
Model Name: MacBook Pro
Model Identifier: MacBookPro11,3
Processor Name: Intel Core i7
Processor Speed: 2.6 GHz
Number of Processors: 1
Total Number of Cores: 4
L2 Cache (per Core): 256 KB
L3 Cache: 6 MB
Memory: 16 GB
```

### Java
- Download and install java.
  - The following version was used to produce the results:

	```
	openjdk version "1.8.0_212"
	OpenJDK Runtime Environment (AdoptOpenJDK)(build 1.8.0_212-b03)
	OpenJDK 64-Bit Server VM (AdoptOpenJDK)(build 25.212-b03, mixed mode)
	```
- ensure the proper version of java is active (via `export JAVA_HOME=/path/to/java/home`)


### Tomcat
- Download/install Apache Tomcat.
  - Version 9.0.20 was used to produce the results.


### Modified Phosphor
- Download/clone the modified Phosphor from the [github repository.](https://github.com/uvm-plaid/phosphor-mod)
- cd to the cloned folder and then cd to `Phosphor`
- Run `maven clean package`
- Run `/path/to/java -jar Phosphor-0.0.4-SNAPSHOT.jar /path/to/jre target/jre-inst-mod`
- Set the instrumented jre to the active java via `export JAVA_HOME=/path/to/jre-inst-mod`
- Also set the jre home via `export JRE_HOME=/path/to/jre-inst-mod`


### MySQL
- Install mysql
- Set and remember the password of the user `root` 


### OpenMRS
- Download OpenMRS from [their github](https://openmrs.org/download/) or [their website.](https://openmrs.org/download/) 
  - Commit `33f4e7aad` was used to produce the results.
- Create the .war file by running `mvn clean package`
- Place the .war file in the tomcat `webapps` directory.
- Download the collection of Reference Application 2.9.0 Addons (or the version corresponding to the .war downloaded) from the [OpenMRS website](https://openmrs.org/download/)
- Place the desired modules in ~/.OpenMRS/modules
  - The following modules were used to produce the results:

	```
	addresshierarchy-2.11.0.omod
	adminui-1.2.4.omod
	allergyui-1.8.1.omod
	appframework-2.13.0.omod
	appointmentscheduling-1.10.0.omod
	appointmentschedulingui-1.7.0.omod
	appui-1.9.0.omod
	atlas-2.2.omod
	attachments-2.1.0.omod
	calculation-1.2.omod
	chartsearch-2.1.0.omod
	coreapps-1.21.0.omod
	dataexchange-1.3.3.omod
	emrapi-1.27.0.omod
	event-2.7.0.omod
	fhir-1.18.0.omod
	formentryapp-1.4.2.omod
	htmlformentry-3.8.0.omod
	htmlformentryui-1.7.0.omod
	htmlwidgets-1.10.0.omod
	idgen-4.5.0.omod
	legacyui-1.5.0.omod
	metadatadeploy-1.11.0.omod
	metadatamapping-1.3.4.omod
	metadatasharing-1.5.0.omod
	owa-1.10.0.omod
	providermanagement-2.10.0.omod
	referenceapplication-2.9.0.omod
	referencedemodata-1.4.4.omod
	referencemetadata-2.9.0.omod
	registrationapp-1.13.0.omod
	registrationcore-1.8.0.omod
	reporting-1.17.0.omod
	reportingcompatibility-2.0.6.omod
	reportingrest-1.10.0.omod
	reportingui-1.6.0.omod
	serialization.xstream-0.2.14.omod
	uicommons-2.6.1.omod
	uiframework-3.15.0.omod
	uilibrary-2.0.6.omod
	```
- Export the following environment variable:
  
  ```
  export JAVA_OPTS="-Xbootclasspath/a:/path/to/Phosphor-0.0.4-SNAPSHOT.jar -javaagent:/path/to/Phosphor-0.0.4-SNAPSHOT.jar=taintSources=/path/to/openmrs-sources.txt,taintSinks=/path/to/openmrs-sinks.txt,taintSanitizers=/path/to/openmrs-sanitizers.txt,logPath=/path/to/log.txt"
  ```
  Where log.txt is desired text file to write output to.
- Then, start tomcat via `catalina run`.
- Navigate to localhost:8080/openmrs/ in a browser.
- Enter the requested information to complete the OpenMRS setup. Use the 'Simple' setup and check the option that adds demo data to the database.



### OpenMRS Fuzzer
- Download/clone the fuzzer from the [github repository.](https://github.com/uvm-plaid/openmrs-fuzzer)
- Install selenium for python (via `pip/pip3 install selenium`)
- Run the fuzzer via `python/python3 Fuzzer.py`

The results were produced by running the Fuzzer twice with OpenMRS re-written with the modified Phosphor (once uninitialized and once after the Fuzzer had gone through all the pages), and once with OpenMRS running without any modifications to the JVM. To maintain consistency of data, the database was cleared before running the Fuzzer again with the unmodified OpenMRS. To clear the database, do the following:

- Login to mysql with the root user
- Execute `drop database openmrs;`
- Delete `openmrs-runtime.properties` from `~/.Openmrs`
- Run OpenMRS and regenerate the demo data