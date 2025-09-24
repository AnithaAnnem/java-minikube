package org.example

class GitUtils implements Serializable {
    static void checkout(script, String branch, String url) {
        script.checkout([$class: 'GitSCM', branches: [[name: branch]], userRemoteConfigs: [[url: url]]])
    }
}
