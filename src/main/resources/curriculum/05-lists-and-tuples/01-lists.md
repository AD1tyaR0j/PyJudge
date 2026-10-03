---
title: Lists
minutes: 9
practice: arrays
---
A **list** holds an ordered collection of values in square brackets. Unlike strings, lists are **mutable**: you can change them in place.

```pyrun
scores = [88, 92, 75]
print(scores[0], scores[-1], len(scores))

scores[1] = 95          # change an item
scores.append(60)       # add to the end
scores.insert(0, 100)   # insert at an index
print(scores)
```

Indexing and slicing work exactly like strings: `scores[1:3]`, `scores[::-1]`, and so on.

## Removing items

```pyrun
items = ["a", "b", "c", "d", "b"]
last = items.pop()        # remove and return the last item
first = items.pop(0)      # remove and return the item at index 0
items.remove("b")         # remove the first "b" (by value)
print(last, first, items)
del items[0]
print(items)
```

`remove()` raises a `ValueError` if the value isn't there, so check with `in` first if you're not sure.

## Useful operations

```pyrun
nums = [3, 1, 4, 1, 5, 9, 2, 6]
print(len(nums), sum(nums), min(nums), max(nums))
print(4 in nums, 7 in nums)
print(nums.count(1), nums.index(5))
print([0] * 5)            # [0, 0, 0, 0, 0]
print([1, 2] + [3, 4])    # [1, 2, 3, 4]
```

## Building a list from input

The exercise input format "`n` on the first line, then `n` numbers" turns into a list like this:

```pyrun
n = int(input())
nums = list(map(int, input().split()))
print(nums, "has", n, "numbers")
print("average:", sum(nums) / n)
### stdin
5
10 20 30 40 50
```

## Aliasing: two names, one list

Assignment doesn't copy a list. Both names refer to the **same** object:

```pyrun
a = [1, 2, 3]
b = a            # b is the SAME list
b.append(4)
print(a)         # [1, 2, 3, 4], so a changed too!

c = a.copy()     # or a[:] or list(a): a real copy
c.append(5)
print(a, c)
```

> **Grid trap:** `[[0] * 3] * 3` makes three references to the *same* row. Use `[[0] * 3 for _ in range(3)]` to get independent rows. (List comprehensions are coming soon.)

<!-- quiz -->

Q: What is `nums` after this code?
```python
nums = [5, 6, 7]
nums.append([8, 9])
```
- [ ] `[5, 6, 7, 8, 9]`
- [x] `[5, 6, 7, [8, 9]]`
- [ ] `[[8, 9], 5, 6, 7]`
- [ ] An error
> `append` adds its argument as **one** item, even if it's a list. Use `extend([8, 9])` to add each element.

Q: What prints?
```python
a = [1, 2]
b = a
b.append(3)
print(len(a))
```
- [ ] 2
- [x] 3
- [ ] 1
- [ ] An error
> `b = a` doesn't copy; both names point to the same list, so `a` sees the append.

Q: What does `[10, 20, 30].pop()` return?
- [ ] `10`
- [x] `30`
- [ ] `None`
- [ ] `[10, 20]`
> `pop()` with no argument removes and returns the **last** item.
