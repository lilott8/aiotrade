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
package org.aiotrade.lib.view.securities.persistence

import java.text.NumberFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.computable.DefaultFactor
import org.aiotrade.lib.math.timeseries.computable.Factor
import org.aiotrade.lib.math.timeseries.computable.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.descriptor.AnalysisContents
import org.aiotrade.lib.charting.chart.handledchart.HandledChart
import org.aiotrade.lib.charting.chart.segment.ValuePoint
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.securities.Sec
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.AttributesImpl
import org.xml.sax.helpers.DefaultHandler
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.Stack

/**
 *
 * @author Caoyuan Deng
 */
class ContentsParseHandler extends DefaultHandler {
  private val NUMBER_FORMAT = NumberFormat.getInstance
    
  private var contents: AnalysisContents = _
    
  private var indicatorDescriptor: IndicatorDescriptor = _
  private var factors: ArrayBuffer[Factor] = _
    
  private var drawingDescriptor: DrawingDescriptor = _
  private var handledChartMapPoints: HashMap[HandledChart, ArrayBuffer[ValuePoint]] = _
  private var handledChartClassName: String = _
  private var points: ArrayBuffer[ValuePoint] = _
    
  val DEBUG = false
    
  private var buffer: StringBuffer = new StringBuffer(500)
  private val context = new Stack[Array[Object]]
    
  private val calendar = Calendar.getInstance
    
  @throws(classOf[SAXException])
  final override def startElement(ns: String, name: String, qname: String, attrs: Attributes) {
    dispatch(true)
    context.push(Array(qname, new AttributesImpl(attrs)))
    if ("handle".equals(qname)) {
      handle_handle(attrs)
    } else if ("indicator".equals(qname)) {
      start_indicator(attrs)
    } else if ("chart".equals(qname)) {
      start_chart(attrs)
    } else if ("opt".equals(qname)) {
      handle_opt(attrs)
    } else if ("indicators".equals(qname)) {
      start_indicators(attrs)
    } else if ("drawings".equals(qname)) {
      start_drawings(attrs)
    } else if ("sec".equals(qname)) {
      start_sec(attrs)
    } else if ("layer".equals(qname)) {
      start_layer(attrs)
    } else if ("sources".equals(qname)) {
      start_sources(attrs)
    } else if ("source".equals(qname)) {
      start_source(attrs)
    }
  }
    
  /**
   *
   * This SAX interface method is implemented by the parser.
   */
  @throws(classOf[SAXException])
  final override def endElement(ns: String, name: String, qname: String) {
    dispatch(false);
    context.pop
    if ("indicator".equals(qname)) {
      end_indicator()
    } else if ("chart".equals(qname)) {
      end_chart()
    } else if ("indicators".equals(qname)) {
      end_indicators()
    } else if ("drawings".equals(qname)) {
      end_drawings()
    } else if ("sec".equals(qname)) {
      end_sec()
    } else if ("layer".equals(qname)) {
      end_layer()
    } else if ("sources".equals(qname)) {
      end_sources()
    }
  }
    
    
  @throws(classOf[SAXException])
  private def dispatch(fireOnlyIfMixed: Boolean)  {
    if (fireOnlyIfMixed && buffer.length == 0) {
      return; //skip it
    }
    val ctx = context.top
    val here = ctx(0).asInstanceOf[String]
    val attrs = ctx(1).asInstanceOf[Attributes]
    buffer.delete(0, buffer.length)
  }
    
    
  @throws(classOf[SAXException])
  def handle_handle(meta: Attributes) {
    if (DEBUG) {
      System.err.println("handle_handle: " + meta)
    }
    try {
      val point = new ValuePoint
      point.t = NUMBER_FORMAT.parse(meta.getValue("t").trim).longValue
      point.v = NUMBER_FORMAT.parse(meta.getValue("v").trim).floatValue
      points += point;
    } catch {case ex: ParseException => ex.printStackTrace}
  }
    
