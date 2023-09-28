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
        CLUSTER_NAME = 'extravagant-outfit-1695878394'
        CLUSTER_REGION = 'ap-south-1'
        AWS_CREDENTIALS_ID = 'AWS_CREDENTIALS'
        ARGOCD_CREDENTIALS_ID = 'ARGOCD_CREDENTIALS'
        ARGOCD_SERVER="ac61c769c232b476182346c67c7e43e9-485145539.ap-south-1.elb.amazonaws.com"
        HELM_REPO="chartmuseum"
        HELM_URL="http://af363009483764474abc47c611b6cf3f-1954008745.ap-south-1.elb.amazonaws.com:8080"
        APP_NAME="hello-world-app"
        GITHUB_CREDENTIALS_ID = 'GITKEYS'
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

        stage('Create the Build Tag') {
            steps {
                script {
                    def branchName = env.BRANCH_NAME
                    def buildNumber = env.BUILD_NUMBER
                    def sprintNumberFile = new File("${env.WORKSPACE}/sprint.txt")
                    def sprintNumber = sprintNumberFile.text
                    def sprintYearFile = new File("${env.WORKSPACE}/sprint_year.txt")
                    def sprintYear = sprintYearFile.text
                    def buildTag = "pr-${sprintYear}.${sprintNumber}.${buildNumber}"
                    if (branchName == 'main') {
                        buildTag = "${sprintYear}.${sprintNumber}.${buildNumber}"
                    }
                    if (branchName.startsWith('release/')) {
                        def releaseVersion=branchName.spilt('/')[1]
                        buildTag = "${releaseVersion}.${sprintYear}.${buildNumber}"
                    }
                    env.BUILD_TAG = buildTag
                    env.BUILD_TAG_WITHOUT_PR = buildTag.replace('pr-', '')
                    echo "BUILD_TAG: ${env.BUILD_TAG}"
                    echo "BUILD_TAG_WITHOUT_PR: ${env.BUILD_TAG_WITHOUT_PR}"
                    currentBuild.displayName = "#${buildTag}"
                }
            }
        }
        stage('Build and Push Docker Image') {
            steps {
                script {
                    def dockerImage = "${DOCKER_REGISTRY}/${env.DOCKER_REPO}:${env.BUILD_TAG}"
                    sh "docker build --build-arg VERSION=${env.BUILD_TAG} -t ${dockerImage} ."
                    sh "docker push ${dockerImage}"
                    sh "docker rmi ${dockerImage}"
                    }
                }
            }
    stage('Test Docker Image') {
    steps {
        script {
            def dockerImage = "${DOCKER_REGISTRY}/${env.DOCKER_REPO}:${env.BUILD_TAG}"
            def containerName = "test-${env.BUILD_TAG}"
            
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
            sh "apt-get update"
            sh "apt-get install curl unzip jq -y"
        }
    }
    }
    stage('Install AWS CLI') {
        steps {
            sh """
            if [ ! -f /usr/local/bin/aws ];
            then
                echo "Installing AWS CLI"
                curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
                unzip awscliv2.zip
                yes | ./aws/install
                aws --version
            fi
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
            """
        }
    }
    stage("Get the AWS Credentials"){
        steps {
            withCredentials([usernamePassword(credentialsId: AWS_CREDENTIALS_ID, passwordVariable: 'AWS_SECRET_KEY', usernameVariable: 'AWS_ACCESS_KEY')]){
                sh """
                aws configure set aws_access_key_id \${AWS_ACCESS_KEY}
                aws configure set aws_secret_access_key \${AWS_SECRET_KEY}
                aws configure list
                """
            }
    }
    }
    stage("Connect to EKS cluster"){
        steps {
            sh """
            eksctl utils write-kubeconfig --cluster=${CLUSTER_NAME} --region=${CLUSTER_REGION}
            kubectl get nodes
            """
        }
    }
    stage("Login to ArgoCD"){
        steps {
            withCredentials([usernamePassword(credentialsId: ARGOCD_CREDENTIALS_ID, passwordVariable: 'ARGOCD_PASSWORD', usernameVariable: 'ARGOCD_USERNAME')]) {
                sh """
                argocd login \${ARGOCD_SERVER} --username \${ARGOCD_USERNAME} --password \${ARGOCD_PASSWORD} --insecure
                """
            }
        }
    }
    stage("Install helm push"){
        steps {
            sh "helm plugin install https://github.com/chartmuseum/helm-push"
        }
    }
    stage("Build and Push Helm Charts"){
            steps{
                sh "helm repo add ${HELM_REPO} ${HELM_URL}"
                sh "helm repo update"
                sh "sed -i 's/#VERSION#/${BUILD_TAG_WITHOUT_PR}/g' charts/${APP_NAME}/Chart.yaml"
                sh "sed -i 's/#APP_VERSION#/${BUILD_TAG}/g' charts/${APP_NAME}/Chart.yaml"
                sh "helm dependency update charts/${APP_NAME}"
                sh "helm lint charts/${APP_NAME}"
                sh "helm cm-push charts/${APP_NAME} ${HELM_REPO}"
                sh "helm repo update"
                sh "helm search repo ${APP_NAME} --versions | grep ${BUILD_TAG_WITHOUT_PR}"
            }
            post {
                always {
                    cleanWs()
                }
            }
        }
        stage('Deploy to cluster') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: GITHUB_CREDENTIALS_ID, keyFileVariable: 'GITKEYS')]) {
                sh 'git clone git@github.com/jilanisayyad/gitops-deployments.git'
                sh 'cd gitops-deployments && git checkout main'
                sh 'sed -i "s/\\(targetRevision:\\) .*/\\1 ${BUILD_TAG_WITHOUT_PR}/" {APP_NAME}/application.yaml'
                sh 'git add .'
                sh 'git commit -m "Deploy ${BUILD_TAG_WITHOUT_PR} to ${CLUSTER_NAME}"'
                sh 'GIT_SSH_COMMAND="ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no -i ${GITKEYS}" git push origin main'
                sh 'kubectl apply -f default'
                sh 'kubectl apply -f ${APP_NAME}/helm'
                }
            }
            post {
                always {
                    cleanWs()
                }
            }
        }
}
}
