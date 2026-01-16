# Release Procedure

Here it is described the list of steps on how to make a new release for `EvoMaster`.
However, a new release of `EvoMaster` should be done only by the project manager.

In the following, with `???` we refer to secrets (e.g., passwords).

## Pre-requisites

In your local `~/.m2` Maven repository, you need to create a `settings.xml` file with the following:
```
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" 
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
<servers>
<server>
      <id>central</id>
      <username>???</username>
      <password>???</password>
    </server>
</servers>
</settings>
```

The username and password are linked to the account that owns the domain `evomaster.org`.

You need to have installed `GPG` on your machine (used to sign files).
Create a public-private keyset with:
```
gpg --gen-key
```

You can list the generated key with:
```
gpg --list-keys
```

Upload the public key with:
```
gpg --keyserver https://keys.openpgp.org --send-keys  ???
```
where `???` needs to be replaced with the id of the generated public key.
You can also read more details [here](https://central.sonatype.org/pages/working-with-pgp-signatures.html).


## Release Notes

Update the release notes (i.e., the `release_notes.md` file), by replacing the SNAPSHOT version with the new release version.

## Maven Central Release

The Java client needs to be deployed on Maven Central. 
First, you need to set a new version number for the new release.
We use semantic versioning `x.y.z`: `x` for major releases, `y` for minor releases, and 
`z` for patches.
Note: a patch `z` should never break backward compatibility. Breaking changes should only 
happen in  `x` major releases, and avoided if possible in `y` minor releases.
Recall that if there is a breaking change, still all the SUT drivers in EMB would need to be
updated, manually...  

Given a current snapshot version `0.3.1-SNAPSHOT`, a new release could be `0.4.0`, i.e.,
increase minor version `y` by 1, and reset patch version `z` to 0.

To change the version on all `pom.xml` files at once, from project root folder run, could use the `mvn versions:set` command.
However, there are other files besides the pom ones that need to be updated, like for example `makeExecutable.sh`.
So, the update of versions should be done with the `version.py` script. E.g.,
```
py version.py x.y.z
```


From project root  folder, execute:
```
mvn clean -Pdeployment -DskipTests  deploy
```

If everything went well, you should be able to see the deployed files at
[https://central.sonatype.com/artifact/org.evomaster/evomaster-client-java-controller](https://central.sonatype.com/artifact/org.evomaster/evomaster-client-java-controller). 
However, it might take some hours before those are in sync with Maven Central,
which you can check at [https://central.sonatype.com/publishing/deployments](https://central.sonatype.com/publishing/deployments) (requires login).




## GitHub Release

Push the version changes in the `pom.xml` files on Git.

Create a Git _tag_ in the form `v<version>`, e.g., `v0.4.0`, using the command:

```
git tag v<x.y.z>
git push origin v<x.y.z>   
```
This will trigger a special build on GitHub Action (see `.github/workflows/release.yml`).
If everything goes correctly, then after that action is completed the new release should be available on the [release page](https://github.com/WebFuzzing/EvoMaster/releases).

In case of problems, will need to remove the created (locally and remotely), before trying again:

```
git tag  --delete v<x.y.z>
git push --delete origin v<x.y.z>
```

Once a release is completed, make sure to close all the [GitHub Issues](https://github.com/WebFuzzing/EvoMaster/issues) that were marked with '__FIXED in SNAPSHOT__'.


## SNAPSHOT Update

Once `EvoMaster` is released on both Maven Central and GitHub, you need to prepare
the next snapshot version, which will have the same version with `z+1` and suffix
`-SNAPSHOT`, e.g, given `0.4.0`, the following snapshot version would 
be `0.4.1-SNAPSHOT`:
```
py version.py 0.4.1-SNAPSHOT
```



## WFD Release

After completing the release of a new version of `EvoMaster`, you might, or might not, need to make a new
release for [https://github.com/WebFuzzing/Dataset](https://github.com/WebFuzzing/Dataset) as well.
Changes in EM only impact _white-box_ settings, and __NOT__ the _black-box_ ones.
Ideally, should make a new WFD release, but, if not, still need to update snapshot EM version in the _develop_ branch:

`python3 scripts/version.py  em <VERSION>` 

If you are making a new release for WFD, need to update its version as well:

`python3 scripts/version.py  wfd <VERSION>`

Note: no longer WFD is aligned with EM. 
Versions number will be usually different.

On `develop` branch, versions should always be `-SNAPSHOT`.
On `master` they will be release versions.
The `master` branch should always point to last commit of latest release, and no SNAPSHOT.



To make a new release for WFD, you then need to do:
1. in `develop` branch, use `version.py` to set new release versions for both `em` and `wfd`.
2. push `develop`.
3. switch `master`, pull from `develop`, and push.
4. on GitHub, manually make new release, with tag matching release version of `wfd`.
5. switch to `develop`, and use `version.py` to make new `-SNAPSHOT` versions, for `em` and `wfd`.
6. push `develop`.


## Example Update

Every time we make a new release, we should also update the examples in [https://github.com/WebFuzzing/rest-api-example](https://github.com/EMResearch/rest-api-example).
This means:
* increase the dependency version of EM in the pom file
* remove/fix any deprecated function in the implemented driver
* regenerate all the tests, using one of built executables (e.g., `evomaster.exe` on Windows)



## (DEPRECATED) NPM Release

After the version number has been updated with `version.py` script, need to make a release on NPM as well.
From folder:

`client-js/evomaster-client-js`

run the following commands:
```
npm run build
npm login
npm publish
```

Note that login will ask for username/password.
The release is linked to NPM's user `arcuri82`.
Password is ???.

## (DEPRECATED) NuGet Release

After the version number has been updated with `version.py` script, need to make a release on NuGet as well for the .NET libraries.

First, build .NET libraries with:

`dotnet build`

Then, execute:

`./client-dotnet/publish.sh ???`

It takes as input the API-KEY linked to the namespace `EvoMaster.*`.
Note: API-KEYs only last 1 year, and then a new one needs to be created.

