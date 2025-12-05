Java?
high-level, object-oriented, platform-independent programming language.
Java programs run on any device that has a JVM (Java Virtual Machine).

Key Features of Java:-
1. Platform Independent
2. Object-Oriented
Java uses concepts like:
Classes, Objectsl, Inheritance, Polymorphism
structured, reusable programs.
3. Robust & Secure
Automatic memory management using Garbage Collector
Strong exception handling
Security features in JVM
4. Multithreading Support
Can run multiple tasks at the same time
5. Rich Standard Library

Application Flow Diagram:-
<img width="757" height="221" alt="image" src="https://github.com/user-attachments/assets/50b1df46-cfb8-421c-a3f8-ddd45bf92bc9" />
<img width="600" height="401" alt="0_Cdg8CBPWokYfi9WV" src="https://github.com/user-attachments/assets/8b8c7586-3f02-477a-8647-2e06494fd861" />
<img width="275" height="359" alt="Framework-based-on-Apache-Tomcat" src="https://github.com/user-attachments/assets/66ee8814-046d-42b0-b06b-35727d95a48f" />
Application flow = Browser → Frontend → Backend → Database via a server.

### Data Types and Wrapper Classes
Java has 8 primitive data types.
#### Primitive Data Types
Primitive types are divided into Numeric and Non-Numeric types
A. Numeric Types
1. byte

Size: 1 byte

Range: –128 to 127

Default: 0

Use: Very small numbers

2. short

Size: 2 bytes

Range: –32,768 to 32,767

Default: 0

3. int (Most commonly used)

Size: 4 bytes

Range: –2³¹ to 2³¹–1

Default: 0

4. long

Size: 8 bytes

Range: –2⁶³ to 2⁶³–1

Default: 0L

5. float

Size: 4 bytes

Range: 1.4E-45 to 3.4E+38

Default: 0.0f

Use: Decimal numbers with less precision

6. double (Default decimal type)

Size: 8 bytes

Range: 4.9E-324 to 1.7E+308

Default: 0.0d

B. Non-Numeric Types
7. char

Size: 2 bytes (Unicode)

Range: ‘\u0000’ to ‘\uFFFF’

Default: ‘\u0000’

8. boolean

Size: JVM-dependent (usually 1 bit)

Values: true / false

Default: false


Wrapper Classes
Primitive → Wrapper Class
These wrappers turn primitive values into objects.

Primitive	Wrapper
byte	Byte
short	Short
int	Integer
long	Long
float	Float
double	Double
char	Character
boolean	Boolean

Byte b = Byte.valueOf((byte)10);
Short s = Short.valueOf((short)100);
Integer i = Integer.valueOf(1000);
Long l = Long.valueOf(100000L);
Float f = Float.valueOf(3.14f);
Double d = Double.valueOf(3.14159);
Character c = Character.valueOf('A');
Boolean bool = Boolean.valueOf(true);

Autoboxing / Unboxing
Autoboxing
Primitive → Wrapper class automatically\
int primitiveInt = 42;
Integer wrapperInt = primitiveInt;
Unboxing

Wrapper → Primitive automatically

Integer wrapper = 100;
int primitive = wrapper; // Auto: Integer -> int
Collections cannot store primitive types.
List<Integer> numbers = new ArrayList<>();
numbers.add(5);     // Autoboxing (int → Integer)
int num = numbers.get(0);  // Unboxing (Integer → int)

Differences Between Primitive & Wrapper Classes
| Feature       | Primitive           | Wrapper            |
| ------------- | ------------------- | ------------------ |
| Type          | Simple values       | Objects            |
| Memory        | Fixed small size    | Larger memory      |
| Storage       | Stored in **stack** | Stored in **heap** |
| Nullability   | Cannot be null      | Can be null        |
| Performance   | Faster              | Slower             |
| Methods       | No methods          | Has many methods   |
| Default Value | 0, false            | null               |

Default Initial Values
| Data Type        | Default Value |
| ---------------- | ------------- |
| byte             | 0             |
| short            | 0             |
| int              | 0             |
| long             | 0L            |
| float            | 0.0f          |
| double           | 0.0d          |
| char             | '\u0000'      |
| boolean          | false         |
| All object types | null          |

Memory Size and Locations
Memory Sizes (Primitive Types)

byte, boolean: 1 byte

short, char: 2 bytes

int, float: 4 bytes

long, double: 8 bytes
Wrapper Objects Memory

Wrappers take more memory, e.g.,

Integer object = ~16 bytes
Because objects have:

Header
Memory Locations
Primitive Types

Local variable → Stored in stack

Instance variable → Stored inside object on heap

Arrays → Elements stored together in heap
Value field

Wrapper Types

Object always in heap

Reference stored in stack (local) or heap (instance)

class Demo {
    int x = 10;      // inside object → heap
    static int y=20; // inside method area

    public static void main(String[] args) {
        int z = 30;     // stack
        Demo d1 = new Demo(); // reference stack, object heap
        Demo d2 = new Demo(); // separate object in heap
    }
}

Autoboxing
int primitive = 42;
Integer obj = primitive; // int -> Integer
System.out.println(obj);

Unboxing
Integer obj = 100;
int primitive = obj; // Integer -> int
System.out.println(primitive);




















































































































































































































































































































































































































































































































































































































































































