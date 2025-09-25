package org.example

class Pipeline implements Serializable {
    def script

    Pipeline(script) { this.script = script }

    void run(Map config = [:]) {
        script.node {
            // Checkout
            script.stage('Checkout') {
                GitUtils.checkout(
                    script, 
                    config.BRANCH ?: 'main', 
                    config.GIT_URL ?: 'https://github.com/AnithaAnnem/java-minikube.git'
                )
            }

            // Credential Scanning
            script.stage('Credential Scanning - Gitleaks') {
                SecurityUtils.gitleaksScan(script)
            }

            // Package Services
            script.stage('Package Services') {
                MavenUtils.packageService(script, 'spring-minikube/student-service')
                MavenUtils.packageService(script, 'spring-minikube/rating-service')
            }

            // Unit Tests
            script.stage('Unit Tests') {
                MavenUtils.runTests(script, 'spring-minikube/student-service')
                MavenUtils.runTests(script, 'spring-minikube/rating-service')
            }

            // Dependency Scanning
            script.stage('Dependency Scanning') {
                SecurityUtils.dependencyCheck(script)
            }

            // SonarQube Analysis (per service)
            script.stage('SonarQube Analysis - student-service') {
                script.withCredentials([script.string(
                    credentialsId: config.SONARQ_CRED ?: 'anitha-sonar',
                    variable: 'SONAR_AUTH_TOKEN'
                )]) {
                    MavenUtils.sonarAnalysis(
                        script,
                        'spring-minikube/student-service',
                        'student-service',
                        config.SONARQUBE_ENV ?: 'sonar-java',
                        script.env.SONAR_AUTH_TOKEN
                    )
                }
            }

            script.stage('SonarQube Analysis - rating-service') {
                script.withCredentials([script.string(
                    credentialsId: config.SONARQ_CRED ?: 'anitha-sonar',
                    variable: 'SONAR_AUTH_TOKEN'
                )]) {
                    MavenUtils.sonarAnalysis(
                        script,
                        'spring-minikube/rating-service',
                        'rating-service',
                        config.SONARQUBE_ENV ?: 'sonar-java',
                        script.env.SONAR_AUTH_TOKEN
                    )
                }
            }

            // Build & Push Docker Images
            script.stage('Build & Push Docker Images') {
                DockerUtils.buildPush(script, 'student-service', 'spring-minikube/student-service')
                DockerUtils.buildPush(script, 'rating-service', 'spring-minikube/rating-service')
            }

            // Start Minikube
            script.stage('Start Minikube') {
                MinikubeUtils.start(script)
            }

            // Apply Kubernetes configs
            script.stage('Setup K8s Configs') {
                MinikubeUtils.applyConfigs(script)
            }

            // Deploy services
            script.stage('Deploy Services') {
                MinikubeUtils.deployServices(script)
            }

            // DAST - OWASP ZAP Scan
            script.stage('DAST - OWASP ZAP Scan') {
                SecurityUtils.dastScan(script)
            }

            script.echo "Pipeline Completed Successfully!"
        }
    }
}
