## Java?

Java is a high-level, object-oriented, platform-independent programming language developed by Sun Microsystems (now owned by Oracle). Java programs run on any device that has a JVM (Java Virtual Machine), making it "Write Once, Run Anywhere" (WORA).

### Key Features of Java

1. **Platform Independent**
   - Java code is compiled to bytecode (.class files)
   - Bytecode runs on JVM, which is platform-specific
   - Same code runs on Windows, Linux, macOS without modification

2. **Object-Oriented**
   - Java uses concepts like:
     - **Classes**: Blueprint for objects
     - **Objects**: Instances of classes
     - **Inheritance**: Code reusability through parent-child relationships
     - **Polymorphism**: One interface, multiple implementations
   - Enables structured, reusable, and maintainable programs

3. **Robust & Secure**
   - Automatic memory management using Garbage Collector
   - Strong exception handling mechanism
   - Security features built into JVM (sandboxing, bytecode verification)
   - No pointers (prevents memory corruption)

4. **Multithreading Support**
   - Can run multiple tasks simultaneously
   - Built-in support for concurrent programming
   - Improves application performance

5. **Rich Standard Library**
   - Extensive API (Application Programming Interface)
   - Collections Framework
   - I/O operations
   - Networking capabilities
   - GUI components (Swing, JavaFX)

### Application Flow Diagram

<img width="757" height="221" alt="image" src="https://github.com/user-attachments/assets/50b1df46-cfb8-421c-a3f8-ddd45bf92bc9" />
<img width="600" height="401" alt="0_Cdg8CBPWokYfi9WV" src="https://github.com/user-attachments/assets/8b8c7586-3f02-477a-8647-2e06494fd861" />
<img width="275" height="359" alt="Framework-based-on-Apache-Tomcat" src="https://github.com/user-attachments/assets/66ee8814-046d-42b0-b06b-35727d95a48f" />

**Application Flow:** Browser → Frontend → Backend → Database via a server.

---

## Data Types and Wrapper Classes

Java has **8 primitive data types** and their corresponding **wrapper classes**.

### Primitive Data Types

Primitive types are divided into **Numeric** and **Non-Numeric** types.

#### A. Numeric Types

**1. byte**
- **Size:** 1 byte (8 bits)
- **Range:** –128 to 127
- **Default:** 0
- **Use:** Very small numbers, file I/O operations
- **Example:**
```java
byte age = 25;
byte temperature = -10;
byte maxValue = 127;  // Maximum value
byte minValue = -128; // Minimum value
```

**2. short**
- **Size:** 2 bytes (16 bits)
- **Range:** –32,768 to 32,767
- **Default:** 0
- **Use:** Small integers, memory-efficient when int is too large
- **Example:**
```java
short year = 2024;
short count = 1000;
short maxValue = 32767;
```

**3. int (Most commonly used)**
- **Size:** 4 bytes (32 bits)
- **Range:** –2³¹ to 2³¹–1 (-2,147,483,648 to 2,147,483,647)
- **Default:** 0
- **Use:** Default choice for integers
- **Example:**
```java
int age = 25;
int population = 1000000;
int maxValue = Integer.MAX_VALUE; // 2,147,483,647
int minValue = Integer.MIN_VALUE; // -2,147,483,648
```

**4. long**
- **Size:** 8 bytes (64 bits)
- **Range:** –2⁶³ to 2⁶³–1
- **Default:** 0L
- **Use:** Very large integers
- **Example:**
```java
long worldPopulation = 8000000000L;  // Note: 'L' suffix
long distance = 150000000L;
long maxValue = Long.MAX_VALUE;
```

**5. float**
- **Size:** 4 bytes (32 bits)
- **Range:** 1.4E-45 to 3.4E+38
- **Default:** 0.0f
- **Precision:** ~7 decimal digits
- **Use:** Decimal numbers with less precision, memory-efficient
- **Example:**
```java
float price = 99.99f;  // Note: 'f' suffix
float temperature = -5.5f;
float pi = 3.14159f;
```

