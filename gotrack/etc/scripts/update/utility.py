__author__ = 'mjacobson'

'''
Utility Functions
'''

import sys
# from datetime import datetime
import time

COLORS = {'red': '\033[0;31m',
          'green': '\033[0;32m',
          'yellow': '\033[0;33m',
          'blue': '\033[0;34m',
          'purple': '\033[0;35m',
          'cyan': '\033[0;36m',
          'white': '\033[0;37m',
          'NC': '\033[0m'
          }


def timeit(method):
    def timed(*args, **kw):
        ts = time.time()
        result = method(*args, **kw)
        te = time.time()

        print '%r: %2.2f sec' % \
              (method.__name__, te - ts)
        return result

    return timed


def max_default(iterable, default=0):
    if len(iterable) == 0:
        return default
    else:
        return max(iterable)


def color(s, c):
    try:
        col = COLORS[c]
    except KeyError:
        # Unknown Colors; do nothing
        return s
    return col + s + COLORS['NC']


def set_color(c, out=None):
    try:
        col = COLORS[c]
    except KeyError:
        # Unknown Colors; do nothing
        return
    if out is not None:
        out.write(col)
    else:
        sys.stdout.write(col)


def query_yes_no(question, default=None):
    """Ask a yes/no question via raw_input() and return their answer.

    "question" is a string that is presented to the user.
    "default" is the presumed answer if the user just hits <Enter>.
        It must be "yes" (the default), "no" or None (meaning
        an answer is required of the user).

    The "answer" return value is True for "yes" or False for "no".
    """
    try:
        set_color('yellow')
        valid = {"yes": True, "y": True, "ye": True,
                 "no": False, "n": False}
        if default is None:
            prompt = " [y/n] "
        elif default == "yes":
            prompt = " [Y/n] "
        elif default == "no":
            prompt = " [y/N] "
        else:
            raise ValueError("invalid default answer: '%s'" % default)

        while True:
            sys.stdout.write(question + prompt)
            choice = raw_input().lower()
            if default is not None and choice == '':
                return valid[default]
            elif choice in valid:
                return valid[choice]
            else:
                sys.stdout.write("Please respond with 'yes' or 'no' (or 'y' or 'n').\n")
    finally:
        set_color('NC')


def grouper(page_size, iterable):
    page = []
    for item in iterable:
        page.append(item)
        if len(page) == page_size:
            yield page
            page = []
    yield page


# class Log:
#
#     COLORS = {'red': '\033[0;31m',
#               'green': '\033[0;32m',
#               'yellow': '\033[0;33m',
#               'blue': '\033[0;34m',
#               'purple': '\033[0;35m',
#               'cyan': '\033[0;36m',
#               'white': '\033[0;37m',
#               'NC': '\033[0m'
#               }
#
#     def __init__(self, out=sys.stdout, **kwargs):
#
#         self.out = out
#
#         cols = list()
#         cols.append(kwargs.get('info', 'green'))
#         cols.append(kwargs.get('warning', 'cyan'))
#         cols.append(kwargs.get('error', 'red'))
#         cols.append(kwargs.get('default', 'NC'))
#
#         try:
#             self.cols = [Log.COLORS[c] for c in cols]
#         except KeyError as inst:
#             print "Unknown Key", inst
#
#     def info(self, *msg, **kwargs):
#         sep = kwargs.get('sep', ' ')
#         self.out.write('%s%s INFO > %s%s\n' %
#                        (self.cols[0], str(datetime.now()), sep.join(map(str, msg)), self.cols[3]))
#
#     def warn(self, *msg, **kwargs):
#         sep = kwargs.get('sep', ' ')
#         self.out.write('%s%s WARNING > %s%s\n' %
#                        (self.cols[1], str(datetime.now()), sep.join(map(str, msg)), self.cols[3]))
#
#     def error(self, *msg, **kwargs):
#         sep = kwargs.get('sep', ' ')
#         self.out.write('%s%s ERROR > %s%s\n' %
#                        (self.cols[2], str(datetime.now()), sep.join(map(str, msg)), self.cols[3]))
