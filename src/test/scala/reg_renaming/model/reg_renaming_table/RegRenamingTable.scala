package reg_renaming.model.reg_renaming_table

import reg_renaming.model.{Op, OpConfig}

case class RegRenamingTableConfig(ptagNum: Int)

object RegRenamingTable {
  def apply(tableConfig: RegRenamingTableConfig, opConfig: OpConfig) = {
    // TODO:
    ???
  }

  def available(): Boolean = {
    // TODO:
    ???
  }

  def process(op: Op) = {
    // TODO:
    ???
  }

  def commit(op: Op) = {
    // TODO:
    ???
  }

}