**6. double (Default decimal type)**
- **Size:** 8 bytes (64 bits)
- **Range:** 4.9E-324 to 1.7E+308
- **Default:** 0.0d
- **Precision:** ~15-17 decimal digits
- **Use:** Default choice for decimal numbers
- **Example:**
```java
double pi = 3.141592653589793;
double salary = 50000.50;
double scientific = 1.5e10;  // Scientific notation
```

#### B. Non-Numeric Types

**7. char**
- **Size:** 2 bytes (16 bits) - Unicode
- **Range:** '\u0000' to '\uFFFF' (0 to 65,535)
- **Default:** '\u0000'
- **Use:** Single characters, Unicode support
- **Example:**
```java
char grade = 'A';
char symbol = '$';
char unicode = '\u0041';  // 'A' in Unicode
char newline = '\n';
```

**8. boolean**
- **Size:** JVM-dependent (usually 1 bit, but stored as 1 byte)
- **Values:** true / false
- **Default:** false
- **Use:** Logical conditions
- **Example:**
```java
boolean isActive = true;
boolean isComplete = false;
boolean isValid = (age >= 18);
```

### Wrapper Classes

Wrapper classes convert primitive values into objects, enabling them to be used in collections and with generics.

| Primitive | Wrapper Class |
|-----------|---------------|
| byte      | Byte          |
| short     | Short         |
| int       | Integer       |
| long      | Long          |
| float     | Float         |
| double    | Double        |
| char      | Character     |
| boolean   | Boolean       |

**Creating Wrapper Objects:**
```java
// Using valueOf() method (recommended - uses caching)
Byte b = Byte.valueOf((byte)10);
Short s = Short.valueOf((short)100);
Integer i = Integer.valueOf(1000);
Long l = Long.valueOf(100000L);
Float f = Float.valueOf(3.14f);
Double d = Double.valueOf(3.14159);
Character c = Character.valueOf('A');
Boolean bool = Boolean.valueOf(true);

// Using constructor (deprecated in Java 9+)
Integer i2 = new Integer(100);  // Not recommended

// Direct assignment (autoboxing)
Integer i3 = 100;  // Autoboxing
```

**Useful Wrapper Class Methods:**
```java
// Parsing strings to primitives
int num = Integer.parseInt("123");
double price = Double.parseDouble("99.99");
boolean flag = Boolean.parseBoolean("true");

// Converting primitives to strings
String str = Integer.toString(123);
String str2 = String.valueOf(123);

// Getting min/max values
int max = Integer.MAX_VALUE;
int min = Integer.MIN_VALUE;

// Character utility methods
boolean isDigit = Character.isDigit('5');
boolean isLetter = Character.isLetter('A');
char upper = Character.toUpperCase('a');
```

### Autoboxing and Unboxing

**Autoboxing:** Automatic conversion from primitive type to wrapper class.

```java
// Autoboxing examples
int primitiveInt = 42;
Integer wrapperInt = primitiveInt;  // int → Integer automatically
System.out.println(wrapperInt); // 42

// In method calls
List<Integer> numbers = new ArrayList<>();
numbers.add(5);     // Autoboxing: int → Integer
numbers.add(10);    // Autoboxing: int → Integer
```

**Unboxing:** Automatic conversion from wrapper class to primitive type.

```java
// Unboxing examples
Integer wrapper = 100;
int primitive = wrapper;  // Integer → int automatically
System.out.println(primitive); // 100

// In operations
Integer a = 10;
Integer b = 20;
int sum = a + b;  // Unboxing: Integer → int, then addition
```

**Collections and Autoboxing/Unboxing:**
```java
List<Integer> numbers = new ArrayList<>();
numbers.add(5);           // Autoboxing (int → Integer)
numbers.add(10);          // Autoboxing (int → Integer)
int num = numbers.get(0); // Unboxing (Integer → int)

// Important: Collections cannot store primitive types directly
// List<int> invalid;  // Compilation error!
```

### Differences Between Primitive & Wrapper Classes

