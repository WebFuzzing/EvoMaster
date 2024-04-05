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
      <id>ossrh</id>
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

To change the version on all `pom.xml` files at once, from project root folder run:
```
mvn versions:set -DnewVersion=x.y.z
```

where `x.y.z` should be substituted with the actual version number, e.g., `0.4.0`.
However, there are other files besides the pom ones that need to be updated, like for example `makeExecutable.sh`.
So, the update of versions should be done with the `version.py` script. E.g.,
```
py version.py x.y.z
```


From project root  folder, execute:
```
mvn  -N -Pdeployment -DskipTests  deploy
cd client-java
mvn clean -Pdeployment -DskipTests  deploy
```

If everything went well, you should be able to see the deployed files at
[https://oss.sonatype.org/](https://oss.sonatype.org/). 
However, it might take some hours before those are in sync with Maven Central,
which you can check at [https://search.maven.org/](https://search.maven.org/).


## Release Notes

Update the release notes (i.e., the `release_notes.md` file), by replacing the SNAPSHOT version with the new release version. 

## GitHub Release

Push the version changes in the `pom.xml` files on Git.

Create a Git _tag_ in the form `v<version>`, e.g., `v0.4.0`, using the command:

```
git tag v<x.y.z>
git push origin v<x.y.z>   
```
This will trigger a special build on GitHub Action (see `.github/workflows/release.yml`).
If everything goes correctly, then after that action is completed the new release should be available on the [release page](https://github.com/EMResearch/EvoMaster/releases).

## GitHub Release (OLD MANUAL VERSION) 

Push the version changes in the `pom.xml` files on Git.
Build the whole `EvoMaster` from project root folder with:
```
mvn clean package -DskipTests
``` 

Make sure to use `package` and __NOT__ `install` (more on this later).
Furthermore, compilation __MUST__ be done with the _lowest_ JDK version currently
supported in _EvoMaster_.

From the [release](https://github.com/EMResearch/EvoMaster/releases) page
on GitHub, create a new release.
It needs to be tagged, with `v` prefix, e.g., `v0.4.0`.
On GitHub, upload the `core/target/evomaster.jar` executable as part of the release 
(there should be an option for _attaching binaries_).

Update: now we are building `.msi`/`.deb`/`.dmg` files as well, as part of GitHub Action CI. Download those from the release commit, and upload them here in the release page. 

## SNAPSHOT Update

Once `EvoMaster` is released on both Maven Central and GitHub, you need to prepare
the next snapshot version, which will have the same version with `z+1` and suffix
`-SNAPSHOT`, e.g, given `0.4.0`, the following snapshot version would 
be `0.4.1-SNAPSHOT`:
```
py version.py 0.4.1-SNAPSHOT
```



## EMB Release

After completing the release of a new version of `EvoMaster`, you need to make a new
release for [https://github.com/EMResearch/EMB](https://github.com/EMResearch/EMB) as well.
The two repositories __MUST__ have their version numbers aligned.

You need to update all `pom.xml` files with the same:
```
mvn versions:set -DnewVersion=x.y.z
```  

However, there are some other places in which the version number needs to be updated as well:

* in the `<evomaster-version>` variable in the root `pom.xml` project file.
* in the `EVOMASTER_VERSION` variable in the `scripts/dist.py` script file.
* in all the case study `pom.xml` files where the `evomaster-client-database-spy`
dependency is used (until we automate this task with a script, you will need to search
for those dependencies manually from your IDE).

To simplify all these previous steps, you can simply run `./scripts/version.py`.

Once those changes are pushed, create a new [release](https://github.com/EMResearch/EMB/releases) 
on GitHub.
Tag it with the same version as `EvoMaster`, but no need to attach/upload any file.

After this is done, update to a new SNAPSHOT version, by replacing __ALL__ the 
occurrences of the release version in the project (e.g., all `pom.xml` and 
`dist.py` files) by using `./scripts/version.py`.  
However, before doing this, it can be good to test the non-SNAPSHOT version of _EvoMaster_.
The reasoning is to force the downloading of all the dependencies from Maven Central,
to check if anything is missing.
And this is why it was important to build the non-SNAPSHOT with `package` instead of `install`. 

Note: the change to the version number for new release needs to be done (and pushed) on the `develop` branch. Then, `master` has to pull from `develop`, and push it.
Once the release (from `master` branch) is done on GitHub, switch back to `develop`, and push the new SNAPSHOT update.
In other words, `master` branch should always point to last commit of latest release, and no SNAPSHOT. 

## Example Update

Every time we make a new release, we should also update the examples in [https://github.com/EMResearch/rest-api-example](https://github.com/EMResearch/rest-api-example).
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

