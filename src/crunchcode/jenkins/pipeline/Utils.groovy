package crunchcode.jenkins.pipeline;

import groovy.json.JsonSlurper
import java.text.*

class Utils {

    //def responseText = ""

    static def getGitBranches(gitURL) {

        def command = "git ls-remote --heads ${gitURL}"

        def branches = []
        // File debug_file = new File("/tmp/getGitBranches.txt")
        // debug_file << "DEBUG 0: ${command}\n"

        try {
            def proc = command.execute()
            proc.waitFor()
            for (int i = 0; proc.exitValue() != 0; i++) {
                // debug_file << "Error:: Inside Far loop, ${proc.err.text} and iteration # ${i}\n"
                sleep(3000);
                proc = command.execute()
                proc.waitFor()
            }

            branches = proc.in.text.readLines()
            // debug_file << "DEBUG 1: branches: ${branches}\n"
            def no_refs = []
            for (branch in branches) {
                no_refs.add(branch.replaceAll(/[a-z0-9]*\trefs\/heads\//, ''))
            }
            branches = no_refs
            // collect method does not work in Jenkins pipeline!
            //branches = proc.in.text.readLines().collect {
            //    it.replaceAll(/[a-z0-9]*\trefs\/heads\//, '')
            //}

            branches = branches.indexOf("develop").with { idx ->
                if (idx > -1 && branches[idx].length() == 7) {
                    new ArrayList(branches).with { a ->
                        a.remove(idx)
                        a
                    }
                } else branches
            }
            // debug_file << "DEBUG 2: branches: ${branches}\n"

            branches.add(0, "develop")
            // debug_file << "DEBUG 3: branches: ${branches}\n"
            return branches
        } catch (Exception ex) {
            // Note that print statements will not work in Jenkins
            // scripted pipeline
            println "Exception:\n"
            println(ex.toString())
            // debug_file << "Exception::\n"
            //     debug_file << (ex.toString())
            //     debug_file << "\n"
            //     debug_file << (ex.getMessage())
            //     debug_file << "\n"
            //     debug_file << (ex.getStackTrace())
        }
        // debug_file << "DEBUG 4: branches: ${branches}\n"
        return branches

    }

    static
    def getArtifactAttributes(String server_url, String dirPath, String artifactPath, String repo, String username, String password, String propertyName) {
        //common function to connect to Enterprise Artifactory and call Artifactory REST API to get artifact attributes
        //File debug_file = new File("/data/jenkins_home/jobs/Retrieve_Pipeline_Libraries/workspace/getArtifactAttributes.txt")

        //debug_file << "DEBUG: dirPath=${dirPath}\n"
        //debug_file << "DEBUG: artifactPath=${artifactPath}\n"

        def artifactPathTokenized = artifactPath.split('/')
        //debug_file << "DEBUG: artifactPathTokenized=${artifactPathTokenized}\n"
        def artifactFullPath = artifactPath + "/" + artifactPathTokenized[1] + "-" + artifactPathTokenized[2] + ".tar.gz"

        //debug_file << "DEBUG: artifactFullPath=${artifactFullPath}\n"

        def address = server_url + "/api/storage/" + repo + "/" + artifactFullPath + "?properties=" + propertyName
        def HTTP_MAX_ATTEMPTS = 10
        def HTTP_SLEEP_TIME = 60000 // time is in milliseconds, 60 seconds.
        def HTTP_TIMEOUT = 7000 // time is in milliseconds, 7 seconds.

        def responseText = ""
        def authString = "${username}:${password}".getBytes().encodeBase64().toString()

        // DEBUG
        // debug_file << "DEBUG: removeString=${removeString}\n"
        // debug_file << "DEBUG: address=${address}\n"
        // debug_file << "DEBUG: authString=${authString}\n"

        try {
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("forwardproxy", 3128));
            //conn = address.toURL().openConnection(proxy)
            def conn = address.toURL().openConnection()
            conn.setConnectTimeout(HTTP_TIMEOUT)
            conn.setReadTimeout(HTTP_TIMEOUT)
            conn.setRequestProperty("Authorization", "Basic ${authString}")
            responseText = conn.content.text
            //debug_file << "DEBUG: responseText=${responseText}\n"
            //return responseText

        } catch (java.net.SocketTimeoutException ex) {
            //println "http timeout. sleeping until next http attempt"
            sleep HTTP_SLEEP_TIME
        }

        def tempResult = new JsonSlurper().parseText(responseText)
        def value
        def propertyFound = false
        //debug_file << "DEBUG: tempResult=${tempResult}\n"
        if (tempResult.properties) {
            //debug_file << "DEBUG: tempResult.hasProperty(properties)\n\n"
            tempResult.properties.each { child ->
                //debug_file << "DEBUG: child: ${child}\n"
                String childStr = child.toString()
                String propertyNameStr = propertyName.toString()
                int match = childStr.indexOf(propertyNameStr)
                //debug_file << "childStr.indexof(propertyNameStr)=${match}\n"
                //if( match > -1 )
                if (childStr.indexOf(propertyNameStr) > -1) {
                    //debug_file << "DEBUG: child.${propertyName} present\n"
                    value = childStr.split('=')[1]
                    value = value.replace("[", "")
                    value = value.replace("]", "")
                    value = value.replace(" ", "")
                    //debug_file << "DEBUG: child.${propertyName}: ${value}\n"
                    propertyFound = true
                    //return value  //doesnt work?
                }
            }
        }
        if (propertyFound)
            return value
        return "noPropertyFound"
    }

    static def getArtifactsData(String server_url, String dir_path, String repo, String username, String password) {
        //common function to connect to Enterprise Artifactory and call Artifactory REST API to get data
        // File debug_file = new File("/tmp/getArtifactsData.txt")

        def address = server_url + "/api/search/gavc?g=" + dir_path + "&repos=" + repo
        def HTTP_MAX_ATTEMPTS = 10
        def HTTP_SLEEP_TIME = 60000 // time is in milliseconds, 60 seconds.
        def HTTP_TIMEOUT = 7000 // time is in milliseconds, 7 seconds.

        def responseText = ""
        def authString = "${username}:${password}".getBytes().encodeBase64().toString()

        // DEBUG
        // debug_file << "DEBUG: removeString=${removeString}\n"
        // debug_file << "DEBUG: address=${address}\n"
        // debug_file << "DEBUG: authString=${authString}\n"

        try {
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("forwardproxy", 3128));
            //conn = address.toURL().openConnection(proxy)
            def conn = address.toURL().openConnection()
            conn.setConnectTimeout(HTTP_TIMEOUT)
            conn.setReadTimeout(HTTP_TIMEOUT)
            conn.setRequestProperty("Authorization", "Basic ${authString}")
            responseText = conn.content.text
            // debug_file << "DEBUG: responseText=${responseText}\n"
            //return responseText

        } catch (java.net.SocketTimeoutException ex) {
            //println "http timeout. sleeping until next http attempt"
            sleep HTTP_SLEEP_TIME
        }
    }

