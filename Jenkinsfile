pipeline {
    agent none
    stages {
        stage("Validate") {
            matrix {
                axes {
                    axis {
                        name 'JAVA_VERSION'
                        values 'java8', 'neo-java'
                    }
                }
                agent {
                    label env.JAVA_VERSION
                }
    stages {
        stage('Build') {
            steps {
                sh './gradlew tasks # make sure gradle-wrapper has installed'
                sh "( ./gradlew --version ; ./gradlew projects buildEnvironment ) | tee build-environment-${JAVA_VERSION}.log"
                sh './gradlew --info --console=plain --parallel assemble compileTest'
            }
            post {
                always {
                    recordIssues enabledForFailure: true, tools: [java(id:"java-$JAVA_VERSION", name:"Compilation $JAVA_VERSION")]
                    archiveArtifacts artifacts: "build-environment-${JAVA_VERSION}.log"
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
                      javaDoc(id:"javaDoc-$JAVA_VERSION", name: "JavaDoc $JAVA_VERSION"),
                      taskScanner(id:"tasks-$JAVA_VERSION", name: "Tasks $JAVA_VERSION", includePattern: '**/*.java,**/*.groovy,**/*.gradle,**/*.kts', lowTags: 'WIBNIF', normalTags: 'TODO, FIXME', highTags: 'ASAP')
                    ]
                    //Note: Javadoc archiver only works for one directory :-(
                    javadoc javadocDir: 'gestalt-entity-system/build/docs/javadoc', keepAll: false
                }
            }
        }
        stage('Publish') {
            when {
                expression { env.JAVA_VERSION == 'java8' }
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
        }
    }
}
