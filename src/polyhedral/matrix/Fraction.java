package src.polyhedral.matrix;

public class Fraction {
    private long numerator;
    private long denominator;

    public Fraction(long numerator, long denominator) {
        if (denominator == 0) {
            throw new IllegalArgumentException("Denominator cannot be zero.");
        }
        this.numerator = numerator;
        this.denominator = denominator;
        simplify();
    }

    public Fraction(int num) {
        this.numerator = num;
        this.denominator = 1;
    }

    public Fraction(Fraction fraction) {
        this.numerator = fraction.numerator;
        this.denominator = fraction.denominator;
    }

    public int numerator() {
        return (int) numerator;
    }

    public int denominator() {
        return (int) denominator;
    }

    private void simplify() {
        if (numerator == 0) {
            denominator = 1;
        } else {
            long gcd = gcd(numerator, denominator);
            numerator /= gcd;
            denominator /= gcd;
        }
        if (denominator < 0) {
            denominator = -denominator;
            numerator = -numerator;
        }
    }

    private long gcd(long a, long b) {
        while (b != 0) {
            long tmp = b;
            b = a % b;
            a = tmp;
        }
        return a;
    }

    public Fraction add(Fraction other) {
        return new Fraction(numerator * other.denominator + denominator * other.numerator, denominator * other.denominator);
    }


    public Fraction sub(Fraction other) {
        return new Fraction(numerator * other.denominator - denominator * other.numerator, denominator * other.denominator);
    }

    public Fraction neg() {
        return new Fraction(-this.numerator, this.denominator);
    }

    public Fraction mul(Fraction other) {
        return new Fraction(this.numerator * other.numerator, this.denominator * other.denominator);
    }

    public Fraction div(Fraction other) {
        if (other.numerator == 0) {
            throw new ArithmeticException("Cannot divide by zero fraction.");
        }
        return new Fraction(this.numerator * other.denominator, this.denominator * other.numerator);
    }

    public boolean equal(Fraction other) {
        return (this.sub(other).numerator == 0);
    }

    public boolean equal(long val) {
        return (this.numerator == val && this.denominator == 1);
    }

    public boolean less(Fraction other) {
        var tmp = this.sub(other);
        return tmp.numerator * tmp.denominator < 0;
    }

    public boolean less(long val) {
        return numerator * denominator < 0;
    }


    public int toInt() {
        if (denominator < 0) {
            denominator = -denominator;
            numerator = -numerator;
        }
        if (denominator != 1) {
            throw new ArithmeticException("Cannot transform to Integer.");
        }
        return (int) numerator;
    }


    public int toIntWithMul(int mul) {
        if (denominator < 0) {
            denominator = -denominator;
            numerator = -numerator;
        }
        if (mul % denominator != 0) {
            throw new ArithmeticException("Cannot transform to Integer.");
        }
        return (int) (numerator * mul / denominator);
    }

    @Override
    public String toString() {
        if (denominator == 1) {
            return numerator + "";
        } else if (denominator == -1) {
            return -numerator + "";
        }
        return numerator + "/" + denominator;
    }
}
