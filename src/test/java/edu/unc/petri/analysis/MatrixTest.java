package edu.unc.petri.analysis;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MatrixTest {

  private Matrix matrix;

  @BeforeEach
  void setUp() {
    int[][] data = {{1, 2, 3}, {4, 5, 6}};
    matrix = new Matrix(data);
  }

  @Test
  void constructor_withDimensions_initializesWithZeros() {
    Matrix zeroMatrix = new Matrix(2, 3);
    assertEquals(2, zeroMatrix.getRowDimension());
    assertEquals(3, zeroMatrix.getColumnDimension());
    assertTrue(zeroMatrix.isZeroMatrix());
  }

  @Test
  void constructor_withArray_initializesCorrectly() {
    assertEquals(2, matrix.getRowDimension());
    assertEquals(3, matrix.getColumnDimension());
    assertEquals(5, matrix.get(1, 1));
  }

  @Test
  void constructor_withJaggedArray_throwsIllegalArgumentException() {
    int[][] jagged = {{1, 2}, {3, 4, 5}};
    assertThrows(IllegalArgumentException.class, () -> new Matrix(jagged));
  }

  @Test
  void identity_createsIdentityMatrix() {
    Matrix identityMatrix = Matrix.identity(3, 3);
    assertEquals(1, identityMatrix.get(0, 0));
    assertEquals(1, identityMatrix.get(1, 1));
    assertEquals(1, identityMatrix.get(2, 2));
    assertEquals(0, identityMatrix.get(0, 1));
  }

  @Test
  void getAndSet() {
    matrix.set(0, 0, 99);
    assertEquals(99, matrix.get(0, 0));
  }

  @Test
  void copy_createsDeepCopy() {
    Matrix copiedMatrix = matrix.copy();
    assertNotSame(matrix, copiedMatrix);
    assertArrayEquals(matrix.getArray(), copiedMatrix.getArray());
    copiedMatrix.set(0, 0, 99);
    assertEquals(1, matrix.get(0, 0));
  }

  @Test
  void transpose_flipsMatrix() {
    Matrix transposed = matrix.transpose();
    assertEquals(3, transposed.getRowDimension());
    assertEquals(2, transposed.getColumnDimension());
    assertEquals(2, transposed.get(1, 0));
    assertEquals(4, transposed.get(0, 1));
  }

  @Test
  void isZeroMatrix() {
    assertFalse(matrix.isZeroMatrix());
    Matrix zeroMatrix = new Matrix(2, 2);
    assertTrue(zeroMatrix.isZeroMatrix());
  }

  @Test
  void eliminateCol() {
    Matrix reduced = matrix.eliminateCol(1);
    assertEquals(2, reduced.getColumnDimension());
    assertEquals(1, reduced.get(0, 0));
    assertEquals(3, reduced.get(0, 1));
  }

  @Test
  void appendVector() {
    Matrix vector = new Matrix(new int[][] {{9}, {9}});
    Matrix appended = matrix.appendVector(vector);
    assertEquals(4, appended.getColumnDimension());
    assertEquals(9, appended.get(0, 3));
    assertEquals(9, appended.get(1, 3));
  }

  @Test
  void getGreatestCommonDivisor() {
    Matrix m = new Matrix(new int[][] {{12}, {18}, {30}});
    assertEquals(6, m.getGreatestCommonDivisor());
  }

  @Test
  void divideEquals() {
    matrix.divideEquals(2);
    assertEquals(0, matrix.get(0, 0)); // 1/2 = 0
    assertEquals(1, matrix.get(0, 1)); // 2/2 = 1
    assertEquals(2, matrix.get(1, 0)); // 4/2 = 2
  }

  @Test
  void nonZeroIndices() {
    Matrix indices = matrix.nonZeroIndices();
    assertEquals(1, indices.get(0, 0));
    assertEquals(1, indices.get(0, 1));
    assertEquals(2, indices.get(1, 0));
  }

  @Test
  void findNonMinimal() {
    Matrix m = new Matrix(new int[][] {{1, 1, 0}, {1, 1, 1}});
    assertEquals(0, m.findNonMinimal());
  }

  @Test
  void findNonMinimal_noNonMinimal() {
    Matrix m = new Matrix(new int[][] {{1, 0, 1}, {0, 1, 0}});
    assertEquals(-1, m.findNonMinimal());
  }

  @Test
  void getMatrix() {
    Matrix sub = matrix.getMatrix(0, 1, 1, 2);
    assertEquals(2, sub.getRowDimension());
    assertEquals(2, sub.getColumnDimension());
    assertEquals(2, sub.get(0, 0));
    assertEquals(3, sub.get(0, 1));
    assertEquals(5, sub.get(1, 0));
    assertEquals(6, sub.get(1, 1));
  }

  @Test
  void getMatrix_throwsExceptionOnInvalidIndices() {
    assertThrows(ArrayIndexOutOfBoundsException.class, () -> matrix.getMatrix(0, 2, 0, 0));
  }

  @Test
  void rowWithNegativeElement() {
    Matrix m = new Matrix(new int[][] {{1, 2}, {-1, 3}});
    assertEquals(1, m.rowWithNegativeElement());
  }

  @Test
  void rowWithNegativeElement_noNegative() {
    assertEquals(-1, matrix.rowWithNegativeElement());
  }

  @Test
  void firstNonZeroRowIndex() {
    Matrix m = new Matrix(new int[][] {{0, 0}, {0, 1}});
    assertEquals(1, m.firstNonZeroRowIndex());
  }

  @Test
  void firstNonZeroRowIndex_allZeros() {
    Matrix m = new Matrix(2, 2);
    assertEquals(-1, m.firstNonZeroRowIndex());
  }

  @Test
  void firstNonZeroElementIndex() {
    Matrix m = new Matrix(new int[][] {{0, 0, 5, 0}, {1, 2, 3, 4}});
    assertEquals(2, m.firstNonZeroElementIndex(0));
    assertEquals(0, m.firstNonZeroElementIndex(1));
  }

  @Test
  void firstNonZeroElementIndex_allZeros() {
    Matrix m = new Matrix(new int[][] {{0, 0, 0}, {1, 1, 1}});
    assertEquals(-1, m.firstNonZeroElementIndex(0));
  }

  @Test
  void checkCase11_allNonNegative() {
    Matrix m = new Matrix(new int[][] {{1, 0, 3}, {4, 5, 6}});
    assertTrue(m.checkCase11());
  }

  @Test
  void checkCase11_allNonPositive() {
    Matrix m = new Matrix(new int[][] {{-1, -2, 0}, {-4, -5, -6}});
    assertTrue(m.checkCase11());
  }

  @Test
  void checkCase11_mixed() {
    Matrix m = new Matrix(new int[][] {{1, -2, 3}, {4, 5, 6}});
    assertTrue(m.checkCase11());
  }

  @Test
  void checkCase11_zeroRow() {
    Matrix m = new Matrix(new int[][] {{1, -2, 3}, {0, 0, 0}});
    assertFalse(m.checkCase11());
  }

  @Test
  void cardinalityCondition_positiveCountOne() {
    Matrix m = new Matrix(new int[][] {{1, -2, -3}, {2, 3, 4}});
    assertEquals(0, m.cardinalityCondition());
  }

  @Test
  void cardinalityCondition_negativeCountOne() {
    Matrix m = new Matrix(new int[][] {{1, 2, 3}, {-1, 2, 3}});
    assertEquals(1, m.cardinalityCondition());
  }

  @Test
  void cardinalityCondition_noMatch() {
    Matrix m = new Matrix(new int[][] {{1, 2, -3, -4}, {5, 6, 7, 8}});
    assertEquals(-1, m.cardinalityCondition());
  }

  @Test
  void cardinalityOne_positive() {
    Matrix m = new Matrix(new int[][] {{1, -2, -3}, {2, 3, 4}});
    assertEquals(0, m.cardinalityOne());
  }

  @Test
  void cardinalityOne_negative() {
    Matrix m = new Matrix(new int[][] {{1, 2, 3}, {-1, 2, 3}});
    assertEquals(0, m.cardinalityOne());
  }

  @Test
  void cardinalityOne_noMatch() {
    Matrix m = new Matrix(new int[][] {{1, 2, -3, -4}, {5, 6, 7, 8}});
    assertEquals(-1, m.cardinalityOne());
  }

  @Test
  void colsToUpdate() {
    Matrix m = new Matrix(new int[][] {{1, -2, -3}, {1, 2, 0}});
    assertArrayEquals(new int[] {2, 3}, m.colsToUpdate());
  }

  @Test
  void getPositiveIndices() {
    Matrix m = new Matrix(new int[][] {{1, -2, 3, 0, 5}});
    assertArrayEquals(new int[] {1, 3, 5}, m.getPositiveIndices(0));
  }

  @Test
  void getNegativeIndices() {
    Matrix m = new Matrix(new int[][] {{1, -2, 3, 0, -5}});
    assertArrayEquals(new int[] {2, 5}, m.getNegativeIndices(0));
  }

  @Test
  void findRemainingNonZeroIndices() {
    Matrix m = new Matrix(new int[][] {{0, 1, 0, 2, 3}});
    assertArrayEquals(new int[] {3, 4}, m.findRemainingNonZeroIndices(0));
  }

  @Test
  void findRemainingNonZeroCoefficients() {
    Matrix m = new Matrix(new int[][] {{0, 1, 0, 2, 3}});
    assertArrayEquals(new int[] {2, 3}, m.findRemainingNonZeroCoefficients(0));
  }

  @Test
  void linearlyCombine_firstSignature() {
    Matrix m = new Matrix(new int[][] {{1, 2, 3}, {4, 5, 6}});
    m.linearlyCombine(2, 2, new int[] {1}, new int[] {1}); // k=2, chk=2, j={1}, chj={1}
    // colToUpdate = 0. val = 1 * get(row, 2) + 2 * get(row, 0)
    assertEquals(5, m.get(0, 0)); // 1*3 + 2*1
    assertEquals(2, m.get(0, 1)); // unchanged
    assertEquals(3, m.get(0, 2)); // unchanged
    assertEquals(14, m.get(1, 0)); // 1*6 + 2*4
    assertEquals(5, m.get(1, 1)); // unchanged
    assertEquals(6, m.get(1, 2)); // unchanged
  }

  @Test
  void linearlyCombine_secondSignature() {
    Matrix m = new Matrix(new int[][] {{1, 2, 3}, {4, 5, 6}});
    m.linearlyCombine(0, new int[] {2, 3}, new int[] {1, 2}, new int[] {1, 1});
    assertEquals(1, m.get(0, 0)); // unchanged
    assertEquals(4, m.get(0, 1)); // 2*1 + 1*2
    assertEquals(6, m.get(0, 2)); // 3*1 + 1*3
    assertEquals(4, m.get(1, 0)); // unchanged
    assertEquals(13, m.get(1, 1)); // 2*4 + 1*5
    assertEquals(18, m.get(1, 2)); // 3*4 + 1*6
  }
}