| Feature       | Primitive Types              | Wrapper Classes                        |
|---------------|------------------------------|----------------------------------------|
| **Type**      | Simple values                | Objects                                |
| **Memory**    | Fixed small size (1-8 bytes) | Larger (object overhead ~16 bytes)     |
| **Storage**   | Stack (local) or heap (instance) | Always stored in heap                |
| **Default Value** | 0, 0.0, false, '\u0000'  | null                                   |
| **Performance** | Faster (no object overhead) | Slower (object creation, GC overhead)  |
| **Usage**     | Best for simple calculations | Required for Collections, Generics     |
| **Nullability** | Cannot be null            | Can be null                            |
| **Methods**   | No methods                   | Has many methods (parseInt, compareTo) |
| **Conversion** | Autoboxing → wrapper      | Unboxing → primitive                   |

**Practical Examples:**

```java
// Primitive: Fast and efficient
int count = 0;  // Stored in stack, 4 bytes
count++;        // Direct operation, very fast

// Wrapper: More memory, but useful for collections
Integer countObj = 0;  // Stored in heap, ~16 bytes
countObj++;            // Requires unboxing, increment, then autoboxing

// When to use primitives:
int[] numbers = new int[1000];  // Efficient array
int sum = 0;  // Fast calculations

// When to use wrappers:
List<Integer> numbers = new ArrayList<>();  // Collections require objects
Integer id = null;  // Can represent "no value"
```

### Default Initial Values

| Data Type        | Default Value |
|------------------|---------------|
| byte             | 0             |
| short            | 0             |
| int              | 0             |
| long             | 0L            |
| float            | 0.0f          |
| double           | 0.0d          |
| char             | '\u0000'      |
| boolean          | false         |
| All object types | null          |

**Example:**
```java
public class DefaultValues {
    // Instance variables get default values
    int count;           // 0
    double price;        // 0.0
    boolean active;      // false
    String name;         // null
    Integer id;          // null
    
    public static void main(String[] args) {
        DefaultValues obj = new DefaultValues();
        System.out.println(obj.count);   // 0
        System.out.println(obj.price);   // 0.0
        System.out.println(obj.active);  // false
        System.out.println(obj.name);    // null
        
        // Local variables must be initialized
        int localVar;  // Compilation error if used without initialization
        // System.out.println(localVar);  // Error!
    }
}
```

### Memory Size and Locations

#### Memory Sizes (Primitive Types)

- **1 byte:** byte, boolean (JVM-dependent)
- **2 bytes:** short, char
- **4 bytes:** int, float
- **8 bytes:** long, double

#### Wrapper Objects Memory

Wrapper objects take more memory due to object overhead:
- **Integer object:** ~16 bytes
  - 12 bytes = object header (mark word, class pointer)
  - 4 bytes = actual int value
- **Long object:** ~24 bytes
  - 12 bytes = object header
  - 8 bytes = actual long value
  - 4 bytes = alignment padding

#### Memory Locations

**Primitive Types:**
- **Local variable** → Stored in **stack**
- **Instance variable** → Stored inside object on **heap**
- **Static variable** → Stored in **method area**
- **Array elements** → Stored together in **heap** (arrays are objects)

**Wrapper Types:**
- **Object** → Always stored on **heap**
- **Reference** → Stored in stack (local) or inside object (heap)

**Example:**
```java
class Demo {
    int x = 10;           // Instance variable → inside object on heap
    static int y = 20;    // Static variable → method area
    
    public static void main(String[] args) {
        int z = 30;                    // Local variable → stack
        Demo d1 = new Demo();         // Reference in stack, object in heap
        Demo d2 = new Demo();         // Separate object in heap
        
        Integer num1 = 100;           // Object in heap, reference in stack
        Integer num2 = 200;           // Another object in heap
    }
}
```

**Memory Explanation:**
- **Method Area:** `y = 20` (static)
- **Stack:** `z = 30`, `d1` (reference), `d2` (reference), `num1` (reference), `num2` (reference)
- **Heap:** 
  - Object 1 (d1): `x = 10`
  - Object 2 (d2): `x = 10`
  - Integer object 1: `value = 100`
  - Integer object 2: `value = 200`

**Important:** Static variables are NOT stored inside objects. They are shared across all instances.

