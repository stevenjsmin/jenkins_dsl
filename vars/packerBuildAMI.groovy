def call(body) {
    // evaluate the body block and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    // Run the packer build
    // ansiColor('xterm')
    wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
        withCredentials([
            usernamePassword(
                credentialsId: 'artifactory_esg_build_creds',
                passwordVariable: 'artifactory_password',
                usernameVariable: 'artifactory_username')]) {
        sh """
        set -o pipefail
        set +x
        {
            [ \$? -eq 0 ] || die "Unable to assume role '${config.role_arn}'"
            AWS_ROLE=`cat -`
            set -- \$AWS_ROLE
            export AWS_SECRET_ACCESS_KEY="\$1"
            export AWS_SESSION_TOKEN="\$2"
            export AWS_SECURITY_TOKEN="\$2"
            export EC2_SECURITY_TOKEN="\$2"
            export AWS_ACCESS_KEY_ID="\$3"
        }<<EOF_ASSUME_ROLE
`aws sts assume-role --role-arn ${config.role_arn} --role-session-name "Provision-\$\$" --output text --query "Credentials.[SecretAccessKey,SessionToken,AccessKeyId]"`
EOF_ASSUME_ROLE
        set -x

        #BaseAMI - HIP AMI
        BASE_AMI=\$(aws ec2 describe-images --image-ids ${env.common_ami} --owners self --region ${config.region} --query 'Images[0].Tags[?Key == `BaseAMI`].Value' --output text)
        HIP_AMI=\${BASE_AMI}

        packer build --only=${config.packer_only} \
          -var-file=${config.packer_var_file} \
          -var ${config.hip_ami_var}=\$HIP_AMI \
          -var artifactory_username="${artifactory_username}" \
          -var artifactory_password="${artifactory_password}" \
          -var artifactory_esg_repo="${env.ARTIFACTORY_REPO}" \
          -var artifactory_esg_env_repo="${env.ARTIFACTORY_ENV_REPO}" \
          -var artifactory_host="artifactory.aus.thenational.com" \
          ${config.packer_template} | tee packer_output.txt

        grep "${config.region}: ami-" packer_output.txt  #do not remove - for error-checking

        # Write ID
        grep '${config.region}: ami-[a-f|0-9]*' packer_output.txt | sed 's/.*: \\(ami-[a-f|0-9]*\\)/\\1/' > ami_id.txt

        # Refresh AMI list
        aws ec2 describe-images --owners self --region ${config.region} --owner self --output json > "${env.WORKSPACE}/api${env.AWS_CONF}_amis.json"

        """
        }
        env.packer_built_ami = readFile "${env.WORKSPACE}/ami_id.txt"
        update_ami_list_job = "Update_AMIs_API_Accounts"
        if (config.awsAccount)
        {
            if(config.awsAccount == "hip_shared_account")
                update_ami_list_job = "Update_AMIs_SC_Accounts"
        }
        build job: update_ami_list_job
        # Test
    }
}
