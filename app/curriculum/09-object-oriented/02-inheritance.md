---
title: Inheritance
minutes: 8
practice: simulation
---
**Inheritance** lets a new class reuse and extend an existing one. The new class is a **subclass** (child); the original is the **superclass** (parent).

```pyrun
class Animal:
    def __init__(self, name):
        self.name = name

    def speak(self):
        return "..."

    def introduce(self):
        return f"I am {self.name} and I say {self.speak()}"

class Cat(Animal):          # Cat inherits from Animal
    def speak(self):        # override the parent's method
        return "meow"

class Cow(Animal):
    def speak(self):
        return "moo"

for pet in [Cat("Tom"), Cow("Daisy"), Animal("Mystery")]:
    print(pet.introduce())
```

`Cat` didn't define `introduce` or `__init__`. It inherited them. But when `introduce` calls `self.speak()`, Python uses the **subclass's** version. This is called **polymorphism**: the same call behaves differently depending on the object.

## Extending with `super()`

Use `super()` to run the parent's version of a method and then add more:

```pyrun
class Employee:
    def __init__(self, name, salary):
        self.name = name
        self.salary = salary

    def describe(self):
        return f"{self.name} earns {self.salary}"

class Manager(Employee):
    def __init__(self, name, salary, team):
        super().__init__(name, salary)   # let Employee set name and salary
        self.team = team

    def describe(self):
        return super().describe() + f" and manages {len(self.team)} people"

m = Manager("Grace", 9000, ["Ann", "Bob"])
print(m.describe())
print(isinstance(m, Employee), isinstance(m, Manager))
```

## "Is-a" vs "has-a"

Inherit when the child truly **is a** kind of the parent: a `Manager` *is an* `Employee`. When one thing just **has** another, like a `Car` *has an* `Engine`, store it as an attribute instead. That's called **composition**, and it's often the simpler choice.

## Exceptions use inheritance too

```pyrun
print(issubclass(ZeroDivisionError, ArithmeticError))
print(issubclass(KeyError, LookupError), issubclass(IndexError, LookupError))
try:
    [1, 2][9]
except LookupError:          # catches IndexError AND KeyError
    print("caught via the parent class")
```

<!-- quiz -->

Q: Given `class B(A): pass`, which statement is true?
- [ ] `A` gets all of `B`'s methods
- [x] `B` gets all of `A`'s methods
- [ ] `A` and `B` can't both be used
- [ ] `B` must define `__init__`
> A subclass inherits everything from its parent and can override or add methods.

Q: What does `super().__init__(name)` do inside a subclass's `__init__`?
- [ ] Creates a second object
- [x] Runs the parent class's `__init__` on this same object
- [ ] Deletes the parent class
- [ ] Calls `__init__` recursively forever
> It reuses the parent's setup code, so you don't duplicate it.

Q: Which relationship fits **composition** (has-a) better than inheritance?
- [ ] `Square` and `Shape`
- [ ] `Manager` and `Employee`
- [x] `Car` and `Engine`
- [ ] `Cat` and `Animal`
> A car *has* an engine; it isn't a kind of engine. Store the engine as an attribute.
