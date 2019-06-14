# _ServerScript 说明_ ©
## 1. 介绍
>`给玩家和管理员助力` 是 _ServerScript_ 的目标。
ServerScript, 即 _SS_, 是一个基于JavaScript的, 应用于OpenCraft服务器, 为每个成员提供服务的脚本语言。
有了它, 你可以仅用几行代码完成许多麻烦和无聊的工作。  
以下是一些教程和实例。
## 2. 教程
**注意: 这是教程。如果您想浏览API说明的话, 前往[API-list](#api-list)**。
### 对于普通玩家和管理员
#### 基础知识
##### 第一课：你好世界！
以下程序将做单行输出。
```js
print('你好世界！')
```
你应该在聊天栏里看到 ``你好世界！``。
在这个例子里，``print``是一个**函数(function)**，意思是将紧随其后的括号中的内容打印至输出流当中。  
``你好世界！``是一个**字符串(string)** 常数(constant)。字符串是字符的集合，可以用一些函数自由地拼接、分割和合并，我们将之后介绍他们。    
##### 第二课：参与文字处理
下面的程序将做双行输出。
```js
print('我是OpenCraft服务器')
print('的玩家')
```
你应该在聊天栏里看到以下内容：
```text
我是OpenCraft服务器
的玩家
```
正如你所见，_SS_ 代码总是从上至下执行。
***
你可以用``;``将两行代码合二为一。
```js
print('我是OpenCraft服务器');print('的玩家')
```
你应该得到同样的结果。
***
此外，你可以用``+``将两行文字输出在一行。
```js
print('我是OpenCraft服务器' + '的玩家')
```
你应该在聊天栏里看到 ``我是OpenCraft服务器的玩家``。  
##### 第三课：变量和运算
以下程序将输出**数字** ``2019``.
```js
print(2019)
```
你会发现括号里的内容和我们曾做过的有所不同，因为在``(``和``)``之间没有出现``'``。
这是因为``2019``是一个**数字(number)**常量，可以参与运算。
例如：
```js
print(1 + 3)
```
在聊天栏里，你应该看到 ``4`` 而不是 ``1 + 3``。
***
你亦可如此为止：
```js
let a = 1, b = 3;
print(a + b)
```
你应该在聊天栏里看到 ``4`` 而不是 ``a + b``.  
**关键字(keyword)** ``let`` 意思是令一个变量，并用后面的内容命名。随后使用``=``给这个变量一个值。
***
你可以不给变量赋值。例如：
```js
let a;
print(a)
```
你在聊天栏应该看到``undefined``。它是 _JavaScript_ 中空变量的默认值。
***
以下是一些运算符和它们的名称：  

| 字符  |    名称   |
|:----------:|:---------:|
|     +      |    加   |
|     -      |  减 |
|     *      |  乘 |
|     /      |   除  |
|     %      | 取余数 |
它们都遵循四则运算法则。例如：
```js
2 * 3 + 4; // => 10
2 * (3 + 4) // => 14
```
##### 第四课：条件陈述
你可以使用 **关键字** ``if`` 和 ``{`` 以及 ``}`` 告诉电脑如果条件满足了，应该做些什么。例如：
```js
if ('您好' === '你好') {
    print('也许吧。')
}
```
此程序什么都不会做。  
在这则例子中，你对比了``'您好'``和``'你好'``，使用的**关键字**是``===``。它用于比较两边的值，过程中电脑将比较每一个字符。如果有任意一对不相同，条件``'您好' === '你好'``即可被认为是``false``（假），意味着不要执行从句``print('也许吧。')``。
***
你可以为**条件陈述**添加``else``来解释当条件**不满足**时应该做什么。
```js
if ('您好' === '你好') {
    print('也许吧。')
} else {
    print('确实。')
}
```
你应该在聊天栏里看到``确实。``。
***
值得注意的是``{`` 和 ``}``当**只有一个从句**时可省略。例如：
```js
if ('您好' === '你好')
    print('也许吧。')
else
    print('确实。')
```
你应该得到同样的结果。
***
不仅是字符串，数字也是**可比较的**。例如：
```js
if (1 + 1 === 2)
    print('的确。')
```
并且数字有 _One More Thing_。
```js
if (1 + 1 > 2)
    print('天呐。')
else if (2 + 2 <= 4)
    print('确实。')
else
    print('疯狂的世界啊。')
```
你应该在聊天栏中得到 ``确实。``。
在这则例子中，电脑会首先检查``1 + 1 > 2``是否为``true``。``true``（真）是``false``的反面，意味着从句应该被执行。显然，``1 + 1``没有比``2``大，因此条件即可认为是``false``，随后电脑会检查第二个条件。我们知道``2 + 2``等于``4``，所有第二个条件满足。最终，第二个从句会被执行，而最后一个从句则会被跳过。
以下是一些数字的比较符：

| 字符 |          名称          |
|:---------:|:----------------------:|
|     >     |      大于      |
|     <     |       小于        |
|    >=     |大于等于|
|    <=     |  小于等于 |
|   !==     |       不等于     |
|   ===     |          等于      |

***
**否则如果(else if)陈述**可以出现多次，但**如果(if)陈述**只出现一次，而**否则(else)陈述**最多出现一次，否则 _ServerScript 执行器_ 将会抛出异常。
##### 第五课：WHILE循环陈述
**关键字** ``while`` 意思是在随后的条件**不**满足之前一直执行从句。例如：
```js
let i = 0;
while(i <= 100)
    i = i + 1;
print(i)
```
结果你应该得到``101``。
这个例子中，电脑会首先令一个变量，赋值为``0``，随后进入**WHILE循环陈述**。第一次将会检查条件``i <= 100``是否为``true``，得到``true``之后就会执行``i = i + 1``，意味着给``i``加上``1``得到``1``。随后，电脑将回到第二行的``while``句子来检查条件是否仍然满足。这将会持续到一个**临界值**，当``i``等于``101``的时候，``i <= 100``会变为``false``，然后**WHILE循环陈述**将被**打破(break)**。 
***
你也可以这么写上述例子：
```js
let i = 0;
while(i <= 100)
    i++;
print(i)
```
因为**关键字** ``a ++``的意思是给数字``a``加上``1``，相当于``a = a + 1``。此外，``a += m``的意思是给``a``加上``m``，所以``a += 1``亦可成立。
还有一些相似的用法：

|操作符|  意思  |
|:-------:|:---------:|
|  a += m | a = a + m |
|  a -= m | a = a - m |
|  a *= m | a = a * m |
|  a /= m | a = a / m |
***
此程序也能胜任：
```js
let i = 0;
while(true){
    i++;
    if (i > 100)
        break;
}
print(i)
```
**关键字** ``break`` 意味着**手动**打破循环。在第一个例子中，循环是在条件不满足的时候打破的，所以我们可以通过**条件陈述**来还原这个过程：
```js
if (!(i <= 100))
```
这相当于
```js
if (i > 100)
```
如你所见，当``A``为``true``时，``!A``为``false``。符号``!``被称为**非(NOT)**。
##### 第六课：FOR循环陈述
有些人认为**WHILE循环陈述**太蛊人了，他们为此发明了**FOR循环陈述**。例如：
```js
let s = 0;
for (let i = 1; i <= 100; i++)
    s += i;
print(s)
```
结果应该为``5050``。该例子得到的是1到100的和。
其中，电脑会首先令变量名为``s``，并赋值为``0``，随后进入**FOR循环陈述**。陈述当中，变量``i``被赋值为``1``。首先，电脑将会执行陈述``s += i``，将``s``变为``1``。随后会执行``i++``，将``i``变为``2``。第三步，电脑会检查条件``i <= 100``是否仍然满足，显然满足，便以此往复，直到``i <= 100``不再满足。此时，``s``是``5050``而``i``是``101``。
***
对比使用**WHILE循环陈述**:
```js
let s = 0, i = 1;
while (i <= 100){
    s += i;
    i ++;
}
print(s)
```
**FOR循环陈述**对于只有**一个自变量的**循环而言通常更加更加清晰。
值得注意的是循环的自变量通常是**正整数**。
##### 第七课：认识更多数据类型
至今为止，我们已经学习了几种数据类型：

|   名称  |               取值范围            | 用途 |
|:-------:|:---------------------------------------:|:----|
| 布尔Boolean |                 true,false              |表示两种相对的状态，或者代表某个条件是否满足。|
|  数字Number |-2^63+1 to 2^63-1,+Infinity,-Infinity,NaN|表示数字和参与计算。|
|   空Null  |                   null                  |表示变量为空。|
|为定义Undefined|                 undefined               |表示变量未定义。|
|  字符串String |              Most characters            |表示字符。|
|  符号Symbol |               Not specific              |创建唯一量。|
|  对象Object |               Not specific              |将这些类型集中到一起，创建一种新类型。|
***
1. 类型``数字Number``同时支持整数和分数。
2. 要创建``符号Symbol``，使用``Symbol()`` 或 ``Symbol(String 注释)``。如果你不理解此陈述，访问[第八课：函数](#第八课：函数)。
3. ``空Null`` 和 ``为定义Undefined``可以直接通过``let a = null`` 以及 ``let a = undefined``来使用。
4. 我们将在教程后期谈论``对象Object``。
5. ``String``可直接通过``'Something'``及``"Something"``定义，两者效果相同。
##### 第八课：函数
有时一个陈述需要调用许多次。
```js
print('起床了！');
print('起床了！');
print('起床了！');
print('起床了！');
print('起床了！');
...
```
Then you may think of loops:
```js
for (let i = 0; i < 10; i++)
    print('起床了！');
```
这将会做十行输出。
但如果要做一些不同的事情呢？比方说，在同一行输出几遍“起床了！”：
```js
function waken(last){
    if (last >= 10){
        return "起床了！";
    }
    return "起床了！" + waken(last + 1)
}
print(waken(1))
```
结果应为 ``起床了！起床了！起床了！起床了！起床了！起床了！起床了！起床了！起床了！起床了！``。
**关键字** ``function``允许你定义自己的函数。例如：
要定义没有参数的函数_f_：
```js
function f(){
    //做些什么
}
```
要定义函数 _f(x,y)_，其中一个参数为_x_，另一个为_y_：
```js
function f(x,y){
    //做些什么
}
```
**关键字** ``return``意味着将整个函数的值设置为其后的表达式，同时不执行后面的称述。例如定义函数 _f(x,y)_使其返还``x``和``y``的和：
```js
function f(x,y){
    return x + y;
    print("这不会被打印。")
}
```
***
现在，让我向你展示第一个例子是如何工作的。我想在这再粘贴一遍代码。
```js
function waken(last){
    if (last >= 10){
        return "起床了！";
    }
    return "起床了！" + waken(last + 1)
}
print(waken(1))
```
首先定义了函数 _waken(last)_。调用它时，电脑首先会检查``last >= 10``是否为``true``。如果不是，程序会返还``"起床了！" + waken(last + 1)``，否则会返还``"起床了！"``  。
定义完成后，电脑会想要``print``。然而，为了实现这一点，必须先求出``waken(1)``的值，电脑会这么做。让我们把这一过程称为**第一次调用**。因为``last``传入了``1``，所以``last >= 10``会是``false``，第一个返还陈述不会执行。电脑则会转向``return "起床了！" + waken(last + 1)``，然而要执行这句陈述，它又必须求出``waken(last + 1)``的值，所以调用函数``waken``又传入了``last + 1``，这相当于传入``2``。让我们称此过程为**第二次调用**。程序将以此往复，直到**第十次调用**时，``last >= 10``，电脑就不再调用``waken``函数了。**第十次调用**返还``"起床了！"``，然后**第九次调用**、**第八次调用**、一直到**第一次调用**的值都会确定。
|调用的次序|返还值|
|:---:|:---:|
|第十次|起床了！|
|第九次|起床了！起床了！|
|第八次|起床了！起床了！起床了！|
|...|...|
|第一次|起床了！起床了！起床了！起床了！起床了！起床了！起床了！起床了！起床了！起床了！起床了！|
最终，我们会得到**第一次调用**的值作为结果，并把它打印出来。
***
函数也可以被认为是变量。例如：
```js
function bar(f){
    f()
}
let foo = function(){
    print('来自变量的函数。')
}
bar(foo)
```
你的结果应该为``来自变量的函数。``。
正如你所见，在调用函数时我们用``foo()``，而在引用变量时，我们用``foo``。
你亦可如此完成该例子：
```js
function bar(f){
    f()
}
let foo = () => {
    print('来自变量的函数。')
}
bar(foo)
```
在该例子中，``{``和``}``可以省略。
```js
...
let foo = () => print('来自变量的函数。');
...
```
结果应该是相同的。
##### 第九课：认识对象
``object``（对象）是一类用于收集其他类型数据，然后创建新类型的数据。例如：
```js
let jobs = {
    firstName: 'Steve',
    lastName: 'Jobs',
    dateOfBirth: '1955/2/4',
    age: 56
};
print(jobs.lastName)
```
你的聊天栏里应该有``Jobs``。这个例子使用的是**字面对象构造器**。
亦可使用**对象构造器**定义对象。例如：
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
结果应该是相同的。
要创建多个**实例**时，选择**对象构造器**更佳。
```js
...
let jobs = new Person('Steve','Jobs','1955/2/4',56);
let gates = new Person('Bill','Gates','1955/10/28',63);
let torvalds = new Person('Linus','Torvalds','1969/12/28',49);
...
```
***
对象可以包含函数。
```js
let foo = {
    bar: () => print('来自foo::bar.')
};
foo.bar();
function Foo(){
    this.bar = () => print('来自用构造器创建的foo。')
}
let foo = new Foo();
foo.bar()
```
You should get the following content as a result:
```text
来自foo::bar.
来自用构造器创建的foo。
```
***
正如你所见，你用``foo.bar``引用实例``foo``的**参数**``bar``。但万一不清楚要引用的参数名怎么办？阅读这个例子：
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
结果应为下面的内容。
```text
firstName:Steve
lastName:Jobs
dateOfBirth:1955/2/4
age:56
```
在这个例子中，你使用了带有``in``的**FOR循环陈述**列出每一个参数的名称，``[``和``]``来获取它们的值。这与**数组**很相似。
##### 第十课：认识数组
**数组**是数据的集合，可以使用数字来索引。例如：
```js
let corporations = new Array("苹果", "谷歌", "巨硬");
print(corporations[1])
```
在聊天栏里你应该得到``谷歌``。
你或许会感到疑惑，因为“谷歌”是数组``corporations``中的第二个项目，但**索引**的数字却是``1``。你便可得到，数组中项目的索引比它的次序**小一**。这样，``corporations[0]``得到的应该是``苹果``，而``corporation[2]``则是``巨硬``。
***
正如对一个对象，你也可以对数组进行遍历。例如：
```js
let corporations = new Array("苹果", "谷歌", "巨硬");
for (i in corporations){
    print(i + ':' + corporations[i])
}
```
因为数组使用数字进行编号，你也可以这样实现上一个例子：
```js
let corporations = new Array("苹果", "谷歌", "巨硬");
for (let i = 0; i < corporations.length; i++){
    print(i + ':' + corporations[i])
}
```
结果应该是相同的。
值得注意的``corporations[corporations.length]``是``undefined``因为最后一个索引是``corporations.length - 1``。
***
我已经想你介绍过将函数用作变量了，而正因为 _SS_ 的这一特性，你可以这样遍历一个数组：
```js
let corporations = new Array("苹果", "谷歌", "巨硬");
corporations.forEach(
    (item, index) => print(index + ':' + item)
)
```
结果应该和上一个例子是相同的。
我们将这个特性称作 **lambda**。
##### 第十一课：控制数组
首先，你定义了这样一个对象。
```js
function Fruit(name, color){
    this.name = name;
    this.color = color;
}
```
然后你创建了一个数组：
```js
...
let fruits = new Array(
    new Fruit('苹果', '红色'), new Fruit('梨', '绿色'), 
    new Fruit('西瓜', '深浅条纹相间的绿色')
)
```
然而，在程序运行期间，你想要将“菠萝”加入到数组``fruits``。你可以通过使用``Array.push(元素1[, 元素2[, ...]])``实现这一点。例如：
```js
...
fruits.push(new Fruit('菠萝','黄色'))
```
这一陈述将在数组``fruits``末尾加入“菠萝”。此外，要删除最后一个元素，你可以使用``Array.pop()``。
你然后想在数组的开头插入“葡萄”。使用``Array.unshift(元素1[, 元素2[, ...]])``：
```js
...
fruits.unshift(new Fruit('葡萄','紫色'))
```
要删除第一个元素，你可以使用使用``Array.shift()``。
这四种操作都会改变数组的长度。
***
最后，你想要修改第二个元素。例如：
```js
...
fruits[1] = 'Banksia grandis' //天知道这是什么
```
然后你了解到一个数组的数据不必都是同一个类型的。
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
> Notice: Call [setYawAndPitch](#void-seryawandpitchnumber-yaw-number-pitch) if you would like to set both of them together.
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