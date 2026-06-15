pipeline {
agent any

```
options {
    timestamps()
    disableConcurrentBuilds()
    buildDiscarder(logRotator(
        numToKeepStr: '30',
        artifactNumToKeepStr: '30'
    ))
}

triggers {
    pollSCM('H/5 * * * *')
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

    stage('Build and Test') {
        steps {
            withCredentials(getCredentialsList()) {
                sh 'bash scripts/ci/setup_env.sh'
                sh 'bash scripts/ci/build_and_test.sh'
            }
        }
    }

    stage('Deploy') {
        steps {
            withCredentials(getCredentialsList()) {
                sh 'bash scripts/ci/setup_env.sh'
                sh 'bash scripts/ci/deploy_local.sh'
            }
        }
    }
}

post {

    success {
        sh '''
            mkdir -p "${CI_STATE_DIR}"
            git rev-parse HEAD > "${LAST_SUCCESSFUL_COMMIT_FILE}"
        '''

        script {
            sendEmail(
                'SUCCESS',
                'Build, tests, and deployment completed successfully.'
            )
        }
    }

    unstable {
        script {
            sendEmail(
                'UNSTABLE',
                'Build completed but some tests are unstable.'
            )
        }
    }

    failure {
        script {

            def previousCommit = ''

            if (fileExists(env.LAST_SUCCESSFUL_COMMIT_FILE)) {
                previousCommit = readFile(
                    env.LAST_SUCCESSFUL_COMMIT_FILE
                ).trim()
            }

            attemptRollback(previousCommit)

            sendEmail(
                'FAILED',
                """Build failed.
```

Build URL:
${env.BUILD_URL}

Console:
${env.BUILD_URL}console
"""
)
}
}

```
    always {

        script {
            try {
                archiveArtifacts(
                    artifacts: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml,frontend/coverage/**',
                    allowEmptyArchive: true
                )

                junit(
                    testResults: '**/target/surefire-reports/*.xml,**/build/test-results/test/*.xml',
                    allowEmptyResults: true
                )
            } catch (err) {
                echo "Archive skipped: ${err.getMessage()}"
            }
        }

        sh '''
            rm -rf \
              ./users-service/.env.users \
              ./gateway/.env.gateway \
              ./products-service/.env.product \
              ./media-service/.env.media \
              ./certs \
              users-service/certs \
              gateway/certs \
              products-service/certs \
              media-service/certs
        '''
    }
}
```

}

| /*                                                                         |
| -------------------------------------------------------------------------- |
| Helpers                                                                    |
| -------------------------------------------------------------------------- |
| */                                                                         |

def getCredentialsList() {
return [
file(credentialsId: 'env-users',   variable: 'USR_ENV'),
file(credentialsId: 'env-product', variable: 'PRDCT_ENV'),
file(credentialsId: 'env-gateway', variable: 'GATEWAY_ENV'),
file(credentialsId: 'env-media',   variable: 'MDA_ENV'),

```
    file(credentialsId: 'truststore',  variable: 'TRUSTSTORE'),

    file(credentialsId: 'gate-cert',   variable: 'GATE_CERT'),
    file(credentialsId: 'prod-cert',   variable: 'PROD_CERT'),
    file(credentialsId: 'media-cert',  variable: 'MEDIA_CERT'),
    file(credentialsId: 'usr-cert',    variable: 'USR_CERT')
]
```

}

def sendEmail(String status, String message) {

```
try {

    emailext(
        to: env.NOTIFICATION_EMAIL,
        subject: "Build ${status}: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        body: """
```

Status: ${status}
Job: ${env.JOB_NAME}
Build: #${env.BUILD_NUMBER}
URL: ${env.BUILD_URL}

${message}
""",
mimeType: 'text/plain'
)

```
} catch (err) {

    echo "emailext failed: ${err.getMessage()}"

    mail(
        to: env.NOTIFICATION_EMAIL,
        subject: "Build ${status}: ${env.JOB_NAME}",
        body: message
    )
}
```

}

def attemptRollback(String previousCommit) {

```
if (!previousCommit?.trim()) {
    echo 'No previous successful commit found. Rollback skipped.'
    return
}

echo "Starting rollback to ${previousCommit}"

try {

    withCredentials(getCredentialsList()) {

        sh 'bash scripts/ci/setup_env.sh'

        sh """
            bash scripts/ci/rollback_local.sh ${previousCommit}
        """
    }

    echo "Rollback completed successfully."

} catch (err) {

    echo "Rollback failed: ${err.getMessage()}"
}
```

}
