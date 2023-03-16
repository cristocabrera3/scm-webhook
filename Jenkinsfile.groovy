pipeline {
    agent any

    environment {
        AWS_REGION = 'us-east-1'
        GITHUB_REPO_URL = 'https://github.com/cristocabrera3/jenkins-lambda.git'
        STACK_NAME = 'my-stack'
        BUCKET_NAME = 'myuniquebucket16032026'
    }

    stages {
        stage('Checkout') {
            steps {
                git branch: 'master', url: env.GITHUB_REPO_URL
            }
        }
        
        stage('Build') {
            steps {
                bat '"C:\\Program Files\\Git\\bin\\bash.exe" -c "mkdir python"'
                bat '"C:\\Program Files\\Git\\bin\\bash.exe" -c "cp lambda_function.py python/lambda_function.py"'
                zip zipFile: 'python.zip', archive: false, dir: 'python'
                archiveArtifacts artifacts: 'python.zip', fingerprint: true
            }
        }

        stage('Create Bucket') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'cloud_user']]) {
                    bat '"C:\\Program Files\\Amazon\\AWSCLIV2\\aws" s3api create-bucket --bucket %BUCKET_NAME% --region %AWS_REGION%'
                }
            }
        }
        stage('Upload to S3') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'cloud_user']]) {
                    bat '"C:\\Program Files\\Amazon\\AWSCLIV2\\aws" s3 cp python.zip s3://%BUCKET_NAME%/'
                }
            }
        }

        stage('Deploy') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'cloud_user']]) {
                    bat '"C:\\Program Files\\Amazon\\AWSCLIV2\\aws" cloudformation deploy --region %AWS_REGION% --template-file template.yaml --stack-name %STACK_NAME% --parameter-overrides BucketName=${bucketName} --capabilities CAPABILITY_IAM'
                }
            }
        }
        
        stage('Cleaning up') {
            steps {
                bat '"C:\\Program Files\\Git\\bin\\bash.exe" -c "rm -r python"'
                bat '"C:\\Program Files\\Git\\bin\\bash.exe" -c "rm python.zip"'
            }
        }
        // stage('Test1') {
        //     steps {
        //         script {
        //             def validateTemplate = sh(script: '"C:\\Program Files\\Amazon\\AWSCLIV2\\aws" cloudformation validate-template --template-body file://template.yaml --region %AWS_REGION%', returnStatus: true)

        //             if (validateTemplate == 0) {
        //                 echo "CloudFormation template is valid"
        //             } else {
        //                 error "CloudFormation template is invalid"
        //             }
        //         }
        //     }
        // }
    }
}