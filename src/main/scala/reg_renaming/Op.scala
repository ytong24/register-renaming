package reg_renaming

import chisel3._
import chisel3.util._

case class OpConfig(numSrcMax: Int, numDstMax: Int, archIdNum: Int)

class Op(config: OpConfig) extends Bundle {
  val numSrc = Input(UInt(log2Ceil(config.numSrcMax + 1).W))
  val numDst = Input(UInt(log2Ceil(config.numDstMax + 1).W))
  val archSrcIds = Input(Vec(config.numSrcMax, UInt(log2Ceil(config.archIdNum + 1).W)))
  val archDstIds = Input(Vec(config.numDstMax, UInt(log2Ceil(config.archIdNum + 1).W)))

  val ptagSrcIds = Output(Vec(config.numSrcMax, UInt(log2Ceil(config.numSrcMax + 1).W)))
  val ptagDstIds = Output(Vec(config.numDstMax, UInt(log2Ceil(config.numDstMax + 1).W)))
  //  val in = new Bundle {
  //    val numSrc = Input(UInt(log2Ceil(config.numSrcMax + 1).W))
  //    val numDst = Input(UInt(log2Ceil(config.numDstMax + 1).W))
  //    val archSrcIds = Input(Vec(config.numSrcMax, UInt(log2Ceil(config.archIdNum).W)))
  //    val archDstIds = Input(Vec(config.numDstMax, UInt(log2Ceil(config.archIdNum).W)))
  //  }
  //  val out = new Bundle {
  //    val ptagSrcIds = Output(Vec(config.numSrcMax, UInt(log2Ceil(config.numSrcMax).W)))
  //    val ptagDstIds = Output(Vec(config.numDstMax, UInt(log2Ceil(config.numDstMax).W)))
  //  }
}

