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

    static void deployServices(script) {
        script.sh """
        minikube kubectl -- apply -f spring-minikube/devops/deployment-student.yml
        minikube kubectl -- apply -f spring-minikube/devops/service-student.yml
        minikube kubectl -- apply -f spring-minikube/devops/deployment-rating.yml
        minikube kubectl -- apply -f spring-minikube/devops/service-rating.yml
        """
    }
}
