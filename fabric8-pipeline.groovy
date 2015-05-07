buildPipelineView('fabric8-pipeline') {
  selectedJob('origin-schema-generator')
  title('Fabric8 Pipeline')
  showPipelineDefinitionHeader(true)
  showPipelineParameters(true)
  showPipelineParametersInHeaders(true)
  displayedBuilds(10)
  consoleOutputLinkStyle(OutputStyle.NewWindow)
}

buildMonitorView('fabric8 CD') {
 description('All jobs for the fabric8 CD pipeline')
 jobs {
     name('origin-schema-generator')
     name('fabric8')
     name('quickstarts-apps')
     name('quickstarts-quickstarts')
     name('fabric8-deploy')
 }
}

mavenJob('origin-schema-generator') {
  using('base-maven-build')
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
  rootPOM('src/github.com/fabric8io/origin-schema-generator/pom.xml')
  goals('clean deploy')
  goals('-DaltDeploymentRepository=local-nexus::default::${STAGING_REPO}')
  goals('-Prelease')
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
  using('base-maven-build')
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
  }
  goals('clean deploy')
  goals('-DaltDeploymentRepository=local-nexus::default::${STAGING_REPO}')
  publishers {
    downstreamParameterized {
      trigger('fabric8-apps', 'UNSTABLE_OR_BETTER', true) {
        currentBuild()
        predefinedProp('FABRIC8_VERSION', '${POM_VERSION}')
      }
    }
  }
}

mavenJob('fabric8-apps') {
  using('base-maven-build')
  wrappers {
    timeout {
      elastic(
              400, // Build will timeout when it take 3 time longer than the reference build duration, default = 150
              5,   // Number of builds to consider for average calculation
              120   // 120 minutes default timeout (no successful builds available as reference)
      )
      failBuild()
    }
  }
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
    shell('find * -name pom.xml | xargs perl -pi -e "s/<kubernetes-model.version>.+<\\/kubernetes-model.version>/<kubernetes-model.version>${KUBERNETES_MODEL_VERSION}<\\/kubernetes-model.version>/g"')
    shell('find * -name pom.xml | xargs perl -pi -e "s/<fabric8.version>.+<\\/fabric8.version>/<fabric8.version>${FABRIC8_VERSION}<\\/fabric8.version>/g"')
    shell('find * -name pom.xml | xargs perl -pi -e "s/<fabric8.release.version>.+<\\/fabric8.release.version>/<fabric8.release.version>${FABRIC8_VERSION}<\\/fabric8.release.version>/g"')
    maven {
      mavenInstallation('3.3.1')
      goals('build-helper:parse-version')
      goals('versions:set')
      goals('-DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}')
    }
  }
  goals('clean deploy')
  goals('-DaltDeploymentRepository=local-nexus::default::${STAGING_REPO}')
  goals('-Ddocker.registryPrefix=registry.os1.fabric8.io/')
  // This works around this bug of the settings not being found by the furnace maven plugin: https://issues.jboss.org/browse/FURNACE-45
  goals('-settings /var/jenkins_home/.m2/settings.xml')
  // TODO can't push yet until we figure out authentication on the local registry...
  // goals('-Pjube,docker-push')
  goals('-Pcanary,apps,jube,docker-build')

  // lets deploy to the itest namespace
  postBuildSteps {
    maven {
      mavenInstallation('3.3.1')
      rootPOM("app-groups/kitchen-sink/pom.xml")
      goals('io.fabric8:fabric8-maven-plugin:${FABRIC8_VERSION}:apply')
      goals('-Dfabric8.apply.recreate=true')
      goals('-Dfabric8.apply.domain=itest.os1.fabric8.io')
      goals('-Dfabric8.apply.namespace=itest')
    }
  }
  publishers {
    downstreamParameterized {
      trigger('fabric8-quickstarts', 'UNSTABLE_OR_WORSE', true) {
        currentBuild()
      }
    }
  }
}


mavenJob('fabric8-quickstarts') {
  using('base-maven-build')
  wrappers {
    timeout {
      elastic(
              400, // Build will timeout when it take 3 time longer than the reference build duration, default = 150
              5,   // Number of builds to consider for average calculation
              120   // 120 minutes default timeout (no successful builds available as reference)
      )
      failBuild()
    }
  }
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
    shell('find * -name pom.xml | xargs perl -pi -e "s/<kubernetes-model.version>.+<\\/kubernetes-model.version>/<kubernetes-model.version>${KUBERNETES_MODEL_VERSION}<\\/kubernetes-model.version>/g"')
    shell('find * -name pom.xml | xargs perl -pi -e "s/<fabric8.version>.+<\\/fabric8.version>/<fabric8.version>${FABRIC8_VERSION}<\\/fabric8.version>/g"')
    shell('find * -name pom.xml | xargs perl -pi -e "s/<fabric8.release.version>.+<\\/fabric8.release.version>/<fabric8.release.version>${FABRIC8_VERSION}<\\/fabric8.release.version>/g"')
    maven {
      mavenInstallation('3.3.1')
      goals('build-helper:parse-version')
      goals('versions:set')
      goals('-DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}')
    }
  }
  goals('clean deploy')
  goals('-DaltDeploymentRepository=local-nexus::default::${STAGING_REPO}')
  goals('-Ddocker.registryPrefix=registry.os1.fabric8.io/')
  // This works around this bug of the settings not being found by the furnace maven plugin: https://issues.jboss.org/browse/FURNACE-45
  goals('-settings /var/jenkins_home/.m2/settings.xml')
  // TODO can't push yet until we figure out authentication on the local registry...
  // goals('-Pjube,docker-push')
  goals('-Pcanary,quickstarts,jube,docker-build')

  // lets deploy to the itest namespace
  publishers {
    downstreamParameterized {
      trigger('fabric8-deploy', 'UNSTABLE_OR_WORSE', true) {
        currentBuild()
      }
    }
  }
}

freeStyleJob('fabric8-deploy') {
  using('base-freestyle-build')
  wrappers {
    credentialsBinding {
      file('SIGNING_KEY_FILE', 'fusesource-gpg-signing-key')
      string('SIGNING_KEY_PASSPHRASE', 'fusesource-gpg-signing-key-passphrase')
    }
  }
  steps {
    shell('rm -rf *')
    copyArtifacts('origin-schema-generator', '**/*') {
      upstreamBuild(true)
    }
    copyArtifacts('fabric8', '**/*') {
      upstreamBuild(true)
    }
    copyArtifacts('fabric8-apps', '**/*') {
      upstreamBuild(true)
    }
    copyArtifacts('fabric8-quickstarts', '**/*') {
      upstreamBuild(true)
    }
    shell('''
      KEY_ID=$(gpg --import ${SIGNING_KEY_FILE} 2>&1|head -1|grep -Eo '([A-F0-8]{8})')
      for i in $(find . -type f -print)
      do
        gpg --no-tty --batch --passphrase ${SIGNING_KEY_PASSPHRASE} -ab -o ${i}.asc -u ${KEY_ID} --sign ${i}
      done
    ''')
  }
}
