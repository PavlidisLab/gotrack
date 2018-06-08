#
# The GOTrack project
#
# Copyright (c) 2018 University of British Columbia
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
#

"""Helper Functions"""
from __future__ import division
import logging
import numpy as np

__author__ = 'mjacobson'

log = logging.getLogger(__name__)


# log.addHandler(logging.NullHandler())

class Memoized:
    def __init__(self, f):
        self.f = f
        self.cache = {}

    def __call__(self, *args):
        if args in self.cache:
            return self.cache[args]
        else:
            value = self.f(*args)
            self.cache[args] = value
            return value


def tversky_proto_weighted(prototype, variant):
    if len(prototype) == 0 and len(variant) == 0: return 1.0
    if len(prototype) == 0 or len(variant) == 0: return 0.0
    intersect_size = len(prototype.intersection(variant))
    return intersect_size / (intersect_size + len(prototype.difference(variant)))


def jaccard_similarity(a, b):
    if len(a) == 0 and len(b) == 0: return 1.0
    if len(a) == 0 or len(b) == 0: return 0.0
    return len(a.intersection(b)) / len(a.union(b))


def mean(data):
    """Return the sample arithmetic mean of data."""
    n = len(data)
    if n < 1:
        raise ValueError('mean requires at least one data point')
    return sum(data) / n  # in Python 2 use sum(data)/float(n)


def _ss(data):
    """Return sum of square deviations of sequence data."""
    c = mean(data)
    ss = sum((x - c) ** 2 for x in data)
    return ss


def sstdev(data):
    """Calculates the sample standard deviation."""
    n = len(data)
    if n < 2:
        raise ValueError('variance requires at least two data points')
    ss = _ss(data)
    pvar = ss / (n - 1)  # the sample variance
    return pvar ** 0.5


def median(data):
    """Return the median (middle value) of numeric data.

    When the number of data points is odd, return the middle data point.
    When the number of data points is even, the median is interpolated by
    taking the average of the two middle values:

    median([1, 3, 5]) -> 3
    median([1, 3, 5, 7]) -> 4.0

    """
    data = sorted(data)
    n = len(data)
    if n == 0:
        raise ValueError("no median for empty data")
    if n % 2 == 1:
        return data[n // 2]
    else:
        i = n // 2
        return (data[i - 1] + data[i]) / 2


def flatten(l):
    return (item for sublist in l for item in sublist)


def r_square(p1d, x, y):
    # fit values, and mean
    yhat = p1d(x)  # or [p(z) for z in x]
    ybar = np.sum(y) / len(y)  # or sum(y)/len(y)
    ssreg = np.sum((yhat - ybar) ** 2)  # or sum([ (yihat - ybar)**2 for yihat in yhat])
    sstot = np.sum((y - ybar) ** 2)  # or sum([ (yi - ybar)**2 for yi in y])
    return ssreg / sstot
