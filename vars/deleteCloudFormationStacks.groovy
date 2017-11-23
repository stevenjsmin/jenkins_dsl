def call(body) {
    // evaluate the body block and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.stacks_query_contains) {
        config.stacks_query_contains = " && ${config.stacks_query_contains}"
    } else {
        config.stacks_query_contains = ""
    }

    sh """
    set -e

    die()
    {
        echo "*ERROR* \$@" >&2
        exit 1
    }

    export AWS_DEFAULT_REGION=${config.region}
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
        export AWS_DEFAULT_REGION=${config.region}
    }<<EOF_ASSUME_ROLE
`aws sts assume-role --role-arn ${config.role_arn} --role-session-name "Provision-\$\$" --output text --query "Credentials.[SecretAccessKey,SessionToken,AccessKeyId]"`
EOF_ASSUME_ROLE
    set -x

    MATCHING_STACKS=
    for STACK_NAME in ${config.stack_names_match}; do
      MATCHING_STACKS="\${MATCHING_STACKS}\${MATCHING_STACKS:+
}\$(aws cloudformation describe-stacks --query 'Stacks[?contains(StackName, `'"\$STACK_NAME"'`)${config.stacks_query_contains}].StackName' --output text | tr '\\t' '\\n')"
    done

    # Remove duplicate entries
    MATCHING_STACKS=`echo "\$MATCHING_STACKS" | awk '!a[\$0]++'`

    echo "Found the following stacks, about to delete:"
    echo "\$MATCHING_STACKS"

    for stack in \$MATCHING_STACKS; do
        ansible -i localhost, -c local localhost -m cloudformation -a "stack_name=\${stack} state=absent region=${config.region}"
    done
    """
}
