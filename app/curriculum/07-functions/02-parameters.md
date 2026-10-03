---
title: Parameters and Arguments
minutes: 9
practice: basics
---
## Default values

Parameters can have defaults, which makes them optional:

```pyrun
def greet(name, greeting="Hello"):
    return f"{greeting}, {name}!"

print(greet("Ada"))
print(greet("Ada", "Welcome"))
```

## Keyword arguments

Callers can name arguments, in any order. This makes calls self-documenting:

```pyrun
def power(base, exponent=2):
    return base ** exponent

print(power(3))
print(power(exponent=3, base=2))
print(sorted([3, 1, 2], reverse=True))   # you've used keyword arguments already!
```

## Any number of arguments: `*args` and `**kwargs`

```pyrun
def total(*numbers):              # numbers is a tuple
    return sum(numbers)

print(total(1, 2), total(5, 5, 5, 5), total())

def describe(**info):             # info is a dict
    for key, value in info.items():
        print(f"{key} = {value}")

describe(name="Ada", born=1815)
```

The `*` also works the other way, spreading a list into separate arguments:

```pyrun
def add3(a, b, c):
    return a + b + c

nums = [1, 2, 3]
print(add3(*nums))
print(*nums, sep=" + ")
```

## The mutable default trap

Default values are created **once**, when the function is defined, not on every call:

```pyrun
def add_item(item, bucket=[]):      # BUG: one shared list
    bucket.append(item)
    return bucket

print(add_item("a"))
print(add_item("b"))                # ['a', 'b']: surprise!

def add_item_fixed(item, bucket=None):
    if bucket is None:
        bucket = []                 # a fresh list on every call
    bucket.append(item)
    return bucket

print(add_item_fixed("a"), add_item_fixed("b"))
```

Use `None` as the default for lists, dicts, and sets.

## Functions are values

You can pass a function to another function, as you did with `sorted(key=len)`:

```pyrun
def apply_twice(func, value):
    return func(func(value))

def add_ten(x):
    return x + 10

print(apply_twice(add_ten, 1))
print(apply_twice(str.upper, "hi"))
```

<!-- quiz -->

Q: What does `greet("Bo")` return, given `def greet(name, punct="!"): return name + punct`?
- [ ] `"Bo"`
- [x] `"Bo!"`
- [ ] An error, because `punct` is missing
- [ ] `None`
> `punct` has a default, so it's optional and becomes `"!"`.

Q: Inside `def f(*args)`, what type is `args`?
- [ ] list
- [x] tuple
- [ ] dict
- [ ] set
> Extra positional arguments are collected into a **tuple**. `**kwargs` collects keyword arguments into a dict.

Q: Why is `def f(items=[])` risky?
- [ ] Lists can't be default values
- [x] The same list is reused across calls, so changes leak between calls
- [ ] It makes the function slower
- [ ] `items` will always be empty
> Defaults are evaluated once at definition time. Use `items=None` and create a new list inside.
