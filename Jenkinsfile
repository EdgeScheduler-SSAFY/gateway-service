pipeline {
    agent any
        
    environment {
        imageName = "edgescheduler/backend"
        registryCredential = 'hyunsoo31'
        dockerImage = ''
        
        releaseServerAccount = 'ubuntu'
        releaseServerUri = 'k10s201.p.ssafy.io'
        releasePort = '8080'
    }
        
    stages {
        
        
        stage('build'){
            steps{
                script{
                    // dir('/') {
                    buildSpring()
                    // }
                }
            }
            post {
                success { 
                   echo 'Successfully Jar build'
                }
               	failure {
                   error 'Jar build is failed'
                }
			}
        }
        
        
        stage('deploy'){
            steps{
                script{
                    // dir('/') {
                    deploySpring()   
                    // }
                }
            }
        }
    }
}

def buildSpring() {
    // sh 'cp -r /var/jenkins_home/backend/env/env.yml /var/jenkins_home/workspace/dev-backend/backend/spring-api/src/main/resources/'
    // sh 'cp -r /var/jenkins_home/backend/env/env.yml /var/jenkins_home/workspace/dev-backend/backend/spring-api/src/test/resources/'
    // sh 'cp -r /var/jenkins_home/backend/env/application-oauth.yml /var/jenkins_home/workspace/dev-backend/backend/spring-api/src/main/resources/'
    // sh 'cp -r /var/jenkins_home/backend/env/application-oauth.yml /var/jenkins_home/workspace/dev-backend/backend/spring-api/src/test/resources/'
    sh 'chmod +x gradlew'
    sh './gradlew build -x test'
}

def deploySpring() {
    sh 'docker stop discovery-app || true'
    sh 'docker rm discovery-app || true '
    sh 'docker rmi backend-cloud-discovery-app || true'
    // sh 'docker stop spring-app && docker rm spring-app && docker rmi spring-api-spring-app'
    sh "docker compose up -d"
}
