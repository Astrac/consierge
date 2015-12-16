# Consierge

Project to scan for new issues on a Github repository and auto-reply with a fixed message.

[![Join the chat at https://gitter.im/Astrac/consierge](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Astrac/consierge?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Build Status](https://travis-ci.org/rumoku/consierge.svg)](https://travis-ci.org/rumoku/consierge)
[![codecov.io](http://codecov.io/github/rumoku/consierge/coverage.svg?branch=master)](http://codecov.io/github/rumoku/consierge?branch=master)





## Getting started

First, you need to generate a "personal access token" so the app can access your repos:

1. Go to [https://github.com/settings/profile]()
2. Click "Personal access tokens" in the left-hand menu
3. Click "Generate a new token"
4. Authenticate
5. Enter a description and select scopes:
   - check "public_repos" (and nothing else) if you want to set up on a public repo;
   - check "repos" and "public_repos" (and nothing else) if you want to set up on a private repo.
6. Click "Generate token" and *write the token down*.

Next, you need to generate a config file for the project. Create a file called `consierge.conf` and paste the following into it:

~~~
consierge = {
  owner        = "<<OWNER_OF_GITHUB_REPO_TO_WATCH>>"
  repo         = "<<NAME_OF_GITHUB_REPO_TO_WATCH>>"
  credentials  = {
    username     = "<<GITHUB_USER_TO_RUN_AS>>"
    accessToken  = "<<PERSONAL_ACCESS_TOKEN>>"
  }
  messageFile  = "/path/to/message/file.txt"
  pollInterval = "10 seconds"
  timeout      = "500 milliseconds"
  fetchOpts {
    contributorFilter = true # ignore tickets created by contributor
    sinceEnabled = true # fetch filter updated since previous execution
  }  
}
~~~

Finally, run the app as follows:

~~~
sbt run -Dconfig.file=/path/to/consierge.conf
~~~

## Running without a config file

If you want to skip creating a config file, you can specify all of the config parameters on the command line:

~~~ bash
$ sbt run -Dconsierge.owner=OWNER_OF_GITHUB_REPO_TO_WATCH \
          -Dconsierge.repo=NAME_OF_GITHUB_REPO_TO_WATCH \
          -Dconsierge.credentails.username=GITHUB_USER_TO_RUN_AS \
          -Dconsierge.credentails.accessToken=PERSONAL_ACCESS_TOKEN \
          -Dconsierge.messageFile=/path/to/message/file.txt \
          -Dconsierge.pollInterval=10s \
          -Dconsierge.timeout=500ms
~~~
