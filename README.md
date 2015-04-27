# Fabric8 pipeline DSL for continuous delivery

## Required Jenkins plugins

* Build Pipeline Plugin
* build timeout plugin
* Copy Artifact Plugin
* Delivery Pipeline Plugin
* Environment Injector Plugin
* GitHub plugin
* GitHub Pull Request Builder
* Go Plugin
* Groovy Postbuild
* Job Configuration History Plugin
* Job DSL
* Parameterized Trigger plugin
* promoted builds plugin
* Timestamper

## Steps to set up

1. Install plugins above
2. Add JDK installation called `java8`
3. Add Groovy installation called `2.4.3`
4. Add Maven installation called `3.3.1`
5. Add golang installation for `1.4.2`

## Create jobs

1. Create `base-template` freestyle build with one step `Process Job DSLs` with contents of `base-template.groovy`
2. Create `pipeline-template` freestyle build with one step `Process Job DSLs` with contents of `fabric8-pipeline.groovy`
3. Run `base-template` build & all other builds & views should be generated

## Run jobs
1. Run `origin-schema-generator` build & all should follow

Although it's broken right now...
