script.stage('SonarQube Analysis') {
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

        MavenUtils.sonarAnalysis(
            script,
            'spring-minikube/rating-service',
            'rating-service',
            config.SONARQUBE_ENV ?: 'sonar-java',
            script.env.SONAR_AUTH_TOKEN
        )
    }
}
