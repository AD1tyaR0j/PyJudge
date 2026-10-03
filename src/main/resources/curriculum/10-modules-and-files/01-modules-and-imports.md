---
title: Modules and Imports
minutes: 8
practice: math
---
A **module** is a file of Python code you can reuse. `import` loads it:

```pyrun
import math
import random

print(math.sqrt(81), math.pi)
random.seed(1)                 # fixed seed makes "random" results repeatable
print(random.randint(1, 6))    # a dice roll
print(random.choice(["rock", "paper", "scissors"]))
```

## Import styles

```pyrun
import math                      # use as math.sqrt
from math import gcd, lcm        # import specific names directly
import statistics as stats       # give a module a short alias
from collections import Counter

print(math.floor(2.7), gcd(12, 18), lcm(4, 6))
print(stats.mean([2, 4, 9]), stats.median([5, 1, 3]))
print(Counter("hello").most_common(1))
```

Avoid `from module import *`. It dumps every name into your program and makes it unclear where things come from.

## Standard library highlights

| Module | Use it for |
|---|---|
| `math` | `sqrt`, `gcd`, `factorial`, `inf`, `isclose` |
| `random` | random numbers, shuffling, sampling |
| `collections` | `Counter`, `defaultdict`, `deque` |
| `itertools` | combinations, permutations, `groupby` |
| `functools` | `lru_cache`, `reduce` |
| `bisect` | binary search on sorted lists |
| `heapq` | priority queues: smallest-item-first |
| `datetime` | dates and times |
| `string` | handy constants like `ascii_lowercase` |
| `sys` | fast input (`sys.stdin`), recursion limit |

```pyrun
import string, itertools
from datetime import date

print(string.ascii_lowercase)
print(list(itertools.combinations("ABC", 2)))
print(date(2024, 12, 25).strftime("%A"))
print((date(2025, 1, 1) - date(2024, 12, 25)).days, "days")
```

## Faster input for big tests

For inputs with hundreds of thousands of lines, `sys.stdin` is much faster than calling `input()` repeatedly:

```pyrun
import sys
data = sys.stdin.read().split()      # every token in the whole input
n = int(data[0])
nums = list(map(int, data[1:1 + n]))
print(sum(nums))
### stdin
5
1 2 3 4 5
```

## Your own modules and `__name__`

Any `.py` file is a module. Code that should run only when the file is executed directly, not when it's imported, goes under this guard:

```python
def main():
    print("running as a script")

if __name__ == "__main__":
    main()
```

<!-- quiz -->

Q: After `from math import sqrt`, how do you call it?
- [ ] `math.sqrt(9)`
- [x] `sqrt(9)`
- [ ] `import.sqrt(9)`
- [ ] `math::sqrt(9)`
> `from ... import name` brings the name directly into your program.

Q: Which module provides `Counter`, `deque`, and `defaultdict`?
- [ ] `itertools`
- [ ] `math`
- [x] `collections`
- [ ] `random`
> They live in `collections`, the go-to module for specialised containers.

Q: Why call `random.seed(42)` before generating numbers?
- [ ] To make the numbers more random
- [x] To make the sequence repeatable, which helps testing and debugging
- [ ] It's required before using `random`
- [ ] To speed up `random`
> The same seed always produces the same sequence of "random" numbers.
