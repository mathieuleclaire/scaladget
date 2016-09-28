package fr.iscpif.demo

import org.scalajs.dom.Element

/*
 * Copyright (C) 19/08/16 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import fr.iscpif.scaladget.api.BootstrapTags.ModalDialog
import fr.iscpif.scaladget.stylesheet.{all => sheet}
import fr.iscpif.scaladget.api.{BootstrapTags => bs}
import scalatags.JsDom.all._
import sheet._

object ModalDialogDemo extends Demo {

  val sc = sourcecode.Text {

    import org.scalajs.dom
    import scalatags.JsDom.tags

    // Create the Modal dialog
    lazy val modalDialog: ModalDialog = bs.ModalDialog()

    // Append header, body, footer elements
    modalDialog header bs.ModalDialog.headerDialogShell(div("Header"))
    modalDialog body bs.ModalDialog.bodyDialogShell(div("My body !"))
    modalDialog footer bs.ModalDialog.footerDialogShell(
      bs.buttonGroup()(
        tags.button(btn_info, "OK"),
        tags.button(btn_info, "Cancel")
      )
    )

    // Append the modal dialog to the DOM
    val modal = modalDialog.dialog
    dom.document.body.appendChild(modal)

    // Build the button trigger (to be also appended to the DOM)
    tags.span(
      modalDialog.triggerButton("Modal !", btn_primary),
      modalDialog.trigger(tags.span(glyph_settings +++ sheet.paddingLeft(5) +++ pointer))
    ).render
  }


  val elementDemo = new ElementDemo {
    def title: String = "Modal"

    def code: String = sc.source

    def element: Element = sc.value
  }

}