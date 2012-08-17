Data import scripts
===================

Requirements:
* a diffbot account
* redis
* redis-py
* python 2.7+
* nltk
* urllib3
* gevent

This directory contains a bunch of scripts that will give you a start on training your models.

A text file with urls is provided to seed a corpus of data.

Run the ./load_data_batched.py script to read the file and fetch the text from diffbot.


