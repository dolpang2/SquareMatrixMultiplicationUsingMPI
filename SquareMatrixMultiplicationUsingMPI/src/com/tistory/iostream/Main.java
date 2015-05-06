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
    int N = 1500;
    int MASTER = 0;
    int FROM_MASTER = 1;
    int FROM_WORKER = 2;
    int numtasks, /* number of tasks in partition */
    taskid, /* a task identifier */
    numworkers, /* number of worker tasks */
    source, /* task id of message source */
    dest, /* task id of message destination */
    nbytes, /* number of bytes in message */
    mtype, /* message type */
    intsize, /* size of an integer in bytes */
    dbsize, /* size of a double float in bytes */
    averow, extra, /* used to determine rows sent to each worker */
    //i, j, k, 
    /* misc */
    count;
    int[] a = new int[N * N]; /* matrix A to be multiplied */
    int[] b = new int[N * N]; /* matrix B to be multiplied */
    int[] c = new int[N * N]; /* result matrix C */
    int[] offset = new int[1];
    int[] rows = new int[1]; /* rows of matrix A sent to each worker */

    long[] computeTime = new long[1];
    long[] maxComputeTime = new long[1];
    MPI.Init(args);
    taskid = MPI.COMM_WORLD.Rank();
    numtasks = MPI.COMM_WORLD.Size();
    numworkers = numtasks - 1;

    /* *************** Master Task ****************** */
    if (taskid == MASTER) {
      // Init matrix A,B
      for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
          a[(i * N) + j] = 1;
          b[(i * N) + j] = 2;
        }
      }

      // Send matrix data to worker tasks
      long start = System.currentTimeMillis();
      averow = N / numworkers;
      extra = N % numworkers;
      offset[0] = 0;
      mtype = FROM_MASTER;

      long startsend = System.currentTimeMillis();
      for (dest = 1; dest <= numworkers; dest++) {
        if (dest <= extra) {
          rows[0] = averow + 1;
        } else {
          rows[0] = averow;
        }
        MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, dest, mtype);
        MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, dest, mtype);
        count = rows[0] * N;
        MPI.COMM_WORLD.Send(a, (offset[0] * N), count, MPI.INT, dest, mtype);
        count = N * N;
        MPI.COMM_WORLD.Send(b, 0, count, MPI.INT, dest, mtype);
        offset[0] = offset[0] + rows[0];
      }
      long stopsend = System.currentTimeMillis();
      // Wait for results from all worker tasks
      computeTime[0] = 0;
      mtype = FROM_WORKER;
      for (int i = 1; i <= numworkers; i++) {
        source = i;
        MPI.COMM_WORLD.Recv(computeTime, 0, 1, MPI.LONG, source, mtype);
        System.out.println("Rank " + i + " uses " + computeTime[0] + " for computing");
        MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, mtype);
        MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, mtype);
        count = rows[0] * N;
        MPI.COMM_WORLD.Recv(c, offset[0] * N, count, MPI.INT, source, mtype);
      }
      long stop = System.currentTimeMillis();
      // System.out.println("Result of matrix c[0] = " + c[0] + ", c[1000*1000] = " + c[100*100]);
      System.out.println("Time Usage = " + (stop - start));
      System.out.println("Sending Time Usage = " + (stopsend - startsend));
    }

    /* *************************** worker task *********************************** */
    if (taskid > MASTER) {
      mtype = FROM_MASTER;
      source = MASTER;
      MPI.COMM_WORLD.Recv(offset, 0, 1, MPI.INT, source, mtype);
      MPI.COMM_WORLD.Recv(rows, 0, 1, MPI.INT, source, mtype);
      count = rows[0] * N;
      MPI.COMM_WORLD.Recv(a, 0, count, MPI.INT, source, mtype);
      count = N * N;
      MPI.COMM_WORLD.Recv(b, 0, count, MPI.INT, source, mtype);

      long startCompute = System.currentTimeMillis();
      for (int i = 0; i < rows[0]; i++) {
        for (int k = 0; k < N; k++) {
          c[(i * N) + k] = 0;
          for (int j = 0; j < N; j++) {
            c[(i * N) + k] = c[(i * N) + k] + a[(i * N) + j] * b[(j * N) + k];
          }
        }
      }
      long stopCompute = System.currentTimeMillis();
      computeTime[0] = (stopCompute - startCompute);
      mtype = FROM_WORKER;
      MPI.COMM_WORLD.Send(computeTime, 0, 1, MPI.LONG, MASTER, mtype);
      MPI.COMM_WORLD.Send(offset, 0, 1, MPI.INT, MASTER, mtype);
      MPI.COMM_WORLD.Send(rows, 0, 1, MPI.INT, MASTER, mtype);
      MPI.COMM_WORLD.Send(c, 0, rows[0] * N, MPI.INT, MASTER, mtype);
    }

    MPI.COMM_WORLD.Reduce(computeTime, 0, maxComputeTime, 0, 1, MPI.LONG, MPI.MAX, 0);
    if (taskid == 0) {
      System.out.println("Max compute time/machine = " + maxComputeTime[0]);
    }
    MPI.Finalize();

  }

}
