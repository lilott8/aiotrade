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
package org.aiotrade.lib.view.securities

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Comparator
import java.util.Locale
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.RowSorter
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.securities.Sec
import org.aiotrade.lib.securities.Ticker
import org.aiotrade.lib.securities.TickerEvent
import scala.collection.mutable.HashMap
import scala.swing.Reactor

/**
 *
 * @author  Caoyuan Deng
 */
object RealTimeWatchListPanel {
  val orig_pgup = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP.toChar)   // Fn + UP   in mac
  val orig_pgdn = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN.toChar) // Fn + DOWN in mac
  val meta_pgup = KeyStroke.getKeyStroke(KeyEvent.VK_UP.toChar,   InputEvent.META_MASK)
  val meta_pgdn = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN.toChar, InputEvent.META_MASK)
}

import RealTimeWatchListPanel._
class RealTimeWatchListPanel extends JPanel with Reactor {

  private val SYMBOL     = "Symbol"
  private val TIME       = "Time"
  private val LAST_PRICE = "Last"
  private val DAY_VOLUME = "Volume"
  private val PREV_CLOSE = "Prev. cls"
  private val DAY_CHANGE = "Change"
  private val PERCENT    = "Percent"
  private val DAY_HIGH   = "High"
  private val DAY_LOW    = "Low"
  private val DAY_OPEN   = "Open"
  private val colNameToCol = Map[String, Int](
    SYMBOL     -> 0,
    TIME       -> 1,
    LAST_PRICE -> 2,
    DAY_VOLUME -> 3,
    PREV_CLOSE -> 4,
    DAY_CHANGE -> 5,
    PERCENT    -> 6,
    DAY_HIGH   -> 7,
    DAY_LOW    -> 8,
    DAY_OPEN   -> 9
  )

  private val colNames = {
    val names = new Array[String](colNameToCol.size)
    for ((name, col) <- colNameToCol) {
      names(col) = name
    }
    
    names
  }
  
  private val symbolToInWatching = new HashMap[String, Boolean]
  private val symbolToPrevTicker = new HashMap[String, Ticker]
  private val symbolToColColors  = new HashMap[String, HashMap[String, Color]]

  val table = new JTable
  private val tableModel: WatchListTableModel = new WatchListTableModel(colNames , 0)
  private val df = new SimpleDateFormat("hh:mm", Locale.US)
  private val cal = Calendar.getInstance
  private val bgColorSelected = new Color(56, 86, 111)//new Color(24, 24, 24) //new Color(169, 178, 202)

  initTable

  val scrollPane = new JScrollPane
  scrollPane.setViewportView(table)
  scrollPane.setBackground(LookFeel().backgroundColor)
  scrollPane.setFocusable(true)

  scrollPane.registerKeyboardAction(scrollPane.getActionMap.get("scrollUp"),   "pageup",   meta_pgup, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  scrollPane.registerKeyboardAction(scrollPane.getActionMap.get("scrollDown"), "pagedown", meta_pgdn, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)

  //scrollPane.getVerticalScrollBar.setUI(new BasicScrollBarUI)

  setLayout(new BorderLayout)
  add(BorderLayout.CENTER, scrollPane)

  reactions += {
    case TickerEvent(sec: Sec, ticker: Ticker) =>
      /*
       * To avoid:
       java.lang.NullPointerException
       at javax.swing.DefaultRowSorter.convertRowIndexToModel(DefaultRowSorter.java:501)
       at javax.swing.JTable.convertRowIndexToModel(JTable.java:2620)
       at javax.swing.JTable.getValueAt(JTable.java:2695)
       at javax.swing.JTable.prepareRenderer(JTable.java:5712)
       at javax.swing.plaf.basic.BasicTableUI.paintCell(BasicTableUI.java:2072)
       * We should call addRow, removeRow, setValueAt etc in EventDispatchThread
       */
      SwingUtilities.invokeLater(new Runnable {
          def run {
            updateByTicker(sec.uniSymbol, ticker)
          }
        })
  }

  /** forward focus to scrollPane, so it can response UP/DOWN key event etc */
  override def requestFocusInWindow: Boolean = {
    scrollPane.requestFocusInWindow
  }

  private def initTable {
    table.setFont(LookFeel().defaultFont)
    table.setModel(
      new DefaultTableModel(
        Array[Array[Object]](),
        Array[Object]()
      )
    )
    table.setModel(tableModel)
    table.setDefaultRenderer(classOf[Object], new TrendSensitiveCellRenderer)
    table.setBackground(LookFeel().backgroundColor)
    table.setGridColor(LookFeel().backgroundColor)
    table.setFillsViewportHeight(true)
    val header = table.getTableHeader
    header.setForeground(Color.WHITE)
    header.setBackground(LookFeel().backgroundColor)
    header.setReorderingAllowed(true)
    header.getDefaultRenderer.asInstanceOf[DefaultTableCellRenderer].setHorizontalAlignment(SwingConstants.CENTER)

    // --- sorter
    table.setAutoCreateRowSorter(false)
    val sorter = new TableRowSorter(tableModel)
    val comparator = new Comparator[Object] {
      def compare(o1: Object, o2: Object): Int = {
        (o1, o2) match {
          case (s1: String, s2: String) =>
            val idx1 = s1.indexOf('%')
            val s11 = if (idx1 > 0) s1.substring(0, idx1) else s1
            val idx2 = s2.indexOf('%')
            val s12 = if (idx2 > 0) s2.substring(0, idx2) else s2
            try {
              val d1 = s11.toDouble
              val d2 = s12.toDouble
              if (d1 > d2) 1 else if (d1 < d2) -1 else 0
            } catch {case _ => s1 compareTo s2}
          case _ => 0
        }
      }
    }
    for (col <- 1 until tableModel.getColumnCount) {
      sorter.setComparator(col, comparator)
    }
    // default sort order and precedence
    val sortKeys = new java.util.ArrayList[RowSorter.SortKey]
    sortKeys.add(new RowSorter.SortKey(colOfName(PERCENT), javax.swing.SortOrder.DESCENDING))
    sorter.setSortKeys(sortKeys)
    // @Note sorter.setSortsOnUpdates(true) almost work except that the cells behind sort key
    // of selected row doesn't refresh, TableRowSorter.sort manually

    table.setRowSorter(sorter)
  }

  class WatchListTableModel(columnNames: Array[String], rowCount: Int) extends DefaultTableModel(columnNames.asInstanceOf[Array[Object]], rowCount) {

    private val types = Array(
      classOf[String], classOf[String], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object]
    )
    private val canEdit = Array(
      false, false, false, false, false, false, false, false, false, false
    )

    override def getColumnClass(columnIndex: Int): Class[_] = {
      types(columnIndex)
    }

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = {
      canEdit(columnIndex)
    }
  }

