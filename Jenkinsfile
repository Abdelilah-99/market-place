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
    SMTP_USERNAME = credentials('gmail-smtp-username-v2')
    SMTP_PASSWORD = credentials('gmail-smtp-password')
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
        sh 'mkdir -p "${CI_STATE_DIR}"'
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
      sh 'git rev-parse HEAD > "${LAST_SUCCESSFUL_COMMIT_FILE}"'
      sh 'bash scripts/ci/notify.sh success'
      emailext(
        to: "${NOTIFICATION_EMAILS}",
        subject: "✅ Build Successful: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
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
    }
    unstable {
      sh 'bash scripts/ci/notify.sh unstable'
      emailext(
        to: "${NOTIFICATION_EMAILS}",
        subject: "⚠️ Build Unstable: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
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
    }
    failure {
      sh 'bash scripts/ci/notify.sh failure'
      emailext(
        to: "${NOTIFICATION_EMAILS}",
        subject: "❌ Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
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
    }
    always {
      archiveArtifacts artifacts: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml,frontend/coverage/**', allowEmptyArchive: true
      junit testResults: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml', allowEmptyResults: true
    }
  }
}
