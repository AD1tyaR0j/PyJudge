---
title: While Loops
minutes: 7
practice: loops, math
---
A `while` loop repeats its block **as long as** a condition stays true:

```pyrun
count = 3
while count > 0:
    print(count)
    count -= 1
print("Liftoff!")
```

Each pass through the block is an **iteration**. Something inside the loop must eventually make the condition false. Otherwise you get an **infinite loop**, which on PyJudge ends with *Time Limit Exceeded*.

## When to use `while`

Use `while` when you don't know in advance how many iterations you need, such as "keep going until the number runs out of digits":

```pyrun
n = 90817
digit_sum = 0
while n > 0:
    digit_sum += n % 10    # take the last digit
    n //= 10               # drop the last digit
print(digit_sum)           # 9 + 0 + 8 + 1 + 7 = 25
```

Or Euclid's algorithm for the greatest common divisor:

```pyrun
a, b = 48, 180
while b:
    a, b = b, a % b
print(a)    # 12
```

## `break` and `continue`

- `break` exits the loop immediately
- `continue` skips to the next iteration

```pyrun
n = 1
while True:              # loop "forever"...
    n *= 2
    if n > 100:
        break            # ...until we break out
print(n)                 # 128

i = 0
while i < 6:
    i += 1
    if i % 2 == 0:
        continue         # skip even numbers
    print(i)
```

## Reading until there's no more input

Some programs read lines until input runs out. `input()` raises `EOFError` at the end, which you can catch (error handling is covered later):

```pyrun
total = 0
while True:
    try:
        line = input()
    except EOFError:
        break
    total += int(line)
print(total)
### stdin
5
10
20
```

<!-- quiz -->

Q: How many times does this print `hi`?
```python
i = 0
while i < 5:
    print("hi")
    i += 2
```
- [ ] 2
- [x] 3
- [ ] 5
- [ ] Forever
> `i` takes the values 0, 2, 4 (all < 5), then becomes 6 and the loop ends.

Q: What does `continue` do inside a loop?
- [ ] Ends the loop
- [x] Skips the rest of this iteration and moves to the next one
- [ ] Restarts the program
- [ ] Pauses until input arrives
> `continue` jumps back to the loop condition; `break` leaves the loop entirely.

Q: Your submission gets *Time Limit Exceeded* and uses `while x != 0: ...`. What's the most likely cause?
- [ ] `while` loops are slow in Python
- [x] The loop never changes `x` to exactly 0, so it never ends
- [ ] You need to import `time`
- [ ] The judge doesn't support `while`
> An infinite loop is the usual culprit. Make sure each iteration moves toward the stop condition.