    static def getArtifactNames(String server_url, String dir_path, String repo, String username, String password) {
        // DEBUG
        File debug_file = new File("/tmp/getArtifactNames.txt")

        def removeString = server_url + "/api/storage/" + repo + "/"
        def result = []
        def newresult = []

        //call common function to connect to Enterprise Artifactory and call Artifactory REST API to get data
        //def responseText = getArtifactsData(server_url, dir_path, repo, username, password)

        def address = server_url + "/api/search/gavc?g=" + dir_path + "&repos=" + repo
        def HTTP_MAX_ATTEMPTS = 10
        def HTTP_SLEEP_TIME = 60000 // time is in milliseconds, 60 seconds.
        def HTTP_TIMEOUT = 7000 // time is in milliseconds, 7 seconds.

        def responseText = ""
        def authString = "${username}:${password}".getBytes().encodeBase64().toString()

        // DEBUG
        debug_file << "DEBUG: removeString=${removeString}\n"
        debug_file << "DEBUG: address=${address}\n"
        debug_file << "DEBUG: authString=${authString}\n"

        try {
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("forwardproxy", 3128));
            //conn = address.toURL().openConnection(proxy)
            def conn = address.toURL().openConnection()
            conn.setConnectTimeout(HTTP_TIMEOUT)
            conn.setReadTimeout(HTTP_TIMEOUT)
            conn.setRequestProperty("Authorization", "Basic ${authString}")
            responseText = conn.content.text
            debug_file << "DEBUG: responseText=${responseText}\n"

        } catch (java.net.SocketTimeoutException ex) {
            //println "http timeout. sleeping until next http attempt"
            sleep HTTP_SLEEP_TIME
        }

