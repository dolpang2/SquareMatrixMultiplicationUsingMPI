import mpi.MPI;


public class MasterTask implements MPIHelper {
  private SquareMatrix matrixA;
  private SquareMatrix matrixB;
  private int numOfWorkers;
  private static final int FROM_MASTER = MessageTag.FROM_MASTER.getValue();

  /**
   * @param matrixA
   * @param matrixB
   * @param numOfWorkers
   */
  public MasterTask(SquareMatrix matrixA, SquareMatrix matrixB, int numOfWorkers) {
    super();
    this.matrixA = matrixA;
    this.matrixB = matrixB;
    this.numOfWorkers = numOfWorkers;
  }

  @Override
  public void sendTask() {
    final int dimension = matrixA.getSize();
    final int numOfComputingRow = dimension / numOfWorkers;
    final int numOfRemainderRow = dimension % numOfWorkers;

    int[] numOfSendingRow = new int[1];
    int[] offsetInfo = new int[1];
    for (int workerID = 1; workerID <= numOfWorkers; ++workerID) {
      if (workerID <= numOfRemainderRow) {
        numOfSendingRow[0] = numOfComputingRow + 1;
      } else {
        numOfSendingRow[0] = numOfComputingRow;
      }

      sendOffsetInfo(offsetInfo, workerID);
      sendComputingRowInfo(numOfSendingRow, workerID);
      sendInfoOfMatrixA(offsetInfo, numOfSendingRow, workerID);
      sendInfoOfMatrixB(workerID);
      updateOffset(offsetInfo, numOfSendingRow);
    }

  }

  private void sendOffsetInfo(int[] offsetInfo, int workerID) {
    MPI.COMM_WORLD.Send(offsetInfo, 0, 1, MPI.INT, workerID, FROM_MASTER);
  }

  private void sendComputingRowInfo(int[] numOfSendingRow, int workerID) {
    MPI.COMM_WORLD.Send(numOfSendingRow, 0, 1, MPI.INT, workerID, FROM_MASTER);
  }
  
  private void sendInfoOfMatrixA(int[] offsetInfo, int[] numOfSendingRow, int workerID) {
    final int dimension = matrixA.getSize();
    final int startPosition = offsetInfo[0] * dimension;
    final int sendingSize = numOfSendingRow[0] * dimension;

    MPI.COMM_WORLD.Send(matrixA.getMatrix(), startPosition, sendingSize, MPI.INT, workerID,
        FROM_MASTER);
  }
  
  private void sendInfoOfMatrixB(int workerID) {
    final int dimension = matrixB.getSize();
    final int sendingSize = dimension * dimension;
    MPI.COMM_WORLD.Send(matrixB.getMatrix(), 0, sendingSize, MPI.INT, workerID, FROM_MASTER);
  }

  private void updateOffset(int[] offsetInfo, int[] numOfSendingRow) {
    offsetInfo[0] += numOfSendingRow[0];
  }

  @Override
  public void receiveTask() {
    // TODO Auto-generated method stub

  }

}