### Types of Conversions

#### Implicit Conversion (Widening)

Done automatically by compiler. Small → large type. No data loss.

**Widening Conversion Order:**
```
byte → short → int → long → float → double
char → int → long → float → double
```

**Total: 19 widening conversions**

- byte → short, int, long, float, double (5)
- short → int, long, float, double (4)
- int → long, float, double (3)
- long → float, double (2)
- float → double (1)
- char → int, long, float, double (4)

**Examples:**
```java
byte b = 10;
int i = b;        // byte → int (automatic)
long l = i;       // int → long (automatic)
float f = l;      // long → float (automatic)
double d = f;     // float → double (automatic)

char c = 'A';
int charValue = c;  // char → int (ASCII: 65)
```

#### Explicit Conversion (Narrowing)

Done manually by programmer using cast `()`. Large → small type. Possible data loss.

**Total: 23 narrowing conversions**

- byte → char (1)
- short → byte, char (2)
- int → byte, short, char (3)
- long → byte, short, int, char (4)
- float → byte, short, int, long, char (5)
- double → byte, short, int, long, float, char (6)
- char → byte, short (2)

**Examples:**
```java
int i = 65;
char c = (char) i;      // int → char: 65 → 'A'
byte b = (byte) i;      // int → byte: 65 → 65
short s = (short) i;    // int → short: 65 → 65

double d = 99.99;
int intValue = (int) d;     // double → int: 99.99 → 99 (data loss!)
float f = (float) d;         // double → float: possible precision loss

long l = 100000L;
int i2 = (int) l;       // long → int: possible overflow if l > Integer.MAX_VALUE
```

**Boolean Conversion Rule:**
- `boolean` **cannot** be converted to any other type
- No other type can be converted to `boolean`

```java
// boolean cannot be converted
boolean flag = true;
// int num = flag;  // Compilation error!
// boolean b = 1;   // Compilation error!
```

### Operations on Data Types

**Result Type Rules:**
1. **(byte + short + int + char)** → **int**
2. Adding **long** → result becomes **long**
3. Adding **float** → result becomes **float**
4. Adding **double** → result becomes **double**

**Result = largest datatype in the expression**

**Examples:**
```java
byte a = 5;
byte b = 10;
int sum = a + b;  // byte + byte => int (not byte!)
// byte sum2 = a + b;  // Compilation error!

short s1 = 100;
short s2 = 200;
int result = s1 + s2;  // short + short => int

int x = 10;
long y = 20L;
long sum2 = x + y;  // int + long => long

float f1 = 3.14f;
double d1 = 2.5;
double result2 = f1 + d1;  // float + double => double

char c1 = 'A';
char c2 = 'B';
int charSum = c1 + c2;  // char + char => int (65 + 66 = 131)
```

### ASCII (American Standard Code for Information Interchange)

ASCII is a character encoding standard that represents characters as numbers.

**ASCII Rules:**
- **Uppercase letters:** A=65, B=66, ..., Z=90
  - Formula: `65 + (position - 1)`
- **Lowercase letters:** a=97, b=98, ..., z=122
  - Formula: `97 + (position - 1)`
- **Digits (0-9):** Range 48-57
  - 0=48, 1=49, ..., 9=57

**Common ASCII Values:**

**Symbols Before Digits (33-47):**
- `!` = 33, `"` = 34, `#` = 35, `$` = 36, `%` = 37
- `&` = 38, `'` = 39, `(` = 40, `)` = 41, `*` = 42
- `+` = 43, `,` = 44, `-` = 45, `.` = 46, `/` = 47

**Symbols After Digits (58-64):**
- `:` = 58, `;` = 59, `<` = 60, `=` = 61, `>` = 62
- `?` = 63, `@` = 64

**Symbols After Uppercase (91-96):**
- `[` = 91, `\` = 92, `]` = 93, `^` = 94, `_` = 95, `` ` `` = 96

**Symbols After Lowercase (123-126):**
- `{` = 123, `|` = 124, `}` = 125, `~` = 126

