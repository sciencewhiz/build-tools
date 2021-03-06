def basePath = 'wpiutil'
folder(basePath)

['Windows', 'Mac', 'Linux'].each { platform ->
    def prJob = job("$basePath/wpiutil $platform - PR") {
        label(platform.toLowerCase())
        steps {
            gradle {
                tasks('clean')
                tasks('build')
                switches('-PjenkinsBuild -PskipAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace --refresh-dependencies')
                configure {
                    passAllAsProjectProperties = false
                    passAllAsSystemProperties = false
                }
            }
        }
    }
    setupProperties(prJob)
    setupPrJob(prJob, platform)
}

def armPrJob = job("$basePath/wpiutil ARM - PR") {
    steps {
        gradle {
            tasks('clean')
            tasks('build')
            switches('-PjenkinsBuild -PonlyAthena -PreleaseBuild -PbuildAll --console=plain --stacktrace --refresh-dependencies')
        }
    }
}
setupProperties(armPrJob)
setupPrJob(armPrJob, 'ARM')

def developmentJob = pipelineJob("$basePath/wpiutil - Development") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/wpiutil-development.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/wpiutil-development.groovy'))
            }
            sandbox()
        }
    }
    triggers {
        scm('H/15 * * * *')
    }
}

setupProperties(developmentJob)

def releaseJob = pipelineJob("$basePath/wpiutil - Release") {
    definition {
        cps {
            try {
                script(readFileFromWorkspace('jenkins-jobs/pipeline-scripts/wpiutil-release.groovy'))
            } catch (Exception e) {
                script(readFileFromWorkspace('pipeline-scripts/wpiutil-release.groovy'))
            }
            sandbox()
        }
    }
}

setupProperties(releaseJob)

def setupProperties(job) {
    job.with {
        // Note: the pull request builder plugin will fail without this property set.
        properties {
            githubProjectUrl('https://github.com/wpilibsuite/wpiutil')
        }
        logRotator {
            numToKeep(50)
            artifactNumToKeep(10)
        }
    }
}

def setupPrJob(job, name) {
    job.with {
        scm {
            git {
                remote {
                    url('https://github.com/wpilibsuite/wpiutil.git')
                    refspec('+refs/pull/*:refs/remotes/origin/pr/*')
                }
                branch('${sha1}')
            }
        }
        triggers {
            githubPullRequest {
                admins(['333fred', 'PeterJohnson', 'bradamiller', 'Kevin-OConnor'])
                orgWhitelist('wpilibsuite')
                useGitHubHooks()
                extensions {
                    commitStatus {
                        context("frcjenkins - $name")
                    }
                }
            }
        }
    }
}
