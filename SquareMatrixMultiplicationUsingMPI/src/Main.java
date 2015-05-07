/**
 * 
 */


import mpi.MPI;

/**
 * @author Lee
 *
 */
public class Main {

  /**
   * @param args
   */
  public static void main(String[] args) {
    runMultiplication(args);
  }

  /**
   * @param args
   */
  private static void runMultiplication(String[] args) {
    int sizeOfMatrix = 1500;

    SquareMatrix matrixA = new SquareMatrix(sizeOfMatrix);
    SquareMatrix matrixB = new SquareMatrix(sizeOfMatrix);
    SquareMatrix resultMatrix = new SquareMatrix(sizeOfMatrix);

    int[] offset = new int[1];
    int[] rows = new int[1];

    long[] computeTime = new long[1];
    long[] maxComputeTime = new long[1];

    MPI.Init(args);
    int currentTaskID = MPI.COMM_WORLD.Rank();
    int numOfTasks = MPI.COMM_WORLD.Size();
    int numOfWorkers = numOfTasks - 1;

    /* *************** Master Task ****************** */
    if (currentTaskID == MessageTag.MASTER.getValue()) {
      // Send matrix data to worker tasks
      long start = System.currentTimeMillis();
      offset[0] = 0;

      long startTimeOfSend = System.currentTimeMillis();
      masterSendTask(matrixA, matrixB, offset, rows, numOfWorkers);
      long endTimeOfSend = System.currentTimeMillis();
      // Wait for results from all worker tasks
      computeTime[0] = 0;

      masterReceiveTask(resultMatrix, offset, rows, computeTime, numOfWorkers);
      long stop = System.currentTimeMillis();

      System.out.println("Time Usage = " + (stop - start));
      System.out.println("Sending Time Usage = " + (endTimeOfSend - startTimeOfSend));
    }

    /* *************************** worker task *********************************** */
    if (currentTaskID > MessageTag.MASTER.getValue()) {
      workerReceiveTask(matrixA, matrixB, offset, rows);

      long startTimeOfComputation = System.currentTimeMillis();
      multiplyMatrix(resultMatrix, rows, matrixA, matrixB);
      long endTimeOfComputation = System.currentTimeMillis();

      computeTime[0] = endTimeOfComputation - startTimeOfComputation;
      workerSendTask(resultMatrix, offset, rows, computeTime);
    }

    MPI.COMM_WORLD.Reduce(computeTime, 0, maxComputeTime, 0, 1, MPI.LONG, MPI.MAX, 0);
    if (currentTaskID == 0) {
      System.out.println("Max compute time/machine = " + maxComputeTime[0]);
    }
    MPI.Finalize();
  }

