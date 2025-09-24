package org.example

class Pipeline implements Serializable {
    def script

    Pipeline(script) { this.script = script }

    void run(Map config = [:]) {
        script.pipeline {
            agent any

            parameters {
                script.string(name: 'GIT_URL', defaultValue: config.GIT_URL ?: 'https://github.com/AnithaAnnem/java-minikube.git', description: 'Git repository URL')
                script.string(name: 'BRANCH', defaultValue: config.BRANCH ?: 'main', description: 'Branch to build')
            }

            environment {
                SONARQUBE_ENV = config.SONARQUBE_ENV ?: 'sonar-java'
                SONAR_AUTH_TOKEN = script.credentials(config.SONARQ_CRED ?: 'anitha-sonar')
                MINIKUBE_HOME = config.MINIKUBE_HOME ?: '/var/lib/jenkins/.minikube'
                HOME = config.HOME ?: '/var/lib/jenkins'
                DOCKERHUB_USER = config.DOCKER_USER ?: 'anithaannem'
            }

            stages {
                stage('Checkout') { script.steps { GitUtils.checkout(script, script.params.BRANCH, script.params.GIT_URL) } }
                stage('Credential Scanning - Gitleaks') { script.steps { SecurityUtils.gitleaksScan(script) } }
                stage('Package Services') {
                    script.steps {
                        MavenUtils.packageService(script, 'spring-minikube/student-service')
                        MavenUtils.packageService(script, 'spring-minikube/rating-service')
                    }
                }
                stage('Unit Tests') {
                    script.steps {
                        MavenUtils.runTests(script, 'spring-minikube/student-service')
                        MavenUtils.runTests(script, 'spring-minikube/rating-service')
                    }
                }
                stage('Dependency Scanning') { script.steps { SecurityUtils.dependencyCheck(script) } }
                stage('SonarQube Analysis') {
                    script.steps {
                        MavenUtils.sonarAnalysis(script, 'spring-minikube/student-service', 'student-service', script.SONARQUBE_ENV, script.SONAR_AUTH_TOKEN)
                        MavenUtils.sonarAnalysis(script, 'spring-minikube/rating-service', 'rating-service', script.SONARQUBE_ENV, script.SONAR_AUTH_TOKEN)
                    }
                }
                stage('Build & Push Docker Images') {
                    script.steps {
                        DockerUtils.buildPush(script, 'student-service', 'spring-minikube/student-service')
                        DockerUtils.buildPush(script, 'rating-service', 'spring-minikube/rating-service')
                    }
                }
                stage('Start Minikube') { script.steps { MinikubeUtils.start(script) } }
                stage('Setup K8s Configs') { script.steps { MinikubeUtils.applyConfigs(script) } }
                stage('Deploy Services') { script.steps { MinikubeUtils.deployServices(script) } }
                stage('DAST - OWASP ZAP Scan') { script.steps { SecurityUtils.dastScan(script) } }
            }

            post {
                success { script.echo "Pipeline Successful: Build, Analysis, Docker Push & Deployment Completed!" }
                failure { script.echo "Pipeline Failed!" }
            }
        }
    }
}
