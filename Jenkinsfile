/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

pipeline {
    agent {
        label "light-java"
    }
    stages {
        stage('Build') {
            steps {
                sh './gradlew --info --console=plain jar'
            }
        }
        stage('Analytics') {
            steps {
                sh './gradlew --info --console=plain javadoc check'
            }
        }
        stage('Publish') {
            when {
                anyOf {
                    branch 'master'
                    branch pattern: "release/v\\d+.x", comparator: "REGEXP"
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'artifactory-gooey', usernameVariable: 'artifactoryUser', passwordVariable: 'artifactoryPass')]) {
                    sh './gradlew --info --console=plain -Dorg.gradle.internal.publish.checksums.insecure=true publish -PmavenUser=${artifactoryUser} -PmavenPass=${artifactoryPass}'
                }
            }
        }
        stage('Record') {
            steps {
                junit testResults: '**/build/test-results/test/*.xml',  allowEmptyResults: true
                recordIssues tool: javaDoc()
                //Note: Javadoc archiver only works for one directory :-(
                step([$class: 'JavadocArchiver', javadocDir: 'nui/build/docs/javadoc', keepAll: false])
                recordIssues tool: checkStyle(pattern: '**/build/reports/checkstyle/*.xml')
                recordIssues tool: spotBugs(pattern: '**/build/reports/spotbugs/main/*.xml', useRankAsPriority: true)
                recordIssues tool: pmdParser(pattern: '**/build/reports/pmd/*.xml')
                recordIssues tool: taskScanner(includePattern: '**/*.java,**/*.groovy,**/*.gradle', lowTags: 'WIBNIF', normalTags: 'TODO', highTags: 'ASAP')
            }
        }
    }
}
