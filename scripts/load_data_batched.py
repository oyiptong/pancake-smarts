#!/usr/bin/env python
import eventlet.patcher
eventlet.patcher.monkey_patch()

import re
import json
import logging
import time
import sys
import signal
import argparse
import urllib
import threading

import redis
import urllib3
from urllib3.exceptions import TimeoutError
import nltk
from nltk.stem.porter import PorterStemmer
from unidecode import unidecode
from stopwords import STOPWORDS

DIFFBOT_TOKEN = 'XXX'
DIFFBOT_BATCH_CMD = 'http://www.diffbot.com/api/batch'
DIFFBOT_RELATIVE_URL = '/api/article?{0}'
MAX_BATCH_SIZE = 50

parser = argparse.ArgumentParser(description='Process user data')
parser.add_argument('data_filename', type=argparse.FileType('r'))
parser.add_argument('--plain', dest='plain_text', action='store_const',
                   const=True, default=False,
                   help='whether to parse the input file as a file with just urls or as a graph db dump')
parser.add_argument('--stem', dest='stem', action='store_const',
                   const=True, default=False,
                   help='whether to use a stemmer when generating tokens')
args = parser.parse_args()
f = args.data_filename
if args.plain_text:
    dataset = f.readlines()
else:
    dataset = json.load(f)
f.close()

r = redis.StrictRedis(db=3)
http = urllib3.PoolManager(10, timeout=120)
porter = PorterStemmer()
gpool = eventlet.GreenPool(10)

non_alnum = re.compile('[\W_]+')
#en_stopwords = nltk.corpus.stopwords.words('english')

url_count = 0
added_count = 0
skipped_count = 0
sent_count = 0
errored_count = 0

start = None

last_update = time.time()

diffbot_errors = []
diffbot_no_text = []
http_errors = []
dupes = []

def get_url_commands(dataset):
    global skipped_count, dupes, url_count
    url_commands = []

    if args.plain_text:
        for url in dataset:
            try:
                url = url.encode('utf8')
            except UnicodeDecodeError:
                print url
                sys.exit(1)
            if not r.exists("text:{0}".format(url)):
                url_commands.append({'method':'GET', 'relative_url': DIFFBOT_RELATIVE_URL.format(urllib.urlencode({'url':url}))})
            else:
                dupes.append(url)
                skipped_count += 1
            url_count += 1
    else:
        for user in dataset:
            for stack in user['stacks']:
                for url in stack:
                    url = url.encode('utf8')
                    if not r.exists("text:{0}".format(url)):
                        url_commands.append({'method':'GET', 'relative_url': DIFFBOT_RELATIVE_URL.format(urllib.urlencode({'url':url}))})
                    else:
                        dupes.append(url)
                        skipped_count += 1
                    url_count += 1
    return url_commands

def fetch_batch(batch):
    global skipped_count, added_count, sent_count, errored_count, diffbot_errors, diffbot_no_text, http_errors
    sent_count += len(batch)
    try:
        resp = http.request('POST', DIFFBOT_BATCH_CMD, fields={'token':DIFFBOT_TOKEN, 'batch':json.dumps(batch)}, encode_multipart=False)
        if resp.status == 200:
            results = json.loads(resp.data)
            p = r.pipeline()
            for keypair in results:
                try:
                    data = json.loads(keypair['body'])
                    if data.has_key('errorCode'):
                        diffbot_errors.append(data)
                    else:
                        url = data['url']
                        p.set('raw:{0}'.format(url), resp.data)

                        if data.has_key('text'):
                            word_list = data['text'].split()
                            words = []
                            for w in word_list:
                                w = non_alnum.sub('',  unidecode(w.lower()))
                                if args.stem:
                                    w = porter.stem(w)
                                if w and not w in STOPWORDS:
                                    words.append(w)
                            p.sadd('global:urls', url)
                            p.set("text:{0}".format(url), data['text'])
                            if len(words) > 0:
                                p.set("tokens:{0}".format(url), " ".join(words))
                                added_count += 1
                            else:
                                skipped_count += 1
                        else:
                            diffbot_no_text.append(data)
                            errored_count += 1
                except ValueError:
                    print "Error:"
                    print results
                    errored_count += 1
            p.execute()
        else:
            http_errors.append(resp.data)
            errored_count += len(batch)
    except TimeoutError:
        errored_count += len(batch)

class UpdateThread(threading.Thread):
    def __init__(self):
        super(UpdateThread, self).__init__()
        self._stop = threading.Event()

    def stop(self):
        self._stop.set()

    def stopped(self):
        return self._stop.isSet()
        
    def run(self):
        while not self.stopped():
            now = time.time()
            add_rate = added_count/(now-start)
            sys.stderr.write("total:{0} sent:{4} added:{1} skipped:{2} errored:{5} add_rate: {3} doc/sec".format(url_count, added_count, skipped_count, add_rate, sent_count, errored_count))
            sys.stderr.write("\r")
            sys.stderr.flush()
            time.sleep(1.0)

updater_thread = UpdateThread()

def exit_summary():
    finish = time.time()
    minutes = (finish-start)/60.0
    print "\n\n----"
    print "time taken: {0:.2f} mins for {1} docs".format(minutes, url_count)
    print "rate: {0:0.2f} doc/sec".format(url_count/(finish-start))
    print "added: {0} skipped: {1} errored: {2}".format(added_count, skipped_count, errored_count)

def signal_handler(signal, frame):
    print "task interrupted"
    updater_thread.stop()
    exit_summary()
    sys.exit(0)
signal.signal(signal.SIGINT, signal_handler)

if __name__ == "__main__":

    start = time.time()
    updater_thread.start()

    url_commands = get_url_commands(dataset)

    batches = []
    offset = 0
    while offset < len(url_commands):
        batches.append(url_commands[offset:offset+MAX_BATCH_SIZE])
        offset += MAX_BATCH_SIZE

    for _ in gpool.imap(fetch_batch, batches):
        pass
    updater_thread.stop()
    updater_thread.join()
    exit_summary()
    sys.exit(0)
