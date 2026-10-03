---
title: Input and Output
minutes: 8
practice: basics
---
Programs become useful when they react to **input**. On PyJudge, every exercise feeds your program text through *standard input* (stdin), and checks what you print to *standard output* (stdout).

## Reading a line with `input()`

`input()` reads **one line** of text and returns it as a string. The playground below has an **Input** box. Whatever is in it is what `input()` reads.

```pyrun
name = input()
print("Hello,", name + "!")
### stdin
World
```

Each call to `input()` reads the *next* line:

```pyrun
first = input()
second = input()
print(second, first)
### stdin
apple
banana
```

## Input is always text

Even if the user types `5`, `input()` returns the string `"5"`. Convert it before doing math:

```pyrun
a = int(input())
b = int(input())
print(a + b)
### stdin
12
30
```

## Several values on one line

Exercises often put several numbers on one line, like `3 4`. Split the line into pieces and convert each one:

```pyrun
line = input()          # "3 4"
parts = line.split()    # ["3", "4"]
a = int(parts[0])
b = int(parts[1])
print(a * b)

# The same thing in one line, the idiom you'll use constantly:
x, y = map(int, input().split())
print(x - y)
### stdin
3 4
10 7
```

`map(int, ...)` applies `int` to every piece, and `x, y = ...` unpacks the two results.

## Controlling `print()`

```pyrun
print("a", "b", "c", sep="-")     # a-b-c
print("no newline", end=" ")
print("same line!")
nums = [1, 2, 3]
print(*nums)                      # 1 2 3, the * spreads the list out
```

> **Exercise tip:** print *exactly* what the problem asks for. Don't add messages like `"The answer is"` unless they're part of the expected output. The judge compares your output line by line.

<!-- quiz -->

Q: The input line is `7 8`. What does `int(input())` do?
- [ ] Returns `7`
- [ ] Returns `[7, 8]`
- [x] Raises a `ValueError`, because `"7 8"` isn't a single integer
- [ ] Returns `15`
> `input()` returns the whole line `"7 8"`. Split it first: `a, b = map(int, input().split())`.

Q: What does `print(1, 2, 3, sep="")` output?
- [ ] `1 2 3`
- [x] `123`
- [ ] `1,2,3`
- [ ] `6`
> `sep` is the text placed between values. An empty separator glues them together.

Q: An exercise says "print the sum of the two numbers". Which output passes for input `2 3`?
- [x] `5`
- [ ] `The sum is 5`
- [ ] `Sum: 5`
- [ ] `2 + 3 = 5`
> The judge compares output exactly, so print only what's asked.
