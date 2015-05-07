/**
 * 
 */


import java.util.Random;

/**
 * @author Lee
 *
 */
public class SquareMatrix {
  private int[] matrix;
  private int sizeOfMatrix;

  /**
   * @param size: Square Matrix Size
   */
  public SquareMatrix(int sizeOfMatrix) {
    super();
    this.sizeOfMatrix = sizeOfMatrix;
    matrix = new int[sizeOfMatrix * sizeOfMatrix];
    
    initializeMatrix();
  }

  /**
   *
   */
  private void initializeMatrix() {
    final int bound = 5;
    Random randomGenerator = new Random();
    
    for (int i = 0; i < sizeOfMatrix; ++i) {
      for (int j = 0; j < sizeOfMatrix; ++j) {
        matrix[(i * sizeOfMatrix) + j] = randomGenerator.nextInt(bound);
      }
    }
  }

  /**
   * @return the matrix
   */
  public int[] getMatrix() {
    return matrix;
  }

  /**
   * @return the sizeOfMatrix
   */
  public int getSizeOfMatrix() {
    return sizeOfMatrix;
  }
}
