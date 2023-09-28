    pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'registry-1.docker.io'
        DOCKER_REPO = 'hello-world'
        DOCKER_CREDENTIAL_ID = 'DOCKER_CREDENTIALS'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        BRANCH_NAME = "${env.BRANCH_NAME}"
        COMMIT_SHA = "${env.GIT_COMMIT}"
        VERSION_FILE = 'version.txt'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Docker Login') {
             steps {
                withCredentials([usernamePassword(credentialsId: DOCKER_CREDENTIAL_ID, passwordVariable: 'DOCKER_PASSWORD', usernameVariable: 'DOCKER_USERNAME')]) {
                    sh """
                        docker login -u \${DOCKER_USERNAME} -p \${DOCKER_PASSWORD}
                    """
                }
            }
        }

         stage('Determine Tag') {
            steps {
                script {
                    def branchName = env.BRANCH_NAME
                    def version = readFile(env.VERSION_FILE).trim()
                    def tag

                    if (branchName == 'master') {
                        tag = "latest-${version}"
                    } else if (branchName.startsWith('pull/')) {
                        tag = "${branchName.replaceAll('/', '-').toLowerCase()}-pr-${version}"
                    } else {
                        tag = "build-${env.BUILD_NUMBER}-${version}"
                    }

                    echo "Using Docker tag: ${tag}"
                    env.DOCKER_TAG = tag
                }
            }
        }

         stage('Build and Push Docker Image') {
            steps {
                script {
                    def dockerImage = "${DOCKER_REGISTRY}/${env.DOCKER_REPO}:${env.DOCKER_TAG}"
                    sh "docker build --build-arg VERSION=${env.DOCKER_TAG} -t ${dockerImage} ."
                    sh "docker push ${dockerImage}"
                    sh "docker rmi ${dockerImage}"
                    }
                }
            }
        }

    stage('Test Docker Image') {
    steps {
        script {
            def dockerImage = "${DOCKER_REGISTRY}/${env.DOCKER_REPO}:${env.DOCKER_TAG}"
            def containerName = "test-${env.DOCKER_TAG}"
            
            try {
                def exitCode = docker.image(dockerImage).withRun("-d --name ${containerName}") { c ->
                    sleep(30)
                    sh "exit 1"
                }

                if (exitCode != 0) {
                    error "Test failed inside the Docker container."
                }
            } finally {
                sh "docker rm -f ${containerName}"
            }
        }
    }
}
}
}
