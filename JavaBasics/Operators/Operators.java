public class Operators {
    public static void main(String[] args) {
        ArithmeticOperators arithmetic = new ArithmeticOperators();
        RelationalOperators relational = new RelationalOperators();
        LogicalOperators logical = new LogicalOperators();
        BitwiseOperators bitwise = new BitwiseOperators();
        AssignmentOperators assignment = new AssignmentOperators();

        arithmetic.demo();
        relational.demo();
        logical.demo();
        bitwise.demo();
        assignment.demo();
    }
}

class ArithmeticOperators {
    public void demo() {
        int a = 2;
        int b = 3;
        System.out.println("Arithmetic Operators:");
        System.out.println("a + b = " + (a + b));
        System.out.println("a - b = " + (a - b));
        System.out.println("a * b = " + (a * b));
        System.out.println("a / b = " + (a / b));
        System.out.println("a % b = " + (a % b));
        System.out.println("a++ = " + (a++));
        System.out.println("++a = " + (++a));
        System.out.println();
    }
}

class RelationalOperators {
    public void demo() {
        int a = 2;
        int b = 3;
        System.out.println("Relational Operators:");
        System.out.println("a == b: " + (a == b));
        System.out.println("a != b: " + (a != b));
        System.out.println("a > b: " + (a > b));
        System.out.println("a < b: " + (a < b));
        System.out.println("a >= b: " + (a >= b));
        System.out.println("a <= b: " + (a <= b));
        System.out.println();
    }
}

class LogicalOperators {
    public void demo() {
        boolean x = true;
        boolean y = false;
        System.out.println("Logical Operators:");
        System.out.println("x && y: " + (x && y));
        System.out.println("x || y: " + (x || y));
        System.out.println("!x: " + (!x));
        System.out.println();
    }
}

class BitwiseOperators {
    public void demo() {
        int a = 2; // 10 in binary
        int b = 3; // 11 in binary
        System.out.println("Bitwise Operators:");
        System.out.println("a & b: " + (a & b)); // 10 & 11 = 10 (2)
        System.out.println("a | b: " + (a | b)); // 10 | 11 = 11 (3)
        System.out.println("a ^ b: " + (a ^ b)); // 10 ^ 11 = 01 (1)
        System.out.println("~a: " + (~a)); // ~10 = 01 (1's complement)
        System.out.println("a << 1: " + (a << 1)); // 10 << 1 = 100 (4)
        System.out.println("a >> 1: " + (a >> 1)); // 10 >> 1 = 1 (1)
        System.out.println();
    }
}

class AssignmentOperators {
    public void demo() {
        int a = 2;
        int b = 3;
        System.out.println("Assignment Operators:");
        a += b;
        System.out.println("a += b: " + a);
        a -= b;
        System.out.println("a -= b: " + a);
        a *= b;
        System.out.println("a *= b: " + a);
        a /= b;
        System.out.println("a /= b: " + a);
        a %= b;
        System.out.println("a %= b: " + a);
        a &= b;
        System.out.println("a &= b: " + a);
        a |= b;
        System.out.println("a |= b: " + a);
        a ^= b;
        System.out.println("a ^= b: " + a);
        a <<= 1;
        System.out.println("a <<= 1: " + a);
        a >>= 1;
        System.out.println("a >>= 1: " + a);
        System.out.println();
    }
}