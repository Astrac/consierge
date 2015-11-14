# Github Autoresponder

Project to scan for new issues on a Github repository and auto-reply with a fixed message.

## Running

You need to specify four config keys on the command line:

~~~ bash
$ sbt run -Dautoresponder.repo=OWNER_OF_GITHUB_REPO_TO_WATCH \
          -Dautoresponder.repo=NAME_OF_GITHUB_REPO_TO_WATCH \
          -Dautoresponder.credentails.username=GITHUB_USER_TO_RUN_AS \
          -Dautoresponder.credentails.accessToken=PERSONAL_ACCESS_TOKEN_TO_AUTH_WITH
~~~

You can generate a personal access token [here](https://github.com/settings/tokens).
