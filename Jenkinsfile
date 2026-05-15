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
    CI_STATE_DIR = "${WORKSPACE}/.jenkins-state"
    LAST_SUCCESSFUL_COMMIT_FILE = "${WORKSPACE}/.jenkins-state/last_successful_commit"
    NOTIFICATION_EMAILS = 'bouchikhiabdelilah0@gmail.com'
    SMTP_SERVER = 'smtp.gmail.com'
    SMTP_PORT = '587'
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        sh 'mkdir -p "${CI_STATE_DIR}"'
        sh 'echo "=== Verifying Git Repository ===" && pwd && ls -la && git remote -v && git log --oneline -1 && git status'
      }
    }

    stage('Setup Env Files') {
      steps {
        withCredentials([
            file(credentialsId: 'env-users', variable: 'USR_ENV'),
            file(credentialsId: 'env-product', variable: 'PRDCT_ENV'),
            file(credentialsId: 'env-media', variable: 'MDA_ENV')
        ]) {
        sh '''
          set -euo pipefail

            mkdir -p ./users-service ./products-service ./media-service

          rm -f ./users-service/.env.users
            cp "$USR_ENV"    ./users-service/.env.users

          rm -f ./products-service/.env.product
            cp "$PRDCT_ENV"  ./products-service/.env.product

          rm -f ./media-service/.env.media
            cp "$MDA_ENV"    ./media-service/.env.media

            chmod 600 ./users-service/.env.users
            chmod 600 ./products-service/.env.product
            chmod 600 ./media-service/.env.media
          '''
        }
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
          def previousCommit = ''
          if (fileExists(env.LAST_SUCCESSFUL_COMMIT_FILE)) {
            previousCommit = readFile(env.LAST_SUCCESSFUL_COMMIT_FILE).trim()
          }

          env.PREVIOUS_SUCCESSFUL_COMMIT = previousCommit

          try {
            sh 'bash scripts/ci/deploy_local.sh'
          } catch (err) {
            if (previousCommit) {
              echo "Deployment failed. Rolling back to ${previousCommit}"
              sh "bash scripts/ci/rollback_local.sh ${previousCommit}"
            } else {
              echo 'Deployment failed and no previous successful commit is available for rollback.'
            }
            throw err
          }
        }
      }
    }
  }

  post {
    success {
      script {
        try {
          sh 'git rev-parse HEAD > "${LAST_SUCCESSFUL_COMMIT_FILE}"'
          sh 'bash scripts/ci/notify.sh success'
        } catch (err) {
          echo "Skipping workspace-dependent success steps: ${err.getMessage()}"
        }

        try {
          emailext(
            to: "${NOTIFICATION_EMAILS}",
            subject: "Build Successful: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
            Build Status: SUCCESS

            Job: ${env.JOB_NAME}
            Build Number: ${env.BUILD_NUMBER}
            Build URL: ${env.BUILD_URL}
            Commit: ${env.GIT_COMMIT}
            Branch: ${env.GIT_BRANCH}

            All tests passed successfully!
            """,
            mimeType: 'text/plain'
          )
        } catch (mailErr) {
          echo "emailext failed in success block: ${mailErr.getMessage()}"
          mail to: "${NOTIFICATION_EMAILS}",
               subject: "Build Successful: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
               body: "Build Status: SUCCESS\nJob: ${env.JOB_NAME}\nBuild Number: ${env.BUILD_NUMBER}\nBuild URL: ${env.BUILD_URL}"
        }
      }
    }
    unstable {
      script {
        try {
          sh 'bash scripts/ci/notify.sh unstable'
        } catch (err) {
          echo "Skipping workspace-dependent unstable steps: ${err.getMessage()}"
        }

        try {
          emailext(
            to: "${NOTIFICATION_EMAILS}",
            subject: "Build Unstable: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
            Build Status: UNSTABLE

            Job: ${env.JOB_NAME}
            Build Number: ${env.BUILD_NUMBER}
            Build URL: ${env.BUILD_URL}
            Commit: ${env.GIT_COMMIT}
            Branch: ${env.GIT_BRANCH}

            Some tests failed or warnings were detected.
            Please review the build logs for details.
            """,
            mimeType: 'text/plain'
          )
        } catch (mailErr) {
          echo "emailext failed in unstable block: ${mailErr.getMessage()}"
          mail to: "${NOTIFICATION_EMAILS}",
               subject: "Build Unstable: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
               body: "Build Status: UNSTABLE\nJob: ${env.JOB_NAME}\nBuild Number: ${env.BUILD_NUMBER}\nBuild URL: ${env.BUILD_URL}"
        }
      }
    }
    failure {
      script {
        try {
          sh 'bash scripts/ci/notify.sh failure'
        } catch (err) {
          echo "Skipping workspace-dependent failure steps: ${err.getMessage()}"
        }

        try {
          emailext(
            to: "${NOTIFICATION_EMAILS}",
            subject: "Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
            Build Status: FAILED

            Job: ${env.JOB_NAME}
            Build Number: ${env.BUILD_NUMBER}
            Build URL: ${env.BUILD_URL}
            Commit: ${env.GIT_COMMIT}
            Branch: ${env.GIT_BRANCH}

            The build has failed. Please check the logs immediately.

            Review the full details at: ${env.BUILD_URL}console
            """,
            mimeType: 'text/plain'
          )
        } catch (mailErr) {
          echo "emailext failed in failure block: ${mailErr.getMessage()}"
          mail to: "${NOTIFICATION_EMAILS}",
               subject: "Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
               body: "Build Status: FAILED\nJob: ${env.JOB_NAME}\nBuild Number: ${env.BUILD_NUMBER}\nBuild URL: ${env.BUILD_URL}\nSee: ${env.BUILD_URL}console"
        }
      }
    }
    always {
      script {
        try {
          archiveArtifacts artifacts: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml,frontend/coverage/**', allowEmptyArchive: true
          junit testResults: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml', allowEmptyResults: true
        } catch (err) {
          echo "Skipping workspace-dependent archive/junit steps: ${err.getMessage()}"
        }
      }

        sh '''
          rm -f ./users-service/.env.users
          rm -f ./products-service/.env.product
          rm -f ./media-service/.env.media
        '''
    }
  }
}
