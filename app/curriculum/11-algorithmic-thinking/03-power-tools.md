---
title: Python Power Tools
minutes: 10
practice: hash-map, bfs, graphs, sliding-window
---
The standard library contains data structures that make classic algorithms short and fast. These four show up constantly in competitive programming.

## `Counter`: counting in one line

```pyrun
from collections import Counter

words = "the cat and the hat and the bat".split()
counts = Counter(words)
print(counts)
print(counts["the"], counts["dog"])     # missing keys count as 0
print(counts.most_common(2))
print(Counter("listen") == Counter("silent"))   # anagram check
```

## `defaultdict`: no more `KeyError`

A `defaultdict` creates a default value the first time you touch a missing key:

```pyrun
from collections import defaultdict

groups = defaultdict(list)
for word in ["apple", "avocado", "banana", "blueberry", "cherry"]:
    groups[word[0]].append(word)
print(dict(groups))

graph = defaultdict(list)                # adjacency list for a graph
for a, b in [(1, 2), (1, 3), (2, 4)]:
    graph[a].append(b)
    graph[b].append(a)
print(dict(graph))
```

## `deque`: a fast queue, and breadth-first search

`deque` adds and removes at **both ends** in O(1). That makes it the right tool for **BFS**, which explores a grid or graph level by level and finds shortest paths:

```pyrun
from collections import deque

grid = ["S.#.",
        "..#.",
        "...E"]
rows, cols = len(grid), len(grid[0])
start, end = (0, 0), (2, 3)

dist = {start: 0}
queue = deque([start])
while queue:
    r, c = queue.popleft()                 # O(1); list.pop(0) would be O(n)
    if (r, c) == end:
        break
    for dr, dc in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
        nr, nc = r + dr, c + dc
        if 0 <= nr < rows and 0 <= nc < cols and grid[nr][nc] != "#" and (nr, nc) not in dist:
            dist[(nr, nc)] = dist[(r, c)] + 1
            queue.append((nr, nc))
print("shortest path:", dist.get(end, -1), "steps")
```

That's the full idea behind *Shortest Path in a Maze* and *Number of Islands*.

## Sliding window

When a problem asks about the best **contiguous** stretch of a list or string, move a window across it instead of re-checking every substring:

```pyrun
def longest_unique(s):
    seen = set()
    left = best = 0
    for right, ch in enumerate(s):
        while ch in seen:                # shrink until the repeat is gone
            seen.remove(s[left])
            left += 1
        seen.add(ch)
        best = max(best, right - left + 1)
    return best

print(longest_unique("abcabcbb"), longest_unique("pwwkew"))
```

Each character enters and leaves the window at most once, so it's O(n).

## `heapq`: always grab the smallest

```pyrun
import heapq

tasks = [(3, "write tests"), (1, "fix bug"), (2, "review PR")]
heapq.heapify(tasks)                     # a min-heap ordered by priority
while tasks:
    priority, task = heapq.heappop(tasks)
    print(priority, task)
print(heapq.nlargest(2, [5, 1, 9, 3, 7]))
```

## `itertools`: combinations without nested loops

```pyrun
from itertools import combinations, permutations, product, accumulate

print(list(combinations([1, 2, 3], 2)))
print(len(list(permutations("abcd"))))   # 4! = 24
print(list(product("ab", repeat=2)))
print(list(accumulate([1, 2, 3, 4])))    # running totals: prefix sums
```

> You've reached the end of the course. Head to the **Hard** exercises and put these tools to work! 🎓

<!-- quiz -->

Q: What does `Counter("banana").most_common(1)` return?
- [ ] `["a"]`
- [x] `[("a", 3)]`
- [ ] `{"a": 3}`
- [ ] `3`
> `most_common(k)` returns a list of `(item, count)` pairs, most frequent first.

Q: Why use `deque.popleft()` for BFS instead of `list.pop(0)`?
- [ ] `list.pop(0)` returns the wrong item
- [x] `popleft()` is O(1), while `pop(0)` shifts every remaining element: O(n)
- [ ] Lists can't store tuples
- [ ] `deque` sorts the items automatically
> On big grids, O(n) pops would make BFS quadratic and time out.

Q: With `d = defaultdict(int)`, what is `d["new"]` before anything is assigned?
- [ ] A `KeyError`
- [ ] `None`
- [x] `0`
- [ ] `""`
> `int()` returns `0`, so missing keys start at 0, perfect for counting.
