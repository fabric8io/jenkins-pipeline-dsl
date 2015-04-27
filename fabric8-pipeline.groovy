buildPipelineView('fabric8-pipeline') {
  selectedJob('origin-schema-generator')
  title('Fabric8 Pipeline')
  showPipelineParameters()
}

job('origin-schema-generator') {
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
  steps {
    shell('go get github.com/tools/godep')
    shell('godep go build ./cmd/generate/generate.go')
    shell('./generate | python -m json.tool > kubernetes-model/src/main/resources/schema/kube-schema.json')
    maven {
      mavenInstallation('3.3.1')
      goals('build-helper:parse-version versions:set -DnewVersion=${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}-${BUILD_NUMBER}')
    }
    maven {
      mavenInstallation('3.3.1')
      goals('clean install')
    }
  }
  publishers {
    fingerprint('kubernetes-model/target/classes/schema/kube-schema.json', true)
    archiveArtifacts {
      pattern('kubernetes-model/target/classes/schema/kube-schema.json')
      latestOnly()
    }

    downstreamParameterized {
      trigger('fabric8', 'SUCCESS', true) {
        currentBuild()
        predefinedProp('KUBERNETES_MODEL_BUILD_NUMBER', '${BUILD_NUMBER}')
      }
    }
  }
}

job('fabric8') {
  using('base-config-build')
  parameters {
    stringParam('KUBERNETES_MODEL_BUILD_NUMBER')
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
  steps {
    shell('echo ${KUBERNETES_MODEL_BUILD_NUMBER}')
    copyArtifacts('origin-schema-generator', '**/*') {
      buildNumber('KUBERNETES_MODEL_BUILD_NUMBER')
    }
    maven {
      mavenInstallation('3.3.1')
      goals('clean install')
    }
  }
  publishers {
    fingerprint('**/target/**/*.jar', true)
    archiveArtifacts {
      pattern('**/target/**/*.jar')
      pattern('**/target/**/*.war')
      pattern('**/target/**/*.zip')
      latestOnly()
    }
  }
}
