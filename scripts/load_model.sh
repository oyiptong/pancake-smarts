#!/bin/bash
curl --data-binary @tokens_hn_10k.txt.gz http://localhost:9000/smarts/model/hnlinks
