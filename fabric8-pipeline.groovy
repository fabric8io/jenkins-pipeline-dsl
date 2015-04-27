buildPipelineView('fabric8-pipeline') {
  selectedJob('origin-schema-generator')
  title('Fabric8 Pipeline')
  showPipelineParameters()
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
  mavenInstallation('3.3.1')
  preBuildSteps {
    shell('go get github.com/tools/godep')
    shell('godep go build ./cmd/generate/generate.go')
    shell('./generate | python -m json.tool > kubernetes-model/src/main/resources/schema/kube-schema.json')
    maven {
      mavenInstallation('3.3.1')
      goals('build-helper:parse-version')
      goals('versions:set')
      goals('-DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}')
    }
  }
  goals('clean deploy -DaltDeploymentRepository=nexus::default::${STAGING_REPO}')
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
      goals('org.codehaus.mojo:versions-maven-plugin:2.2:update-property -DnewVersion=${KUBERNETES_MODEL_VERSION} property=kubernetes-model.version')
    }
  }
  mavenInstallation('3.3.1')
  goals('clean deploy -DaltDeploymentRepository=nexus::default::${STAGING_REPO}')
}
