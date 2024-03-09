package reg_renaming

import chisel3._
import chisel3.util._

case class OpConfig(numSrcMax: Int, numDstMax: Int, archIdNum: Int)

class Operand(config: OpConfig) extends Bundle {
  val numSrc = UInt(log2Ceil(config.numSrcMax + 1).W)
  val numDst = UInt(log2Ceil(config.numDstMax + 1).W)
  val archSrcIds = Vec(config.numSrcMax, UInt(log2Ceil(config.archIdNum).W))
  val archDstIds = Vec(config.numDstMax, UInt(log2Ceil(config.archIdNum).W))
  val ptagSrcIds = Vec(config.numSrcMax, UInt(log2Ceil(config.numSrcMax).W))
  val ptagDstIds = Vec(config.numDstMax, UInt(log2Ceil(config.numDstMax).W))
}

