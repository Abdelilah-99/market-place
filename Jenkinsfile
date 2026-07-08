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
        summarizeFailureLogs()
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

def summarizeFailureLogs() {
  sh(
    label: 'failure-summary',
    script: '''
      set +e

      pattern='error|exception|failed|failure|fatal|caused by|could not|cannot|denied|timeout|no space left|BUILD FAILED|BUILD FAILURE|Compilation failed|returned non-zero|script returned exit code'
      log_dir="${CI_STATE_DIR:-.jenkins-state}/logs"

      echo "========== JENKINS ERROR SUMMARY =========="
      if [ -d "$log_dir" ]; then
        found_logs=false
        for file in "$log_dir"/*.log; do
          [ -f "$file" ] || continue
          found_logs=true
          echo "--- $(basename "$file") ---"
          grep -iE "$pattern" "$file" | tail -n 120 || true
        done
        [ "$found_logs" = true ] || echo "No step log files found."
      else
        echo "No Jenkins step log directory found: $log_dir"
      fi

      echo "========== RECENT DOCKER ERRORS =========="
      if command -v docker >/dev/null 2>&1; then
        echo "--- docker disk usage ---"
        docker system df || true
        echo "--- host disk usage ---"
        df -h || true
        docker ps --format "{{.Names}}" 2>/dev/null | while read -r container; do
          [ -n "$container" ] || continue
          echo "--- $container ---"
          docker logs "$container" --tail 300 2>&1 \
            | grep -iE "$pattern|500|502|503|504|warn|DiskThresholdMonitor|flood stage|watermark" \
            | grep -viE 'WiredTiger message|WT_SESSION.checkpoint|checkpoint snapshot' \
            | tail -n 80 || true
        done
      else
        echo "Docker CLI not available on this Jenkins agent."
      fi
    '''
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
