Pancake Smarts
==============

#### Warning: This project is exploratory and no particular care was given in making this a production ready application

Pancake Smarts is an experiment to see if it is possible to implement a decent url recommendation engine using Topic Modelling, and using Locality Sensitive Hashing for similarity matching.

 The algorithms used are Latent Dirichlet Allocation and Random Projections.

Requirements:
* Play! Framework (2.0.3 used in dev)
* a MySQL daemon (5.5 used in dev)
* an ElasticSearch instance (0.19.9 used in dev)
* a [diffbot](https://www.diffbot.com) API key
* a java runtime (Oracle JDK 7 used in dev)

Overview
--------

Pancake Smarts does three main things:

1.  Using [Mallet] (http://mallet.cs.umass.edu/)'s implementation of Latent Dirichlet Allocation, Smarts generates a topic model, which is persisted, along with the latent topics.
2.  Using the generated topic model, Smarts infers the topic distribution of each document in the training set. The top N topic distributions are used to generate a hash code, using Random Projections. That data is also persisted and indexed in ElasticSearch.
3.  When a url is entered in a query form for the model, its topic distribution is inferred and a hash code generated. This hash code is used to find similar documents.

Results
-------

The combination of LDA and LSH seems to be fairly good. That said, to make a good recommendation engine, some other factors need to be considered.

If the intent is to create an "interesting article" generator, some element randomness can be injected, as well as some measure of security and perhaps "diversity".

Possible Improvements
---------------------

Pancake Smarts can be improved by:

* Using an internal page scraper and content extraction implementation
* The LDA implementation requires all the data to remain in memory. An online version can be used.
* Moreover, the LDA modelling can be distributed
* Using an algorithm in the Locality Sensitive Hashing that's more accurate than Random Projections. The results of the random projections vary a little bit
* Levenshtein distance is used to find similars. Since its implementation a pair-wise function, requires a running time of O(m*n) where m is the average word length and n is the number of documents. Lucene 4.0 is [reported] (http://blog.mikemccandless.com/2011/03/lucenes-fuzzyquery-is-100-times-faster.html) to have a more efficient implementation of "fuzzy matching", which will compute levenshtein distances in about O(n), i.e. in linear time wrt the nubmer of documents
* An alternative to using levenshtein distance matching is to use the hamming distance, since all hash codes are of the same length for a given topic model
* A better (?) alternative for similarity matching is to use an inverted index, with each bit of the hashcode encoded as a token. This data is already generated and persisted in the current version, but not fed to ElasticSearch

Getting Started
---------------

### Data import

You need to create the topic model first by training with a reasonable-sized corpus of data.

The data input is a gzipped file in the mallet input format as follows:

> [URL] [GROUP_NAME] [TOKENS]

Where TOKENS is a space-separated group of tokens. GROUP_NAME has to be supplied, but will be ignored for now.

This file can be sent to Smarts as follows:

> $ curl --data-binary @data.txt.gz http://hostname:9000/smarts/model/my-awesome-model

When this completes, one can then obtain recommendations at http://hostname:9000/smarts/model/my-awesome-model

Credits & Thanks
----------------

Pancake Smarts was developed using:
* [Play Framework 2.0.3] (http://www.playframework.org/), [Apache License 2.0] (http://www.apache.org/licenses/LICENSE-2.0.html)
* [Mallet 2.0.7] (http://mallet.cs.umass.edu/), [CPL License] (http://opensource.org/licenses/cpl1.0.php)
* Part of [Google Guava 13.0.1] (http://code.google.com/p/guava-libraries/), for its murmur3 implementation, [Apache License 2.0] (http://www.apache.org/licenses/LICENSE-2.0.html)
* Code adapted from [Joseph Turian]'s (https://github.com/turian/pyrandomprojection) Random Projection implementation

Thanks in no particular order to:
* [Zach Aysan] (http://zachaysan.tumblr.com/)
* [David Warde-Farley] (http://www-etud.iro.umontreal.ca/~wardefar/)
* [Martin Ostrovsky] (https://twitter.com/TheRealMoj)
* [Mike Tung] (http://miketung.com/)
* [Tyler Neylon] (https://twitter.com/tylerneylon)

License
-------

The code itself is under the [Mozilla Public License 2.0] (http://mozilla.org/MPL/2.0/)