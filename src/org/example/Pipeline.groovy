package org.example

class Pipeline implements Serializable {
    def script

    Pipeline(script) { this.script = script }

    void run(Map config = [:]) {
        script.node {
            script.stage('Checkout') {
                GitUtils.checkout(script, config.BRANCH ?: 'main', config.GIT_URL ?: 'https://github.com/AnithaAnnem/java-minikube.git')
            }

            script.stage('Credential Scanning - Gitleaks') {
                SecurityUtils.gitleaksScan(script)
            }

            script.stage('Package Services') {
                MavenUtils.packageService(script, 'spring-minikube/student-service')
                MavenUtils.packageService(script, 'spring-minikube/rating-service')
            }

            script.stage('Unit Tests') {
                MavenUtils.runTests(script, 'spring-minikube/student-service')
                MavenUtils.runTests(script, 'spring-minikube/rating-service')
            }

            script.stage('Dependency Scanning') {
                SecurityUtils.dependencyCheck(script)
            }

            script.stage('SonarQube Analysis') {
                MavenUtils.sonarAnalysis(
                    script,
                    'spring-minikube/student-service',
                    'student-service',
                    config.SONARQUBE_ENV ?: 'sonar-java',
                    script.credentials(config.SONARQ_CRED ?: 'anitha-sonar')
                )
                MavenUtils.sonarAnalysis(
                    script,
                    'spring-minikube/rating-service',
                    'rating-service',
                    config.SONARQUBE_ENV ?: 'sonar-java',
                    script.credentials(config.SONARQ_CRED ?: 'anitha-sonar')
                )
            }

            script.stage('Build & Push Docker Images') {
                DockerUtils.buildPush(script, 'student-service', 'spring-minikube/student-service')
                DockerUtils.buildPush(script, 'rating-service', 'spring-minikube/rating-service')
            }

            script.stage('Start Minikube') {
                MinikubeUtils.start(script)
            }

            script.stage('Setup K8s Configs') {
                MinikubeUtils.applyConfigs(script)
            }

            script.stage('Deploy Services') {
                MinikubeUtils.deployServices(script)
            }

            script.stage('DAST - OWASP ZAP Scan') {
                SecurityUtils.dastScan(script)
            }

            script.echo " Pipeline Completed Successfully!"
        } 
    }
}
