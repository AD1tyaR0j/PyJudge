---
title: Special (Dunder) Methods
minutes: 9
practice: simulation, math
---
Methods with double underscores, like `__init__`, are called **dunder methods**. Python calls them behind the scenes, and defining them makes your objects work with built-in syntax like `print()`, `==`, `+`, and `len()`.

## `__str__` and `__repr__`

```pyrun
class Point:
    def __init__(self, x, y):
        self.x, self.y = x, y

p = Point(2, 3)
print(p)          # <__main__.Point object at 0x...>: not helpful!

class Point:
    def __init__(self, x, y):
        self.x, self.y = x, y
    def __str__(self):              # friendly text for print()
        return f"({self.x}, {self.y})"
    def __repr__(self):             # unambiguous text for debugging
        return f"Point({self.x}, {self.y})"

p = Point(2, 3)
print(p)
print([p, Point(0, 0)])             # lists show the repr of their items
```

## Equality and arithmetic

```pyrun
class Vector:
    def __init__(self, x, y):
        self.x, self.y = x, y
    def __add__(self, other):
        return Vector(self.x + other.x, self.y + other.y)
    def __mul__(self, k):
        return Vector(self.x * k, self.y * k)
    def __eq__(self, other):
        return (self.x, self.y) == (other.x, other.y)
    def __abs__(self):
        return (self.x ** 2 + self.y ** 2) ** 0.5
    def __repr__(self):
        return f"Vector({self.x}, {self.y})"

a, b = Vector(1, 2), Vector(3, 4)
print(a + b, a * 3)
print(a + b == Vector(4, 6))
print(abs(Vector(3, 4)))
```

Without `__eq__`, `==` compares **identity** (is it the same object?), so two equal-looking vectors would compare `False`.

## Making objects behave like containers

```pyrun
class Playlist:
    def __init__(self, *songs):
        self.songs = list(songs)
    def __len__(self):
        return len(self.songs)
    def __getitem__(self, i):
        return self.songs[i]
    def __contains__(self, song):
        return song in self.songs

mix = Playlist("Intro", "Groove", "Outro")
print(len(mix), mix[1], "Groove" in mix)
for song in mix:          # __getitem__ also makes it iterable
    print("♪", song)
```

## Dataclasses: less boilerplate

For classes that mainly hold data, `@dataclass` writes `__init__`, `__repr__`, and `__eq__` for you:

```pyrun
from dataclasses import dataclass

@dataclass
class Student:
    name: str
    score: int = 0

s = Student("Ada", 95)
print(s, s == Student("Ada", 95))
print(sorted([Student("B", 70), Student("A", 90)], key=lambda st: -st.score))
```

<!-- quiz -->

Q: Which method does `print(obj)` use to get its text?
- [ ] `__init__`
- [x] `__str__` (falling back to `__repr__`)
- [ ] `__print__`
- [ ] `__len__`
> `print` calls `str(obj)`, which uses `__str__`, or `__repr__` if `__str__` isn't defined.

Q: Which dunder method makes `a + b` work for your class?
- [ ] `__plus__`
- [x] `__add__`
- [ ] `__sum__`
- [ ] `__concat__`
> `a + b` is translated into `a.__add__(b)`.

Q: Without defining `__eq__`, what does `Point(1, 2) == Point(1, 2)` return?
- [ ] `True`
- [x] `False`, because they're two different objects
- [ ] An error
- [ ] `None`
> The default `==` checks identity. Define `__eq__` (or use `@dataclass`) to compare by value.
