/** *************************************************************************************
 * File         : op.scala
 * Authors      : Yan Tong, Yinyuan Zhao
 * Date         : 03/04/2024
 * Description  : Scala implementation of Operand
 * ************************************************************************************* */

package reg_renaming.model

import reg_renaming.OpConfig

class Op(
          config: OpConfig,
          private val _numSrc: Int,
          private val _numDst: Int,
          private val _archSrcIds: Array[Int],
          private val _archDstIds: Array[Int],
        ) {
  // TODO: assert srcId < archIdNum
  // TODO: assert dstId < archIdNum
  private var _ptagSrcIds: Array[Int] = Array.ofDim[Int](config.numSrcMax)
  private var _ptagDstIds: Array[Int] = Array.ofDim[Int](config.numDstMax)

  def getNumSrc: Int = _numSrc

  def getNumDst: Int = _numDst

  def getArchSrcId(index: Int): Int = {
    require(index >= 0 && index < _archSrcIds.length, "Invalid index for archSrcIds")
    _archSrcIds(index)
  }

  def getArchDstId(index: Int): Int = {
    require(index >= 0 && index < _archDstIds.length, "Invalid index for archDstIds")
    _archDstIds(index)
  }

  def getPtagSrcId(index: Int): Int = {
    require(index >= 0 && index < _ptagSrcIds.length, "Invalid index for ptagSrcIds")
    _ptagSrcIds(index)
  }

  def setPtagSrcId(index: Int, value: Int): Unit = {
    require(index >= 0 && index < _ptagSrcIds.length, "Invalid index for ptagSrcIds")
    _ptagSrcIds(index) = value
  }

  def getPtagDstId(index: Int): Int = {
    require(index >= 0 && index < _ptagDstIds.length, "Invalid index for ptagDstIds")
    _ptagDstIds(index)
  }

  def setPtagDstId(index: Int, value: Int): Unit = {
    require(index >= 0 && index < _ptagDstIds.length, "Invalid index for ptagDstIds")
    _ptagDstIds(index) = value
  }
}
