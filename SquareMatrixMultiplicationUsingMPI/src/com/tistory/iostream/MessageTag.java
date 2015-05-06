/**
 * 
 */
package com.tistory.iostream;

/**
 * @author Lee
 *
 */
public enum MessageTag {
  MASTER(0), FROM_MASTER(1), FROM_WORKER(2);
  
  private int value;

  /**
   * @param value
   */
  private MessageTag(int value) {
    this.value = value;
  }

  /**
   * @return the value
   */
  public int getValue() {
    return value;
  }
}
