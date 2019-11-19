package scaladget.bootstrapnative

import org.scalajs.dom.raw._
import scalatags.JsDom.{TypedTag, tags}
import scalatags.JsDom.all._
import scaladget.tools.{ModifierSeq, emptyMod}
import scaladget.tools.Utils._
import scaladget.tools.JsRxTags._
import bsn._
import rx._
import scaladget.bootstrapnative.Table.BSTableStyle

object Table {

  case class BSTableStyle(tableStyle: TableStyle = emptyMod,
                          headerStyle: ModifierSeq = emptyMod,
                          selectionColor: String = "#e1e1e1")

  case class Header(values: Seq[String])

  case class Row(values: Seq[TypedTag[HTMLElement]], rowStyle: ModifierSeq = emptyMod)

  sealed trait Cell {
    def value: TypedTag[HTMLElement]

    def cellIndex: Int
  }

  case class VarCell(value: TypedTag[HTMLElement], cellIndex: Int) extends Cell

  case class FixedCell(value: TypedTag[HTMLElement], cellIndex: Int) extends Cell

  def collectVar(cells: Seq[Cell]) = cells.collect { case v: VarCell => v }

  def reactiveRow(cells: Seq[Cell], rowStyle: ModifierSeq = emptyMod) =
    ReactiveRow(uuID.short("rr"), cells, rowStyle)

  case class ReactiveRow(uuid: ID, cells: Seq[Cell] = Seq(), rowStyle: ModifierSeq = emptyMod) {

    lazy val tr = tags.tr(id := uuid)(cells.map { c =>
      tags.td(c.value)
    }
      //      ,
      //      backgroundColor := Rx {
      //        if (Some(row) == selected()) tableStyle.selectionColor else ""
      //      }
    )(rowStyle)
      //    (onclick := { () =>
      //      table.selected() = Some(this)
      //    }
      //    )
      .render


    def varCells = (uuid, collectVar(cells))
  }

  def subID(id: ID) = s"${id}sub"

  type RowType = (String, Int) => TypedTag[HTMLElement]

  case class SubRow(element: Rx[TypedTag[HTMLElement]], trigger: Rx[Boolean] = Rx(false)) {
    val expander = div(Rx {
      trigger.expand(element())
    }
    )
  }

  implicit def rowToReactiveRow(r: Row): ReactiveRow = reactiveRow(r.values.zipWithIndex.map { v => VarCell(v._1, v._2) }, r.rowStyle)

}

import Table._

