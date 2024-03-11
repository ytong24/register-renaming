package reg_renaming.tester

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import reg_renaming.reg_renaming_table.FreeList

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
}