**Examples:**
```java
char c = 'A';
int ascii = c;  // 65
System.out.println(ascii);

char digit = '5';
int digitValue = digit;  // 53
System.out.println(digitValue);

// Converting ASCII to character
int code = 67;
char ch = (char) code;  // 'C'
System.out.println(ch);

// Character arithmetic
char c1 = 'A';
char c2 = 'B';
int diff = c2 - c1;  // 1
System.out.println(diff);
```

**Visual Reference:**

Wrappers = classes (objects)
Primitives = simple values

<img width="561" height="241" alt="image" src="https://github.com/user-attachments/assets/5b0cda0e-4491-41f0-ad1e-163dbee14609" />
<img width="965" height="677" alt="image" src="https://github.com/user-attachments/assets/a4a2bba6-99d7-404d-b4fe-496eeb23b61b" />

---

## Java Operators

Operators are symbols that perform operations on variables and values.

### 1. Arithmetic Operators

| Operator | Meaning        | Example     | Result |
|----------|----------------|-------------|--------|
| `+`      | Addition       | `5 + 3`     | 8      |
| `−`      | Subtraction    | `5 - 3`     | 2      |
| `*`      | Multiplication | `5 * 3`     | 15     |
| `/`      | Division       | `10 / 3`    | 3      |
| `%`      | Remainder      | `10 % 3`    | 1      |
| `++`     | Increment      | `a++` or `++a` | Increases by 1 |
| `--`     | Decrement      | `a--` or `--a` | Decreases by 1 |

**Examples:**
```java
int a = 10, b = 3;

System.out.println(a + b);  // 13
System.out.println(a - b);  // 7
System.out.println(a * b);  // 30
System.out.println(a / b);  // 3 (integer division)
System.out.println(a % b);  // 1 (remainder)

// Increment and Decrement
int x = 5;
x++;        // Post-increment: x becomes 6
++x;        // Pre-increment: x becomes 7
x--;        // Post-decrement: x becomes 6
--x;        // Pre-decrement: x becomes 5

// Pre vs Post increment
int i = 5;
int j = ++i;  // i becomes 6, j = 6
int k = i++;  // k = 6, then i becomes 7
```

### 2. Relational Operators

Used to compare two values. Return `boolean` (true/false).

| Operator | Meaning          | Example     | Result |
|----------|------------------|-------------|--------|
| `==`     | equal            | `5 == 5`    | true   |
| `!=`     | not equal        | `5 != 3`    | true   |
| `>`      | greater          | `5 > 3`     | true   |
| `<`      | less             | `5 < 3`     | false  |
| `>=`     | greater or equal | `5 >= 5`    | true   |
| `<=`     | less or equal    | `5 <= 3`    | false  |

**Examples:**
```java
int a = 10, b = 5;

System.out.println(a == b);  // false
System.out.println(a != b);  // true
System.out.println(a > b);   // true
System.out.println(a < b);   // false
System.out.println(a >= b);  // true
System.out.println(a <= b);  // false

// Used in conditions
if (a > b) {
    System.out.println("a is greater");
}
```

### 3. Logical Operators

Used to combine multiple boolean expressions.

**AND Operator (`&&`):**

| A     | B     | A && B |
|-------|-------|--------|
| true  | true  | true   |
| true  | false | false  |
| false | true  | false  |
| false | false | false  |

**OR Operator (`||`):**

| A     | B     | A \|\| B |
|-------|-------|----------|
| true  | true  | true     |
| true  | false | true     |
| false | true  | true     |
| false | false | false    |

**NOT Operator (`!`):**

| A     | !A    |
|-------|-------|
| true  | false |
| false | true  |

**Examples:**
```java
boolean a = true, b = false;

System.out.println(a && b);  // false
System.out.println(a || b);  // true
System.out.println(!a);      // false

// Short-circuit evaluation
int x = 5;
if (x > 0 && x < 10) {  // Both conditions checked
    System.out.println("x is between 0 and 10");
}

// Short-circuit: if first condition is false, second is not evaluated
if (x != 0 && (10 / x) > 1) {  // Safe division
    System.out.println("Safe operation");
}
```

### 4. Assignment Operators