  private def updateByTicker(symbol: String, ticker: Ticker) {
    val prevTicker = symbolToPrevTicker.get(symbol) getOrElse {
      val x = new Ticker
      symbolToPrevTicker(symbol) = x
      x
    }
    
    val inWatching = symbolToInWatching.get(symbol) getOrElse {false}
    if (!inWatching) {
      return
    }

    /**
     * @Note
     * Should set columeColors[] before addRow() or setValue() of table to
     * make the color effects take place at once.
     */
    var updated = false
    symbolToColColors.get(symbol) match {
      case Some(colNameToColor) =>
        if (ticker.isDayVolumeGrown(prevTicker)) {
          setColColorsByTicker(colNameToColor, ticker, prevTicker, inWatching)

          val rowData = composeRowData(symbol, ticker)
          val row = rowOfSymbol(symbol)
          for (i <- 0 until rowData.length) {
            table.setValueAt(rowData(i), row, i)
          }
          updated = true
        }
      case None =>
        val colNameToColor = new HashMap[String, Color]
        for (name <- colNameToCol.keysIterator) {
          colNameToColor(name) = LookFeel().nameColor
        }
        symbolToColColors(symbol) = colNameToColor

        setColColorsByTicker(colNameToColor, ticker, null, inWatching)

        val rowData = composeRowData(symbol, ticker)
        tableModel.addRow(rowData)
        updated = true
    }

    prevTicker.copyFrom(ticker)
    
    if (updated) {
      table.getRowSorter.asInstanceOf[TableRowSorter[_]].sort // force to re-sort all rows
    }
  }

  private val SWITCH_COLOR_A = LookFeel().nameColor
  private val SWITCH_COLOR_B = new Color(128, 192, 192) //Color.CYAN;

  private def setColColorsByTicker(colNameToColor: HashMap[String, Color], ticker: Ticker, prevTicker: Ticker, inWatching: Boolean) {
    val fgColor = if (inWatching) LookFeel().nameColor else Color.GRAY.brighter

    for (name <- colNameToColor.keysIterator) {
      colNameToColor(name) = fgColor
    }

    val neutralColor  = LookFeel().getNeutralBgColor
    val positiveColor = LookFeel().getPositiveBgColor
    val negativeColor = LookFeel().getNegativeBgColor

    if (inWatching) {
      /** color of volume should be recorded for switching between two colors */
      colNameToColor(DAY_VOLUME) = fgColor
    }

    if (ticker != null) {
      if (ticker.dayChange > 0) {
        colNameToColor(DAY_CHANGE) = positiveColor
        colNameToColor(PERCENT)    = positiveColor
      } else if (ticker.dayChange < 0) {
        colNameToColor(DAY_CHANGE) = negativeColor
        colNameToColor(PERCENT)    = negativeColor
      } else {
        colNameToColor(DAY_CHANGE) = neutralColor
        colNameToColor(PERCENT)    = neutralColor
      }

      def setColorByPrevClose(value: Float, columnName: String) {
        if (value > ticker.prevClose) {
          colNameToColor(columnName) = positiveColor
        } else if (value < ticker.prevClose) {
          colNameToColor(columnName) = negativeColor
        } else {
          colNameToColor(columnName) = neutralColor
        }
      }

      setColorByPrevClose(ticker.dayOpen,   DAY_OPEN)
      setColorByPrevClose(ticker.dayHigh,   DAY_HIGH)
      setColorByPrevClose(ticker.dayLow,    DAY_LOW)
      setColorByPrevClose(ticker.lastPrice, LAST_PRICE)

      if (prevTicker != null) {
        if (ticker.isDayVolumeChanged(prevTicker)) {
          /** lastPrice's color */
          /* ticker.compareLastCloseTo(prevTicker) match {
           case 1 =>
           symbolToColColor += (LAST_PRICE -> positiveColor)
           case 0 =>
           symbolToColColor += (LAST_PRICE -> neutralColor)
           case -1 =>
           symbolToColColor += (LAST_PRICE -> negativeColor)
           case _ =>
           } */

          /** volumes color switchs between two colors if ticker renewed */
          if (colNameToColor(DAY_VOLUME) == SWITCH_COLOR_A) {
            colNameToColor(DAY_VOLUME) = SWITCH_COLOR_B
          } else {
            colNameToColor(DAY_VOLUME) = SWITCH_COLOR_A
          }
        }
      }
    }

  }


