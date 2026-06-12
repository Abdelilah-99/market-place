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
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
        sh 'mkdir -p "${CI_STATE_DIR}"'
      }
    }

    stage('Setup Env Files') {
      steps {
        withCredentials([
          file(credentialsId: 'env-users',   variable: 'USR_ENV'),
          file(credentialsId: 'env-product', variable: 'PRDCT_ENV'),
          file(credentialsId: 'env-gateway', variable: 'GATEWAY_ENV'),
          file(credentialsId: 'env-media',   variable: 'MDA_ENV'),
          file(credentialsId: 'truststore',  variable: 'TRUSTSTORE'),
          file(credentialsId: 'gate-cert',   variable: 'GATE_CERT'),
          file(credentialsId: 'prod-cert',   variable: 'PROD_CERT'),
          file(credentialsId: 'media-cert',  variable: 'MEDIA_CERT'),
          file(credentialsId: 'usr-cert',    variable: 'USR_CERT')
        ]) {
          sh '''#!/usr/bin/env bash
            set -euo pipefail
            mkdir -p certs gateway/certs products-service/certs media-service/certs users-service/certs

            cp "$USR_ENV"     ./users-service/.env.users
            cp "$GATEWAY_ENV" ./gateway/.env.gateway
            cp "$PRDCT_ENV"   ./products-service/.env.product
            cp "$MDA_ENV"     ./media-service/.env.media

            chmod 600 \
              ./users-service/.env.users \
              ./gateway/.env.gateway \
              ./products-service/.env.product \
              ./media-service/.env.media

            declare -A certs=(
              ["$TRUSTSTORE"]="certs/truststore.p12"
              ["$GATE_CERT"]="gateway/certs/gateway.p12"
              ["$PROD_CERT"]="products-service/certs/products-service.p12"
              ["$MEDIA_CERT"]="media-service/certs/media-service.p12"
              ["$USR_CERT"]="users-service/certs/users-service.p12"
            )

            for src in "${!certs[@]}"; do
              dest="${certs[$src]}"
              cp "$src" "$dest"
              chmod 600 "$dest"
            done
          '''
        }
        sh '''
          echo "After Setup Env Files:"
          ls -la products-service/.env.product
        '''
      }
    }

    stage('Build and Test') {
      steps {
        sh 'bash scripts/ci/build_and_test.sh'
      }
    }

    stage('Deploy') {
      steps {
        script {
          def previousCommit = fileExists(env.LAST_SUCCESSFUL_COMMIT_FILE)
            ? readFile(env.LAST_SUCCESSFUL_COMMIT_FILE).trim()
            : ''

          try {
            sh 'bash scripts/ci/deploy_local.sh'
          } catch (err) {
            attemptRollback('Deployment failed')
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
      script { sendEmail('UNSTABLE', 'Some tests failed or warnings were detected.') }
    }
    failure {
      script {
        attemptRollback('Build failed')
        sendEmail('FAILED', "The build failed. Check the logs: ${env.BUILD_URL}console")
      }
    }
    always {
      script {
        try {
          archiveArtifacts artifacts: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml,frontend/coverage/**', allowEmptyArchive: true
          junit testResults: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml', allowEmptyResults: true
        } catch (err) {
          echo "Archive/junit skipped: ${err.getMessage()}"
        }
      }
      sh '''
        rm -rf \
          ./users-service/.env.users \
          ./gateway/.env.gateway \
          ./products-service/.env.product \
          ./media-service/.env.media \
          ./certs \
          ./gateway/certs \
          ./products-service/certs \
          ./media-service/certs \
          ./users-service/certs
      '''
    }
  }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

def sendEmail(String status, String message) {
  try {
    emailext(
      to: env.NOTIFICATION_EMAIL,
      subject: "Build ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      body: """Build Status:  ${status}
Job:           ${env.JOB_NAME}
Build Number:  ${env.BUILD_NUMBER}
Build URL:     ${env.BUILD_URL}
Commit:        ${env.GIT_COMMIT}
Branch:        ${env.GIT_BRANCH}

${message}""",
      mimeType: 'text/plain'
    )
  } catch (err) {
    echo "emailext failed (${status}): ${err.getMessage()}"
    mail(
      to: env.NOTIFICATION_EMAIL,
      subject: "Build ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
      body: "Build Status: ${status}\nJob: ${env.JOB_NAME}\nBuild URL: ${env.BUILD_URL}"
    )
  }
}

// Shared helper to attempt a rollback based on the last successful commit file.
def attemptRollback(String prefix) {
  try {
    def previousCommit = fileExists(env.LAST_SUCCESSFUL_COMMIT_FILE)
      ? readFile(env.LAST_SUCCESSFUL_COMMIT_FILE).trim()
      : ''

    if (previousCommit) {
      echo "${prefix} — rolling back to ${previousCommit}"
      sh "bash scripts/ci/rollback_local.sh ${previousCommit}"
    } else {
      echo "${prefix} — no previous commit available for rollback."
    }
  } catch (err) {
    echo "Rollback attempt failed: ${err.getMessage()}"
  }
}