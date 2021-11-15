pipeline{
    agent {
        label 'slave'
    }

    stages{
        stage('安卓程序源码同步') {
            steps {
            //多次运行加-P不报错
                sh 'mkdir -p AndroidSampleApp'
                //类似checkout AndroidSampleApp
                dir("AndroidSampleApp"){
                    git branch:'master', url:'https://github.com/wuchao01/AndroidSampleApp.git'
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


        stage('安卓部署') {
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

        stage('自动测试程序源码同步') {
            steps {
                sh 'mkdir -p iAppBVT'
                dir("iAppBVT"){
                    git branch:'master', url:'https://github.com/wuchao01/iAppBVT.git'
                }
            }
        }

        stage('运行自动化测试') {
            steps {
                sh '''
                    . ~/.bash_profile

                    cd iAppBVT
                    mvn clean install
                '''
            }
        }
    }

    post {
        success {
        //得写项目的workspace相对路径，否则jenkins会报错没有权限
            archiveArtifacts artifacts: 'AndroidSampleApp/app/build/outputs/apk/debug/app-debug.apk'
        }
        //无论是否成功还是失败都会发送email
        always {
            emailext body: '$DEFAULT_CONTENT', recipientProviders: [[$class: 'RequesterRecipientProvider']], subject: '$DEFAULT_SUBJECT'
        }
    }
}