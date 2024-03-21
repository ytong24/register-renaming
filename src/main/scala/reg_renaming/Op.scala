/** *************************************************************************************
 * File         : op.scala
 * Authors      : Yan Tong, Yinyuan Zhao
 * Date         : 03/04/2024
 * Description  : Chisel implementation of Operand
 * ************************************************************************************* */

package reg_renaming

import chisel3._
import chisel3.util._

/** Configuration class for the `Op` bundle.
 *
 * @param numSrcMax Maximum number of source operands an operation can have.
 * @param numDstMax Maximum number of destination operands an operation can have.
 * @param archIdNum Number of architectural IDs.
 */
case class OpConfig(numSrcMax: Int, numDstMax: Int, archIdNum: Int)

/** Represents an operation in a Register Renaming Table.
 *
 * @param config Configuration parameters for the operation.
 */
class Op(config: OpConfig) extends Bundle {
  val numSrc = Input(UInt(log2Ceil(config.numSrcMax + 1).W))
  val numDst = Input(UInt(log2Ceil(config.numDstMax + 1).W))
  val archSrcIds = Input(Vec(config.numSrcMax, UInt(log2Ceil(config.archIdNum + 1).W)))
  val archDstIds = Input(Vec(config.numDstMax, UInt(log2Ceil(config.archIdNum + 1).W)))

  val ptagSrcIds = Output(Vec(config.numSrcMax, UInt(log2Ceil(config.numSrcMax + 1).W)))
  val ptagDstIds = Output(Vec(config.numDstMax, UInt(log2Ceil(config.numDstMax + 1).W)))
}

