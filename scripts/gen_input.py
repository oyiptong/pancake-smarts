#!/usr/bin/env python

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

import redis
import re
import argparse
import json

parser = argparse.ArgumentParser(description='Generate an input file for learning')
parser.add_argument('outfile', type=argparse.FileType('w'))
parser.add_argument('--text', dest='tokenize', action='store_const',
                   const=False, default=True,
                   help='set to grab full text rather than tokens from db. this will include punctuation and common words')
parser.add_argument('--format', dest='format', type=str, default="m", help='Choose output format: "vw" for Vowpal Wabbit or "m" for mallet, "tmt" for stanford\'s topic modelling toolkit. "js" for JSON. The default is mallet')
parser.add_argument('--limit', dest='limit', type=int, default=-1, help='Set a limit for the number of documents returned')
args = parser.parse_args()
f = args.outfile

r = redis.StrictRedis(db=3)
urls = r.smembers('global:urls')
if args.tokenize:
    token_keys = ["tokens:{0}".format(url) for url in urls]
else:
    token_keys = ["text:{0}".format(url) for url in urls]
tokens = r.mget(*token_keys)

newline = re.compile('\n')

if args.format == 'js':
    data = []

for i, url in enumerate(urls):
    if args.limit > 0 and i > args.limit:
        break
    url = url.strip()
    token_list = tokens[i]
    if token_list is not None:
        token_list = newline.sub('', token_list)
        if token_list:
            if args.format == 'm':
                print >> f, "{0} en {1}".format(url, token_list)
            elif args.format == 'vw':
                print >> f, "{0} |Tokens:1.0 {1}".format(i, token_list)
            elif args.format == 'tmt':
                print >> f, "{0},{1},{2}".format(i, url, token_list)
            elif args.format == 'js':
                data.append({'name': url, 'group': 'en', 'text': token_list})

if args.format == 'js':
    json.dump(data, f)

f.close()
