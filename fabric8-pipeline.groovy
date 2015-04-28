buildPipelineView('fabric8-pipeline') {
  selectedJob('origin-schema-generator')
  title('Fabric8 Pipeline')
  showPipelineDefinitionHeader(true)
  showPipelineParameters(true)
  showPipelineParametersInHeaders(true)
  displayedBuilds(10)
  consoleOutputLinkStyle(OutputStyle.NewWindow)
}

mavenJob('origin-schema-generator') {
  using('base-config-build')
  scm {
    git {
      remote {
        github(
          'fabric8io/origin-schema-generator',
          'git'
        )
      }
      branch('master')
      clean(true)
      createTag(false)
      relativeTargetDir('src/github.com/fabric8io/origin-schema-generator')
    }
  }
  preBuildSteps {
    shell('''
      go get github.com/tools/godep
      cd src/github.com/fabric8io/origin-schema-generator
      godep go build ./cmd/generate/generate.go
      ./generate | python -m json.tool > kubernetes-model/src/main/resources/schema/kube-schema.json
    ''')
    maven {
      mavenInstallation('3.3.1')
      rootPOM('src/github.com/fabric8io/origin-schema-generator/pom.xml')
      goals('build-helper:parse-version')
      goals('versions:set')
      goals('-DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}')
    }
  }
  mavenInstallation('3.3.1')
  localRepository(LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
  rootPOM('src/github.com/fabric8io/origin-schema-generator/pom.xml')
  goals('clean deploy')
  goals('-DaltDeploymentRepository=local-nexus::default::${STAGING_REPO}')
  publishers {
    downstreamParameterized {
      trigger('fabric8', 'SUCCESS', true) {
        currentBuild()
        predefinedProp('KUBERNETES_MODEL_VERSION', '${POM_VERSION}')
      }
    }
  }
}

mavenJob('fabric8') {
  using('base-config-build')
  parameters {
    stringParam('KUBERNETES_MODEL_VERSION')
  }
  scm {
    git {
      remote {
        github(
          'fabric8io/fabric8',
          'git'
        )
      }
      branch('master')
      clean(true)
      createTag(false)
      cloneTimeout(30)
    }
  }
  preBuildSteps {
    shell('echo Using kubernetes-model ${KUBERNETES_MODEL_VERSION}')
    maven {
      mavenInstallation('3.3.1')
      goals('build-helper:parse-version')
      goals('versions:set')
      goals('-DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}')
    }
    maven {
      mavenInstallation('3.3.1')
      rootPOM('bom/pom.xml')
      goals('build-helper:parse-version')
      goals('versions:set')
      goals('-DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}')
    }
    shell('find * -name pom.xml | xargs perl -pi -e "s/<kubernetes-model.version>.+<\\/kubernetes-model.version>/<kubernetes-model.version>${KUBERNETES_MODEL_VERSION}<\\/kubernetes-model.version>/g"')
/*
    maven {
      mavenInstallation('3.3.1')
      goals('org.codehaus.mojo:versions-maven-plugin:2.1:update-property')
      goals('-DnewVersion=${KUBERNETES_MODEL_VERSION}')
      goals('-Dproperty=kubernetes-model.version')
    }
*/
  }
  mavenInstallation('3.3.1')
  localRepository(LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
  goals('clean deploy')
  goals('-DaltDeploymentRepository=local-nexus::default::${STAGING_REPO}')
  publishers {
    downstreamParameterized {
      trigger('quickstarts', 'UNSTABLE_OR_BETTER', true) {
        currentBuild()
        predefinedProp('FABRIC8_VERSION', '${POM_VERSION}')
      }
    }
  }
}

mavenJob('quickstarts') {
  using('base-config-build')
  parameters {
    stringParam('KUBERNETES_MODEL_VERSION')
    stringParam('FABRIC8_VERSION')
  }
  scm {
    git {
      remote {
        github(
          'fabric8io/quickstarts',
          'git'
        )
      }
      branch('master')
      clean(true)
      createTag(false)
      cloneTimeout(30)
    }
  }
  preBuildSteps {
    shell('echo Using kubernetes-model ${KUBERNETES_MODEL_VERSION}')
    shell('echo Using fabric8 ${FABRIC8_VERSION}')
    maven {
      mavenInstallation('3.3.1')
      goals('build-helper:parse-version')
      goals('versions:set')
      goals('-DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}')
    }
    shell('find * -name pom.xml | xargs perl -pi -e "s/<kubernetes-model.version>.+<\\/kubernetes-model.version>/<kubernetes-model.version>${KUBERNETES_MODEL_VERSION}<\\/kubernetes-model.version>/g"')
    shell('find * -name pom.xml | xargs perl -pi -e "s/<fabric8.version>.+<\\/fabric8.version>/<fabric8.version>${FABRIC8_VERSION}<\\/fabric8.version>/g"')
    shell('find * -name pom.xml | xargs perl -pi -e "s/<fabric8.release.version>.+<\\/fabric8.release.version>/<fabric8.release.version>${FABRIC8_VERSION}<\\/fabric8.release.version>/g"')
/*
    maven {
      shell('echo Using fabric8 ${FABRIC8_VERSION}')
      mavenInstallation('3.3.1')
      goals('org.codehaus.mojo:versions-maven-plugin:2.1:update-property')
      goals('-DnewVersion=${KUBERNETES_MODEL_VERSION}')
      goals('-Dproperty=kubernetes-model.version')
    }
    maven {
      mavenInstallation('3.3.1')
      goals('org.codehaus.mojo:versions-maven-plugin:2.1:update-property')
      goals('-DnewVersion=${FABRIC8_VERSION}')
      goals('-Dproperty=fabric8.version')
    }
    maven {
      shell('echo Using fabric8 ${FABRIC8_VERSION}')
      mavenInstallation('3.3.1')
      goals('org.codehaus.mojo:versions-maven-plugin:2.1:update-property')
      goals('-DnewVersion=${FABRIC8_VERSION}')
      goals('-Dproperty=fabric8.release.version')
    }
*/
  }
  mavenInstallation('3.3.1')
  localRepository(LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
  goals('clean deploy')
  goals('-DaltDeploymentRepository=local-nexus::default::${STAGING_REPO}')
}
