/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
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

import java.util.Calendar
import scala.swing.event.Event

/**
 *
 * This is just a lightweight value object. So, it can be used to lightly store
 * tickers at various time. That is, you can store many many tickers for same
 * symbol efficiently, as in case of composing an one minute ser.
 *
 * The TickerSnapshot will present the last current snapshot ticker for one
 * symbol, and implement Observable. You only need one TickerSnapshot for each
 * symbol.
 *
 * @author Caoyuan Deng
 */
case class TickerEvent (source: Sec, ticker: Ticker) extends Event
case class TickersEvent(source: Sec, ticker: List[Ticker]) extends Event

@serializable @cloneable
class Ticker(val depth: Int) extends LightTicker {
  @transient final protected var isChanged: Boolean = _

  /**
   * 0 - bid price
   * 1 - bid size
   * 2 - ask price
   * 3 - ask size
   */
  private val bidAsks = new Array[Float](depth * 4)

  def this() = this(5)

  override protected def updateFieldValue(fieldIdx: Int, v: Float): Boolean = {
    isChanged = super.updateFieldValue(fieldIdx, v)
    isChanged
  }

  final def bidPrice(idx: Int) = bidAsks(idx * 4)
  final def bidSize (idx: Int) = bidAsks(idx * 4 + 1)
  final def askPrice(idx: Int) = bidAsks(idx * 4 + 2)
  final def askSize (idx: Int) = bidAsks(idx * 4 + 3)

  final def setBidPrice(idx: Int, v: Float) = updateDepthValue(idx * 4, v)
  final def setBidSize (idx: Int, v: Float) = updateDepthValue(idx * 4 + 1, v)
  final def setAskPrice(idx: Int, v: Float) = updateDepthValue(idx * 4 + 2, v)
  final def setAskSize (idx: Int, v: Float) = updateDepthValue(idx * 4 + 3, v)

  private def updateDepthValue(idx: Int, v: Float) {
    isChanged = bidAsks(idx) != v
    bidAsks(idx) = v
  }

  override def reset {
    super.reset

    var i = 0
    while (i < bidAsks.length) {
      bidAsks(i) = 0
      i += 1
    }
  }

  def copyFrom(another: Ticker): Unit = {
    super.copyFrom(another)
    System.arraycopy(another.bidAsks, 0, bidAsks, 0, bidAsks.length)
  }

  final def isValueChanged(another: Ticker): Boolean = {
    if (super.isValueChanged(another)) {
      return true
    }

    var i = 0
    while (i < bidAsks.length) {
      if (bidAsks(i) != another.bidAsks(i)) {
        return true
      }

      i += 1
    }

    false
  }

  final def isDayVolumeGrown(prevTicker: Ticker): Boolean = {
    dayVolume > prevTicker.dayVolume // && isSameDay(prevTicker) @todo
  }

  final def isDayVolumeChanged(prevTicker: Ticker): Boolean = {
    dayVolume != prevTicker.dayVolume // && isSameDay(prevTicker) @todo
  }

  final def isSameDay(prevTicker: Ticker, cal: Calendar): Boolean = {
    cal.setTimeInMillis(time)
    val monthA = cal.get(Calendar.MONTH)
    val dayA = cal.get(Calendar.DAY_OF_MONTH)
    cal.setTimeInMillis(prevTicker.time)
    val monthB = cal.get(Calendar.MONTH)
    val dayB = cal.get(Calendar.DAY_OF_MONTH)
        
    monthB == monthB && dayA == dayB
  }

  final def changeInPercent: Float = {
    if (prevClose == 0) 0f  else (lastPrice - prevClose) / prevClose * 100f
  }

  final def compareLastCloseTo(prevTicker: Ticker): Int = {
    if (lastPrice > prevTicker.lastPrice) 1
    else if (lastPrice == prevTicker.lastPrice) 0
    else 1
  }

  def toLightTicker: LightTicker = {
    val light = new LightTicker
    light.copyFrom(this)
    light
  }

  override def clone: Ticker = {
    try {
      return super.clone.asInstanceOf[Ticker]
    } catch {case ex: CloneNotSupportedException => ex.printStackTrace}
        
    null
  }
}
