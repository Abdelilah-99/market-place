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
    CI_STATE_DIR               = "${WORKSPACE}/.jenkins-state"
    LAST_SUCCESSFUL_COMMIT_FILE = "${WORKSPACE}/.jenkins-state/last_successful_commit"
    NOTIFICATION_EMAIL         = 'bouchikhiabdelilah0@gmail.com'
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
          file(credentialsId: 'truststore', variable: 'TRUSTSTORE'),
          file(credentialsId: 'gate-cert', variable: 'GATE_CERT'),
          file(credentialsId: 'prod-cert', variable: 'PROD_CERT'),
          file(credentialsId: 'media-cert', variable: 'MEDIA_CERT'),
          file(credentialsId: 'usr-cert', variable: 'USR_CERT')
        ]) {
          sh '''#!/usr/bin/env bash
            set -euo pipefail
            mkdir -p certs gateway/certs products-service/certs media-service/certs users-service/certs

            cp "$USR_ENV"     ./users-service/.env.users
            cp "$GATEWAY_ENV" ./gateway/.env.gateway
            cp "$PRDCT_ENV"   ./products-service/.env.product
            cp "$MDA_ENV"     ./media-service/.env.media
            
            declare -A files=(
              ["TRUSTSTORE"]="certs/truststore.p12"
              ["GATE_CERT"]="gateway/certs/gateway.p12"
              ["PROD_CERT"]="products-service/certs/products-service.p12"
              ["MEDIA_CERT"]="media-service/certs/media-service.p12"
              ["USR_CERT"]="users-service/certs/users-service.p12"
            )

            for key in "${!files[@]}"; do
              src=${!key}            # indirect expansion: variable named by $key (e.g. $TRUSTSTORE)
              dest=${files[$key]}
              if [ -n "$src" ] && [ -f "$src" ]; then
                mkdir -p "$(dirname "$dest")"
                cp "$src" "$dest"
                chmod 600 "$dest"
              else
                echo "Warning: credential $key not provided or file missing: $src"
              fi
            done

            # Verify that the credential temp file and the workspace copy match
            if [ -z "${PROD_CERT:-}" ]; then
              echo "ERROR: PROD_CERT variable is not set" >&2
              exit 1
            fi

            if [ ! -f "$PROD_CERT" ]; then
              echo "ERROR: credential temp file not found: $PROD_CERT" >&2
              exit 1
            fi

            if [ ! -f products-service/certs/products-service.p12 ]; then
              echo "ERROR: workspace copy not found: products-service/certs/products-service.p12" >&2
              exit 1
            fi

            prod_sum=$(sha256sum "$PROD_CERT" | awk '{print $1}')
            copy_sum=$(sha256sum products-service/certs/products-service.p12 | awk '{print $1}')

            echo "PROD_CERT temp: $PROD_CERT"
            echo "prod_sum=${prod_sum}"
            echo "copy_sum=${copy_sum}"

            if [ "$prod_sum" != "$copy_sum" ]; then
              echo "ERROR: prod-cert mismatch between Jenkins credential and workspace copy" >&2
              exit 1
            fi

            chmod 600 \
              ./users-service/.env.users \
              ./gateway/.env.gateway \
              ./products-service/.env.product \
              ./media-service/.env.media
          '''
        }

        // Also compute the stored credential checksum from the controller credential store
        script {
          // This runs on the controller and inspects the FileCredentialsImpl with id 'prod-cert'
          try {
            def id = 'prod-cert'
            def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
              com.cloudbees.plugins.credentials.impl.FileCredentialsImpl.class,
              jenkins.model.Jenkins.instance,
              null,
              null
            )
            def c = creds.find { it.id == id }
            if (c == null) {
              echo "Jenkins credential not found: ${id}"
            } else {
              def data = c.file.bytes
              def md = java.security.MessageDigest.getInstance('SHA-256').digest(data)
              def hex = md.collect { String.format('%02x', it) }.join()
              echo "jenkins_store_prod_sum=${hex}"
            }
          } catch (err) {
            echo "Could not compute stored credential checksum: ${err.getMessage()}"
          }
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
          def previousCommit = fileExists(env.LAST_SUCCESSFUL_COMMIT_FILE)
            ? readFile(env.LAST_SUCCESSFUL_COMMIT_FILE).trim()
            : ''

          try {
            sh 'bash scripts/ci/deploy_local.sh'
          } catch (err) {
            if (previousCommit) {
              echo "Deployment failed — rolling back to ${previousCommit}"
              sh "bash scripts/ci/rollback_local.sh ${previousCommit}"
            } else {
              echo 'Deployment failed — no previous commit available for rollback.'
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
      script { sendEmail('SUCCESS', 'All tests passed successfully.') }
    }
    unstable {
      script { sendEmail('UNSTABLE', 'Some tests failed or warnings were detected.') }
    }
    failure {
      script { sendEmail('FAILED', "The build failed. Check the logs: ${env.BUILD_URL}console") }
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