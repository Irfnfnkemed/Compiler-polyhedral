package src.polyhedral.matrix;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

public class Matrix {
    private Fraction[][] data;
    private int rows;
    private int columns;

    public Matrix(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.data = new Fraction[rows][columns];
    }

    public void setElement(int row, int column, Fraction value) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            throw new IndexOutOfBoundsException("Element (" + row + ", " + column + ") out of bounds.");
        }
        data[row][column] = value;
    }

    public Fraction getElement(int row, int column) {
        if (row < 0 || row >= rows || column < 0 || column >= columns) {
            throw new IndexOutOfBoundsException("Element (" + row + ", " + column + ") out of bounds.");
        }
        return data[row][column];
    }

    public int row() {
        return rows;
    }

    public Matrix add(Matrix other) {
        if (rows != other.rows || columns != other.columns) {
            throw new IllegalArgumentException("Matrices dimensions do not match.");
        }
        Matrix result = new Matrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                result.setElement(i, j, data[i][j].add(other.getElement(i, j)));
            }
        }
        return result;
    }

    public Matrix sub(Matrix other) {
        if (rows != other.rows || columns != other.columns) {
            throw new IllegalArgumentException("Matrices dimensions do not match.");
        }
        Matrix result = new Matrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                result.setElement(i, j, data[i][j].sub(other.getElement(i, j)));
            }
        }
        return result;
    }

    public Matrix scalarMul(Fraction scalar) {
        Matrix result = new Matrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                result.setElement(i, j, data[i][j].mul(scalar));
            }
        }
        return result;
    }

    public Matrix mul(Matrix other) {
        if (this.columns != other.rows) {
            throw new IllegalArgumentException("The number of columns of the first matrix must be equal to the number of rows of the second matrix.");
        }
        Matrix result = new Matrix(this.rows, other.columns);
        for (int i = 0; i < result.rows; i++) {
            for (int j = 0; j < result.columns; j++) {
                Fraction sum = new Fraction(0); // 初始化为0/1，即0
                for (int k = 0; k < this.columns; k++) {
                    sum = sum.add(this.getElement(i, k).mul(other.getElement(k, j)));
                }
                result.setElement(i, j, sum);
            }
        }
        return result;
    }

    public Matrix trans() {
        Matrix result = new Matrix(columns, rows);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                result.setElement(j, i, data[i][j]);
            }
        }
        return result;
    }

    public Fraction det() {
        if (this.columns != this.rows) {
            throw new IllegalArgumentException("Not square matrix.");
        }
        Matrix cal = new Matrix(columns, rows);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                cal.setElement(i, j, new Fraction(data[i][j]));
            }
        }
        boolean zero = false;
        boolean sign = true;
        for (int row = 0; row < rows; row++) {
            if (cal.data[row][row].equal(0)) {
                boolean flag = false;
                for (int trow = row + 1; trow < rows; trow++) {
                    if (!cal.data[trow][row].equal(0)) {
                        flag = true;
                        for (int i = row; i < columns; i++) {
                            Fraction temp = cal.data[row][i];
                            cal.data[row][i] = cal.data[trow][i];
                            cal.data[trow][i] = temp;
                        }
                        sign = !sign;
                        break;
                    }
                }
                if (!flag) {
                    zero = true;
                    break;
                }
            }

            for (int trow = row + 1; trow < rows; trow++) {
                Fraction k;
                if (cal.data[trow][row].equal(0)) {
                    continue;
                }
                k = cal.data[trow][row].div(cal.data[row][row]);
                for (int col = row; col < columns; col++) {
                    cal.data[trow][col] = cal.data[trow][col].sub(k.mul(cal.data[row][col]));
                }
            }
        }
        if (zero) {
            return new Fraction(0);
        } else {
            Fraction ans = new Fraction(sign ? 1 : -1);
            for (int row = 0; row < rows; row++) {
                ans = ans.mul(cal.data[row][row]);
            }
            return ans;
        }
    }

    public Matrix inverse() {
        if (rows != columns) {
            throw new IllegalArgumentException("Inverse can only be calculated for square matrices.");
        }
        Fraction det = det();
        if (det.equal(0)) {
            throw new ArithmeticException("Matrix is singular and not invertible.");
        }
        Matrix result = new Matrix(rows, columns);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Matrix minor = new Matrix(rows - 1, columns - 1);
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < columns; c++) {
                        if (r != i && c != j) {
                            int newR = (r < i) ? r : r - 1;
                            int newC = (c < j) ? c : c - 1;
                            minor.setElement(newR, newC, data[r][c]);
                        }
                    }
                }
                Fraction element = minor.det().div(det);
                if ((i + j) % 2 != 0) {
                    element = element.neg();
                }
                result.setElement(j, i, element);
            }
        }
        return result;
    }

    public List<Integer> findDependent() {
        List<Integer> valid = new ArrayList<>();
        Matrix cal = new Matrix(columns, rows);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                cal.setElement(i, j, new Fraction(data[i][j]));
            }
        }
        int[] id = new int[rows];
        for (int i = 0; i < rows; i++) {
            id[i] = i;
        }
        for (int row = 0; row < rows; row++) {
            if (cal.data[row][row].equal(0)) {
                boolean flag = false;
                for (int trow = row + 1; trow < rows; trow++) {
                    if (!cal.data[trow][row].equal(0)) {
                        flag = true;
                        for (int i = row; i < columns; i++) {
                            Fraction temp = cal.data[row][i];
                            cal.data[row][i] = cal.data[trow][i];
                            cal.data[trow][i] = temp;
                        }
                        int tmp = id[row];
                        id[row] = id[trow];
                        id[trow] = tmp;
                        break;
                    }
                }
                if (!flag) {
                    continue;
                }
            }
            for (int trow = row + 1; trow < rows; trow++) {
                Fraction k;
                if (cal.data[trow][row].equal(0)) {
                    continue;
                }
                k = cal.data[trow][row].div(cal.data[row][row]);
                for (int col = row; col < columns; col++) {
                    cal.data[trow][col] = cal.data[trow][col].sub(k.mul(cal.data[row][col]));
                }
            }
            valid.add(id[row]);
        }
        return valid;
    }

    public Matrix getHermite() {
        Matrix cal = new Matrix(columns, rows);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                cal.setElement(i, j, new Fraction(data[j][i]));
            }
        }
        for (int row = 0; row < rows; row++) {
            while (true) {
                Fraction min = new Fraction(Integer.MAX_VALUE);
                int trowSelect = -1;
                for (int trow = row; trow < rows; trow++) {
                    if (!cal.data[trow][row].equal(0) && cal.data[trow][row].less(min)) {
                        min = cal.data[trow][row];
                        trowSelect = trow;
                    }
                }
                if (trowSelect == -1) {
                    cal.data[0][0] = new Fraction(0); // no solution
                    return cal;
                }
                for (int i = row; i < columns; i++) { // swap
                    Fraction temp = cal.data[row][i];
                    cal.data[row][i] = cal.data[trowSelect][i];
                    cal.data[trowSelect][i] = temp;
                }
                boolean flag = true;
                for (int trow = row + 1; trow < rows; trow++) {
                    if (cal.data[trow][row].equal(0)) {
                        continue;
                    }
                    flag = false;
                    Fraction k = new Fraction(cal.data[trow][row].toInt() / cal.data[row][row].toInt());
                    for (int col = row; col < columns; col++) {
                        cal.data[trow][col] = cal.data[trow][col].sub(k.mul(cal.data[row][col]));
                    }
                }
                if (flag) {
                    for (int trow = row + 1; trow < rows; trow++) {
                        if (cal.data[trow][row + 1].less(0)) {
                            for (int col = row + 1; col < columns; col++) {
                                cal.data[trow][col] = cal.data[trow][col].neg();
                            }
                        }
                    }
                    break;
                }
            }
        }
        for (int i = 0; i < rows; ++i) {
            if (cal.data[i][i].less(0)) {
                for (int j = 0; j < columns; ++j) {
                    cal.data[i][j] = cal.data[i][j].neg();
                }
            }
        }
        return cal.trans();
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                sb.append(data[i][j]).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}