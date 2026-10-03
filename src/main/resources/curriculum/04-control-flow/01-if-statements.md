---
title: Making Decisions with if
minutes: 8
practice: conditionals, basics
---
An `if` statement runs a block of code only when a condition is true:

```pyrun
temperature = 31
if temperature > 30:
    print("It's hot!")
    print("Drink water.")
print("This line always runs.")
```

## Indentation is the syntax

Python uses **indentation** (4 spaces by convention) to group the lines that belong to the `if`. Everything indented under the colon `:` is the block. Get it wrong and you'll see an `IndentationError`, or worse, code that silently runs at the wrong time.

## `else` and `elif`

```pyrun
score = 72
if score >= 90:
    grade = "A"
elif score >= 80:
    grade = "B"
elif score >= 70:
    grade = "C"
else:
    grade = "F"
print(grade)
```

Python checks the conditions **top to bottom** and runs only the **first** branch that is true. That's why the order matters: put the most specific checks first.

## FizzBuzz logic

The classic interview question combines `%` with `elif`:

```pyrun
for n in range(1, 16):
    if n % 15 == 0:          # check "both" FIRST
        print("FizzBuzz")
    elif n % 3 == 0:
        print("Fizz")
    elif n % 5 == 0:
        print("Buzz")
    else:
        print(n)
```

If you checked `n % 3 == 0` first, 15 would print `Fizz` and never reach `FizzBuzz`.

## Conditional expressions

For simple either/or values, there's a one-line form:

```pyrun
n = 7
parity = "Even" if n % 2 == 0 else "Odd"
print(parity)
```

## Nested conditions

You can put an `if` inside another, but deep nesting gets hard to read. Often `and` is clearer:

```python
# nested
if age >= 18:
    if has_id:
        print("Welcome")

# flatter, same meaning
if age >= 18 and has_id:
    print("Welcome")
```

<!-- quiz -->

Q: What prints?
```python
x = 15
if x > 10:
    print("big")
elif x > 5:
    print("medium")
else:
    print("small")
```
- [x] `big`
- [ ] `big` then `medium`
- [ ] `medium`
- [ ] `small`
> Only the first true branch runs. `x > 10` is true, so the `elif` is never checked.

Q: In FizzBuzz, why must `n % 15 == 0` be checked before `n % 3 == 0`?
- [ ] `%` only works with 15 first
- [x] Otherwise multiples of 15 would match the `% 3` branch and print `Fizz`
- [ ] Python requires larger numbers first
- [ ] It's faster
> `if/elif` stops at the first true condition, so the most specific case must come first.

Q: What does Python use to mark which lines belong to an `if` block?
- [ ] Curly braces `{ }`
- [ ] The keyword `end`
- [x] Indentation after a colon
- [ ] Semicolons
> Python uses indentation as syntax. Lines indented under `if ...:` form its block.
