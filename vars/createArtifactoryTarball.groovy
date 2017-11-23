def call(body) {
    // evaluate the body block and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def tar_options = ""
    if ( config.tar_options ) {
        tar_options = config.tar_options
    }

    // deleteDir()
    // git url: "${config.git_url}", branch: "${env.branch}"
    retry(4)
    {
        checkout([
                $class: 'GitSCM',
                branches: [[name: "*/${env.branch}"]],
                userRemoteConfigs: [[url: "${config.git_url}"]],
                extensions: [[ $class: 'WipeWorkspace' ]]
        ])
    }
    def uploadSpec = """{
    "files": [
    {
        "pattern": "${env.artifact_file_name}",
        "target": "${env.artifact_target}"
    }]
    }
    """
    sh """
    cd ${env.WORKSPACE}
    echo "BRANCH_NAME=${env.branch_name}" > ${env.WORKSPACE}/branch_name.txt
    tar -czvf ${env.artifact_file_name} ${tar_options} *
    echo ${env.artifact_repo_path} > artifact_version.properties
    """
    def buildInfo = config.artifactory.upload spec: uploadSpec
    config.artifactory.publishBuildInfo buildInfo
    archiveArtifacts 'artifact_version.properties'
    sh """
    cd ${env.WORKSPACE}
    if [ -e "${env.artifact_file_name}" ]; then
        echo "Deleting the '${env.artifact_file_name}'"
        rm -f "${env.artifact_file_name}"
    fi
    """

}
