/**
 * 
 */
package com.tistory.iostream;

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
    int numOfTasks, /* number of tasks in partition */
    taskid, /* a task identifier */
    numworkers, /* number of worker tasks */
    source, /* task id of message source */
    mtype, /* message type */
    averow, extra, /* used to determine rows sent to each worker */

    /* misc */
    count;

    SquareMatrix matrixA = new SquareMatrix(sizeOfMatrix);
    SquareMatrix matrixB = new SquareMatrix(sizeOfMatrix);  

    int[] c = new int[sizeOfMatrix * sizeOfMatrix]; /* result matrix C */

    int[] offset = new int[1];
    int[] rows = new int[1]; /* rows of matrix A sent to each worker */

    long[] computeTime = new long[1];
    long[] maxComputeTime = new long[1];



    MPI.Init(args);
    taskid = MPI.COMM_WORLD.Rank();
    numOfTasks = MPI.COMM_WORLD.Size();
    numworkers = numOfTasks - 1;

    int[] convertedMatrixA = matrixA.getMatrix();
    int[] convertedMatrixB = matrixB.getMatrix();
    /* *************** Master Task ****************** */
    if (taskid == MessageTag.MASTER.getValue()) {
      

      // Send matrix data to worker tasks
      long start = System.currentTimeMillis();
      averow = sizeOfMatrix / numworkers;
      extra = sizeOfMatrix % numworkers;
      offset[0] = 0;
      mtype = MessageTag.FROM_MASTER.getValue();

      long startsend = System.currentTimeMillis();
      for (int dest = 1; dest <= numworkers; dest++) {
        if (dest <= extra) {
          rows[0] = averow + 1;
        } else {
          rows[0] = averow;
        }
        MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, dest, mtype);
        MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, dest, mtype);
        count = rows[0] * sizeOfMatrix;
        MPI.COMM_WORLD.Send(convertedMatrixA, (offset[0] * sizeOfMatrix), count, MPI.INT, dest, mtype);
        count = sizeOfMatrix * sizeOfMatrix;
        MPI.COMM_WORLD.Send(convertedMatrixB, 0, count, MPI.INT, dest, mtype);
        offset[0] = offset[0] + rows[0];
      }
      long stopsend = System.currentTimeMillis();
      // Wait for results from all worker tasks
      computeTime[0] = 0;
      mtype = MessageTag.FROM_WORKER.getValue();
      for (int i = 1; i <= numworkers; i++) {
        source = i;
        MPI.COMM_WORLD.Recv(computeTime, 0, 1, MPI.LONG, source, mtype);
        System.out.println("Rank " + i + " uses " + computeTime[0] + " for computing");
        MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, mtype);
        MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, mtype);
        count = rows[0] * sizeOfMatrix;
        MPI.COMM_WORLD.Recv(c, offset[0] * sizeOfMatrix, count, MPI.INT, source, mtype);
      }
      long stop = System.currentTimeMillis();
      // System.out.println("Result of matrix c[0] = " + c[0] + ", c[1000*1000] = " + c[100*100]);
      System.out.println("Time Usage = " + (stop - start));
      System.out.println("Sending Time Usage = " + (stopsend - startsend));
    }

    /* *************************** worker task *********************************** */
    if (taskid > MessageTag.MASTER.getValue()) {
      mtype = MessageTag.FROM_MASTER.getValue();
      source = MessageTag.MASTER.getValue();
      MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, mtype);
      MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, mtype);
      count = rows[0] * sizeOfMatrix;
      MPI.COMM_WORLD.Recv(convertedMatrixA, 0, count, MPI.INT, source, mtype);
      count = sizeOfMatrix * sizeOfMatrix;
      MPI.COMM_WORLD.Recv(convertedMatrixB, 0, count, MPI.INT, source, mtype);

      long startCompute = System.currentTimeMillis();
      for (int i = 0; i < rows[0]; i++) {
        for (int k = 0; k < sizeOfMatrix; k++) {
          c[(i * sizeOfMatrix) + k] = 0;
          for (int j = 0; j < sizeOfMatrix; j++) {
            c[(i * sizeOfMatrix) + k] =
                c[(i * sizeOfMatrix) + k] + convertedMatrixA[(i * sizeOfMatrix) + j] * convertedMatrixB[(j * sizeOfMatrix) + k];
          }
        }
      }
      long stopCompute = System.currentTimeMillis();
      computeTime[0] = (stopCompute - startCompute);
      mtype = MessageTag.FROM_WORKER.getValue();
      MPI.COMM_WORLD.Send(computeTime, 0, 1, MPI.LONG, MessageTag.MASTER.getValue(), mtype);
      MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, MessageTag.MASTER.getValue(), mtype);
      MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, MessageTag.MASTER.getValue(), mtype);
      MPI.COMM_WORLD.Send(c, 0, rows[0] * sizeOfMatrix, MPI.INT, MessageTag.MASTER.getValue(),
          mtype);
    }

    MPI.COMM_WORLD.Reduce(computeTime, 0, maxComputeTime, 0, 1, MPI.LONG, MPI.MAX, 0);
    if (taskid == 0) {
      System.out.println("Max compute time/machine = " + maxComputeTime[0]);
    }
    MPI.Finalize();
  }

}
