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
                //recordIssues tool: checkStyle(pattern: '**/build/reports/checkstyle/*.xml')
                //recordIssues tool: spotBugs(pattern: '**/build/reports/spotbugs/main/*.xml', useRankAsPriority: true)
                //recordIssues tool: pmdParser(pattern: '**/build/reports/pmd/*.xml')
                recordIssues tool: taskScanner(includePattern: '**/*.java,**/*.groovy,**/*.gradle', lowTags: 'WIBNIF', normalTags: 'TODO', highTags: 'ASAP')
            }
        }
    }
}
