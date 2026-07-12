pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '30'))
  }

  triggers {
    pollSCM('H/2 * * * *')
  }

  environment {
    CI_STATE_DIR                = "${WORKSPACE}/.jenkins-state"
    LAST_SUCCESSFUL_COMMIT_FILE = "${WORKSPACE}/.jenkins-state/last_successful_commit"
    NOTIFICATION_EMAIL          = 'bouchikhiabdelilah0@gmail.com'
    SONAR_HOST_URL              = 'https://sonarcloud.io'
    SONAR_ORGANIZATION          = 'abdelilah-99'
    SKIP_SONARCLOUD             = 'true'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        sh 'mkdir -p "${CI_STATE_DIR}/logs"'
      }
    }

    stage('Build and Test') {
      steps {
        // Wrap blocks to prevent plain-text files on disk
        withCredentials(getCredentialsList()) {
          script {
            runLogged('build-setup-env', 'bash scripts/ci/setup_env.sh')
            runLogged('build-and-test', 'bash scripts/ci/build_and_test.sh')
          }
        }
      }
    }

    stage('SonarCloud Analysis') {
      when {
        expression { env.SKIP_SONARCLOUD != 'true' }
      }
      steps {
        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
          script {
            runLogged('sonarqube-analysis', 'bash scripts/ci/sonarqube_analysis.sh')
          }
        }
      }
    }

    stage('Deploy') {
      steps {
        script {
          def previousCommit = fileExists(env.LAST_SUCCESSFUL_COMMIT_FILE)
            ? readFile(env.LAST_SUCCESSFUL_COMMIT_FILE).trim()
            : ''

          try {
            withCredentials(getCredentialsList()) {
              runLogged('deploy-setup-env', 'bash scripts/ci/setup_env.sh')
              runLogged('deploy-local', 'bash scripts/ci/deploy_local.sh')
            }
          } catch (err) {
            attemptRollback('Deployment failed', previousCommit)
            throw err
          }
        }
      }
    }
  }

  post {
    success {
      sh 'git rev-parse HEAD > "${LAST_SUCCESSFUL_COMMIT_FILE}"'
      script { sendEmail('SUCCESS', 'All tests passed successfully.') }
    }
    unstable {
      script { sendEmail('UNSTABLE', 'Some tests failed.') }
    }
    failure {
      script {
        sendEmail('FAILED', "Build failed. Logs: ${env.BUILD_URL}console")
      }
    }
    always {
      script {
        try {
          archiveArtifacts artifacts: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml,frontend/coverage/**,.jenkins-state/logs/*.log', allowEmptyArchive: true
          junit testResults: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml', allowEmptyResults: true
        } catch (err) {
          echo "Archive skipped: ${err.getMessage()}"
        }
      }
      sh '''
        rm -rf ./users-service/.env.users ./gateway/.env.gateway ./products-service/.env.product ./media-service/.env.media ./opensearch/.env.opensearch ./certs **/certs
      '''
    }
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

def getCredentialsList() {
  return [
    file(credentialsId: 'env-users',   variable: 'USR_ENV'),
    file(credentialsId: 'env-product', variable: 'PRDCT_ENV'),
    file(credentialsId: 'env-gateway', variable: 'GATEWAY_ENV'),
    file(credentialsId: 'env-media',   variable: 'MDA_ENV'),
    file(credentialsId: 'env-opensearch', variable: 'OPENSEARCH_ENV'),
    file(credentialsId: 'jwt-private-key', variable: 'JWT_PRIVATE_KEY'),
    file(credentialsId: 'jwt-public-key',  variable: 'JWT_PUBLIC_KEY')
  ]
}

def runLogged(String logName, String command) {
  sh(
    label: logName,
    script: """
      mkdir -p "\${CI_STATE_DIR}/logs"
      bash -lc 'set -o pipefail; ${command} 2>&1 | tee "\${CI_STATE_DIR}/logs/${logName}.log"; exit \${PIPESTATUS[0]}'
    """
  )
}

def sendEmail(String status, String message) {
  try {
    emailext(
      to: env.NOTIFICATION_EMAIL,
      subject: "Build ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      body: "Status: ${status}\nURL: ${env.BUILD_URL}\n\n${message}",
      mimeType: 'text/plain'
    )
  } catch (err) {
    mail(to: env.NOTIFICATION_EMAIL, subject: "Build ${status}", body: message)
  }
}

def attemptRollback(String prefix, String previousCommit) {
    if (!previousCommit?.trim()) {
        echo "No previous successful commit found. Skipping rollback."
        return
    }

    try {
        withCredentials(getCredentialsList()) {
            runLogged('rollback-setup-env', 'bash scripts/ci/setup_env.sh')
            runLogged('rollback-local', "bash scripts/ci/rollback_local.sh ${previousCommit}")
        }
    } catch (err) {
        echo "Rollback failed: ${err.getMessage()}"
    }
} 
