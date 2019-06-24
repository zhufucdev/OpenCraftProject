# _ServerScript Instructions_ ©
## 1. Self-introduction
>`To empower both players and operators` is the goal of _ServerScript_.
ServerScript, or _SS_, is a script language extended on JavaScript, used in OpenCraft Server, for every member.
With it, you can easily finish troublesome and boring jobs using a few lines of code.  
Here are some tutorials and examples.
## 2. Tutorials
**Notice: This is a tutorial. If you would like to see API References, go ahead to [API-list](#api-list)**
### For Common Players & Operators
#### Basics
##### Lessen One: Hello World!
The following program will make a single line of output.
```js
print('Hello World!')
```
You should see ``Hello World!`` in the chat.  
In this example, ``print`` is a **function**, which means printing the content inside the following parenthesis into the output stream.  
``'Hello World!'`` is a **string** constant. String is a set of characters. it can be carved up, merged, cut off freely using some functions. We will introduce them later.  
##### Lesson Two: Meet text-processing
The following program will make two lines of output.
```js
print('I am a player')
print('of OpenCraft Server')
```
You should see the following content in the chat:
```text
I am a player  
of OpenCraft Server
```
You can see that _SS_ code is executed top to bottom.
***
You can use ``;`` to merge the two lines into one.
```js
print('I am a player');print('of OpenCraft Server')
```
You should see the same result.  
***
Moreover, you can use ``+`` to put the two lines together.
```js
print('I am a player ' + 'of OpenCraft Server')
```
You should see ``I am a player of OpenCraft Server`` in the chat.  
##### Lesson Three: Variables & Arithmetic.
The following program will output **number** ``2019``.
```js
print(2019)
```
You can see that what in the parentheses is different from what we've done, as there is no ``'`` between ``(`` and ``)``.  
This is because ``2019`` is a number constant, which can be joined into arithmetic.  
For example:
```js
print(1 + 3)
```
You should see ``4`` instead of ``1 + 3`` in the chat.  
***
You can also do this:
```js
let a = 1, b = 3;
print(a + b)
```
You should see ``4`` in the chat instead of ``a + b``.  
**keyword** ``let`` means letting a variable be named by the following content. Then use ``=`` to give the variable a value.  
***
You can let a variable without giving value. For example:
```js
let a;
print(a)
```
You should see ``undefined`` in the chat, which is the default value of an empty variable of _JavaScript_.  
***
Here are some basis operational symbols:  

| Character  |    Name   |
|:----------:|:---------:|
|     +      |    plus   |
|     -      |  subtract |
|     *      |  multiply |
|     /      |   divide  |
|     %      | remainder |
They all follow the order of four fundamental operations of arithmetic. For example:
```js
2 * 3 + 4; // => 10
2 * (3 + 4) // => 14
```
##### Lesson Four: IF-statements
You can use **keyword** ``if`` together with ``{`` and ``}`` to tell computers to do something if the condition is satisfied. For example:
```js
if ('Hello' === 'hello') {
    print('Maybe.')
}
```
The program won't do anything.  
In this example, you've compared ``'hello'`` and ``'Hello'`` with **keyword** ``===``, which is used to compare the values of two sides. To achieve this, computer will compare each character. If there is any different pair, the condition ``'Hello' === 'hello'`` can be treated as ``false``, meaning not executing the following statments ``print('Maybe.')``.  
***
You can add ``else`` to an **IF-statement** to explain what to do if the condition is NOT satisified.
```js
if ('Hello' === 'hello') {
    print('Maybe.')
} else {
    print('Exactly.')
}
```
You should see ``Exactly.`` in the chat.
***
It should be noticed that ``{`` and ``}`` can be deleted where there is **only 1** statement. For example:
```js
if ('Hello' === 'hello')
    print('Maybe.')
else
    print('Exactly.')
```
You should have the same result.
***
Not only strings, but also numbers are **comparable**. For example:
```js
if (1 + 1 === 2)
    print('Exactly.')
```
And numbers have one more thing.
```js
if (1 + 1 > 2)
    print('Dear me.')
else if (2 + 2 <= 4)
    print('Exactly.')
else
    print('World of craze.')
```
You should get ``Exactly.`` in the chat.  
In this example, computer would first check whether ``1 + 1 > 2`` is ``true``. ``true`` is the opposite of ``false``, meaning the statements should be executed. Obviously ``1 + 1`` is not greater then ``2``, so the condition would be treated as ``false``, and then it would check the second condition. We know ``2 + 2`` is equal to ``4``, so the second condition is satisfied. As a result, the second statement would be executed, and the last statement would be ignored.  
Here are some comparing symbols of numbers:

| Character |          Name          |
|:---------:|:----------------------:|
|     >     |      Greater Than      |
|     <     |       Less Than        |
|    >=     |Greater Than Or Equal To|
|    <=     |  Less Than Or Equal To |
|   !==     |       Not Equal To     |
|   ===     |          Equal To      |

***
You can have several **ELSE-IF-statements**, but only one **IF-statement** and at most one **ELSE-statement**, otherwise, the _ServerScript Executor_ will throw an exception.
##### Lesson Five: WHILE-statements
**Keyword** ``while`` means keep executing the following statements until the condition is NOT satisfied. For example:
```js
let i = 0;
while(i <= 100)
    i = i + 1;
print(i)
```
You should get ``101`` as a result.  
In this example, computer would first let a variable ``i`` value ``0``, then enter the **WHILE-statement**. It would check whether ``i <= 100`` is ``true`` for the first time. It would get ``true``, and then execute the statement ``i = i + 1``, meaning add ``1`` into ``i``, getting ``1``. And then, it would go back to the ``while`` sentence at line 2 to check whether the condition is still satisfied. This would go on, util a **threshold**, when ``i`` was ``101``. This time, ``i <= 100`` would turn to ``false``, and the **WHILE-statement** would **break**.  
***
You can also write the example in this way:
```js
let i = 0;
while(i <= 100)
    i++;
print(i)
```
This is because **keyword** ``a ++`` means adding 1 into the number ``a``, which does exactly what ``a = a + 1`` does. Moreover, ``a += m`` means adding ``m`` into ``a``. So ``a += 1`` works as well.  
There are some similar usages. For example:

|Operation|  Meaning  |
|:-------:|:---------:|
|  a += m | a = a + m |
|  a -= m | a = a - m |
|  a *= m | a = a * m |
|  a /= m | a = a / m |
***
This program works too:
```js
let i = 0;
while(true){
    i++;
    if (i > 100)
        break;
}
print(i)
```
**Keyword** ``break`` means breaking the loop **manually**. In the first example, the loop would break when the condition is not satisfied, so we can just recover this process by using **IF-statement**:
```js
if (!(i <= 100))
```
which equals to
```js
if (i > 100)
```
You can also see that when ``A`` is ``true``, ``!A`` is ``false``. ``!`` is called **not**.
##### Lesson Six: FOR-statement
Some people think the **WHILE-statement** is confusing, so they've invented the **FOR-statement** especially for loop. For example:
```js
let s = 0;
for (let i = 1; i <= 100; i++)
    s += i;
print(s)
```
You should get ``5050`` as a result. This example gets the summation of 1 to 100.  
In it, the computer would first let a variable be named ``s`` and valued ``0``, then enter the **FOR-statement**. In the statement, variable ``i`` is let and valued ``1``. Firstly, computer would do the following statement ``s += i``, turning ``s`` into ``1``. Then it would execute ``i++``, turning ``i`` into ``2``. Thirdly, computer would check whether the condition ``i <= 100`` is satisfied. Obviously it would be, so it would go this way until ``i <= 100`` is not satisfied, when ``s`` is ``5050`` and ``i`` is ``101``.  
***
Compare to using **WHILE-statement**:
```js
let s = 0, i = 1;
while (i <= 100){
    s += i;
    i ++;
}
print(s)
```
**FOR-statement** is usually cleanner for loops with only **one independent variable**.
It should be noticed that the independent variable of loop is usually a **positive integer**.
##### Lesson Seven: Meet more types
Up to now, we have learnt several types of data.

|   Name  |               Regex of value            | Use |
|:-------:|:---------------------------------------:|:----|
| Boolean |                 true,false              |to present two opposite states, or tell whether a condition is satisfied or not.|
|  Number |-2^63+1 to 2^63-1,+Infinity,-Infinity,NaN|to present and calculate numbers.|
|   Null  |                   null                  |to present an empty variable.|
|Undefined|                 undefined               |to present an undefined variable.|
|  String |              Most characters            |to present characters.|
|  Symbol |               Not specific              |to create an unique value.|
|  Object |               Not specific              |to collect variables of these types together to make a new type.|
***
1. Type ``Number`` provides support for both integers and fractions.
2. To create a ``Symbol``, use ``Symbol()`` or ``Symbol(String annotation)``. If you don't understand this statement, go ahead to [Lesson Eight: Functions](#lesson-eight-functions).
3. Type ``Null`` and ``Undefined`` can be directly used by ``let a = null`` and ``let a = undefined``.
4. We will talk about ``Object`` later in this tutorial.
5. Type ``String`` can be directly defined by not only ``'Something'`` but also ``"Something"``, which have the same affect.
##### Lesson Eight: Functions
Sometimes you have to call some statements for several times.
```js
print('Wake Up!');
print('Wake Up!');
print('Wake Up!');
print('Wake Up!');
print('Wake Up!');
...
```
Then you may think of loops:
```js
for (let i = 0; i < 10; i++)
    print('Wake Up!');
```
This will make ten lines of outputs.  
But what if you have to do something a little different?  For example, to output several "Wake Up!" in the same line:
```js
function waken(last){
    if (last >= 10){
        return "Wake Up!";
    }
    return "Wake Up! " + waken(last + 1)
}
print(waken(1))
```
You should get ``Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up!`` as a result.
**Keyword** ``function`` allows you to define your own functions. For example,
To define function _f_ without parameters:
```js
function f(){
    //Do Something
}
```
To define function _f(x,y)_ with one parameter called _x_, the other called _y_:
```js
function f(x,y){
    //Do Something
}
```
**Keyword** ``return`` means setting the value of the whole function as the statement behind and not executing the remaining statements. For example, to define function _f(x,y)_ that returns the summation of ``x`` and ``y``:
```js
function f(x,y){
    return x + y;
    print("This won't be printed.")
}
```
***
Now let me show you how the first example works. I would like to paste the code again here.
```js
function waken(last){
    if (last >= 10){
        return "Wake Up!";
    }
    return "Wake Up! " + waken(last + 1)
}
print(waken(1))
```
 First, it would define function _waken(last)_. When calling it, computer would first check out whether ``last >= 10`` is ``true``, if not, it would return ``"Wake up! " + waken(last + 1)``, or it would return ``"Wake Up!"``  
After defining it, computer would want to ``print``, to achieve this, however, it had to work out what ``waken(1)`` is. So it would do. Let's call this process **the first call**. Because ``last`` was handed in as ``1``, ``last >= 10`` would be false, so the first return-statement would NOT be executed. It would turn to ``return "Wake Up! " + waken(last + 1)``. To achieve this, however, computer had to workout what ``waken(last + 1)`` is, so it would call the function ``waken`` again with ``last + 1`` which equals to ``2`` handed in. Let's call this process **the second call**. So it would go, until ``last >= 10``, when there was **the tenth call**, it wouldn't call the function ``waken`` anymore. **The tenth call** returned ``"Wake Up!"``, then **then ninth call**, **the eighth call**, to **the first call**, would ensure their own values.

|Order of being called|Return|
|:---:|:---:|
|10th|Wake Up!|
|9th|Wake Up! Wake Up!|
|8th|Wake Up! Wake Up! Wake Up!|
|...|...|
|1st|Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up! Wake Up!|
Finally, we would get the result as what **the first call** returned and print it out.
***
Functions can be treated as variable. For example:
```js
function bar(f){
    f()
}
let foo = function(){
    print('From a function variable.')
}
bar(foo)
```
You should get ``From a function variable.`` as a result.  
You can see that when calling a function, we use ``foo()``. When quoting a variable, however, we use ``foo``.  
You can also write the example in this way:
```js
function bar(f){
    f()
}
let foo = () => {
    print('From a function variable.')
}
bar(foo)
```
In this example, ``{`` and ``}`` can be deleted.
```js
...
let foo = () => print('From a function variable.');
...
```
You should have the same result.
##### Lesson Nine: Meet objects
``object`` is a type of data, which is used to collect other types together to make a new type. For example:
```js
let jobs = {
    firstName: 'Steve',
    lastName: 'Jobs',
    dateOfBirth: '1955/2/4',
    age: 56
};
print(jobs.lastName)
```
You should have ``Jobs`` in chat. This example uses an **Object Literal Constructor**.  
You can also define objects using  **Object Constructor**. For example:
```js
function Person(firstName, lastName, dateOfBirth, age){
    this.fistName = firstName;
    this.lastName = lastName;
    this.dateOfBirth = dateOfBirth;
    this.age = age
}
let jobs = new Person('Steve','Jobs','1955/2/4',56);
print(jobs.lastName)
```
You should have the same result.  
It's better to use **Object Constructor** to create more than one **instances**.
```js
...
let jobs = new Person('Steve','Jobs','1955/2/4',56);
let gates = new Person('Bill','Gates','1955/10/28',63);
let torvalds = new Person('Linus','Torvalds','1969/12/28',49);
...
```
***
Objects can have functions.
```js
let foo = {
    bar: () => print('From foo::bar.')
};
foo.bar();
function Foo(){
    this.bar = () => print('From foo created by constructor.')
}
let foo = new Foo();
foo.bar()
```
You should get the following content as a result:
```text
From foo::bar.
From foo created by constructor.
```
***
As you can see, you use ``foo.bar`` to quote the instance ``foo``'s **property** ``bar``. But what if you don't know the name of the property you want to quote? Read this example:
```js
let jobs = {
    firstName: 'Steve',
    lastName: 'Jobs',
    dateOfBirth: '1955/2/4',
    age: 56
};

for (key in jobs){
    print(key + ':' + jobs[key])
}
```
You should get the following content as a result:
```text
firstName:Steve
lastName:Jobs
dateOfBirth:1955/2/4
age:56
```
In this example, you used **FOR-statement** with ``in`` to list each of the properties' name, ``[`` and ``]`` to get their values. This is simmilar to **Array**.
##### Lesson Ten: Meet arrays
**Array** is a set of data, which can be quoted using numbers. For example:
```js
let corporations = new Array("Apple", "Google", "Microsoft");
print(corporations[1])
```
You should get ``Google`` in the chat.  
You may get confused, because "Google" is the second item in the array ``corporations``, however, the **index** number is ``1``. Then you can see the index of an item in an array is **one less** than its order. In the case, you should get ``Apple`` with ``corporations[0]`` and ``Microsoft`` with ``corporations[2]``.
***
Just like what you've done to an object, you can look through an array as well. For example:
```js
let corporations = new Array("Apple", "Google", "Microsoft");
for (i in corporations){
    print(i + ':' + corporations[i])
}
```
Because arrays use numbers to index, you can achieve the last example in this way:
```js
let corporations = new Array("Apple", "Google", "Microsoft");
for (let i = 0; i < corporations.length; i++){
    print(i + ':' + corporations[i])
}
```
You should have the same result.  
It should be noticed that ``corporations[corporations.length]`` is ``undefined``, because the last index would be ``corporations.length - 1``.
***
I've introduced using functions as a variable to you already. And because of this feature of _SS_, you can use this method to go through an array:
```js
let corporations = new Array("Apple", "Google", "Microsoft");
corporations.forEach(
    (item, index) => print(index + ':' + item)
)
```
You should get the same result as the last example.  
We call this feature **lambda**.

##### Lesson Eleven: Controlling an array
You first defined an object like this:
```js
function Fruit(name, color){
    this.name = name;
    this.color = color;
}
```
Then you created an array:
```js
...
let fruits = new Array(
    new Fruit('apple', 'red'), new Fruit('pear', 'green'), 
    new Fruit('watermelon', 'light-dark-green')
)
```
However, during the execution of the program, you wanted to add 'pineapple' to the array ``fruits``. You can achieve this by using ``Array.push(element1[, element2[, ...]])``. For example:
```js
...
fruits.push(new Fruit('pineapple','yellow'))
```
This statement will add 'pineapple' to the end of the array ``fruits``. Moreover, to delete the last element, you can use ``Array.pop()``.  
You then wanted to add 'grape' to the beginning of the array. Use ``Array.unshift(element1[, element2[, ...]])``:
```js
...
fruits.unshift(new Fruit('grape','purple'))
```
To delete the first element, you can use ``Array.shift()``.  
These four operations all will change the length of the array.

***
Finally, you wanted to modify the second element. For example:
```js
...
fruits[1] = 'Banksia grandis'
```
Then you can see that an array MUSTN'T have all data of the same type.
##### Lesson Twelve: Searching an array
One day, you'd received an unknown array of fruits from a server player, and you'd like to check out whether there is ``Banksia grandis`` or not. You can achieve it in this way:
```js
...
function BG(element){
  return element === 'Banksia grandis' || element.name === 'Banksia grandis'
}
if (fruits.some(BG)){
  print("I haven't seen why to talk about Banksia grandis up to now.")
} else {
  print('Yeah!')
}
```
You should get ``I haven't seen why to talk about Banksia grandis up to now.`` if the array has ``Banksia grandis``, or you should get ``Yeah!``.
In this example, you used **lambda** to tell whether **one of** the elements in the array ``fruit`` satisfied the conditions. Or easilier, lambda can **define** conditions.
To understand what really happened, I would like to show you the following program:
``` js
...
function BG(element){
  return element === 'Banksia grandis' || element.name === 'Banksia grandis'
}
let satisfied = false
for (i in fruits){
  let element = fruits[i];
  if (BG(element)){
    satisfied = true;
    break
  }
}
if (satisfied){
  print("I haven't seen why to talk about Banksia grandis up to now.")
} else {
  print('Yeah!')
}
```
You should get the same result.
In this example, ``satisfied`` would be defined and valued ``false``. Then it would enter a **FOR-statement**. In it, computer would first define variable ``element`` valued ``fruit[i]``, which might be **each** of the array elements in turn. Then, to tell whether to execute the **IF-statements**, computer would first work out the value of ``BG(element)``. This would lead to a call to function ``BG``, with parameter ``element`` handed in. Inside the function, it would return ``element === 'Banksia grandis' || element.name === 'Banksia grandis'``. Let me show you its meaning.
***
You can treat this statement as two parts, ``element === 'Banksia grandis'`` and  `` element.name === 'Banksia grandis'``, divided by ``||``, which means **return ``true`` if one or two of the conditions beside is satisfied, return ``false`` otherwise**. So ``||`` is called **OR**.
To its opposite, there is ``&&``, which means **return ``true`` if both of the conditions beside are satisfied, return ``false`` otherwise**. So ``&&`` is called **AND**.
Together with ``!``, which is called **NOT**, you can tell logical operatings clearly. These three symbols are called **Logical Operators**. Based on them, there are some common combinations. ``!(A||B)``, for example, means if neither of ``A`` and ``B`` is satisfied, which is called **NOR**. And so on, you can see what ``!(A&&B)`` means.
To express more complex conditions, use ``(`` and ``)``. This is simmilar to mathematical expressions. For example, ``!(A&&B) || (C&&D)`` returns ``true`` only when **either or nor** of ``A`` and ``B`` is satisfied **or** **both** of ``C`` and ``D`` are satisfied. You can try to write something like this yourself.
In this way, logical operators can be very very complex. So remember to think twice about the meaning when you meet logical operators next time.
##### Conclusion
That's all for what I want to talk about Basics. And I get some quiz for you. I recommend you to try working them out.
If you can workout most of the following questions yourself, you can begin learning _ServerScript_ API.
1. Output prime numbers that are less than a hundred. [Show answer](#answer-1)
2. Output the first 20 of Fibonacci numbers. If you don't know what is Fibonacci number, search the internet, or watch the following examples ``1,1,2,3,5,8,13...``. [Show answer](#answer-2)
3. There are several groups of numbers. First output the smallest element in each group, then output the biggest one among the smallest ones you've just output. [Show answer](#answer-3)
#### _ServerScript_ API Tutorial
**Notice: This is a tutorial. If you would like to see API Reference, go ahead to [API-list](#api-list)**.
##### Lesson One: Understanding permission
I've shown **WHILE-statements** to you. You can see that a ``while(true){ }`` statement can never stop, taking up much server resource. To prevent simmilar things happening, we've designed a tool to limit players' rights. For example:
```js
for(;;) print('LOL')
```
You should get the following content as a result.
```text
LOL
LOL
LOL
...
LOL
LoopExecutionOutOfBoundError: You can only execute loops for 1000 times.
```
In this example, ``for(;;)`` would be another way to construct an unbreakable loop. Computer would keep printing ``'LOL'`` until touching the boundary.
Also, to use some **methods**, the permissions you need are written in the API List.
***
There are three different groups of script executors in server. They are **Player, Operator, and Server**. Operators have the most methods usable, and Players have the least. This is simmilar to Minecraft command permission control. Then I'll introduce these methods to you.
##### Lesson Two: Meet ``self``
Now, it's time to write a server script. Before first, you should learn object ``self``.  
``self`` is defined as part of _SS_, so it can be used everywhere. It represents the executor of the current script itself. By calling it, you can get and set some of its properties. The following program shows how to print the executor's name.
```js
print(self.getName())
```
As you can see, the style of naming in ``self`` obey **Camel Case**. Basis patterns are:  
To get an property _X_, use ``getX()``. To set an property _X_ as _Y_, use ``setX(Y)``. To call method about ax, by, cz use ``axByCz(...)``.  
These patterns can be applied to many other methods in _SS_.  

# API List
**Notice: You should read this only after learning _SS_ or if you know what you are doing.**
## PlayerSelf self
##### Source
> com.zhufu.opencraft.headers.player_wrap.PlayerSelf
##### Permission
> Everyone

This object doesn't have a constructor.  
It's used to check out and control some of the information of executor of the current script.
### Methods
#### String getName()
##### Returns
> **The name of the executor** in string.
#### String getNickname()
##### Returns
> **The nickname of the executor** which can be changed in the [Web Interface](https://www.open-craft.cn).
#### Boolean isLogin()
##### Returns
> **true** if the executor is log in, **false** otherwise.
#### Boolean isRegistered()
##### Returns
> **true** if the executor has already registered, **false** otherwise.
#### getUUID()
##### Returns
> **The UUID of the executor** if it has ever been in server, **null** otherwise.
#### Number getGameTime()
##### Returns
> **Time in millisecond that the executor has spent in survival world**.
#### String getLanguage()
##### Returns
> **Code of the language** the executor is currently using.

|    Language   | Code |
|:-------------:|:----:|
|    简体中文    |  zh  |
|    English    |  en  |
#### void setLanguage(String code)
##### Parameter ``code``
> Code of the language to set. This operation should be applied in at most 2 seconds.
#### String getState()
##### Returns
> **Current state of the executor** in string.

|State|When it will be held|
|:---:|:------------------:|
|InLobby|When the player is in **world**.|
|MiniGaming|When the player is having a mini game.|
|Surviving|When the player is in **world_survive**|
|Observing|When the player has used ``/user observe ...`` and hasn't exited yet.|
|InTutorial|When the player is playing a tutorial.|
|Building|When the player has used ``/builder`` and hasn't exited yet.|
|Offline|When the executor is not in server.|
#### Number getGameTimeToday()
##### Returns
> **Time in millisecond that the executor has spent in server today** if it has already registered, **null** otherwise.
#### PlayerInventory getInventory()
##### Returns
> **The inventory object of the executor**. See [Player Inventory](#object-playerinventory).
#### Location getLocation()
##### Returns
> **Location where the executor currently is** if it is in server, **null** otherwise.
#### String getGameMode()
##### Returns
> **Game mode the executor has** in string if it is in server, **null** otherwise.  
> All possible results:
- creative
- survival
- spectator
#### Number getYaw()
##### Returns
> **Yaw the executor currently has** if it is in server, **null** otherwise. You can see it through the F3 Debug Menu.
#### void setYaw(Number yaw)
##### Parameter ``yaw``
> Yaw to set for the executor if it is in server.  
> Notice: Call [setYawAndPitch](#void-setyawandpitchnumber-yaw-number-pitch) if you would like to set both of them together.
##### Throws
> **java.lang.IllegalArgumentException** if the executor isn't in server.
#### Number getPitch()
##### Returns
> **Pitch the executor currently has** if it is in server, **null** otherwise. You can see it through the F3 Debug Menu.
#### void setPitch(Number pitch)
##### Parameter ``pitch``
> Pitch to set for the executor if it is in server.  
> Notice: Call [setYawAndPitch](#void-setyawandpitchnumber-yaw-number-pitch) if you would like to set both of them together.
##### Throws
> **java.lang.IllegalArgumentException** if the executor isn't in server.
#### void setYawAndPitch(Number yaw, Number pitch)
To change Yaw and Pitch together for a player, use this method.
#### Number getLevel()
#### Returns
> **Current experience level of the executor** if it is in server, **null** otherwise.
#### Number getFoodLevel()
#### Returns
> **Current food level of the executor** if it is in server, **null** otherwise.
#### Location getSpawnpoint()
##### Returns
> **Spawnpoint of the executor in world_survive** if it has ever been there, **null** otherwise.
#### Number getCurrency()
##### Returns
> **Number of coins the executor currently has**.
### Number getMaxLoopExecution()
##### Returns
> **The maximum number of times to execute loop statements in one script**, default by ``1000``. 
### Info self.getInfo()
##### Permission
> Operator
##### Returns
> **Info of the executor** if it is permitted. See [Info](#object-info).
##### Throws
> **java.lang.IllegalAccessError** if the executor is not permitted.
### Info self.getPlayer()
##### Permission
> Operator
##### Returns
> **Player of the executor** if it is permitted. See [Player](#player-playerstring-name).
##### Throws
> **java.lang.IllegalAccessError** if the executor is not permitted.

## Object info
##### Source
> com.zhufu.opencraft.Info.Companion
##### Permission
> Operator, Server

This object doesn't has a constructor.  
It's used to get player's server profile called **Info**.
### Methods
#### Info findByName(String name)
##### Parameter ``name``  
> Name of the player to look for.
##### Returns
> **Info of the player** if it is online, **null** otherwise.
#### Info findByUUID(UUID uuid)
##### Parameter ``uuid``
> The UUID of the player to look for.  
> See [UUID](#object-uuid)
##### Returns
> **Info of the player** if it is online, **null** otherwise.
#### Array getInfoList()
##### Returns
> A list of Info of online players'.
## Object PlayerInventory
##### Permission
> Player, Operator
##### Source
> com.zhufu.opencraft.headers.player_wrap.PlayerInventory
### Methods
#### String getName()
##### Returns
> **Name of the inventory**.
#### Array getItems()
##### Returns
> **Array of items in the inventory**. The items are [SimpleItemStack](#object-simpleitemstack).
#### void sort()
Sort the inventory according to item's ordinal in [org.bukkit.Material](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html).
#### void sort(function(SimpleItemStack)=>Number)
Sort the inventory according to the given function.
##### Parameter ``function(SimpleItemStack)=>Number``
> Parameter ``SimpleItemStack``  
> The item to be sorted.

> Returns number of **relative** ordinal for the given ``SimpleItemStack`` to be sorted into. The less the number is, the closer the item is to slot zero in the following picture.
![](https://gamepedia.cursecdn.com/minecraft_zh_gamepedia/b/b2/Items_slot_number.png "Picture taken from Minecraft Wiki")

## Object UUID
##### Source
> com.zhufu.opencraft.headers.util.UUIDUtils  
##### Permission
> Everyone

This object doesn't have a constructor.  
It's used to create UUIDs for unique symbol of each player.
### Methods
#### UUID fromString(String src)
##### Parameter ``src``
> The string of the UUID to create.
##### Returns
> **UUID** if ``src`` presents a UUID, **null** otherwise.
#### UUID random()
##### Returns
> A random UUID that has never been created.
## Other Methods
### Player Player(String name)
It's used to get the instance of an online player by its name.
##### Source
> com.zhufu.opencraft.headers.ServerHeaders
##### Permission
> Operator, Server
##### Parameter ``name``
> The name of the player you'd like to look for.
##### Returns
> **The instance of the player** if it's online, **null** otherwise.
### OfflinePlayer OfflinePlayer(String name)
It's used to get the instance of an offline player by its name.
##### Source
> com.zhufu.opencraft.headers.ServerHeaders
##### Permission
> Operator, Server
##### Parameter ``name``
> The name of the player you'd like to look for.
##### Returns
> **The instance of the player** if it exists, **null** otherwise.
# Appendix
## Answers
I write only one of the methods to reach the answer for each question, so don't worry about the difference. You can run your program to check whether it is right or not.
### answer 1
```js
function get() {
	let s = 1;
	function add() {
		s++;
		for (let i = 2; i < s; i++)
			if(s % i === 0)
				return s;
		return add()
	}
	return add
}
let add = get();
for (;;){
	let r = add();
	if (r < 100)
		print(r);
	else
		break;
}
```
The output should contain ``4 6 8 9 10 12 14 15 16 18 20 21 22 24 25 26 27 28 30 32 33 34 35 36 38 39 40 42 44 45 46 48 49 50 51 52 54 55 56 57 58 60 62 63 64 65 66 68 69 70 72 74 75 76 77 78 80 81 82 84 85 86 87 88 90 91 92 93 94 95 96 98 99``.
### answer 2
```js
let a = b = 1;
for (let i = 1; i <= 20; i ++){
	print(a); print(b);
	a += b; b += a;
}
```
The output should contain ``1 1 2 3 5 8 13 21 34 55 89 144 233 377 610 987 1597 2584 4181 6765 10946 17711 28657 46368 75025 121393 196418 317811 514229 832040 1346269 2178309 3524578 5702887 9227465 14930352 24157817 39088169 63245986 102334155``
### answer 3
If there are three groups.

|  Group |  	Members	    |
| :----: | :--------------: |
| Group1 |  38,10,27,1024,0 |
| Group2 |  	127,1,72    |
| Group3 |     -9,7,0,-91   |
```js
let groups = new Array(
	new Array(
		38,10,27,1024,0
	),
	new Array(
		127,1,72
	),
	new Array(
		-9,7,0,-91
	)
);
let smallests = new Array();
groups.forEach(
	(group, index) => {
		let smallest = group[0];
		for (let i = 1; i < group.length; i++)
			if (group[i] < smallest)
				smallest = group[i];
		smallests[index] = smallest;
		print(smallest)
	}
);
let biggest = smallests[0];
for (let i = 1; i < smallests.length; i++)
	if (smallests[i] > biggest)
		biggest = smallests[i];
print(biggest)
```
The output sholud be
```text
0
1
-91
1
```