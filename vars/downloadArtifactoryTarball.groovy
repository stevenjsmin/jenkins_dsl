def call(body) {
    // evaluate the body block and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // Download specs for Artifactory
    def downloadSpec= """{
    "files": [
    {
        "pattern": "${env.ARTIFACTORY_REPO}/${env.artifact_repo_path}/${env.artifact_file_name}",
        "target": "./"
    }]
    }
    """
    // Clean up
    sh """
        if [ -e "${env.artifact_repo_path}/${env.artifact_file_name}" ]; then
            echo "Removing old ${env.artifact_repo_path}/${env.artifact_file_name}"
            rm -rf "${env.artifact_repo_path}/${env.artifact_file_name}"
        fi
    """
    // Download the requested artifact from Artifactory
    config.artifactory.download spec: downloadSpec
    // Unarchive downloaded artifact
    sh """
        pwd
        ls -lt
        cd ${env.WORKSPACE}
        pwd
        ls -lt
        tar xvzf ${env.artifact_repo_path}/${env.artifact_file_name} -C .
    """
}
