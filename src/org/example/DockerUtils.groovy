package org.example

class DockerUtils implements Serializable {
    /**
     * Build and push docker image.
     * - script: pipeline script
     * - imageName: repository name (e.g. student-service)
     * - path: path to docker build context
     * - dockerUserOpt: optional docker username (if provided, will be used; otherwise credentials used)
     */
    static void buildPush(script, String imageName, String path, String dockerUserOpt = null) {
        if (dockerUserOpt) {
            // if docker user provided from pipeline config, prompt for password from credentials and use it
            script.withCredentials([script.usernamePassword(credentialsId: 'dockerhub-creds',
                                                            usernameVariable: 'DOCKER_USER_CRED',
                                                            passwordVariable: 'DOCKER_PASS')]) {
                // if dockerUserOpt differs from credential username, still login with credential username (safer)
                script.sh """
                echo \$DOCKER_PASS | docker login -u \$DOCKER_USER_CRED --password-stdin
                docker build -t ${dockerUserOpt}/${imageName}:1.0 ${path}
                docker push ${dockerUserOpt}/${imageName}:1.0
                """
            }
        } else {
            // fallback: use username from credentials as repo owner
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
}
