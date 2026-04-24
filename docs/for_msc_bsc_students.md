## NOTES FOR MSc/BSc STUDENTS

Welcome to EvoMaster!!!

It is great that you are going to do a MSc/BSc project with EvoMaster. 
We wish this is going to be an exciting experience for your professional development and growth. 

In this document, let us summarize some practical process aspects of your involvement with EvoMaster:

* You start to work on your own Git _fork_. When your first feature/unit-of-development is ready for a Pull Request (PR), you do a PR towards the main repository of EvoMaster. 

* Before making a PR, make sure __all test cases pass__ on your fork's Continuous Integration (CI), i.e., currently GitHub Actions. Also, make sure to pull from latest _master_ branch of EvoMaster before making the PR. 

* On GitHub's interfaces, ask for review of your PR to your direct academic supervisor (e.g., Prof. Galeotti if you are studying in Argentina, Dr. Zhang if in China, or Dr. Sahin if in Türkiye). When you fix the comments of your supervisor, and your supervisor approves your PR, your supervisor will ask for review to the current EvoMaster's [benevolent dictator](https://en.wikipedia.org/wiki/Benevolent_dictator_for_life) (i.e., Prof. Arcuri, in Norway).  

* A PR from a fork is never going to be merged directly into the _master_ branch. It will first be merged into a new branch. The problem is on how CI is currently setup (long story...). When CI is green on the new branch, it will be merged into _master_. 

* When your first PR is merged into _master_, time to celebrate! Thanks for your contribution ;) You will get access to EvoMaster' repository with _write_ rights. From now on, you discard your fork, and work directly in the EvoMaster's repository. 

* The _master_ branch of EvoMaster is protected. You cannot directly modify it. You need to work on branches, and then make PRs, using the same process: CI needs to be green before you ask your direct supervisor for review, which will then ask the "dictator" for a second review. 

* When making PRs, please remember that _small is beautiful_. If you can divide your work in small separated units that can be reviewed separately, that would be better than a 10k-50k LOCs single PR... also, a feature does not need to be fully completed before some code start to get merged via PRs (as long as CI is green, of course).    