/** *************************************************************************************
 * File         : RegFile.scala
 * Authors      : Yinyuan Zhao, Yan Tong
 * Date         : 03/04/2024
 * Description  : Scala implementation of Register File
 * ************************************************************************************* */

package reg_renaming.model.reg_renaming_table

import reg_renaming.reg_renaming_table.RegRenamingTableConfig

object RegFileEntryState extends Enumeration {
  val FREE, ALLOC, COMMIT, DEAD = Value
}

class RegFileEntry(
                    private val _regPtag: Int
                  ) {
  private var _regArchId: Int = -1
  private var _prevSameArchId: Int = -1
  private var _regState: RegFileEntryState.Value = RegFileEntryState.FREE

  def getRegPtag: Int = _regPtag

  def getRegArchId: Int = _regArchId

  def setRegArchId(value: Int): Unit = _regArchId = value

  def getPrevSameArchId: Int = _prevSameArchId

  def setPrevSameArchId(value: Int): Unit = _prevSameArchId = value

  def getRegState: RegFileEntryState.Value = _regState

  def setRegState(value: RegFileEntryState.Value): Unit = _regState = value
}

class RegFile(config: RegRenamingTableConfig) {
  private var _regFileEntries: Array[RegFileEntry] = Array.ofDim[RegFileEntry](config.ptagNum)

  for (i <- _regFileEntries.indices) {
    _regFileEntries(i) = new RegFileEntry(i)
  }

  def getRegFileEntry(index: Int): RegFileEntry = {
    require(index >= 0 && index < _regFileEntries.length, "Invalid index for regFileEntries")
    _regFileEntries(index)
  }
}
