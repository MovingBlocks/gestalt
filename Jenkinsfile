pipeline {
    agent {
        label "android"
    }
    stages {
        stage('Build') {
            steps {
                sh 'echo sdk.dir=/opt/android-sdk > local.properties'
                sh './gradlew --info --console=plain --parallel assemble compileTest'
            }
            post {
                always {
                    recordIssues enabledForFailure: true, tools: [java()]
                }
            }
        }
        stage('Analytics') {
            steps {
                // `test` seems flaky when run with --parallel, so separate it from other checks
                sh './gradlew --info --console=plain --continue test'
                sh './gradlew --info --console=plain --parallel --continue javadoc check --exclude-task test'
            }
            post {
                always {
                    junit testResults: '**/build/test-results/test/*.xml'
                    recordIssues tools: [
                      javaDoc(),
                      taskScanner(includePattern: '**/*.java,**/*.groovy,**/*.gradle,**/*.kts', lowTags: 'WIBNIF', normalTags: 'TODO, FIXME', highTags: 'ASAP')
                    ]
                    //Note: Javadoc archiver only works for one directory :-(
                    javadoc javadocDir: 'gestalt-entity-system/build/docs/javadoc', keepAll: false
                }
            }
        }
        stage('Publish') {
            when {
                anyOf {
                    branch 'develop'
                    branch pattern: "release/v\\d+.x", comparator: "REGEXP"
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactory-gooey', usernameVariable: 'artifactoryUser', passwordVariable: 'artifactoryPass')]) {
                    sh './gradlew --info --console=plain -Dorg.gradle.internal.publish.checksums.insecure=true publish -PmavenUser=${artifactoryUser} -PmavenPass=${artifactoryPass}'
                }
            }
        }
    }
}
