---
title: 'EvoMaster: A Search-Based System Test Generation Tool'
tags:
  - Java
  - Kotlin
  - SBST
  - search-based software engineering
  - test generation
  - system testing
  - REST
  - evolutionary computation

authors:
  - name: Andrea Arcuri
    orcid: 0000-0003-0799-2930
    affiliation: 1
  - name: Juan Pablo Galeotti
    orcid: 0000-0002-0747-8205
    affiliation: 2
  - name: Bogdan Marculescu
    orcid: 0000-0002-1393-4123
    affiliation: 1
  - name: Man Zhang
    orcid: 0000-0003-1204-9322
    affiliation: 1
affiliations:
 - name: Kristiania University College, Department of Technology, Oslo, Norway
   index: 1
 - name: FCEyN-UBA, and ICC, CONICET-UBA, Depto. de Computaci\'on, Buenos Aires, Argentina
   index: 2
date:  April 2020
bibliography: paper.bib
---


# Statement of Need

Testing web/enterprise applications is complex and expensive when done manually.
Often, software testing takes up to half of the development time and cost for a system. 
So much testing is needed because the cost of software failure is simply
too large: for example, in 2017, 304 software failures (reported in the media) impacted 3.6 billion people and $1.7
trillion in assets worldwide [@tricentis2017]. 
Unfortunately, due to its high cost, software testing is often left incomplete, and only applied partially.


To address this problem, in *Software Engineering* (SE) research a lot of effort has been spent in trying 
to design and implement novel techniques aimed at automating several different tasks in SE.
*Search-Based Software Testing* (SBST) [@harman2012search] casts the problem of software testing as an optimization problem,
aimed at, for example, maximizing code coverage and fault detection.   


Our SBST tool called ``EvoMaster`` addresses these challenges by using evolutionary techniques to 
automatically generate test cases.
It currently focuses on RESTful web services, which are the pillars of modern web and enterprise applications  [@fielding2000architectural][@allamaraju2010restful]. 

 
The ``EvoMaster`` tool is aimed  at:
 
* practitioners in industry that want to automatically test their software. 

* researchers that need generated test cases to answer the research questions in the SE 
  problems that they are investigating.  


# Tool Summary

``EvoMaster`` [@arcuri2018evomaster]  is a SBST tool 
that automatically *generates* system-level test cases.
Internally, it uses an *Evolutionary Algorithm* 
and *Dynamic Program Analysis*  to be able to generate effective test cases.
The approach is to *evolve* test cases from an initial population of 
random ones, using code coverage and fault detection as fitness function.

When addressing the testing of real-world web/enterprise applications, there are many challenges. 
To face and overcome those challenges, ``EvoMaster`` has been used to experiment with several novel techniques.
This led to several publications:
novel search algorithms such as *MIO* [@mio2017][@arcuri2018test],
addressing the white-box testing of RESTful APIs [@arcuri2017restful][@arcuri2019restful],
resource-dependency handling [@zhang2019resource], accesses to SQL databases [@arcuri2019sql],
and novel *testability transformations* [@arcuri2020testability].

At the moment, ``EvoMaster`` targets RESTful APIs compiled to 
JVM __8__ and __11__ bytecode.
The APIs must provide a schema in *OpenAPI/Swagger* format (either _v2_ or _v3_).
The tool generates JUnit (version 4 or 5) tests, written in either Java or Kotlin.


# Related Work

In the recent years, different techniques have been proposed for _black-box_ testing 
of RESTful APIs (e.g., [@restlerICSE2019][@karlsson2020QuickREST]
[@viglianisi2020resttestgen][@eddouibi2018automatic]).
However, at the time of this writing, ``EvoMaster`` appears to be the only one can do both
_black-box_ and _white-box_ testing, and that is also released as open-source.


# Acknowledgements
We thank Annibale Panichella for providing a review and fix of our implementation of his MOSA algorithm. 
We also want to thank Agustina Aldasoro for her contributed bug fixes.
This work is funded by the Research Council of Norway (project on Evolutionary Enterprise Testing, grant agreement No 274385), and 
partially by UBACYT-2018 20020170200249BA, PICT-2015-2741.

# References