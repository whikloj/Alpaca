# ![Alpaca](https://cloud.githubusercontent.com/assets/2371345/15409648/16c140b4-1dec-11e6-81d9-41929bc83b1f.png) Alpaca
[![Build Status](https://travis-ci.com/Islandora/Alpaca.svg?branch=master)](https://travis-ci.com/Islandora/Alpaca)
[![Contribution Guidelines](http://img.shields.io/badge/CONTRIBUTING-Guidelines-blue.svg)](./CONTRIBUTING.md)
[![LICENSE](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](./LICENSE)
[![codecov](https://codecov.io/gh/Islandora/Alpaca/branch/master/graph/badge.svg)](https://codecov.io/gh/Islandora/Alpaca)

## Introduction

Event-driven middleware based on [Apache Camel](http://camel.apache.org/) that synchronizes a Fedora repository with a Drupal instance.

## Requirements

This project requires Java 11 and can be built with [Gradle](https://gradle.org). To build and test locally, use `./gradlew build`.

The jar files are then available in each of the services `build/libs` directory.

For example, after running `./gradlew build` in the main Alpaca directory the islandora-indexing-triplestore jar file will be
available in the `islandora-indexing-triplestore/build/libs` directory.

## Configuration

Alpaca is made up of several services, each of these run as a separate executable jar file.

Each of these services takes an external file to configure its behaviour.
Each of these services has a system property it checks for the location of the configuration file.

Each service includes an `example.properties` file to show the minimal required settings. The
system property for each service is listed with the service below.

### islandora-indexing-fcrepo

This service manages a Drupal node into a corresponding Fedora resource.

It's example properties file is [islandora-indexing-fcrepo/example.properties](islandora-indexing-fcrepo/example.properties).

It's external configuration system property is `islandora-indexing-fcrepo.config`.

### islandora-indexing-triplestore

This service indexes the Drupal node into the configured triplestore

It's example properties file is [islandora-indexing-triplestore/example.properties](islandora-indexing-triplestore/example.properties).

It's external configuration system property is `islandora-indexing-triplestore.config`.

### islandora-connector-derivative

This service is used to configure an external microservice. You may (and probably will) want to run multiple
copies of jar. One for each microservice.

It's example properties file is [islandora-connector-derivative/example.properties](islandora-connector-derivative/example.properties).

It's external configuration system property is `islandora-connector-derivative.config`.

### Example starting command

To start the `islandora-indexing-fcrepo` service using an external properties file located at `/opt/my.properties`, I
would run:

```shell
java -Dislandora-indexing-fcrepo.config=file:/opt/my.properties -jar islandora-indexing-fcrepo-1.0.2.jar
```

## Documentation

Further documentation for this module is available on the [Islandora 8 documentation site](https://islandora.github.io/documentation/).

## Troubleshooting/Issues

Having problems or solved a problem? Check out the Islandora google groups for a solution.

* [Islandora Group](https://groups.google.com/forum/?hl=en&fromgroups#!forum/islandora) * [Islandora Dev Group](https://groups.google.com/forum/?hl=en&fromgroups#!forum/islandora-dev)

## Current Maintainers

* [Jared Whiklo](https://github.com/whikloj)

## Sponsors

* Discoverygarden
* LYRASIS
* York University
* McMaster University
* University of Prince Edward Island
* University of Manitoba
* University of Limerick
* Simon Fraser University
* PALS

## Development

If you would like to contribute, please get involved by attending our weekly [Tech Call](https://github.com/Islandora/documentation/wiki). We love to hear from you!

If you would like to contribute code to the project, you need to be covered by an Islandora Foundation [Contributor License Agreement](http://islandora.ca/sites/default/files/islandora_cla.pdf) or [Corporate Contributor License Agreement](http://islandora.ca/sites/default/files/islandora_ccla.pdf). Please see the [Contributors](http://islandora.ca/resources/contributors) pages on Islandora.ca for more information.

We recommend using the [islandora-playbook](https://github.com/Islandora-Devops/islandora-playbook) to get started.

## Licensing
[MIT](/License)
