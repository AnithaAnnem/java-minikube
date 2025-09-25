package org.example

class MavenUtils implements Serializable {

    // Package a Maven service
    static void packageService(script, String path) {
        script.sh "mvn clean package -f ${path}/pom.xml"
    }

    // Run unit tests and publish results
    static void runTests(script, String path) {
        script.sh "mvn test -f ${path}/pom.xml"
        script.junit "${path}/target/surefire-reports/*.xml"
    }

    static void sonarAnalysis(script, String path, String projectKey, String sonarEnv, String sonarToken) {
        script.withSonarQubeEnv("${sonarEnv}") {
            script.sh """
                mvn sonar:sonar -f ${path}/pom.xml \
                    -Dsonar.projectKey=${projectKey} \
                    -Dsonar.host.url=\$SONAR_HOST_URL \
                    -Dsonar.login=${sonarToken}
            """
        }
    }
}