Combine arithmetic operations with assignment.

| Operator | Example | Equivalent To | Meaning   |
|----------|---------|---------------|-----------|
| `=`      | `a = 5` | `a = 5`       | assign    |
| `+=`     | `a += 3`| `a = a + 3`   | add and assign |
| `-=`     | `a -= 2`| `a = a - 2`   | subtract and assign |
| `*=`     | `a *= 4`| `a = a * 4`   | multiply and assign |
| `/=`     | `a /= 2`| `a = a / 2`   | divide and assign |
| `%=`     | `a %= 3`| `a = a % 3`   | modulo and assign |

**Examples:**
```java
int a = 10;

a += 5;   // a = a + 5 → a = 15
a -= 3;   // a = a - 3 → a = 12
a *= 2;   // a = a * 2 → a = 24
a /= 4;   // a = a / 4 → a = 6
a %= 4;   // a = a % 4 → a = 2

System.out.println(a);  // 2
```

### 5. Bitwise Operators

Operate on individual bits of numbers.

| Operator | Meaning              | Example     | Result |
|----------|----------------------|-------------|--------|
| `&`      | AND                  | `5 & 3`     | 1      |
| `\|`     | OR                   | `5 \| 3`    | 7      |
| `^`      | XOR                  | `5 ^ 3`     | 6      |
| `~`      | NOT (complement)     | `~5`        | -6     |
| `<<`     | Left shift           | `5 << 1`    | 10     |
| `>>`     | Right shift          | `5 >> 1`    | 2      |
| `>>>`    | Unsigned right shift | `-5 >>> 1`  | Large positive |

**Examples:**
```java
int x = 5;  // Binary: 0101
int y = 3;  // Binary: 0011

System.out.println(x & y);   // 0101 & 0011 = 0001 → 1
System.out.println(x | y);   // 0101 | 0011 = 0111 → 7
System.out.println(x ^ y);   // 0101 ^ 0011 = 0110 → 6
System.out.println(~x);     // ~0101 = ...11111010 → -6
System.out.println(x << 1); // 0101 << 1 = 1010 → 10 (multiply by 2)
System.out.println(x >> 1); // 0101 >> 1 = 0010 → 2 (divide by 2)

// Practical uses
int num = 8;
int doubled = num << 1;  // 16 (multiply by 2)
int halved = num >> 1;    // 4 (divide by 2)
int quadrupled = num << 2; // 32 (multiply by 4)
```

### 6. Ternary Operator

Conditional operator: `(condition) ? valueIfTrue : valueIfFalse`

**Examples:**
```java
int a = 10, b = 20;

// Find maximum
int max = (a > b) ? a : b;  // 20

// Find minimum
int min = (a < b) ? a : b;  // 10

// Check even/odd
String result = (a % 2 == 0) ? "Even" : "Odd";  // "Even"

// Nested ternary
int x = 5, y = 10, z = 15;
int largest = (x > y) ? ((x > z) ? x : z) : ((y > z) ? y : z);

// Equivalent to if-else
int value = (condition) ? 100 : 0;
// Same as:
// int value;
// if (condition) {
//     value = 100;
// } else {
//     value = 0;
// }
```

### 7. instanceof Operator

Checks if an object is an instance of a specific class or interface.

**Syntax:** `object instanceof Class`

**Examples:**
```java
String s = "Hello";
System.out.println(s instanceof String);  // true
System.out.println(s instanceof Object); // true (String extends Object)

Integer i = 100;
System.out.println(i instanceof Integer); // true
System.out.println(i instanceof Number);   // true (Integer extends Number)

// With null
String str = null;
System.out.println(str instanceof String); // false (null is not an instance)

// Inheritance example
class Animal {}
class Dog extends Animal {}

Animal animal = new Dog();
System.out.println(animal instanceof Dog);    // true
System.out.println(animal instanceof Animal); // true
System.out.println(animal instanceof Object); // true
```

### Pre-increment vs Post-increment

**Pre-increment (`++a`):**
- Increases value **first**, then uses it
- Value is incremented before the expression is evaluated

