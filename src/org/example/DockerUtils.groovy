package org.example

class DockerUtils implements Serializable {
    static void buildPush(script, String imageName, String path) {
        script.withCredentials([script.usernamePassword(credentialsId: 'dockerhub-creds',
                                                        usernameVariable: 'DOCKER_USER',
                                                        passwordVariable: 'DOCKER_PASS')]) {
            script.sh """
            echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin
            docker build -t \$DOCKER_USER/${imageName}:1.0 ${path}
            docker push \$DOCKER_USER/${imageName}:1.0
            """
        }
    }
}
