package org.example

class MinikubeUtils implements Serializable {
    static void start(script) {
        script.sh "minikube delete || true"
        script.sh "minikube start --driver=docker"
        script.sh "minikube status"
    }

    static void applyConfigs(script) {
        script.sh """
        minikube kubectl -- apply -f spring-minikube/devops/configmap.yml || true
        minikube kubectl -- apply -f spring-minikube/devops/secret.yml || true
        minikube kubectl -- apply -f spring-minikube/devops/ingress.yml || true
        """
    }

    // NEW: deploy DB manifests (apply PVC + deployments + services)
    static void deployDatabases(script) {
        script.echo "Deploying databases..."
        script.sh """
        minikube kubectl -- apply -f spring-minikube/devops/student-db.yml
        minikube kubectl -- apply -f spring-minikube/devops/rating-db.yml
        """

        // Wait for DB pods to become ready (timeout 120s each)
        script.sh '''
        echo "Waiting for student DB pod to be ready..."
        minikube kubectl -- wait --for=condition=ready pod -l app=student-mysql --timeout=120s || true
        echo "Waiting for rating DB pod to be ready..."
        minikube kubectl -- wait --for=condition=ready pod -l app=rating-mysql --timeout=120s || true
        '''
    }

    // Deploy app manifests (deployments + services)
    static void deployServices(script) {
        script.echo "Deploying applications..."
        script.sh """
        minikube kubectl -- apply -f spring-minikube/devops/deployment-student.yml
        minikube kubectl -- apply -f spring-minikube/devops/service-student.yml
        minikube kubectl -- apply -f spring-minikube/devops/deployment-rating.yml
        minikube kubectl -- apply -f spring-minikube/devops/service-rating.yml
        """
        // Optionally wait for app pods
        script.sh '''
        minikube kubectl -- wait --for=condition=ready pod -l run=student-service --timeout=120s || true
        minikube kubectl -- wait --for=condition=ready pod -l run=rating-service --timeout=120s || true
        '''
    }

    static void deployIngress(script) {
        script.echo "Applying ingress (if present)..."
        script.sh "minikube kubectl -- apply -f spring-minikube/devops/ingress.yml || true"
    }
}
