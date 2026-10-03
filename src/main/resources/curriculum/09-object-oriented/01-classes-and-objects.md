---
title: Classes and Objects
minutes: 10
practice: simulation
---
A **class** is a blueprint; an **object** (or *instance*) is a thing built from it. The class defines what data each object holds (**attributes**) and what it can do (**methods**).

```pyrun
class Dog:
    def __init__(self, name, age):
        self.name = name          # attributes stored on this object
        self.age = age

    def bark(self):               # a method
        return f"{self.name} says woof!"

    def birthday(self):
        self.age += 1

rex = Dog("Rex", 3)
fido = Dog("Fido", 7)
print(rex.bark())
rex.birthday()
print(rex.name, rex.age)
print(fido.name, fido.age)        # fido is a separate object
```

## `__init__` and `self`

- `__init__` is the **constructor**. It runs automatically when you call `Dog(...)` and sets up the new object.
- `self` is the object the method is working on. Python passes it automatically: `rex.bark()` is really `Dog.bark(rex)`.
- `self.name = name` stores the value **on the object**, so other methods can use it later.

## A more useful example: a bank account

```pyrun
class BankAccount:
    interest_rate = 0.02              # class attribute: shared by all accounts

    def __init__(self, owner, balance=0):
        self.owner = owner
        self.balance = balance
        self.history = []             # each account gets its own list

    def deposit(self, amount):
        if amount <= 0:
            raise ValueError("deposit must be positive")
        self.balance += amount
        self.history.append(("deposit", amount))

    def withdraw(self, amount):
        if amount > self.balance:
            raise ValueError("insufficient funds")
        self.balance -= amount
        self.history.append(("withdraw", amount))

    def add_interest(self):
        self.balance = round(self.balance * (1 + self.interest_rate), 2)

acct = BankAccount("Ada", 100)
acct.deposit(50)
acct.withdraw(30)
acct.add_interest()
print(acct.owner, acct.balance)
print(acct.history)
```

## Why use classes?

- **Bundling:** data and the functions that use it live together.
- **Invariants:** methods like `withdraw` enforce rules, such as "balance never goes negative".
- **Many instances:** create as many accounts as you like, each with its own state.

For short exercise solutions you rarely need classes, but they're essential for organizing bigger programs. PyJudge itself is built from classes like `User`, `Problem`, and `Submission`.

<!-- quiz -->

Q: What is `self` inside a method?
- [ ] The class itself
- [x] The specific object the method was called on
- [ ] A reserved keyword that must be typed in capitals
- [ ] The method's return value
> When you call `rex.bark()`, Python passes `rex` as `self`.

Q: When does `__init__` run?
- [ ] When the class is defined
- [x] Automatically, each time a new object is created
- [ ] Only if you call `obj.__init__()` yourself
- [ ] When the program ends
> Calling the class, like `Dog("Rex", 3)`, creates an object and runs `__init__` to set it up.

Q: Two accounts `a` and `b` are created from `BankAccount`. After `a.deposit(10)`, what happens to `b.balance`?
- [ ] It also increases by 10
- [x] Nothing, because each object has its own `balance` attribute
- [ ] It becomes 0
- [ ] It raises an error
> Attributes assigned through `self` belong to each instance separately.
