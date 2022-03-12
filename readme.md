# ICPC 2022 Source Code

**Ying Zhang, Ya Xiao, Md Mahir Asef Kabir, Daphne Yao, Na Meng. Example-Based Vulnerability Detection and Repair in Java Code 30th IEEE/ACM International Conference on Program Comprehension**

Clone the project: `git clone --recursive git@github.com:NiSE-Virginia-Tech/ying-ICPC-2022.git`

The source code contains three parts:
1. SEADER source code;
2. customized WALA;
3. customized changedistiller

The several runner functions are within runner folder.
The argument should contains jar, the source file location.

to run it, you need to import the wala and changedistiller as submodule, and set changedistiller_test/lib as the lib directory.

`dataset` folder contains the jar file we used to run.

`result` folder contains the running result for tools we listed on the paper.
