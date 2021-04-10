# SVN Migration Scripts

You run the scripts using an executable JAR file.  You can download the JAR from this repository. Alternatively, instructions for building the JAR yourself appear at the bottom of this page. We recommend you simply download the already built JAR.

## Prerequisites 

To use these scripts, make sure you have the following software already installed on your local system:

* A Java runtime, version 6 or newer ([download from Oracle][download a JDK]),
* [Git][], with `git-svn` included, version 1.7.7.5 or newer, and
* [Subversion][], version 1.6.17 or newer.

## Getting the Executable JAR

Download the `svn-migration-scripts.jar` from this repository by doing the following:

1. Click the _Downloads_ link in the repository navigation bar.
2. Select the `svn-migration-scripts.jar` from the list of files.


## Running the scripts from the JAR

Having downloaded the JAR, you can run the scripts it contains. All commands have the format:

    $ java -jar svn-migration-scripts.jar <command> <args>…
    
Leave off the `<command> <args>` to list the available commands:
    
    $ java -jar svn-migration-scripts.jar
	Available commands:
	- authors
	- bitbucket-push
	- clean-git
	- create-disk-image
	- sync-rebase
	- verify
    

All commands optionally take a `--help` argument, displaying usage information about the command.

	$ java -jar svn-migration-scripts.jar authors --help authors
	<URL> [<username> [<password>]]

	Generates an initial authors mapping for the committers to a Subversion
	repository.

	Each line of the author mapping associates a Subversion commiter with their
	full name and e-mail address. For example:

	jane.doe = Jane Doe <jane.d@example.org>

	

#### A note on Subversion permissions

Many of these commands will access your Subversion repository to gather information. In particular, `authors` needs to be run as a user with read access to your entire Subversion tree. If you are using Atlassian OnDemand, by default, no users have read access to the root of the Subversion tree, and as such you will need to grant read access to the user whose credentials you are using for the conversion process. You can read [our documentation on configuring repository permissions at the path level in OnDemand][SVN permissions]; the path that needs to be configured is `/`.

### verify

This command will perform some simple tests to ensure that your system has the required dependencies to convert your Subversion repository to Git.

The command is run as follows:

    $ java -jar svn-migration-scripts.jar verify

In particular, the command checks whether you have sufficiently recent versions of:

* Subversion
* Git
* `git-svn`

The other tests it performs are:

* whether it is possible to directly connect to the internet, and
* whether you're running the command on a case-insensitive file-system.

We do not support running conversions with `git-svn` on a case-insensitive file-system; they can in some cases lead to corrupted conversions. Such corrupted conversions may be evident by having two Subversion branches (with names differing only in case) which appear to me unified in the Git conversion, or a file that always appears modified as per `git status`.

##### Example

    $ java -jar svn-migration-scripts.jar verify
    Git: using version 1.7.9.6
    Subversion: using version 1.6.18
    git-svn: using version 1.7.9.6
    You appear to be running on a case-insensitive file-system. This is unsupported, and can result in data loss.
    $

### mount-disk-image (OS X only)

This command, only available on OS X, allows you to easily create and mount a case-sensitive disk image at some path. This is designed to allow you to work around the problems with case-insensitive file-systems mentioned above (the default OS X file-system, HFS+, is case-insensitive).

The command is run as follows:

    $ java -jar svn-migration-scripts.jar mount-disk-image <size in GB> <path for disk image> <path at which to mount disk image>

The size parameter must be specified in integral gigabytes.

##### Example

