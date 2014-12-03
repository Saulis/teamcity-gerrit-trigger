teamcity-gerrit-trigger
=======================

Plugin for Teamcity which polls Gerrit to trigger builds

#### How it works
- Connects to Gerrit using Gerrits SSH command line API
- Polls for patchsets every 20 seconds (query is limited to fetch only the last 10 patchsets)
- Queues a new build for every new patchset found (new as in created after the last build was queued)

#### Building

- Run mvn package to create a zip file which you can drop under the plugins folder in your Teamcity server.

#### Usage

- After installation, add the _Gerrit Build Trigger_ into your build configuration 
- Configure the trigger:
  - Host: hostname of your Gerrit instance in the form dev.gerrit.com (uses default port 29418, custom ports not supported)
  - Username: SSH username that will be used to open connection (optional, default: the username that runs Teamcity)
  - Custom private key: Full path to the private key you want to use (optional, default: default private key of user)
  - Passphrase: Passphrase for the private key (optional)
  - Project: Filter for querying patchsets (optional)
  - Branch: Filter for querying patchsets (optional)
