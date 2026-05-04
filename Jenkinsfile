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
    NOTIFICATION_EMAILS = 'dev-team@example.com'
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
    }
    unstable {
      sh 'bash scripts/ci/notify.sh unstable'
    }
    failure {
      sh 'bash scripts/ci/notify.sh failure'
        mail to: 'bouchikhiabdelilah0@gmail.com',
          subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
          body: "Something went wrong. Check it here: ${env.BUILD_URL}"
    }
    always {
      archiveArtifacts artifacts: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml,frontend/coverage/**', allowEmptyArchive: true
      junit testResults: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml', allowEmptyResults: true
    }
  }
}
