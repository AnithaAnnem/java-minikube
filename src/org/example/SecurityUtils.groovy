package org.example

class SecurityUtils implements Serializable {
    static void gitleaksScan(script) {
        script.sh """
        echo "Running Gitleaks..."
        if ! command -v gitleaks &> /dev/null; then
            wget https://github.com/gitleaks/gitleaks/releases/download/v8.18.2/gitleaks_8.18.2_linux_x64.tar.gz
            tar -xvf gitleaks_8.18.2_linux_x64.tar.gz
            mkdir -p \$WORKSPACE/bin
            mv gitleaks \$WORKSPACE/bin/
            export PATH=\$WORKSPACE/bin:\$PATH
        fi
        gitleaks detect --source . --report-path gitleaks-report.json || true
        """
        script.archiveArtifacts artifacts: 'gitleaks-report.json', allowEmptyArchive: true
    }

    static void dependencyCheck(script) {
        script.sh """
        mvn org.owasp:dependency-check-maven:check -f spring-minikube/student-service/pom.xml -DfailBuildOnCVSS=11 || true
        mvn org.owasp:dependency-check-maven:check -f spring-minikube/rating-service/pom.xml -DfailBuildOnCVSS=11 || true
        """
        script.archiveArtifacts artifacts: 'spring-minikube/**/target/dependency-check-report.html', allowEmptyArchive: true
    }

    static void dastScan(script) {
        script.sh """
        echo "Running OWASP ZAP DAST Scan..."
        MINIKUBE_IP=\$(minikube ip)
        TARGET_STUDENT="http://\$MINIKUBE_IP:30001/student"
        TARGET_RATING="http://\$MINIKUBE_IP:30002/rating"

        docker run --rm -v \$WORKSPACE:/zap/wrk/:rw owasp/zap2docker-stable zap-baseline.py -t \$TARGET_STUDENT -r zap-student-report.html -J zap-student-report.json || true
        docker run --rm -v \$WORKSPACE:/zap/wrk/:rw owasp/zap2docker-stable zap-baseline.py -t \$TARGET_RATING -r zap-rating-report.html -J zap-rating-report.json || true
        """
        script.archiveArtifacts artifacts: 'zap-*-report.*', allowEmptyArchive: true
        script.publishHTML([allowMissing: false, alwaysLinkToLastBuild: true, keepAll: true,
            reportDir: '.', reportFiles: 'zap-*-report.html', reportName: 'OWASP ZAP Report'])
    }
}
