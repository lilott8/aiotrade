/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.securities

import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.{TVal, TSerEvent, DefaultBaseTSer, TFreq}
import org.aiotrade.lib.securities.model.MoneyFlow
import org.aiotrade.lib.securities.model.Sec

/**
 *
 * @author Caoyuan Deng
 */
class MoneyFlowSer($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {

  private var _shortDescription: String = ""
  var adjusted: Boolean = false

  val totalVolume = TVar[Double]("TV", Plot.Stick)
  val totalAmount = TVar[Double]("TA", Plot.None)
  
  val superVolume = TVar[Double]("SV", Plot.None)
  val superAmount = TVar[Double]("SA", Plot.None)

  val largeVolume = TVar[Double]("LV", Plot.None)
  val largeAmount = TVar[Double]("LA", Plot.None)

  val smallVolume = TVar[Double]("sV", Plot.None)
  val smallAmount = TVar[Double]("sA", Plot.None)

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    tval match {
      case mf: MoneyFlow =>
        totalVolume(time) = mf.totalVolume
        totalAmount(time) = mf.totalAmount
        superVolume(time) = mf.superVolume
        superAmount(time) = mf.superAmount
        largeVolume(time) = mf.largeVolume
        largeAmount(time) = mf.largeAmount
        smallVolume(time) = mf.smallVolume
        smallAmount(time) = mf.smallVolume
      case _ =>
    }
  }

  def valueOf(time: Long): Option[MoneyFlow] = {
    if (exists(time)) {
      val mf = new MoneyFlow
      mf.totalVolume = totalVolume(time)
      mf.totalAmount = totalAmount(time)
      mf.superVolume = superVolume(time)
      mf.superAmount = superAmount(time)
      mf.largeVolume = largeVolume(time)
      mf.largeAmount = largeAmount(time)
      mf.smallVolume = smallVolume(time)
      mf.smallVolume = smallAmount(time)
      Some(mf)
    } else None
  }

  /**
   * Try to update today's quote item according to quote, if it does not
   * exist, create a new one.
   */
  def updateFrom(mf: MoneyFlow) {
    val time = mf.time
    createOrClear(time)

    totalVolume(time) = mf.totalVolume
    totalAmount(time) = mf.totalAmount
    superVolume(time) = mf.superVolume
    superAmount(time) = mf.superAmount
    largeVolume(time) = mf.largeVolume
    largeAmount(time) = mf.largeAmount
    smallVolume(time) = mf.smallVolume
    smallAmount(time) = mf.smallVolume

    /** be ware of fromTime here may not be same as ticker's event */
    publish(TSerEvent.Updated(this, "", time, time))
  }

  /**
   * @param boolean b: if true, do adjust, else, de adjust
   */
//  def adjust(b: Boolean) {
//    var i = 0
//    while (i < size) {
//
//      var prevNorm = close(i)
//      var postNorm = if (b) {
//        /** do adjust */
//        close_adj(i)
//      } else {
//        /** de adjust */
//        close_ori(i)
//      }
//
//      high(i)  = linearAdjust(high(i),  prevNorm, postNorm)
//      low(i)   = linearAdjust(low(i),   prevNorm, postNorm)
//      open(i)  = linearAdjust(open(i),  prevNorm, postNorm)
//      close(i) = linearAdjust(close(i), prevNorm, postNorm)
//
//      i += 1
//    }
//
//    adjusted = b
//
//    val evt = TSerEvent.Updated(this, null, 0, lastOccurredTime)
//    publish(evt)
//  }
    
  /**
   * This function adjusts linear according to a norm
   */
  private def linearAdjust(value: Double, prevNorm: Double, postNorm: Double): Double = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }

  override def shortDescription_=(desc: String): Unit = {
    this._shortDescription = desc
  }
    
  override def shortDescription: String = {
    _shortDescription
  }
    
}





