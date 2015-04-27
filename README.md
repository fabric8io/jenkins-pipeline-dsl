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

1. Create a 'seed' freestyle build
2. configure the Git repository to be https://github.com/fabric8io/jenkins-pipeline-dsl.git and to trigger it whenever there is a push to git
3. Build -> Add Build Step -> Process Job DSLs 
  1. Select Look on Filesystem
  2. USe **/*.groovy for the DSL scripts
4. Run `seed` build & all other builds & views should be generated

## Run jobs

1. Run `origin-schema-generator` build & all should follow

Although it's broken right now...



