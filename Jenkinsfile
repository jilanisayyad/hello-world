pipeline {
    agent any
    
    environment {
        DOCKER_REPO = 'hello-world'
        BUILD_NUMBER = "${env.BUILD_NUMBER}"
        BRANCH_NAME = "${env.BRANCH_NAME}"
        COMMIT_SHA = "${env.GIT_COMMIT}"
    }
    
    stages {
        stage('Docker Login') {
            steps {
                sh 'docker login -u harsh7012@gmail.com -p Harsh7012@gmail.com'
            }
        }

        stage('Tag Docker Image') {
            when {
                expression { BRANCH_NAME == 'master' }
            }
            steps {
                script {
                    sh "docker tag ${DOCKER_REPO}:latest ${DOCKER_REPO}:${BUILD_NUMBER}"
                }
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    def dockerTag = ''

                    if (BRANCH_NAME == 'master') {
                        dockerTag = "latest"
                    } else if (BRANCH_NAME.startsWith('PR-')) {
                        dockerTag = "pr-${BRANCH_NAME.substring(3)}-${BUILD_NUMBER}_pr"
                    } else {
                        dockerTag = "${BRANCH_NAME}-${BUILD_NUMBER}"
                    }

                    sh "docker build -t ${DOCKER_REPO}:${dockerTag} ."
                }
            }
        }
        
        stage('Push Docker Image') {
            steps {
                script {
                    def dockerTag = ''

                    if (BRANCH_NAME == 'master') {
                        dockerTag = "latest"
                    } else if (BRANCH_NAME.startsWith('PR-')) {
                        dockerTag = "pr-${BRANCH_NAME.substring(3)}-${BUILD_NUMBER}_pr"
                    } else {
                        dockerTag = "${BRANCH_NAME}-${BUILD_NUMBER}"
                    }

                    sh "docker push ${DOCKER_REPO}:${dockerTag}"
                }
            }
        }
        
        stage('Test Docker Image') {
            when {
                expression { BRANCH_NAME != 'master' && !BRANCH_NAME.startsWith('PR-') }
            }
            steps {
                script {
                    def dockerTag = "${BRANCH_NAME}-${BUILD_NUMBER}"

                    sh "docker run --rm ${DOCKER_REPO}:${dockerTag} <test_command>"
                }
            }
        }
    }
    
    post {
        success {
            cleanupDockerImages()
        }
    }
}

def cleanupDockerImages() {
    script {
        def dockerTag = ''

        if (BRANCH_NAME == 'master') {
            dockerTag = "latest"
        } else if (BRANCH_NAME.startsWith('PR-')) {
            dockerTag = "pr-${BRANCH_NAME.substring(3)}-${BUILD_NUMBER}_pr"
        } else {
            dockerTag = "${BRANCH_NAME}-${BUILD_NUMBER}"
        }

        def images = sh(script: "docker images --format {{.Repository}}:{{.Tag}}", returnStdout: true).trim()
        def imagesList = images.split('\n')

        for (def image in imagesList) {
            if (image != "${DOCKER_REPO}:${dockerTag}" && !image.endsWith(":latest")) {
                sh "docker rmi -f ${image}"
            }
        }
    }
}