case class Table(reactiveRows: Rx.Dynamic[Seq[ReactiveRow]],
                 subRow: Option[ID => Table.SubRow] = None,
                 headers: Option[Table.Header] = None,
                 bsTableStyle: BSTableStyle = BSTableStyle(default_table, emptyMod)) {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val selected: Var[Option[ReactiveRow]] = Var(None)
  var previousState: Seq[(ID, Seq[VarCell])] = Seq()
  val inDOM: Var[Seq[ID]] = Var(Seq())

  def addHeaders(hs: String*) = copy(headers = Some(Header(hs)))

  def style(tableStyle: TableStyle = default_table, headerStyle: ModifierSeq = emptyMod) = {
    copy(bsTableStyle = BSTableStyle(tableStyle, headerStyle))
  }

  private def buildSubRow(rr: ReactiveRow, sr: ID => SubRow) = {
    val sub = sr(rr.uuid)
    tags.tr(id := subID(rr.uuid) /*, rowStyle*/)(
      tags.td(colspan := 999, padding := 0, borderTop := "0px solid black")(
        sub.expander

      )
    ).render
  }

  private def addRowInDom(r: ReactiveRow) = {
    tableBody.appendChild(r.tr)
    inDOM() = inDOM.now :+ r.uuid

    subRow.foreach { sr =>
      inDOM() = inDOM.now :+ subID(r.uuid)
      tableBody.appendChild(buildSubRow(r, sr).render)
    }
  }

  private def updateValues(element: HTMLTableRowElement, values: Seq[(TypedTag[HTMLElement], Int)]) = {
    for (
      (el, ind) <- values
    ) yield {
      val old = element.childNodes(ind)
      element.replaceChild(td(el), old)
    }
  }


  reactiveRows.trigger {
    val inBody = inDOM.now
    val rowsAndSubs = reactiveRows.now.map {
      r =>
        Seq(r.uuid, subID(r.uuid))
    }.flatten
    val varCells = reactiveRows.now.map {
      _.varCells
    }

    (rowsAndSubs.length - inBody.length) match {
      case x if x > 0 =>
        // CASE ADD
        reactiveRows.now.foreach {
          rr =>
            if (!inDOM.now.contains(rr.uuid)) {
              addRowInDom(rr)
            }
        }
      case x if x < 0 =>
        // CASE DELETE
        inBody.foreach {
          id =>
            if (!rowsAndSubs.contains(id)) {
              findIndex(id).foreach {
                inDOM() = inDOM.now.filterNot(_ == id)

                tableBody.deleteRow
              }
            }
        }
      case _ =>
        // CASE UPDATE
        val di = varCells diff previousState
        di.foreach {
          m =>
            findIndex(m._1).map {
              i =>
                updateValues(tableBody.rows(i).asInstanceOf[HTMLTableRowElement], m._2.map {
                  v => (v.value, v.cellIndex)
                })
            }
        }
    }
    previousState = varCells
  }

  def findIndex(reactiveRow: ReactiveRow): Option[Int] = findIndex(reactiveRow.uuid)

  def findIndex(id: ID): Option[Int] = {
    val lenght = tableBody.rows.length

    def findIndex0(currentIndex: Int, found: Boolean): Option[Int] = {
      if (found) Some(currentIndex - 1)
      else if (currentIndex == lenght) None
      else {
        findIndex0(currentIndex + 1, tableBody.rows(currentIndex).id == id)
      }
    }

    if (lenght == 0) None
    else findIndex0(0, false)
  }

  lazy val tableBody = tags.tbody.render

  lazy val render = {

    tags.table(bsTableStyle.tableStyle)(
      tags.thead(bsTableStyle.headerStyle)(
        tags.tr(
          headers.map {
            h =>
              h.values.map {
                th(_)
              }
          })),
      tableBody
    )
  }

}

  trait EditableCell {
    val editMode: Var[Boolean] = Var(false)

    def build: TypedTag[HTMLElement]
  }

  case class TextCell(value: String) extends EditableCell {
    def build = {
      div(
        Rx {
          if (editMode()) inputTag(value)
          else div(value)
        }
      )
    }
  }


  case class PasswordCell(value: String) extends EditableCell {
    def build = {
      div(
        Rx {
          if (editMode()) inputTag(value)(`type` := "password")
          else div(value.map{c=> raw("&#9679")})
        }
      )
    }
  }
  //  case class LabelCell(options: Seq[String]) extends EditableCell
  case class TriggerCell(trigger: TypedTag[HTMLElement]) extends EditableCell {
    def build = trigger
  }

case class EditableRow(cells: Seq[EditableCell]) {
  def switchEdit = {
    cells.foreach {c=>
      c.editMode() = !c.editMode.now
    }
  }
}

object EdiTable {
  implicit def seqCellsToRow(s: Seq[EditableCell]): EditableRow = EditableRow(s)
}

case class EdiTable(headers: Seq[String],
                      cells: Seq[EditableRow],
                      bsTableStyle: BSTableStyle = BSTableStyle(bordered_table, emptyMod)
                     ) {

    lazy val tableBody = tags.tbody.render

    cells.foreach { row =>
      tableBody.appendChild(
        tags.tr(row.cells.map { c =>
          tags.td(c.build)
        }))
    }

    lazy val render = {
      tags.table(bsTableStyle.tableStyle)(
        tags.thead(bsTableStyle.headerStyle)(
          tags.tr(headers.map {
            th(_)
          })),
        tableBody
      )
    }
  }
