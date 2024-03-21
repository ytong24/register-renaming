/** *************************************************************************************
 * File         : FreeList.scala
 * Authors      : Yinyuan Zhao, Yan Tong
 * Date         : 03/04/2024
 * Description  : Chisel implementation of Free List
 * ************************************************************************************* */

package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._


/**
 * FreeList Module
 *
 * The FreeList module is a stack-like structure used for tracking the availability
 * of physical registers in a register renaming scheme. It supports push and pop
 * operations to manage the pool of free physical tags (ptags).
 *
 * @param ptagNum The total number of physical tags managed by the FreeList.
 *
 *                Functional Description:
 *                - Push operation: Adds a free ptag to the FreeList stack.
 *                - Pop operation: Removes and returns the top ptag from the FreeList stack.
 *                - Size: Provides the current number of free ptags in the FreeList.
 *
 *                Input/Output Ports:
 *                - push: Trigger signal for push operation.
 *                - pop: Trigger signal for pop operation.
 *                - ptagToPush: The ptag to be pushed onto the stack during a push operation.
 *                - ptagPopped: The ptag that was popped from the stack during a pop operation.
 *                - size: The current size of the FreeList.
 */
class FreeList(val ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val push = Input(Bool())
    val pop = Input(Bool())
    val ptagToPush = Input(UInt(log2Ceil(ptagNum + 1).W))
    val ptagPopped = Output(UInt(log2Ceil(ptagNum + 1).W))
    val size = Output(UInt(log2Ceil(ptagNum + 1).W))
  })
  // elements should be 0..ptagNum-1, make ptagNum the invalid value
  val invalidPopValue = ptagNum.U(log2Ceil(ptagNum + 1).W)
  val stack = RegInit(VecInit((0 until ptagNum).map(_.U(log2Ceil(ptagNum + 1).W))))
  val pointer = RegInit(0.U(log2Ceil(ptagNum + 1).W))
  val popValue = RegInit(invalidPopValue)

  // Push operation
  when(io.push && !io.pop && pointer > 0.U) {
    stack(pointer -& 1.U) := io.ptagToPush // use -& or +& to avoid overflow
    pointer := pointer -& 1.U // use -& or +& to avoid overflow
  }

  // Pop operation
  when(io.pop && !io.push && pointer < ptagNum.U) {
    pointer := pointer +& 1.U
    popValue := stack(pointer)
  }

  // Update output
  io.size := ptagNum.U -& pointer
  io.ptagPopped := popValue
}
