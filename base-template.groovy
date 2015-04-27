mavenJob('base-config-build') {
  wrappers {
    timestamps()
    colorizeOutput()
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
      groovy('''
    def gopath = '${JENKINS_HOME}/jobs/${JOB_NAME}/builds/${BUILD_ID}/go'
    new File(gopath + '/bin').mkdirs()
    return [
      GOPATH: gopath + ':$WORKSPACE',
      PATH: gopath + '/bin:$PATH',
      STAGING_REPO: 'http://${env.NEXUS_SERVICE_HOST}:${env.NEXUS_SERVICE_PORT}/content/repositories/staging/',
      RELEASE_REPO: 'http://${env.NEXUS_SERVICE_HOST}:${env.NEXUS_SERVICE_PORT}/content/repositories/releases/'
    ]
      ''')
    }
  }
  jdk('java8')
}
