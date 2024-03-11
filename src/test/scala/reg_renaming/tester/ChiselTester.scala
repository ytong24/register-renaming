package reg_renaming.tester

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import reg_renaming.reg_renaming_table.{FreeList, RegFile, RegFileEntryState}

class ChiselTester extends AnyFlatSpec with ChiselScalatestTester{
  behavior of "FreeList"
  it should "be initialized with the correct size" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      dut.io.size.expect(5.U, "Initial FreeList size should be equal to ptagNum")
    }
  }

  it should "decrease in size by one after a pop" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.size.expect(4.U, "FreeList size should decrease by 1 after pop")
    }
  }

  it should "increase in size by one after a push" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      // First pop to decrease size
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.pop.poke(false.B)

      // Then push to increase size
      dut.io.push.poke(true.B)
      dut.io.ptagToPush.poke(0.U) // Assuming 0 is a valid ptag to push
      dut.clock.step()
      dut.io.size.expect(5.U, "FreeList size should increase by 1 after push")
    }
  }

  it should "pop values within valid range" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.pop.poke(false.B)

      val poppedValue = dut.io.ptagPopped.peek().litValue
      assert(poppedValue >= 0 && poppedValue < 5, "Popped value should be within valid range")
    }
  }


  it should "restore its size after popping and then pushing" in {
    test(new FreeList(ptagNum = 5)) { dut =>
      // First pop to decrease size
      dut.io.pop.poke(true.B)
      dut.clock.step()
      dut.io.pop.poke(false.B)

      // Then push to increase size
      dut.io.push.poke(true.B)
      dut.io.ptagToPush.poke(dut.io.ptagPopped.peek().litValue.U) // Re-push the popped value
      dut.clock.step()
      dut.io.size.expect(5.U, "FreeList size should be restored after pushing the popped element back")
    }
  }


  behavior of "RegFile"
  it should "return a valid RegFileEntry for a valid index" in {
    test(new RegFile(ptagNum = 5)) { dut =>
      dut.io.index.poke(0.U)
      dut.io.writeEnable.poke(true.B)
      dut.io.writeValue.regPtag.poke(1.U)
      dut.io.writeValue.regArchId.poke(1.U)
      dut.io.writeValue.prevSameArchId.poke(0.U)
      dut.io.writeValue.regState.poke(RegFileEntryState.ALLOC)
      dut.clock.step()

      dut.io.index.poke(0.U)
      dut.io.writeEnable.poke(false.B)
      dut.clock.step()

      dut.io.readValue.regPtag.expect(1.U, "Retrieved RegFileEntry should have a regPtag of 1")
      dut.io.readValue.regArchId.expect(1.U, "Retrieved RegFileEntry should have a regArchId of 10")
      dut.io.readValue.prevSameArchId.expect(0.U, "Retrieved RegFileEntry should have a prevSameArchId of 20")
      dut.io.readValue.regState.expect(RegFileEntryState.ALLOC, "Retrieved RegFileEntry should be in ALLOC state")
    }
  }

  it should "correctly set a RegFileEntry at a specific index" in {
    test(new RegFile(ptagNum = 5)) { dut =>
      dut.io.index.poke(1.U)
      dut.io.writeEnable.poke(true.B)
      dut.io.writeValue.regPtag.poke(2.U)
      dut.io.writeValue.regArchId.poke(2.U)
      dut.io.writeValue.prevSameArchId.poke(1.U)
      dut.io.writeValue.regState.poke(RegFileEntryState.COMMIT)
      dut.clock.step()

      dut.io.index.poke(1.U)
      dut.io.writeEnable.poke(false.B)
      dut.clock.step()

      dut.io.readValue.regPtag.expect(2.U, "Set RegFileEntry should have a regPtag of 2")
      dut.io.readValue.regArchId.expect(2.U, "Set RegFileEntry should have a regArchId of 30")
      dut.io.readValue.prevSameArchId.expect(1.U, "Set RegFileEntry should have a prevSameArchId of 40")
      dut.io.readValue.regState.expect(RegFileEntryState.COMMIT, "Set RegFileEntry should be in COMMIT state")
    }
  }
}
