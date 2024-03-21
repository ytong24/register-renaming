package reg_renaming.reg_renaming_table

import chisel3._
import chisel3.util._

class FreeList(val ptagNum: Int) extends Module {
  val io = IO(new Bundle {
    val push = Input(Bool())
    val pop = Input(Bool())
    val ptagToPush = Input(UInt(log2Ceil(ptagNum).W))
    val ptagPopped = Output(UInt(log2Ceil(ptagNum).W))
    val size = Output(UInt(log2Ceil(ptagNum + 1).W))
  })

  // elements should be 0..ptagNum-1
  val stack = RegInit(VecInit((0 until ptagNum).map(_.U(log2Ceil(ptagNum).W))))
  val pointer = RegInit(0.U(log2Ceil(ptagNum + 1).W))
  val popValue = RegInit(0.U(log2Ceil(ptagNum).W))

  // Push operation
  when(io.push && !io.pop && pointer > 0.U) {
    stack(pointer - 1.U) := io.ptagToPush
    pointer := pointer - 1.U
  }

  // Pop operation
  when(io.pop && !io.push && pointer < ptagNum.U) {
    pointer := pointer + 1.U
    popValue := stack(pointer)
  }

  // Update output
  io.size := ptagNum.U - pointer
  io.ptagPopped := popValue
}