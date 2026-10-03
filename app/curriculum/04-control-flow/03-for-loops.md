---
title: For Loops and range()
minutes: 8
practice: loops, basics
---
A `for` loop walks through every item of a sequence: a string, a list, a range of numbers, and more.

```pyrun
for fruit in ["apple", "banana", "cherry"]:
    print("I like", fruit)

for ch in "abc":
    print(ch.upper())
```

## Counting with `range()`

`range` produces a sequence of integers. Like slicing, the stop value is **excluded**:

| Call | Numbers |
|---|---|
| `range(5)` | 0, 1, 2, 3, 4 |
| `range(2, 6)` | 2, 3, 4, 5 |
| `range(0, 10, 3)` | 0, 3, 6, 9 |
| `range(5, 0, -1)` | 5, 4, 3, 2, 1 |

```pyrun
for i in range(1, 6):
    print(f"7 x {i} = {7 * i}")
```

## Accumulating results

A very common pattern is to start with an empty or zero value and update it each iteration:

```pyrun
nums = [4, 8, 15, 16, 23, 42]
total = 0
biggest = nums[0]
for x in nums:
    total += x
    if x > biggest:
        biggest = x
print(total, biggest)
print(sum(nums), max(nums))   # built-ins do the same thing
```

## Index and value together: `enumerate`

```pyrun
for i, name in enumerate(["Ann", "Bob", "Cy"], start=1):
    print(i, name)
```

## Nested loops

A loop inside a loop runs the inner loop completely for **each** outer iteration:

```pyrun
for row in range(1, 4):
    line = ""
    for col in range(1, 4):
        line += f"{row * col:4}"
    print(line)
```

Be careful with nested loops on big inputs: two loops over 100,000 items means 10,000,000,000 steps, far too slow. You'll learn to spot this in *Algorithmic Thinking*.

## `for` vs `while`

Use `for` when you're going through a known collection or range, and `while` when you loop until a condition changes.

<!-- quiz -->

Q: Which numbers does `range(3, 10, 3)` produce?
- [ ] 3, 6, 9, 12
- [x] 3, 6, 9
- [ ] 3, 6
- [ ] 0, 3, 6, 9
> It starts at 3 and adds 3 each time, stopping **before** 10.

Q: What does this print?
```python
total = 0
for i in range(4):
    total += i
print(total)
```
- [ ] 10
- [x] 6
- [ ] 4
- [ ] 0
> `range(4)` is 0, 1, 2, 3, and their sum is 6.

Q: What does `enumerate(["a", "b"])` give you in a loop?
- [ ] Only the indexes 0 and 1
- [x] Pairs of (index, value): (0, "a") and (1, "b")
- [ ] The values in reverse
- [ ] The length of the list
> `enumerate` pairs each item with its index, so you can write `for i, item in enumerate(...)`.