        def tempResult = new JsonSlurper().parseText(responseText)
        debug_file << "DEBUG: tempResult=${tempResult}\n"

        tempResult.results.each { child ->
            result.add(child.uri - removeString)
        }

        TreeMap resultMap = [:]
        debug_file << "DEBUG: result=${result}\n"

        def value = ""
        def tempList = []
        def key = ""
        result.each { child ->
            tempList = child.tokenize('/')
            key = tempList[0] + "/" + tempList[1]

            if (tempList[0].contains("filters")) {
                value = tempList[2].tokenize('.')[1]
            } else if (tempList[0] == "sdk") {
                value = tempList[2]
            } else {
                value = tempList[2]
            }
            value = value.isInteger() ? value.toInteger() : value

            if (resultMap.containsKey(key)) {
                resultMap[key].add(value)
            } else {
                resultMap[key] = [value]
            }
        }

        resultMap.each { k, v ->
            v = v.sort().unique().takeRight(20).reverse()

            v.each { child ->
                if (k.contains('filter')) {
                    newresult.add(k + "/" + "1." + child.toString())
                    println k
                } else {
                    newresult.add(k + "/" + child.toString())
                }

            }
        }
        debug_file << "DEBUG: newresult=${newresult}\n"

        newresult.add('None')

        return newresult

    }

    // Need to use @NonCPS due to the issue with using sort method with custom comparators
    // see this issue https://issues.jenkins-ci.org/browse/JENKINS-44924
    @NonCPS
    static
    def getEsgAmis(String searchParameter, String awsAccount, String branch, String amiFilePath, String serverRole) {

        def result = []
        def error = []
        def ami_file_path = ""
        def searchParam = ""
        def server_Role = ""

        // DEBUG
        // File debug_file = new File("/tmp/getEsgAmis.txt")
        // debug_file << "DEBUG: searchParameter=${searchParameter}\n"
        // debug_file << "DEBUG: awsAccount=${awsAccount}\n"
        // debug_file << "DEBUG: branch=${branch}\n"
        // debug_file << "DEBUG: amiFilePath=${amiFilePath}\n"
        // debug_file << "DEBUG: serverRole=${serverRole}\n"

        try {

            def branchType = "all"

            try {
                ami_file_path = amiFilePath
            } catch (MissingPropertyExceptionmpe) {
                ami_file_path = '/data/jenkins_home/jobs/AWS_Resource_Inventory/workspace/'
            }

            try {
                searchParam = searchParameter
            } catch (MissingPropertyExceptionmpe) {
                return ['searchParameter undefined']
            }

            try {
                server_Role = serverRole
                if (server_Role != '')
                    searchParam = searchParam + '-' + server_Role
            } catch (MissingPropertyExceptionmpe) {
                server_Role = ''
            }

            try {
                branchType = branch
            } catch (MissingPropertyExceptionmpe) {
                branchType = "all"
            }

            searchParam = searchParam.toLowerCase()

            def fileContents = new File(ami_file_path + awsAccount + '_amis.json').text

            def tags = ["BaseAMI", "CommonAMI", "Branch", "Version"]
            def entry = ""
            def tempResult = new JsonSlurper().parseText(fileContents)
            // debug_file << "DEBUG: tempResult=${tempResult}\n"
            tempResult = (tempResult.Images).sort { a, b -> b.CreationDate <=> a.CreationDate }
            // debug_file << "DEBUG: tempResult.sort=${tempResult}\n"
            tempResult.each { child ->
                if (child && child.Name && (child.Name.toLowerCase()).startsWith(searchParam)) {
                    entry = child.ImageId
                    entry = entry + " " + (child.Name).take(15) + ".."
                    entry = entry + "|" + (child.CreationDate).take((child.CreationDate.length()) - 5)
                    if (child.Tags) {
                        def childTags = child.Tags.sort { it.Key }
                        childTags.each { innerchild ->
                            tags.each { tag ->
                                if (innerchild.Key == tag) {
                                    if (innerchild.Value != "")
                                        entry = entry + "|" + tag + ": " + innerchild.Value
                                }
                            }
                        }
                    }
                    if (branchType.equals("all"))
                        result.add(entry)
                    else if (!branchType.equals("all") && entry.contains(branchType))
                        result.add(entry)
                }
            }

            if (!result || result.empty) {
                error.add("No matching AMIs found?")
                error.add("AMISearchPattern: " + searchParam)
                error.add("AMIInfraBuildSearchPattern: " + branchType)
                error.add("serverRole: " + (server_Role != '' ? server_Role : "none"))
                return error
            }

            return result

        } catch (Exception ex) {
            error.add("Exception::")
            error.add(ex.toString())
            error.add(ex.getMessage())
            error.add(ex.getStackTrace())
            return error
        }
    }

    @NonCPS
    static
    def getArtifactNamesFilteredSorted(String server_url, String dir_path, String branch, String repoName, String username, String password) {
        // DEBUG
        File debug_file = new File("/data/jenkins_home/jobs/Retrieve_Pipeline_Libraries/workspace/getArtifactNamesFilteredSorted.txt")

        def removeString = server_url + "/api/storage/" + repoName + "/"
        def result = []
        def newresult = []

        debug_file << "DEBUG: before calling get_artifacts_data"
        //call common function to connect to Enterprise Artifactory and call Artifactory REST API to get data
        //def responseText = get_artifacts_data(server_url, dir_path, repoName, username, password)

        def address = server_url + "/api/search/gavc?g=" + dir_path + "&repos=" + repoName
        def HTTP_MAX_ATTEMPTS = 10
        def HTTP_SLEEP_TIME = 60000 // time is in milliseconds, 60 seconds.
        def HTTP_TIMEOUT = 7000 // time is in milliseconds, 7 seconds.

        def responseText = ""
        def authString = "${username}:${password}".getBytes().encodeBase64().toString()

        // DEBUG
        // debug_file << "DEBUG: removeString=${removeString}\n"
        // debug_file << "DEBUG: address=${address}\n"
        // debug_file << "DEBUG: authString=${authString}\n"

        try {
            //Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("forwardproxy", 3128));
            //conn = address.toURL().openConnection(proxy)
            def conn = address.toURL().openConnection()
            conn.setConnectTimeout(HTTP_TIMEOUT)
            conn.setReadTimeout(HTTP_TIMEOUT)
            conn.setRequestProperty("Authorization", "Basic ${authString}")
            responseText = conn.content.text
            // debug_file << "DEBUG: responseText=${responseText}\n"
            //return responseText

        } catch (java.net.SocketTimeoutException ex) {
            //println "http timeout. sleeping until next http attempt"
            sleep HTTP_SLEEP_TIME
        }

        def tempResult = new JsonSlurper().parseText(responseText)
        debug_file << "DEBUG: tempResult=${tempResult}\n"


        tempResult.results.each { child ->
            removeString = "https://artifactory.aus.thenational.com/api/storage/" + repoName + "/"
            result.add(child.uri - removeString)
        }
        // debug_file << "DEBUG: result=${result}\n"

        TreeMap resultMap = [:]

        def value = ""
        def tempList = []
        def key = ""
        def artifact = ""
        def buildNumberStr = ""
        def buildNumber

        result.each { child ->
            tempList = child.split('/')

            buildNumber = Integer.parseInt(tempList[tempList.length - 2])

            artifact = child - ('/' + tempList[tempList.length - 1])

            if (artifact.tokenize('/')[1] == branch) {
                if (!resultMap.containsKey(buildNumber)) {
                    resultMap[buildNumber] = artifact
                } else {
                    return "error: key '" + key + "' corresponds with 2 values '" + resultMap[buildNumber] + ", " + artifact + "'"
                }
            }
        }

        resultMap = resultMap.sort { it.key }

        resultMap.each { k, v ->
            newresult.add("" + v + "") //cast int to string?
        }

        //return newresult[0]
        return newresult.reverse()

    }

}