  @throws(classOf[SAXException])
  def start_indicator(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_indicator: " + meta.getValue("class"))
    }
    indicatorDescriptor = new IndicatorDescriptor
    indicatorDescriptor.active = (meta.getValue("active").trim).toBoolean
    indicatorDescriptor.serviceClassName = meta.getValue("class")
    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      meta.getValue("nunits").trim.toInt)
    indicatorDescriptor.freq = freq
        
    factors = new ArrayBuffer
  }
    
  @throws(classOf[SAXException])
  def end_indicator() {
    if (DEBUG) {
      System.err.println("end_indicator()")
    }
    indicatorDescriptor.factors = factors.toArray
    contents.addDescriptor(indicatorDescriptor)
  }
    
  @throws(classOf[SAXException])
  def start_chart(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_chart: " + meta)
    }
    handledChartClassName = meta.getValue("class")
    points = new ArrayBuffer[ValuePoint]
  }
    
  @throws(classOf[SAXException])
  def end_chart() {
    if (DEBUG) {
      System.err.println("end_chart()")
    }
        
    val handledChart =
      try {
        Class.forName(handledChartClassName).newInstance.asInstanceOf[HandledChart]
      } catch {case ex: Exception => ex.printStackTrace; null}
    
    if (handledChart !=null) {
      handledChartMapPoints.put(handledChart, points)
    }
  }
    
  @throws(classOf[SAXException])
  def handle_opt(meta: Attributes) {
    if (DEBUG) {
      System.err.println("handle_opt: " + meta.getValue("value"))
    }
        
    val nameStr     = meta.getValue("name")
    val valueStr    = meta.getValue("value")
    val stepStr     = meta.getValue("step")
    val maxValueStr = meta.getValue("maxvalue")
    val minValueStr = meta.getValue("minvalue")
        
    try {
      val value    = NUMBER_FORMAT.parse(valueStr.trim)
      val step     = if (stepStr     == null) null else NUMBER_FORMAT.parse(stepStr.trim)
      val maxValue = if (maxValueStr == null) null else NUMBER_FORMAT.parse(maxValueStr.trim)
      val minValue = if (minValueStr == null) null else NUMBER_FORMAT.parse(minValueStr.trim)
            
      val factor = new DefaultFactor(nameStr, value, step, minValue, maxValue)
      factors += factor
    } catch {case ex: ParseException => ex.printStackTrace}
  }
    
  @throws(classOf[SAXException])
  def start_indicators(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_indicators: " + meta)
    }
  }
    
  @throws(classOf[SAXException])
  def end_indicators() {
    if (DEBUG) {
      System.err.println("end_indicators()")
    }
        
  }

  @throws(classOf[SAXException])
  def start_drawings(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_drawings: " + meta)
    }
  }
    
  @throws(classOf[SAXException])
  def end_drawings() {
    if (DEBUG)
      System.err.println("end_drawings()")
  }
    
  @throws(classOf[SAXException])
  def start_sec(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_sofic: " + meta.getValue("unisymbol"))
    }
    val uniSymbol = meta.getValue("unisymbol")
    contents = new AnalysisContents(uniSymbol)
  }
    
  @throws(classOf[SAXException])
  def end_sec()  {
    if (DEBUG) {
      System.err.println("end_sofic()")
    }
  }
    
  @throws(classOf[SAXException])
  def start_source(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_source: " + meta)
    }
        
    val dataContract = new QuoteContract
        
    dataContract.active = meta.getValue("active").trim.toBoolean
    dataContract.serviceClassName = meta.getValue("class")
        
    dataContract.symbol = meta.getValue("symbol")
    dataContract.secType = Sec.Type.withName(meta.getValue("sectype"))
    dataContract.exchange = meta.getValue("exchange")
    dataContract.primaryExchange = meta.getValue("primaryexchange")
    dataContract.currency = meta.getValue("currency")

    dataContract.dateFormatPattern = meta.getValue("dateformat")
        
    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      meta.getValue("nunits").trim.toInt
    )
    dataContract.freq = freq
        
    dataContract.refreshable = meta.getValue("refreshable").trim.toBoolean
    dataContract.refreshInterval = meta.getValue("refreshinterval").trim.toInt
        
    val sdf = new SimpleDateFormat("yyyy-MM-dd")
        
    try {
      calendar.setTime(sdf.parse(meta.getValue("begdate").trim))
      dataContract.beginDate = calendar.getTime
            
      calendar.setTime(sdf.parse(meta.getValue("enddate").trim))
      dataContract.endDate = calendar.getTime
    } catch {case ex: ParseException => ex.printStackTrace}
        
    dataContract.urlString = meta.getValue("url")
        
    contents.addDescriptor(dataContract)
  }
    
  @throws(classOf[SAXException])
  def start_sources(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_sources: " + meta)
    }
  }
    
  @throws(classOf[SAXException])
  def end_sources() {
    if (DEBUG) {
      System.err.println("end_sources()")
    }
        
  }
    
  @throws(classOf[SAXException])
  def start_layer(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_layer: " + meta)
    }
    drawingDescriptor = new DrawingDescriptor
    drawingDescriptor.serviceClassName = meta.getValue("name")
    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      Integer.parseInt(meta.getValue("nunits").trim))
    drawingDescriptor.freq = freq
        
    handledChartMapPoints = new HashMap
  }
    
  @throws(classOf[SAXException])
  def end_layer()  {
    if (DEBUG) {
      System.err.println("end_layer()")
    }
    drawingDescriptor.setHandledChartMapPoints(handledChartMapPoints)
    contents.addDescriptor(drawingDescriptor)
  }
    
  def getContents: AnalysisContents = {
    contents;
  }
        
}