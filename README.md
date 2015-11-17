# Github Autoresponder

Project to scan for new issues on a Github repository and auto-reply with a fixed message.

[![Join the chat at https://gitter.im/Astrac/github-autoresponder](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Astrac/github-autoresponder?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

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

Next, you need to generate a config file for the project. Create a file called `autoresponder.conf` and paste the following into it:

~~~
autoresponder = {
  owner        = "<<OWNER_OF_GITHUB_REPO_TO_WATCH>>"
  repo         = "<<NAME_OF_GITHUB_REPO_TO_WATCH>>"
  credentials  = {
    username     = "<<GITHUB_USER_TO_RUN_AS>>"
    accessToken  = "<<PERSONAL_ACCESS_TOKEN>>"
  }
  messageFile  = "/path/to/message/file.txt"
  pollInterval = "10 seconds"
  timeout      = "500 milliseconds"
}
~~~

Finally, run the app as follows:

~~~
sbt run -Dconfig.file=/path/to/autoresponder.conf
~~~

## Running without a config file

If you want to skip creating a config file, you can specify all of the config parameters on the command line:

~~~ bash
$ sbt run -Dautoresponder.owner=OWNER_OF_GITHUB_REPO_TO_WATCH \
          -Dautoresponder.repo=NAME_OF_GITHUB_REPO_TO_WATCH \
          -Dautoresponder.credentails.username=GITHUB_USER_TO_RUN_AS \
          -Dautoresponder.credentails.accessToken=PERSONAL_ACCESS_TOKEN \
          -Dautoredponder.messageFile=/path/to/message/file.txt \
          -Dautoredponder.pollInterval=10s \
          -Dautoredponder.timeout=500ms
~~~
