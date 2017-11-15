/*** BEGIN META {"name": "List GIT Branches",
 "comment": "You can use this to fill up a dynamic list parameter with branches. Optionally, you can filter the branches to show.",
 "parameters": [ 'FILTER', 'REMOTE' ],
 "core": "1.580",
 "authors": [{ name : "Jan Vansteenkiste" }]} END META**/

def origin
// Small hack to deal with a empty default value for REMOTE.
if (binding.variables.get('REMOTE') == null) {
    origin = ""
} else {
    origin = REMOTE
}

def filter
if (binding.variables.get('FILTER') == null) {
    filter = ".*"
} else {
    filter = FILTER
}

import hudson.model.*
import java.util.regex.Pattern

// Compile the regex patern for the provided FILTER
def pattern = Pattern.compile(filter)

// Get the current jenkins job.
def jenkinsJob() {
    def threadName = Thread.currentThread().getName()
    def pattern = Pattern.compile("job/(.*)/build")
    def matcher = pattern.matcher(threadName); matcher.find()
    def jobName = matcher.group(1)
    def jenkinsJob = Hudson.instance.getJob(jobName)
}

// Gets the GIT remote (lists configured scms and takes the first one)
def gitRemote(name) {
    if (name == "" || name == null) {
        def urls = jenkinsJob().scm.userRemoteConfigs.collect {
            v -> v.url
        }
        def url = urls[0]
    } else {
        def urls = jenkinsJob().scm.userRemoteConfigs.findAll {
            v -> v.name == name
        }
        def url = urls[0].url
    }
}

def remote_url = gitRemote(origin)
def command = ["/bin/bash", "-c", "git ls-remote --heads " + remote_url + " | awk '{print \$2}' | sort -r -V | sed 's@refs/heads/@@'"]
def process = command.execute();
process.waitFor()

def result = process.in.text.tokenize("\n")

def branches = []
for (i in result) {
    if (pattern.matcher(i).matches()) {
        branches.add(i)
    }
}
return branches