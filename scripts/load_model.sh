#!/bin/bash

# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

curl --data-binary @tokens_hn_10k.txt.gz http://localhost:9000/smarts/model/hnlinks
