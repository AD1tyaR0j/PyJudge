---
title: Sets
minutes: 7
practice: hash-map, arrays
---
A **set** is an unordered collection of **unique** values. Adding a duplicate does nothing.

```pyrun
colors = {"red", "green", "red", "blue"}
print(colors)                  # duplicates vanish; order isn't guaranteed
print(len(colors))
colors.add("yellow")
colors.discard("purple")       # no error if it's missing (remove() would raise)
print("green" in colors)

empty = set()                  # NOT {}, which is an empty dict!
print(type(empty), type({}))
```

## Removing duplicates

```pyrun
nums = [3, 1, 3, 2, 1, 3]
print(set(nums))               # {1, 2, 3}
print(len(set(nums)), "distinct values")
print(sorted(set(nums)))
```

`set()` forgets the original order. To dedupe while **keeping** the first-seen order (the *Remove Duplicates* exercise), track what you've seen:

```pyrun
nums = [3, 1, 3, 2, 1, 3]
seen = set()
result = []
for x in nums:
    if x not in seen:
        seen.add(x)
        result.append(x)
print(result)                  # [3, 1, 2]
```

## Set algebra

```pyrun
python_devs = {"ana", "ben", "cy"}
js_devs = {"cy", "dee", "ana"}
print(python_devs | js_devs)   # union: in either
print(python_devs & js_devs)   # intersection: in both
print(python_devs - js_devs)   # difference: only Python
print(python_devs ^ js_devs)   # symmetric difference: exactly one
```

## Speed: `in` on a set vs a list

`x in some_list` checks items one by one, while `x in some_set` jumps straight to the answer. On 100,000 items that's the difference between *Accepted* and *Time Limit Exceeded*:

```pyrun
import time
data = list(range(100_000))
as_set = set(data)

start = time.perf_counter()
hits = sum(1 for x in range(0, 100_000, 500) if x in data)
print("list:", round(time.perf_counter() - start, 3), "s")

start = time.perf_counter()
hits = sum(1 for x in range(0, 100_000, 500) if x in as_set)
print("set: ", round(time.perf_counter() - start, 4), "s")
```

<!-- quiz -->

Q: What is `len({1, 2, 2, 3, 3, 3})`?
- [ ] 6
- [x] 3
- [ ] 1
- [ ] An error
> A set keeps only unique values: `{1, 2, 3}`.

Q: How do you create an empty set?
- [ ] `{}`
- [x] `set()`
- [ ] `[]`
- [ ] `set[]`
> `{}` creates an empty **dictionary**. Use `set()` for an empty set.

Q: What is `{1, 2, 3} & {2, 3, 4}`?
- [ ] `{1, 2, 3, 4}`
- [x] `{2, 3}`
- [ ] `{1, 4}`
- [ ] `{1}`
> `&` is intersection: the elements found in both sets.
