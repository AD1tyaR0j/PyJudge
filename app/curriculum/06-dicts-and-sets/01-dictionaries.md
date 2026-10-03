---
title: Dictionaries
minutes: 10
practice: hash-map, strings
---
A **dictionary** maps **keys** to **values**. Instead of looking things up by position, you look them up by name:

```pyrun
ages = {"Ada": 36, "Alan": 41}
print(ages["Ada"])
ages["Grace"] = 85        # add a new key
ages["Ada"] = 37          # update an existing key
print(ages)
print(len(ages), "Alan" in ages, "Bob" in ages)
del ages["Alan"]
print(ages)
```

Keys must be unique and **immutable** (strings, numbers, tuples). Values can be anything.

## Safe lookups with `get`

`ages["Bob"]` raises a `KeyError` if the key is missing. `get` returns a default instead:

```pyrun
ages = {"Ada": 36}
print(ages.get("Bob"))        # None
print(ages.get("Bob", 0))     # 0
```

## Looping over a dictionary

```pyrun
stock = {"apples": 5, "pears": 0, "plums": 12}
for fruit in stock:                  # loops over KEYS
    print(fruit)
for fruit, qty in stock.items():     # keys and values together
    print(f"{fruit}: {qty}")
print(list(stock.values()), sum(stock.values()))
```

Dictionaries remember insertion order (Python 3.7+).

## The counting pattern

Counting things is the #1 use of dictionaries:

```pyrun
text = "mississippi"
counts = {}
for ch in text:
    counts[ch] = counts.get(ch, 0) + 1
print(counts)
for ch in sorted(counts):
    print(ch, counts[ch])
```

That's the whole *Character Frequency* exercise. (The `collections.Counter` class does this in one line; see *Algorithmic Thinking*.)

## Lookup tables beat long `if` chains

```pyrun
roman = {"I": 1, "V": 5, "X": 10, "L": 50, "C": 100}
word = "XVI"
print(sum(roman[ch] for ch in word))   # 16 (this simple version ignores subtraction like IV)
```

## Why dictionaries are fast

Checking `key in some_dict` takes about the same time whether it has 10 entries or 10 million. That's how *Two Sum* goes from a slow nested loop to one pass:

```pyrun
nums, target = [2, 7, 11, 15], 9
seen = {}                         # value -> index
for i, x in enumerate(nums):
    if target - x in seen:
        print(seen[target - x], i)
        break
    seen[x] = i
```

<!-- quiz -->

Q: What does `{"a": 1}.get("b", 5)` return?
- [ ] `None`
- [ ] A `KeyError`
- [x] `5`
- [ ] `1`
> `get(key, default)` returns the default when the key is missing.

Q: What is `d` after this code?
```python
d = {}
for w in ["x", "y", "x"]:
    d[w] = d.get(w, 0) + 1
```
- [ ] `{"x": 1, "y": 1}`
- [x] `{"x": 2, "y": 1}`
- [ ] `{"x": 2, "y": 1, "x": 1}`
- [ ] `[2, 1]`
> Each word's count starts at 0 via `get` and goes up by one per occurrence. Keys are unique, so `"x"` appears once with value 2.

Q: Which can't be a dictionary key?
- [ ] `"name"`
- [ ] `42`
- [ ] `(1, 2)`
- [x] `[1, 2]`
> Lists are mutable and therefore unhashable. Use a tuple instead.