This example demonstrates first that it is being run on a case-insensitive file system (touching both `path/foo` and `path/FOO` results in only a single file being created), then mounts a 2 GB disk image at `path` and demonstrates that within the disk image, a case-sensitive file system exists. It also demonstrates that the disk image can be subsequently unattached from the file system via the `umount(1)` command.

    $ ls
    $ mkdir path
    $ touch path/foo path/FOO
    $ ls path
    foo
    $ rm -rf path
    $ java -jar svn-migration-scripts.jar mount-disk-image 2 image path
    created: /Users/huw/Source/svn-migration-scripts/target/image.sparseimage
    /dev/disk1          	GUID_partition_scheme
    /dev/disk1s1        	Apple_HFS                      	/Users/huw/Source/svn-migration-scripts/target/path
    $ ls
    image.sparseimage
    path
    $ touch path/foo path/FOO
    $ ls path
    Foo foo
    $ umount path
    $ ls
    image.sparseimage
    $

### clean-git

This command cleans up a Git repository created with `git-svn`. It creates annotated Git tags corresponding to the Subversion tags detected, creates local branches corresponding to the Subversion branches, and removes any branches or tags which do not currently exist in Subversion (but may have, for example, existed in the past). It also attempts to tidy up tag/branch names which are not allowable in Git.

The command is run as follows:

    $ java -jar svn-migration-scripts.jar clean-git [--dry-run] [--no-delete] [--strip-metadata] <repository-url> ...

If the `--dry-run` option is specified, the command will not perform any actions, but will instead simply show what would be done. If the `--no-delete` option is specified, branches and tags will be created but none will be removed. If the `--strip-metadata` option is specified, the infomartion in Git commit messages created by `git-svn` specifiying the Subversion revision corresponding to the Git commit will be removed. ***Very important:** after metadata has been stripped from the Git repository, subsequent Subversion revisions cannot be fetched into the Git repository*. Note that if grafts or replacement refs exist in the Git repository, metadata will not be stripped.

##### Example

