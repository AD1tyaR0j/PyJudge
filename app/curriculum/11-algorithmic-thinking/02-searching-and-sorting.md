---
title: Searching and Sorting
minutes: 10
practice: binary-search, sorting, two-pointers
---
## Binary search

If a list is **sorted**, you don't need to check every item. Look at the middle: if it's too small, the answer must be in the right half; if too big, the left half. Each step halves the search space, so it's **O(log n)**: about 17 steps for 100,000 items.

```pyrun
def binary_search(nums, target):
    lo, hi = 0, len(nums) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if nums[mid] == target:
            return mid
        if nums[mid] < target:
            lo = mid + 1           # target is in the right half
        else:
            hi = mid - 1           # target is in the left half
    return -1

data = [2, 5, 8, 12, 16, 23, 38, 56, 72, 91]
print(binary_search(data, 23), binary_search(data, 7))
```

Off-by-one mistakes are easy here, so Python gives you the `bisect` module:

```pyrun
from bisect import bisect_left, insort

data = [2, 5, 8, 12, 16, 23]
i = bisect_left(data, 12)              # first position where 12 could go
print(i, i < len(data) and data[i] == 12)
print(bisect_left(data, 13))           # where 13 WOULD go: 4
insort(data, 13)                       # insert while keeping the list sorted
print(data)
```

This is the heart of *Binary Search Queries*.

## Binary search on the answer

Binary search also works on *questions*: "what's the smallest x such that some condition is true?" Here's an integer square root without `math`:

```pyrun
def isqrt(n):
    lo, hi = 0, n
    while lo < hi:
        mid = (lo + hi + 1) // 2
        if mid * mid <= n:
            lo = mid
        else:
            hi = mid - 1
    return lo

print(isqrt(99), isqrt(100), isqrt(10**18))
```

## Sorting as a tool

Sorting first (O(n log n)) often turns a hard problem into an easy scan:

```pyrun
# Closest pair of values: after sorting, the closest pair must be neighbours
nums = [40, 7, 92, 19, 36, 81]
nums.sort()
best = min(b - a for a, b in zip(nums, nums[1:]))
print(nums, "closest gap:", best)

# Anagrams: sorted letters are identical
print(sorted("listen") == sorted("silent"))
```

## How sorting algorithms work

Python's built-in sort (Timsort) is fast and **stable**: equal items keep their original order. You should almost always use it, but it's worth seeing a classic algorithm once. **Merge sort** splits the list, sorts each half, and merges them:

```pyrun
def merge_sort(nums):
    if len(nums) <= 1:
        return nums
    mid = len(nums) // 2
    left, right = merge_sort(nums[:mid]), merge_sort(nums[mid:])
    out, i, j = [], 0, 0
    while i < len(left) and j < len(right):
        if left[i] <= right[j]:
            out.append(left[i]); i += 1
        else:
            out.append(right[j]); j += 1
    return out + left[i:] + right[j:]

print(merge_sort([38, 27, 43, 3, 9, 82, 10]))
```

<!-- quiz -->

Q: What must be true before you can use binary search?
- [ ] The list has no negative numbers
- [x] The list is sorted
- [ ] The list length is a power of 2
- [ ] The target is in the list
> Binary search relies on order to discard half the remaining items each step.

Q: About how many steps does binary search need for 1,000,000 sorted items?
- [ ] 1,000,000
- [ ] 1,000
- [x] 20
- [ ] 2
> log₂(1,000,000) ≈ 20, because each step halves the range.

Q: What does `bisect_left([1, 3, 5], 4)` return?
- [ ] `1`
- [x] `2`
- [ ] `-1`
- [ ] `None`
> It's the index where 4 would be inserted to keep the list sorted: between 3 (index 1) and 5 (index 2).