  private static void masterReceiveTask(SquareMatrix resultMatrix, int[] offset, int[] rows,
      long[] computeTime, int numOfWorkers) {
    int[] convertedResultMatrix = resultMatrix.getMatrix();
    int messageType = MessageTag.FROM_WORKER.getValue();
    int sizeOfMatrix = resultMatrix.getSizeOfMatrix();
    int count;

    for (int i = 1; i <= numOfWorkers; i++) {
      int idOfWorker = i;

      MPI.COMM_WORLD.Recv(computeTime, 0, 1, MPI.LONG, idOfWorker, messageType);
      System.out.println("Rank " + i + " uses " + computeTime[0] + " for computing");
      MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, idOfWorker, messageType);
      MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, idOfWorker, messageType);
      count = rows[0] * sizeOfMatrix;
      MPI.COMM_WORLD.Recv(convertedResultMatrix, offset[0] * sizeOfMatrix, count, MPI.INT,
          idOfWorker, messageType);
    }
  }

  private static void masterSendTask(SquareMatrix a, SquareMatrix b, int[] offset, int[] rows,
      int numOfWorkers) {
    int[] convertedMatrixA = a.getMatrix();
    int[] convertedMatrixB = b.getMatrix();
    int messageType = MessageTag.FROM_MASTER.getValue();
    int sizeOfMatrix = a.getSizeOfMatrix();
    int numOfAverageRow = sizeOfMatrix / numOfWorkers;
    int numOfRemainderRow = sizeOfMatrix % numOfWorkers;
    int sendingSize;

    for (int dest = 1; dest <= numOfWorkers; dest++) {
      if (dest <= numOfRemainderRow) {
        rows[0] = numOfAverageRow + 1;
      } else {
        rows[0] = numOfAverageRow;
      }
      MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, dest, messageType);
      MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, dest, messageType);
      sendingSize = rows[0] * sizeOfMatrix;
      MPI.COMM_WORLD.Send(convertedMatrixA, (offset[0] * sizeOfMatrix), sendingSize, MPI.INT, dest,
          messageType);
      sendingSize = sizeOfMatrix * sizeOfMatrix;
      MPI.COMM_WORLD.Send(convertedMatrixB, 0, sendingSize, MPI.INT, dest, messageType);
      offset[0] = offset[0] + rows[0];
    }
  }

  /**
   * @param resultMatrix
   * @param offset
   * @param rows
   * @param computeTime
   */
  private static void workerSendTask(SquareMatrix resultMatrix, int[] offset, int[] rows,
      long[] computeTime) {
    int messageType = MessageTag.FROM_WORKER.getValue();
    int sizeOfMatrix = resultMatrix.getSizeOfMatrix();
    int[] convertedResultMatrix = resultMatrix.getMatrix();

    MPI.COMM_WORLD.Send(computeTime, 0, 1, MPI.LONG, MessageTag.MASTER.getValue(), messageType);
    MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, MessageTag.MASTER.getValue(), messageType);
    MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, MessageTag.MASTER.getValue(), messageType);
    MPI.COMM_WORLD.Send(convertedResultMatrix, 0, rows[0] * sizeOfMatrix, MPI.INT,
        MessageTag.MASTER.getValue(), messageType);
  }

  /**
   * @param result
   * @param rows
   * @param a
   * @param b
   */
  private static void multiplyMatrix(SquareMatrix result, int[] rows, SquareMatrix a, SquareMatrix b) {
    int sizeOfMatrix = result.getSizeOfMatrix();
    int[] convertedMatrixA = a.getMatrix();
    int[] convertedMatrixB = b.getMatrix();
    int[] resultOfMatrix = result.getMatrix();

    for (int i = 0; i < rows[0]; i++) {
      for (int k = 0; k < sizeOfMatrix; k++) {
        resultOfMatrix[(i * sizeOfMatrix) + k] = 0;

        for (int j = 0; j < sizeOfMatrix; j++) {
          resultOfMatrix[(i * sizeOfMatrix) + k] =
              resultOfMatrix[(i * sizeOfMatrix) + k] + convertedMatrixA[(i * sizeOfMatrix) + j]
                  * convertedMatrixB[(j * sizeOfMatrix) + k];
        }
      }
    }
  }

  /**
   * @param a
   * @param b
   * @param offset
   * @param rows
   */
  private static void workerReceiveTask(SquareMatrix a, SquareMatrix b, int[] offset, int[] rows) {
    int messageType = MessageTag.FROM_MASTER.getValue();
    int source = MessageTag.MASTER.getValue();
    int receivingSize = 0;
    int sizeOfMatrix = a.getSizeOfMatrix();

    MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, messageType);
    MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, messageType);
    receivingSize = rows[0] * sizeOfMatrix;
    MPI.COMM_WORLD.Recv(a.getMatrix(), 0, receivingSize, MPI.INT, source, messageType);
    receivingSize = sizeOfMatrix * sizeOfMatrix;
    MPI.COMM_WORLD.Recv(b.getMatrix(), 0, receivingSize, MPI.INT, source, messageType);
  }
}
