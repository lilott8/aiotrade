package org.aiotrade.lib.backtest

/**
 * 
 * @author Caoyuan Deng
 */
import org.aiotrade.lib.trading.Position

class TradeRule {
  var quantityPerLot = 100
  var tradableProportionOfVolume = 0.1

  def buyPriceRule(o: Double, h: Double, l: Double, c: Double): Double = {
    o
  }

  def sellPriceRule(o: Double, h: Double, l: Double, c: Double): Double = {
    o
  }
  
  def buyQuantityRule(volume: Double, price: Double, fund: Double) = {
    val quantity = maxQuoantity(volume, price, fund)
    roundQuantity(quantity)
  }
  
  def sellQuantityRule(volume: Double, price: Double, quantity: Double) = {
    math.max(quantity, volume * tradableProportionOfVolume)
  }

  protected def maxQuoantity(volume: Double, price: Double, fund: Double) = {
    math.max((fund / (price * quantityPerLot)) * quantityPerLot, volume * tradableProportionOfVolume).toInt
  }
  
  protected def roundQuantity(quantity: Double) = {
    (quantity % quantityPerLot - 1) * quantityPerLot
  }
  
  def buyTimeRule {}
  
  def sellTimeRule {}
  
  def cutLossRule(position: Position): Boolean = {
    val profit = (position.currentPrice - position.price) / position.price
    profit < -0.05
  }
  
  def takeProfitRule(position: Position): Boolean = {
    val profit = (position.currentPrice - position.price) / position.price
    val maxProfit = (position.highestPrice - position.price) / position.price
    profit < maxProfit * 0.4
  }
}