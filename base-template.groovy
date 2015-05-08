mavenJob('base-maven-build') {
  wrappers {
    timestamps()
    colorizeOutput()
    maskPasswords()
    golang('1.4.2')
    timeout {
      elastic(
        450, // Build will timeout when it take 3 time longer than the reference build duration, default = 150
        5,   // Number of builds to consider for average calculation
        120   // 30 minutes default timeout (no successful builds available as reference)
      )
      failBuild()
    }
    environmentVariables {
      // TODO Note that staging repos are only available in Nexus professional so lets use the same repo for now
      // STAGING_REPO: 'http://${env.NEXUS_SERVICE_HOST}:${env.NEXUS_SERVICE_PORT}/content/repositories/staging/',
      /*
      the DNS version:

      STAGING_REPO: 'http://nexus/content/repositories/staging/',
      RELEASE_REPO: 'http://nexus/content/repositories/releases/'
      */
      groovy('''
    def gopath = '${JENKINS_HOME}/jobs/${JOB_NAME}/builds/${BUILD_ID}/go'
    new File(gopath + '/bin').mkdirs()
    return [
      GOPATH: gopath + ':$WORKSPACE',
      PATH: gopath + '/bin:$PATH',
      STAGING_REPO: 'http://${env.NEXUS_SERVICE_HOST}/content/repositories/staging/',
      RELEASE_REPO: 'http://${env.NEXUS_SERVICE_HOST}/content/repositories/releases/\'
    ]
      ''')
    }
  }
  mavenInstallation('3.3.1')
  localRepository(LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
  mavenOpts("-B")
}

freeStyleJob('base-freestyle-build') {
  wrappers {
    timestamps()
    colorizeOutput()
    maskPasswords()
    golang('1.4.2')
    timeout {
      elastic(
        150, // Build will timeout when it take 3 time longer than the reference build duration, default = 150
        3,   // Number of builds to consider for average calculation
        30   // 30 minutes default timeout (no successful builds available as reference)
      )
      failBuild()
    }
    environmentVariables {
      // TODO Note that staging repos are only available in Nexus professional so lets use the same repo for now
      // STAGING_REPO: 'http://${env.NEXUS_SERVICE_HOST}:${env.NEXUS_SERVICE_PORT}/content/repositories/staging/',
      groovy('''
    def gopath = '${JENKINS_HOME}/jobs/${JOB_NAME}/builds/${BUILD_ID}/go'
    new File(gopath + '/bin').mkdirs()
    return [
      GOPATH: gopath + ':$WORKSPACE',
      PATH: gopath + '/bin:$PATH',
      STAGING_REPO: 'http://${env.NEXUS_SERVICE_HOST}/content/repositories/staging/',
      RELEASE_REPO: 'http://${env.NEXUS_SERVICE_HOST}/content/repositories/releases/'
    ]
      ''')
    }
  }
}
