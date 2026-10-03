---
title: Sorting and Iterating
minutes: 9
practice: sorting, arrays, two-pointers
---
## `sorted()` vs `.sort()`

```pyrun
nums = [42, 7, 19, 3]
print(sorted(nums))     # returns a NEW sorted list
print(nums)             # original unchanged

nums.sort()             # sorts IN PLACE and returns None
print(nums)
print(sorted(nums, reverse=True))
```

A classic bug is `nums = nums.sort()`, which sets `nums` to `None`!

## Sorting with a `key`

`key` is a function applied to each item to decide its order:

```pyrun
words = ["banana", "Kiwi", "apple", "fig"]
print(sorted(words))                 # uppercase sorts before lowercase
print(sorted(words, key=str.lower))  # case-insensitive
print(sorted(words, key=len))        # shortest first

people = [("Ana", 31), ("Ben", 25), ("Cai", 31)]
# by age descending, then name ascending, using a tuple key
print(sorted(people, key=lambda p: (-p[1], p[0])))
```

That tuple-key trick, "sort by count descending, then alphabetically", is exactly what *Top K Frequent Words* needs.

## Walking two lists together: `zip`

```pyrun
names = ["Ann", "Bob", "Cy"]
scores = [91, 78, 85]
for name, score in zip(names, scores):
    print(f"{name}: {score}")

matrix = [[1, 2, 3], [4, 5, 6]]
for column in zip(*matrix):    # transpose!
    print(column)
```

## Reversing and looping backwards

```pyrun
nums = [1, 2, 3, 4]
print(nums[::-1])
for x in reversed(nums):
    print(x, end=" ")
print()
```

## Two pointers

Many list problems use two indexes moving through the data. Here's merging two sorted lists in a single pass:

```pyrun
a = [1, 4, 9]
b = [2, 3, 9, 10]
i = j = 0
merged = []
while i < len(a) and j < len(b):
    if a[i] <= b[j]:
        merged.append(a[i]); i += 1
    else:
        merged.append(b[j]); j += 1
merged += a[i:] + b[j:]
print(merged)
```

This visits each element once, which is much faster than re-sorting everything on huge inputs.

<!-- quiz -->

Q: What is `x` after `x = [3, 1, 2].sort()`?
- [ ] `[1, 2, 3]`
- [ ] `[3, 1, 2]`
- [x] `None`
- [ ] An error
> `.sort()` sorts in place and returns `None`. Use `sorted([3, 1, 2])` to get a new sorted list.

Q: What does `sorted(["bb", "a", "ccc"], key=len)` return?
- [x] `["a", "bb", "ccc"]`
- [ ] `["ccc", "bb", "a"]`
- [ ] `["a", "ccc", "bb"]`
- [ ] `[1, 2, 3]`
> The key function `len` makes Python sort by length, shortest first.

Q: What does `list(zip([1, 2, 3], "ab"))` give?
- [ ] `[(1, "a"), (2, "b"), (3, None)]`
- [x] `[(1, "a"), (2, "b")]`
- [ ] An error, because the lengths differ
- [ ] `[1, "a", 2, "b"]`
> `zip` stops at the shortest input.