**Post-increment (`a++`):**
- Uses value **first**, then increases it
- Value is used in expression, then incremented

**Examples:**
```java
int a = 5;

// Pre-increment
int b = ++a;  // a becomes 6, then b = 6
System.out.println(a);  // 6
System.out.println(b);  // 6

// Reset
a = 5;

// Post-increment
int c = a++;  // c = 5, then a becomes 6
System.out.println(a);  // 6
System.out.println(c);  // 5

// In loops
for (int i = 0; i < 5; i++) {  // Post-increment after iteration
    System.out.println(i);  // 0, 1, 2, 3, 4
}

// In expressions
int x = 5;
int y = x++ + ++x;  // y = 5 + 7 = 12, x = 7
// Step 1: x++ returns 5, x becomes 6
// Step 2: ++x makes x 7, returns 7
// Step 3: 5 + 7 = 12
```

---

## Variables in Java

### Variable Declaration and Initialization

**Syntax:** `dataType variableName = value;`

**Examples:**
```java
// Declaration
int age;
String name;

// Initialization
age = 25;
name = "John";

// Declaration and initialization together
int count = 10;
double price = 99.99;
boolean isActive = true;
char grade = 'A';
```

### Types of Variables

1. **Instance Variables (Non-static fields)**
   - Belong to an instance of a class
   - Each object has its own copy
   - Stored in heap

2. **Class Variables (Static fields)**
   - Belong to the class, not instances
   - Shared by all instances
   - Stored in method area

3. **Local Variables**
   - Declared inside methods, constructors, or blocks
   - Must be initialized before use
   - Stored in stack

**Examples:**
```java
class Student {
    // Instance variable
    String name;
    int age;
    
    // Class variable (static)
    static String schoolName = "ABC School";
    static int totalStudents = 0;
    
    public void displayInfo() {
        // Local variable
        String info = "Name: " + name + ", Age: " + age;
        System.out.println(info);
    }
}
```

---

## Control Flow Statements

### If-Else Statements

**Syntax:**
```java
if (condition) {
    // code block
} else if (condition2) {
    // code block
} else {
    // code block
}
```

**Examples:**
```java
int age = 18;

if (age >= 18) {
    System.out.println("Adult");
} else {
    System.out.println("Minor");
}

// Multiple conditions
int score = 85;
if (score >= 90) {
    System.out.println("Grade A");
} else if (score >= 80) {
    System.out.println("Grade B");
} else if (score >= 70) {
    System.out.println("Grade C");
} else {
    System.out.println("Grade D");
}
```

### Switch Statement

**Syntax:**
```java
switch (expression) {
    case value1:
        // code
        break;
    case value2:
        // code
        break;
    default:
        // code
}
```

**Examples:**
```java
int day = 3;
switch (day) {
    case 1:
        System.out.println("Monday");
        break;
    case 2:
        System.out.println("Tuesday");
        break;
    case 3:
        System.out.println("Wednesday");
        break;
    default:
        System.out.println("Other day");
}

// Switch with strings (Java 7+)
String color = "red";
switch (color) {
    case "red":
        System.out.println("Stop");
        break;
    case "green":
        System.out.println("Go");
        break;
    default:
        System.out.println("Wait");
}

// Switch expressions (Java 14+)
String result = switch (day) {
    case 1, 2, 3, 4, 5 -> "Weekday";
    case 6, 7 -> "Weekend";
    default -> "Invalid";
};
```

### Loops

**1. For Loop:**
```java
for (initialization; condition; increment) {
    // code
}

// Example
for (int i = 0; i < 5; i++) {
    System.out.println(i);  // 0, 1, 2, 3, 4
}

// Enhanced for loop (for-each)
int[] numbers = {1, 2, 3, 4, 5};
for (int num : numbers) {
    System.out.println(num);
}
```

**2. While Loop:**
```java
while (condition) {
    // code
}

// Example
int i = 0;
while (i < 5) {
    System.out.println(i);
    i++;
}
```

**3. Do-While Loop:**
```java
do {
    // code
} while (condition);

// Example
int i = 0;
do {
    System.out.println(i);
    i++;
} while (i < 5);
```