In this example, we clean our converted repository, also stripping metadata (which, again, should only be done once you're ready to “flip the switch” and no longer need to fetch new Subversion revisions into your Git repositories). Because this Git repository was cloned from the Subversion project located under `https://studio.atlassian.com/svn/REST`, we pass that URL as a root on the command line.

    $ java -jar svn-migration-scripts.jar clean-git --strip-metadata https://studio.atlassian.com/svn/REST
    # Creating annotated tags...
    tag has diverged: 1.1.1
    Creating annotated tag '1.1.1' at refs/remotes/tags/1.1.1.
    Updated tag '1.1.1' (was 0000000)
    … many more tags created …
    # Creating local branches...
    Creating the local branch '2.0-proposed' for Subversion branch 'refs/remotes/2.0-proposed'.
    Branch 2.0-proposed set up to track local ref refs/remotes/2.0-proposed.
    … many more branches created …
    # Checking for obsolete tags...
    No obsolete tags to remove.
    # Checking for obsolete branches...
    No obsolete branches to remove.
    # Cleaning tag names
    # Cleaning branch names
    # removing Subversion metadata from Git commit messages
    Rewrite 8a975001a657692ec48e9902f525915d8d0c1ff4 (394/394)
    Ref 'refs/heads/master' was rewritten
    $

### authors

This command prints to standard output a list of the user names that have committed to a Subversion repository, in the format of an example author mapping. It is run as follows:

    $ java -jar svn-migration-scripts.jar authors <repository-url> [<username> [<password>]]

If the Subversion repository requires you to authenticate against it, you can specify a user name and optionally a password. If a user name is specified and a password is omitted, you will be interactively prompted for a password.

It is important to note that the URL used should be the top-level of the Subversion repository, rather than the root of any particular project within that repository. For example, even though Atlassian OnDemand uses a repository layout of `https://instance.jira.com/svn/PROJECTROOT/`, you should specify `https://instance.jira.com/svn/` as the repository URL. A consequence of this is that the same authors file can be used for all projects inside a Subversion repository. As mentioned above, for Atlassian OnDemand, accessing the root of the Subversion repository may require adjusting permissions; refer to [the Atlassian OnDemand documentation for how to do this][SVN permissions].

The output file is of the format:

    j.doe = j.doe <j.doe@mycompany.com>
    …

Once the initial file has been generated *it is important to edit it so that it contains the **full names** and e-mail addresses of the committers to the Subversion repository*. If this is not done, commits may not be associated with the appropriate users when you push your repository to Bitbucket. For example, you might edit the file generated above to read

    j.doe = Jane Doe <jane@somecompany.example>
    …

##### Example

In this example, we generate an authors file for the Subversion repository at `https://studio.atlassian.com/svn`. To authenticate against the repository, we supply a username and password on the command line (“test-user” and “test-user-password” respectively).

    $ java -jar svn-migration-scripts.jar authors https://studio.atlassian.com/svn test-user
    > Password: ****
    abhalla = abhalla <abhalla@mycompany.com>
    ahempel = ahempel <ahempel@mycompany.com>
    andreask = andreask <andreask@mycompany.com>
    … many more authors …
    $

#### The authors command in OnDemand

If you run the authors command against an Atlassian OnDemand instance, the command will attempt to look up the full name and e-mail address of each committer in JIRA. If the command is unable to find a corresponding JIRA user for a Subversion committer, the username will be present in the generated authors list without any mapping. *You will need to edit the authors list to supply a valid mapping for such users before you can use the authors file in a conversion*. All such users will appear at the beginning of the generated authors file.

### bitbucket-push

This command pushes the Git repository in the directory it is run from to a repository in [Bitbucket][], creating it if it does not exist. It is run as follows:

    $ java -jar svn-migration-scripts.jar bitbucket-push <username> [<owner>] <repository-name>
    > Password: ****

This will push the Git repository in the current directory to the Bitbucket repository with the name `repository-name` owned by `owner`; if this repository does not exist, it is created, and if the `owner` option is omitted, it defaults to `username`. `username` and `password` are the credentials used to authenticate against Bitbucket. Typically, you might pass your organisation's Bitbucket team as the owner. If the repository is created by this command, it is created as a private repository.

##### Example

In this example, we push our converted repository to Bitbucket. We use the credentials of the user “bbusername”, and because we haven't specified a repository owner, it's defaulted to creating the repository as belonging to “bbusername”. The repository has the name specified on the command line, namely “rest-clone”.

    $ java -jar svn-migration-scripts.jar bitbucket-push bbusername rest-clone
    > Password: ****
    remote: bb/acl: bbusername is allowed. accepted payload.
    To https://bbusername:bbpassword@bitbucket.org/bbusername/rest-clone
     * [new branch]      2.0-proposed -> 2.0-proposed
     … many more branches created …
    remote: bb/acl: bbusername is allowed. accepted payload.
    To https://bbusername:bbpassword@bitbucket.org/hgiddens/rest-clone
     * [new tag]         1.1.1 -> 1.1.1
     … many more tags created …
    Successfully pushed to Bitbucket
    $
    
## Building the JAR Yourself

If you want, you can clone the repository and build the JAR your self. To build the JAR, you need the Oracle JDK 1.6 or higher and [SBT][]. You can [download a JDK][] from Oracle. See [this page][install SBT] for installation instructions for SBT.

Once you've installed Java & SBT, you can create the JAR by doing the following:

1. Clone the svn-migration-scripts repository.
2. Change to the root of the project.
3. Run the following command to create the JAR file under the `target/scala-2.9.1` directory.

    $ sbt proguard

4. Rename the generated JAR to `svn-migration-scripts.jar`.
    

[SBT]: http://www.scala-sbt.org/
[download a JDK]: http://www.oracle.com/technetwork/java/javase/downloads/index.html
[install SBT]: http://www.scala-sbt.org/release/docs/Getting-Started/Setup
[Git]: http://git-scm.com/
[Subversion]: http://subversion.apache.org/
[SVN permissions]: https://confluence.atlassian.com/display/AOD/Configuring+repository+permissions+for+a+project#Configuringrepositorypermissionsforaproject-Configuringrepositorypermissionsatthepathlevel
[Bitbucket]: http://bitbucket.org
