Pancake Smarts
==============

#### Warning: This project is exploratory and no particular care was given in making this a production ready application

Pancake Smarts is an attempt to do topic modeling on web pages.

It includes a Latent Dirichlet Allocation implementation wrapped around a web service.

It requires:
* a MySQL daemon running to persist data to
* a diffbot API key

You need to create the topic model first by training with a reasonable-sized corpus of data.

The web interface allows a user to input a url, and pancake_smarts will give back the top topics it thinks match the best.
