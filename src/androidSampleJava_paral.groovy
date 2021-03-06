pipeline{
    agent {
        label 'slave'
    }

    stages{
        stage('获取源码') {
            //并行拉取代码，节约资源
            parallel {
                stage('安卓程序源码') {
                    steps {
                        sh 'mkdir -p AndroidSampleApp'
                        dir("AndroidSampleApp"){
                            git branch:'master', url:'https://github.com/wuchao01/AndroidSampleApp.git'
                        }
                    }
                }

                stage('自动测试程序源码') {
                    steps {
                        sh 'mkdir -p iAppBVT'
                        dir("iAppBVT"){
                            git branch:'master', url:'https://github.com/wuchao01/iAppBVT.git'
                        }
                    }
                }
            }
        }

        stage('安卓编译打包') {
            steps {
                sh '''
                    . ~/.bash_profile
                    cd AndroidSampleApp
                    sh gradlew clean assembleDebug
                '''
            }
        }

        stage('测试与发布') {
            parallel {
                stage('发布测试包') {
                    steps {
                        archiveArtifacts artifacts: 'AndroidSampleApp/app/build/outputs/apk/debug/app-debug.apk'
                    }
                }

                stage('自动化'){
                    stages{
                        stage('部署') {
                            steps {
                                sh '''
                                    . ~/.bash_profile
                                    cd AndroidSampleApp
                                    apk=app/build/outputs/apk/debug/app-debug.apk
                                    {
                                        #try: 卸载现有的安卓app
                                        adb uninstall com.appsflyer.androidsampleapp
                                    } || {
                                        #catch
                                        echo 'no com.appsflyer.androidsampleapp package'
                                    }
                                    sleep 5

                                    #安装安卓app
                                    adb install $apk
                                '''
                            }
                        }

                        stage('自动测试') {
                            steps {
                                sh '''
                                    . ~/.bash_profile

                                    cd iAppBVT
                                    mvn clean install
                                '''
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            emailext body: '$DEFAULT_CONTENT', recipientProviders: [[$class: 'RequesterRecipientProvider']], subject: '$DEFAULT_SUBJECT'
        }
    }
}