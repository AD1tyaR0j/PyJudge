---
title: How Fast Is Your Code? (Big-O)
minutes: 10
practice: arrays, hash-map
---
**Big-O notation** describes how the running time grows as the input size `n` grows. It ignores constant details and focuses on the shape of the growth.

| Big-O | Name | n = 100,000 ⇒ about | Example |
|---|---|---|---|
| O(1) | constant | 1 step | `d[key]`, `x in some_set` |
| O(log n) | logarithmic | 17 steps | binary search |
| O(n) | linear | 100,000 steps | one loop over the list |
| O(n log n) | linearithmic | 1.7 million | `sorted()` |
| O(n²) | quadratic | 10,000,000,000 | a loop inside a loop |

**Rule of thumb:** Python does roughly 10–50 million simple steps per second. With a 2-second limit and `n = 100,000`, O(n log n) is fine, but O(n²) is hopeless.

## Spotting the complexity

```python
for x in nums:            # O(n)
    total += x

for x in nums:            # O(n²): n iterations, each doing O(n) work
    if nums.count(x) > 1: ...

for x in nums:            # O(n²) hidden inside!
    if x in some_list: ...    # `in` on a list scans every item

for x in nums:            # O(n): set lookups are O(1)
    if x in some_set: ...
```

## Seeing it for yourself

```pyrun
import time

def has_duplicate_slow(nums):          # O(n²)
    for i in range(len(nums)):
        for j in range(i + 1, len(nums)):
            if nums[i] == nums[j]:
                return True
    return False

def has_duplicate_fast(nums):          # O(n)
    return len(set(nums)) != len(nums)

for n in [500, 1000, 2000]:
    nums = list(range(n))              # no duplicates: the worst case
    t = time.perf_counter(); has_duplicate_slow(nums); slow = time.perf_counter() - t
    t = time.perf_counter(); has_duplicate_fast(nums); fast = time.perf_counter() - t
    print(f"n={n:5}  slow={slow:.4f}s  fast={fast:.6f}s")
```

Each time `n` doubles, the slow version takes about **4×** longer (n²), while the fast one barely changes.

## Cost of common operations

| Operation | list | set / dict |
|---|---|---|
| `x in c` | O(n) | O(1) |
| append / add | O(1) | O(1) |
| insert/remove at front | O(n) | n/a |
| get by index | O(1) | n/a |
| sort | O(n log n) | n/a |

Removing from the front of a list shifts every element, which is why `collections.deque` exists (next lessons).

## A strategy for every exercise

1. Read the constraints. `n ≤ 100` means almost anything works; `n ≤ 10⁵` needs O(n log n) or better.
2. Write the obvious solution, then ask: *what am I recomputing or re-searching?*
3. Replace repeated searches with a set or dict, and repeated sub-problems with a cache.

<!-- quiz -->

Q: An exercise has `n ≤ 100,000`. Which approach is likely to pass a 2-second limit?
- [ ] Comparing every pair of elements: O(n²)
- [x] Sorting, then one pass: O(n log n)
- [ ] Trying every subset: O(2ⁿ)
- [ ] All of them
> O(n²) is about 10¹⁰ steps for n = 10⁵, far too slow. O(n log n) is under 2 million.

Q: What is the time complexity of `x in my_list`?
- [ ] O(1)
- [ ] O(log n)
- [x] O(n)
- [ ] O(n²)
> Python has to check list items one by one. For a set or dict it's O(1) on average.

Q: If an O(n²) program takes 1 second for n = 1,000, about how long for n = 2,000?
- [ ] 2 seconds
- [x] 4 seconds
- [ ] 1 second
- [ ] 8 seconds
> Doubling n multiplies n² by 4.