  private def composeRowData(symbol: String, ticker: Ticker): Array[Object] = {
    val rowData = new Array[Object](table.getColumnCount)

    cal.setTimeInMillis(ticker.time)
    val lastTradeTime = cal.getTime

    rowData(colOfName(SYMBOL)) = symbol
    rowData(colOfName(TIME)) = df format lastTradeTime
    rowData(colOfName(LAST_PRICE)) = "%5.2f"   format ticker.lastPrice
    rowData(colOfName(DAY_VOLUME)) = "%5.2f"   format ticker.dayVolume
    rowData(colOfName(PREV_CLOSE)) = "%5.2f"   format ticker.prevClose
    rowData(colOfName(DAY_CHANGE)) = "%5.2f"   format ticker.dayChange
    rowData(colOfName(PERCENT))    = "%3.2f%%" format ticker.changeInPercent
    rowData(colOfName(DAY_HIGH))   = "%5.2f"   format ticker.dayHigh
    rowData(colOfName(DAY_LOW))    = "%5.2f"   format ticker.dayLow
    rowData(colOfName(DAY_OPEN))   = "%5.2f"   format ticker.dayOpen

    rowData
  }

  def watch(sec: Sec) {
    listenTo(sec)

    val symbol = sec.uniSymbol
    symbolToInWatching(symbol) = true
    for (lastTicker <- symbolToPrevTicker.get(symbol);
         colNameToColors <- symbolToColColors.get(symbol)
    ) {
      setColColorsByTicker(colNameToColors, lastTicker, null, true)
      repaint()
    }
  }

  def unWatch(sec: Sec) {
    deafTo(sec)

    val symbol = sec.uniSymbol
    symbolToInWatching(symbol) = false
    for (lastTicker <- symbolToPrevTicker.get(symbol);
         colNameToColors <- symbolToColColors.get(symbol)
    ) {
      setColColorsByTicker(colNameToColors, lastTicker, null, false)
      repaint()
    }
  }

  def clearAllWatch {
    symbolToInWatching.clear
    symbolToPrevTicker.clear
    symbolToColColors.clear
  }

  def colOfName(colName: String): Int = {
    colNameToCol(colName)
  }

  def rowOfSymbol(symbol: String): Int = {
    val colOfSymbol = colOfName(SYMBOL)
    var row = 0
    val nRows = table.getRowCount
    while (row < nRows) {
      if (table.getValueAt(row, colOfSymbol) == symbol) {
        return row
      }
      row += 1
    }

    -1
  }

  def symbolAtRow(row: Int): String = {
    if (row >= 0 && row < table.getRowCount) {
      table.getValueAt(row, colOfName(SYMBOL)).toString
    } else null
  }

  class TrendSensitiveCellRenderer extends JLabel with TableCellRenderer {

    setOpaque(true)

    def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                      hasFocus: Boolean, row: Int, col: Int): Component = {

      /**
       * @Note
       * Here we should use table.getColumeName(column) which is
       * not the same as tableModel.getColumeName(column).
       * Especially: after you draged and moved the table colume, the
       * column index of table will change, but the column index
       * of tableModel will remain the same.
       */
      val symbol = symbolAtRow(row)
      val colNameToColor = symbolToColColors(symbol)

      val colName = table.getColumnName(col)
      if (isSelected) {
        setBackground(bgColorSelected)
      } else {
        setBackground(LookFeel().backgroundColor)
      }

      setForeground(colNameToColor(colName))
      
      setText(null)

      if (value != null) {
        colName match {
          case SYMBOL =>
            setHorizontalAlignment(SwingConstants.LEADING)
          case TIME =>
            setHorizontalAlignment(SwingConstants.CENTER)
          case _ =>
            setHorizontalAlignment(SwingConstants.TRAILING)
        }

        setText(value.toString)
      }

      this
    }
  }

}
