---
title: Tuples and Unpacking
minutes: 6
practice: arrays, basics
---
A **tuple** is like a list that can't be changed: an **immutable** ordered collection, written with parentheses.

```pyrun
point = (3, 4)
print(point[0], point[1], len(point))
rgb = 255, 128, 0           # parentheses are optional
print(type(rgb))
single = (42,)              # a one-item tuple needs a trailing comma
print(type(single), type((42)))
```

`point[0] = 5` raises a `TypeError`. Use tuples for fixed groups of values that belong together, like coordinates, RGB colors, or (name, score) pairs.

## Unpacking

Assign each item of a sequence to its own name in one step:

```pyrun
x, y = (3, 4)
print(x, y)

name, age, city = ["Ada", 36, "London"]
print(f"{name} ({age}) from {city}")

a, b = 1, 2
a, b = b, a                 # swap without a temporary variable
print(a, b)
```

The number of names must match the number of items. Otherwise you get a `ValueError`.

## Star unpacking

```pyrun
first, *rest = [10, 20, 30, 40]
print(first, rest)
*init, last = "hello"
print(init, last)
head, *_, tail = range(10)  # _ is a conventional "don't care" name
print(head, tail)
```

## Returning several values

A function can return a tuple, and the caller unpacks it:

```pyrun
def min_max(nums):
    return min(nums), max(nums)

low, high = min_max([7, 2, 9, 4])
print(low, high)
q, r = divmod(17, 5)
print(q, r)
```

## Why immutability helps

Tuples can be used as **dictionary keys** and **set members**, but lists can't. That makes tuples perfect for things like grid coordinates in a `visited` set, which you'll use in *Number of Islands* and *Shortest Path in a Maze*.

```pyrun
visited = {(0, 0), (0, 1)}
print((0, 1) in visited)
```

<!-- quiz -->

Q: Which creates a tuple with one element?
- [ ] `(5)`
- [x] `(5,)`
- [ ] `tuple(5)`
- [ ] `[5]`
> The trailing comma makes it a tuple. `(5)` is just the number 5 in parentheses.

Q: After `a, b = 1, 2` and then `a, b = b, a`, what are `a` and `b`?
- [ ] `1, 2`
- [x] `2, 1`
- [ ] `2, 2`
- [ ] An error
> The right side is evaluated first into the tuple `(2, 1)`, which is then unpacked.

Q: Why can a tuple be a dictionary key but a list can't?
- [ ] Tuples are faster
- [x] Tuples are immutable, so their hash value can't change
- [ ] Lists are too long
- [ ] Dictionary keys must be numbers
> Dict keys and set members must be hashable. Mutable lists aren't; immutable tuples (of hashable items) are.