**Break and Continue:**
```java
// Break: exit loop
for (int i = 0; i < 10; i++) {
    if (i == 5) {
        break;  // Exit loop when i == 5
    }
    System.out.println(i);  // 0, 1, 2, 3, 4
}

// Continue: skip current iteration
for (int i = 0; i < 10; i++) {
    if (i % 2 == 0) {
        continue;  // Skip even numbers
    }
    System.out.println(i);  // 1, 3, 5, 7, 9
}
```

---

## Arrays

Arrays are containers that hold multiple values of the same type.

### Array Declaration and Initialization

**Syntax:**
```java
// Declaration
dataType[] arrayName;
// or
dataType arrayName[];

// Initialization
arrayName = new dataType[size];

// Declaration and initialization together
dataType[] arrayName = new dataType[size];

// Initialize with values
dataType[] arrayName = {value1, value2, value3};
```

**Examples:**
```java
// Integer array
int[] numbers = new int[5];  // Array of 5 integers
numbers[0] = 10;
numbers[1] = 20;
numbers[2] = 30;

// Initialize with values
int[] nums = {1, 2, 3, 4, 5};

// String array
String[] names = {"Alice", "Bob", "Charlie"};

// Multi-dimensional array
int[][] matrix = new int[3][3];
int[][] matrix2 = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
```

### Array Operations

```java
int[] arr = {10, 20, 30, 40, 50};

// Access elements
System.out.println(arr[0]);  // 10
System.out.println(arr.length);  // 5

// Iterate through array
for (int i = 0; i < arr.length; i++) {
    System.out.println(arr[i]);
}

// Enhanced for loop
for (int num : arr) {
    System.out.println(num);
}

// Find sum
int sum = 0;
for (int num : arr) {
    sum += num;
}
System.out.println("Sum: " + sum);  // 150
```

---

## Strings

String is a sequence of characters. In Java, String is an object, not a primitive type.

### String Creation

```java
// String literal (stored in string pool)
String str1 = "Hello";
String str2 = "Hello";  // Reuses same object from pool

// Using new keyword (creates new object)
String str3 = new String("Hello");
String str4 = new String("Hello");  // Different object

// String comparison
System.out.println(str1 == str2);      // true (same reference)
System.out.println(str1 == str3);      // false (different objects)
System.out.println(str1.equals(str3)); // true (same content)
```

### String Methods

```java
String str = "Hello World";

// Length
int len = str.length();  // 11

// Character at index
char ch = str.charAt(0);  // 'H'

// Substring
String sub = str.substring(0, 5);  // "Hello"
String sub2 = str.substring(6);    // "World"

// Concatenation
String result = str + " Java";  // "Hello World Java"
String result2 = str.concat(" Java");  // Same

// Case conversion
String upper = str.toUpperCase();  // "HELLO WORLD"
String lower = str.toLowerCase();  // "hello world"

// Replace
String replaced = str.replace("World", "Java");  // "Hello Java"

// Split
String[] words = str.split(" ");  // ["Hello", "World"]

// Trim (remove leading/trailing whitespace)
String trimmed = "  Hello  ".trim();  // "Hello"

// Check if contains
boolean contains = str.contains("World");  // true

// Starts with / Ends with
boolean starts = str.startsWith("Hello");  // true
boolean ends = str.endsWith("World");       // true
```

### String Immutability

Strings in Java are immutable (cannot be changed). Operations create new strings.

```java
String str = "Hello";
str = str + " World";  // Creates new string object
// Original "Hello" remains in memory (eligible for GC)
```

### StringBuilder and StringBuffer

For mutable strings (when frequent modifications are needed):

```java
// StringBuilder (not thread-safe, faster)
StringBuilder sb = new StringBuilder("Hello");
sb.append(" World");
sb.insert(5, ",");
String result = sb.toString();  // "Hello, World"

// StringBuffer (thread-safe, slower)
StringBuffer sbf = new StringBuffer("Hello");
sbf.append(" World");
String result2 = sbf.toString();
```





















































































































































































































































































































































































































































































































































































































