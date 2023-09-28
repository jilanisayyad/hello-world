    pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'jilani1'
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

                    if (branchName == 'main') {
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
    stage('Test Docker Image') {
    steps {
        script {
            def dockerImage = "${DOCKER_REGISTRY}/${env.DOCKER_REPO}:${env.DOCKER_TAG}"
            def containerName = "test-${env.DOCKER_TAG}"
            
            try {
                sh "docker pull ${dockerImage}"
                sh "docker run -d --name ${containerName} ${dockerImage}"
                sh "docker ps -a --filter \"name=${containerName}\" --filter \"status=running\" --format \"{{.Names}}\" | grep ${containerName}"
                sh "docker stop ${containerName}"
                sh "docker rm ${containerName}"
            } catch (err) {
                sh "docker stop ${containerName}"
                sh "docker rm ${containerName}"
                throw err
            }
        }
    }
    }
    stage('Install Apt Packages') {
    steps {
        script {
            sh "sudo apt-get update"
            sh "sudo apt-get install curl unzip jq -y"
        }
    }
    }
    stage('Install AWS CLI') {
        steps {
            sh """
            curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
            unzip awscliv2.zip
            ./aws/install
            aws --version
            """
            sh """
            curl -LO https://github.com/eksctl-io/eksctl/releases/download/v0.159.0/eksctl_Linux_amd64.tar.gz
            tar -xzvf eksctl_Linux_amd64.tar.gz
            chmod +x eksctl
            mv eksctl /usr/local/bin
            """
        }
    }
    stage('Install kubectl and HELM'){
        steps {
            sh """
            curl -LO https://storage.googleapis.com/kubernetes-release/release/v1.25.0/bin/linux/amd64/kubectl
            chmod +x ./kubectl
            mv ./kubectl /usr/local/bin/kubectl
            kubectl version --client
            """
            sh """
            curl -LO https://get.helm.sh/helm-v3.13.0-linux-amd64.tar.gz
            tar -zxvf helm-v3.13.0-linux-amd64.tar.gz
            chmod +x linux-amd64/helm
            mv linux-amd64/helm /usr/local/bin/helm
            helm version
            """
        }
    }
    stage("Install ArgoCD CLI"){
        steps {
            sh """
            curl -LO https://github.com/argoproj/argo-cd/releases/download/v2.8.4/argocd-linux-amd64
            chmod +x argocd-linux-amd64
            mv argocd-linux-amd64 /usr/local/bin/argocd
            argocd version
            """
    }
    }
}
