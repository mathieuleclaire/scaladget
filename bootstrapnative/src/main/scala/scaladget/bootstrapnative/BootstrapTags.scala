package scaladget.bootstrapnative

/*
 * Copyright (C) 27/05/15 // mathieu.leclaire@openmole.org
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

//import net.scalapro.sortable.{EventS, Sortable, SortableOptions}

import com.raquo.domtypes.generic.Modifier
import scaladget.bootstrapnative.Popup.{Bottom, ClickPopup, HoverPopup, Manual, PopupPosition, PopupType}
import scaladget.tools.Utils._
import com.raquo.laminar.api.L._
import bsn.spacing._
import scaladget.bootstrapnative.Table.Row
import scalajs.js.|

object BootstrapTags extends BootstrapTags

trait BootstrapTags {
  bstags =>

  type HESetter = Setter[HtmlElement]

  type HESetters = Seq[HESetter]

  implicit def HESetterToHeSetters(setter: HESetter): HESetters = Seq(setter)

  val emptySetters: HESetters = Seq[HESetter]()

  def inputTag(content: String = "") = input(bsn.formControl, value := content)

  //  implicit def inputGroupToDiv(inputGroup: InputGroup): HtmlElement =
  //    inputGroupBS.amend(
  //      inputGroupAppend(inputGroup.app)
  //        //.amend(inputGroupPrepend(inputGroup.pre))
  //        .amend(inputGroup.el)
  //
  //    )
  //
  //  case class InputGroup(pre: HtmlElement = div(), el: HtmlElement = div(), app: HtmlElement = div()) {
  //    def prepend(preElement: HtmlElement)= copy(pre = preElement)
  //
  //    def element(el: HtmlElement) = copy(el = el)
  //
  //    def append(appElement: HtmlElement) = copy(appElement)
  //  }
  //
  //  val inputGroup = InputGroup()

  def stickBefore(element: HtmlElement, withSticker: HtmlElement) =
    div(
      bsnsheet.inputGroupClass,
      inputGroupPrepend(withSticker),
      element
    )

  def stickAfter(element: HtmlElement, withSticker: HtmlElement) =
    div(
      bsnsheet.inputGroupClass,
      element,
      inputGroupAppend(withSticker),
    )

  def inputGroupPrepend(element: HtmlElement) = span(cls("input-group-prepend")).amend(element)

  def inputGroupAppend(element: HtmlElement) = span(cls("input-group-append")).amend(element)

  def inputGroupText(text: String) = span(cls("input-group-text"), text)

  //val input_group_lg = "input-group-lg"

  // for multiple files, add multiple := true attr
  def fileInput(todo: Input => Unit) =
    input(
      idAttr := "fileinput",
      `type` := "file",
      inContext(thisNode => onChange --> { _ => todo(thisNode) })
    )

  // CHECKBOX
  def checkbox(isChecked: Boolean) = input(`type` := "checkbox", checked := isChecked)

  trait Displayable {
    def name: String
  }

  // BUTTONS

  def linkButton(content: String, link: String, buttonStyle: HESetters = bsn.btn_secondary, openInOtherTab: Boolean = true) =
    a(buttonStyle, href := link, role := "button", target := {
      if (openInOtherTab) "_blank" else ""
    }, content)

  // Clickable span containing a glyphicon and a text
  def glyphSpan(glyphicon: HESetters, modifier: Modifier[HtmlElement] = emptyMod, text: String = "") =
    span(text, glyphicon, cursor.pointer, aria.hidden := true, modifier)


  // Close buttons
  def closeButton(dataDismiss: String, todo: () => Unit = () => {}) =
    button(
      onClick --> { _ => todo() }, cls("close"), aria.label := "Close", dataAttr("dismiss") := dataDismiss,
      span(aria.hidden := true, "&#215")
    )


  //TOGGLE BUTTON
  case class ToggleState(text: String, cls: String, todo: () => Unit)

  case class ToggleButtonState(state: ToggleState, activeState: Boolean, unactiveState: ToggleState, onToggled: () => Unit, modifiers: HESetters) {

    val toggled = Var(activeState)

    lazy val element = button(`type` := "button",
      cls <-- toggled.signal.map(t =>
        if (t) state.cls
        else unactiveState.cls
      ),
      child.text <-- toggled.signal.map { t => if (t) state.text else unactiveState.text },
      onClick --> { _ =>
        toggled.update(!_)
        onToggled()
      },
    ).amend(modifiers: _*)

  }

  def toggle(activeState: ToggleState, default: Boolean, unactiveState: ToggleState, onToggled: () => Unit, modifiers: HESetters = emptySetters) =
    ToggleButtonState(activeState, default, unactiveState, onToggled, modifiers)


  case class RadioButtons(states: Seq[ToggleState], activeStates: Seq[ToggleState], unactiveStateClass: String, modifiers: HESetters = emptySetters) {

    lazy val active = Var(activeStates)

    lazy val element = div(bsnsheet.btnGroup,
      for (rb <- states) yield {
        val rbStateCls = active.signal.map(_.filter(_ == rb).headOption.map(_.cls).getOrElse(unactiveStateClass))

        button(
          rb.text,
          cls <-- rbStateCls,
          onClick --> { _ =>
            active.update { ac =>
              val id = ac.indexOf(rb)
              if (id == -1) ac.appended(rb)
              else ac.patch(id, Nil, 1)
            }
            rb.todo()
          }
        )
      }
    )
  }


  def radio(buttons: Seq[ToggleState], activeStates: Seq[ToggleState], unactiveStateClass: String, radioButtonsModifiers: HESetters = emptySetters) =
    RadioButtons(buttons, activeStates, unactiveStateClass, radioButtonsModifiers).element


  // RADIO
  case class ExclusiveRadioButtons(buttons: Seq[ToggleState], unactiveStateClass: String, defaultToggle: ToggleState, radioButtonsModifiers: HESetters) {

    val active = Var(defaultToggle)

    lazy val element = div(bsnsheet.btnGroup, bsnsheet.btnGroupToggle, dataAttr("toggle") := "buttons",
      for ((rb, index) <- buttons.zipWithIndex) yield {
        val isActive = active.signal.map(_ == rb)
        label(
          cls <-- isActive.map { a => if (a) rb.cls else unactiveStateClass },
          cls.toggle("focus active") <-- isActive,
          input(`type` := "radio", name := "options", idAttr := s"option${index + 1}", checked <-- isActive),
          rb.text,
          onClick --> { _ =>
            active.set(rb)
            rb.todo()
          }
        )
      }
    )
  }

  def exclusiveRadio(buttons: Seq[ToggleState], unactiveStateClass: String, defaultToggle: ToggleState, radioButtonsModifiers: HESetters = emptySetters) =
    ExclusiveRadioButtons(buttons, unactiveStateClass, defaultToggle, radioButtonsModifiers).element

  //Label decorators to set the label size
  implicit class TypedTagLabel(badge: Span) {
    def size1(modifierSeq: HESetters = emptySetters) = h1(modifierSeq, badge)

    def size2(modifierSeq: HESetters = emptySetters) = h2(modifierSeq, badge)

    def size3(modifierSeq: HESetters = emptySetters) = h3(modifierSeq, badge)

    def size4(modifierSeq: HESetters = emptySetters) = h4(modifierSeq, badge)

    def size5(modifierSeq: HESetters = emptySetters) = h5(modifierSeq, badge)

    def size6(modifierSeq: HESetters = emptySetters) = h6(modifierSeq, badge)
  }


  // PROGRESS BAR
  def progressBar(barMessage: String, ratio: Int) =
    div(bsnsheet.progress,
      div(bsnsheet.progressBar, width := ratio.toString() + "%", barMessage)
    )


  // BADGE
  def badge(badgeValue: String, badgeStyle: HESetters) = span(cls("badge"), badgeStyle, marginLeft := "4", badgeValue)

  //BUTTON GROUP
  def buttonGroup = div(bsn.btnGroup)

  def buttonToolBar = div(bsn.btnToolbar, role := "toolbar")

  //MODAL
  case class ModalDialog(modalHeader: HtmlElement,
                         modalBody: HtmlElement,
                         modalFooter: HtmlElement,
                         modifiers: HESetters,
                         onopen: () => Unit,
                         onclose: () => Unit) {

    lazy val render = {
      val d = div(
        bsnsheet.modal, bsn.fade, role := "dialog", aria.hidden := true, aria.labelledBy := uuID.short("m"),
        div(bsnsheet.modalDialog, modifiers,
          div(bsn.modalContent,
            div(bsnsheet.modalHeader, modalHeader),
            div(bsnsheet.modalBody, modalBody),
            div(bsnsheet.modalFooter, modalFooter)
          )
        )
      )

      org.scalajs.dom.document.body.appendChild(d.ref)
      d
    }


    lazy val modal = new BSN.Modal(render.ref)

    def show = {
      modal.show
      onopen()
    }

    def hide = {
      modal.hide
      onclose()
    }

    def toggle = modal.toggle

  }

  def modalDialog(modalHeader: HtmlElement, modalBody: HtmlElement, modalFooter: HtmlElement, onopen: () => Unit, onclose: () => Unit, modifiers: HESetters = emptySetters) =
    ModalDialog(modalHeader, modalBody, modalFooter, modifiers, onopen, onclose)


  // NAVS
  case class NavItem(contentDiv: HtmlElement,
                     val todo: () ⇒ Unit = () ⇒ {},
                     extraRenderPair: HESetters = emptySetters,
                     activeDefault: Boolean = false,
                     toRight: Boolean = false) {

    val active: Var[Boolean] = Var(activeDefault)

    val render = li(
      cls := bsn.nav_item,

      a(href := "#",
        cls := bsn.nav_link,
        child <-- active.signal.map { _ => span(cls := "sr-only", "(current)") },
        cls.toggle("active") <-- active.signal,
        lineHeight := "35px",
        onClick --> { _ =>
          todo()
        },
        contentDiv,
        cls.toggle("active") <-- active.signal,
        extraRenderPair
      )
    )

    def right = copy(toRight = true)
  }

  def navItem(content: HtmlElement,
              todo: () => Unit = () => {},
              extraRenderPair: HESetters = emptySetters,
              activeDefault: Boolean = false,
              alignRight: Boolean = false) =
    NavItem(content: HtmlElement, todo, extraRenderPair, activeDefault, alignRight)


  def stringNavItem(content: String, todo: () ⇒ Unit = () ⇒ {}, activeDefault: Boolean = false): NavItem =
    navItem(span(content), todo, activeDefault = activeDefault)

  def navBar(classPair: HESetters, contents: NavItem*) = new NavBar(classPair, None, contents)

  case class NavBarBrand(src: String, modifierSeq: HESetters, todo: () => Unit, alt: String)

  case class NavBar(classPair: HESetters, brand: Option[NavBarBrand], contents: Seq[NavItem]) {

    val navId = uuID.short("n")

    def render: HtmlElement = {

      val sortedContents = contents.partition {
        _.toRight
      }

      def buildUL(cts: Seq[NavItem], modifier: HESetters = emptySetters) =
        ul(bsn.navbar_nav, modifier,
          cts.map { c ⇒
            c.render.amend(onClick --> { _ ⇒
              contents.foreach {
                _.active.set(false)
              }
              c.active.set(true)
            })
          }
        )

      val content = div(cls := "collapse navbar-collapse", idAttr := navId,
        buildUL(sortedContents._2, bsmargin.r.auto),
        buildUL(sortedContents._1, bsmargin.l.auto)
      )

      nav(bsn.navbar, bsn.navbar_expand_lg, classPair,
        for {
          b <- brand
        } yield {
          div(
            a(bsn.navbar_brand, href := "#", padding := "0",
              img(b.modifierSeq, cursor.pointer, alt := b.alt, src := b.src, onClick --> {
                _ => b.todo()
              })
            ),
            button(cls := "navbar-toggler",
              `type` := "button",
              dataAttr("toggle") := "collapse", dataAttr("target") := s"#$navId",
              aria.controls := navId, aria.expanded := false, aria.label := "Toggle navigation",
              span(cls := "navbar-toggler-icon")
            )
          )
        }, content
      )
    }

    def withBrand(src: String, modifierSeq: HESetters = emptySetters, todo: () => Unit = () => {}, alt: String = "") = copy(brand = Some(NavBarBrand(src, modifierSeq, todo, alt)))

  }

  //POPOVER
  type TypedContent = String | HtmlElement

  case class PopoverBuilder(element: HtmlElement,
                            innerElement: HtmlElement,
                            position: PopupPosition = Bottom,
                            trigger: PopupType = HoverPopup,
                            title: Option[String] = None,
                            dismissible: Boolean = false) {
    lazy val render = element.amend(
      dataAttr("toggle") := "popover",
      dataAttr("html") := "true",
      dataAttr("content") := innerElement.ref.innerHTML,
      dataAttr("placement") := position.value,
      dataAttr("trigger") := {
        trigger match {
          case ClickPopup => "focus"
          case Manual => "manual"
          case _ => "hover"
        }
      },
      title.map(dataAttr("title") := _).getOrElse(emptyMod),
      dataAttr("dismissible") := dismissible.toString,
      onClick --> (_ => popover.show),
    )

    lazy val popover: BSN.Popover = new BSN.Popover(render.ref /*, scalajs.js.Dynamic.literal("title" -> "euinesaurtie")*/)

    def show = popover.show

    def hide = popover.hide

    def toggle = popover.toggle

  }

  //TOOLTIP
  class TooltipBuilder(element: HtmlElement,
                       text: String,
                       position: PopupPosition = Bottom,
                       condition: () => Boolean = () => true) {

    lazy val render: HtmlElement = {
      if (condition())
        element.amend(
          dataAttr("placement") := position.value,
          dataAttr("toggle") := "tooltip",
          dataAttr("original-title") := text,
          onMouseOver --> { _ => tooltip }
        )
      else element
    }

    lazy val tooltip = new BSN.Tooltip(render.ref)

    def hide = tooltip.hide
  }

  implicit class PopableTypedTag(element: HtmlElement) {

    def tooltip(text: String,
                position: PopupPosition = Bottom,
                condition: () => Boolean = () => true) = {
      new TooltipBuilder(element, text, position, condition).render
    }

    def popover(content: HtmlElement,
                position: PopupPosition = Bottom,
                trigger: PopupType = HoverPopup,
                title: Option[String] = None,
                dismissible: Boolean = false
               ) =
      PopoverBuilder(element, content, position, trigger, title, dismissible)
  }

  //  //DROPDOWN
  implicit class SelectableSeqWithStyle[T](s: Seq[T]) {
    def options(defaultIndex: Int = 0,
                key: HESetters = emptySetters,
                naming: T => String,
                onclose: () => Unit = () => {},
                onclickExtra: () ⇒ Unit = () ⇒ {},
                decorations: Map[T, HESetters] = Map(),
                fixedTitle: Option[String] = None) =
      Selector.options(s, defaultIndex, key, naming, onclose, onclickExtra, decorations, fixedTitle)

  }

  // COLLAPSERS
  implicit class TagCollapserOnClick[S <: HtmlElement](trigger: S) {
    def expandOnclick[T <: HtmlElement](inner: T) = {

      val clicked: Var[Boolean] = Var(false)
      div(
        trigger.amend(
          onClick --> { _ =>
            clicked.update(!_)
          }
        ),
        clicked.signal.expand(inner)
      )
    }
  }

  implicit class TTagCollapserWithReactive(r: Signal[Boolean]) {

    def expand(inner: HtmlElement) = {
      div(overflow.hidden,
        transition := "height 300ms",
        height <-- r.map { rr =>
          if (rr) inner.ref.style.height
          else "0px"
        },
        inner
      )
    }
  }


  // TABS
  type TabID = String

  case class Tab[T](t: T,
                    title: HtmlElement,
                    content: HtmlElement,
                    onClicked: () => Unit = () => {},
                    onAdded: ()=> Unit = ()=> {},
                    onRemoved: ()=> Unit = ()=> {},
                    tabID: TabID = uuID.short("t"),
                    refID: String = uuID.short("r"))

  object Tabs {
    def tabs[T](initialTabs: Seq[Tab[T]] = Seq(), isClosable: Boolean = false, tabStyle: HESetters = bsnsheet.navTabs) = TabHolder(initialTabs, isClosable, 0, (tab: Tab[T]) => {}, tabStyle)


    //    def defaultSortOptions: (Var[Seq[Tab]], Int => Unit) => SortableOptions = (ts: Var[Seq[Tab]], setActive: Int => Unit) =>
    //      SortableOptions.onEnd(
    //        (event: EventS) ⇒ {
    //          val oldI = event.oldIndex.asInstanceOf[Int]
    //          val newI = event.newIndex.asInstanceOf[Int]
    //          ts.update(cur => cur.updated(oldI, cur(newI)).updated(newI, cur(oldI)))
    //          // ts() = ts.now.updated(oldI, ts.now(newI)).updated(newI, ts.now(oldI))
    //          setActive(newI)
    //        }
    //      )

    case class TabHolder[T](tabs: Seq[Tab[T]], isClosable: Boolean, initIndex: Int /*, sortableOptions: Option[(Var[Seq[Tab]], Int => Unit) => SortableOptions]*/ , onActivation: Tab[T] => Unit, tabStyle: HESetters) {
      def add(tab:Tab[T]): TabHolder[T] = {
        val ts = copy(tabs = this.tabs :+ tab)
        tab.onAdded()
        ts
      }

      def closable = copy(isClosable = true)

      def initialIndex(index: Int) = copy(initIndex = index)

      //  def withSortableOptions(options: (Var[Seq[Tab]], Int => Unit) => SortableOptions) = copy(sortableOptions = Some(options))

    //  def onActivation(onActivation: Tab[T] => Unit = Tab => {}) = copy(onActivation = onActivation)

      def build = {
        Tabs(tabs, isClosable, tabStyle = tabStyle)
      }
    }

  }

  case class Tabs[T](initialTabs: Seq[Tab[T]], isClosable: Boolean, tabStyle: HESetters) {

    val tabs = Var(initialTabs)
    val activeTabID = Var(initialTabs.headOption.map(_.tabID))

    def activeTab = activeTabID.now.map(tab(_))

    def tab(tabID: TabID) = tabs.now.filter {
      _.tabID == tabID
    }.head

    def add(tab: Tab[T], activate: Boolean = true) = {
      tabs.update(t => t :+ tab)
      if(activate) activeTabID.set(Some(tab.tabID))
    }

    def isActive(id: TabID) = Some(id) == activeTabID.now

    def remove(tabID: TabID) = {
      tab(tabID).onRemoved()

      tabs.update { cur =>
        cur.filterNot {
          _.tabID == tabID
        }
      }
      if (isActive(tabID)) activeTabID.set(tabs.now.headOption.map {
        _.tabID
      })
    }

    //def onclose(f: (Tab) => Unit) = copy(onCloseExtra = f)

    lazy val tabClose: HESetters = Seq(
      position := "relative",
      fontSize := "20",
      color := "black",
      right := "-10",
      opacity := "0.3",
      width := "20"
    )

    case class TabRender(tabHeader: Li, tabContent: Div)

    def renderTab(tabID: TabID, initialTab: Tab[T], tabStream: Signal[Tab[T]]): TabRender = {

      val activeSignal = tabStream.combineWith(activeTabID.signal).map { case (t, tID) => Some(t.tabID) == tID }

      val header = li(
        // bsn.presentation_role,
        cls := bsn.nav_item,
        a(
          cls := bsn.nav_link,
          cls.toggle("active") <-- activeSignal,
          // cls.toggle("active") <-- activeSignal,
          a(idAttr := tabID,
            bsn.tab_role,
            cursor.pointer,
            dataAttr("toggle") := "tab",
            dataAttr("height") := "true",
            aria.controls <-- tabStream.map { t => t.refID },
            onClick --> { _ =>
              activeTabID.set(Some(tabID))
              initialTab.onClicked()
            },
            if (isClosable) button(cls := "close", tabClose, `type` := "button", "×",
              onClick --> { e =>
                remove(initialTab.tabID)
              }) else span(),
            child <-- tabStream.map {
              _.title
            }
          )
        )
      )

      val tabDiv =
      // div(idAttr <-- tabStream.map { t => t.refID },
        div(bsn.tab_content,
          div(bsn.tab_panel_role,
            bsn.tab_pane, bsn.fade,
            cls.toggle("active show") <-- activeSignal,
            child <-- tabStream.map { t => t.content }
          )
        )

      //FIXME
      //  Sortable(header, sortableOptions(tabs, setActive))

      TabRender(header, tabDiv)
    }

    def render = {
      div(child <-- tabs.signal.split(_.tabID)(renderTab).map { tr =>
        div(
          ul(bsn.nav, tabStyle, tr.map(_.tabHeader)),
          div(bsn.tab_content, paddingTop := "10", tr.map(_.tabContent))
        )
      })
    }
  }

  //
  //  //TABLE

  def elementTable(rows: Seq[Row] = Seq()) = ElementTableBuilder(rows)

  def dataTable(rows: Seq[Seq[String]] = Seq()) = DataTableBuilder(rows)

  //  // EXCLUSIVE BUTTON GROUPS

  //  def twoStatesGlyphButton(glyph1: ModifierSeq,
  //                           glyph2: ModifierSeq,
  //                           todo1: () ⇒ Unit,
  //                           todo2: () ⇒ Unit,
  //                           preGlyph: ModifierSeq = Seq()
  //                          ) = TwoStatesGlyphButton(glyph1, glyph2, todo1, todo2, preGlyph)
  //
  //  def twoStatesSpan(glyph1: ModifierSeq,
  //                    glyph2: ModifierSeq,
  //                    todo1: () ⇒ Unit,
  //                    todo2: () ⇒ Unit,
  //                    preString: String,
  //                    buttonStyle: ModifierSeq = emptyMod
  //                   ) = TwoStatesSpan(glyph1, glyph2, todo1, todo2, preString, buttonStyle)
  //
  //  sealed trait ExclusiveButton {
  //    def action: () ⇒ Unit
  //  }
  //
  //  trait ExclusiveGlyphButton extends ExclusiveButton {
  //    def glyph: Glyphicon
  //  }
  //
  //  trait ExclusiveStringButton extends ExclusiveButton {
  //    def title: String
  //  }
  //
  //  trait TwoStates extends ExclusiveButton
  //
  //  case class TwoStatesGlyphButton(glyph: ModifierSeq,
  //                                  glyph2: ModifierSeq,
  //                                  action: () ⇒ Unit,
  //                                  action2: () ⇒ Unit,
  //                                  preGlyph: ModifierSeq
  //                                 ) extends TwoStates {
  //    val cssglyph = glyph +++ (paddingLeft := 3).toMS
  //
  //    lazy val div = {
  //      buttonIcon("", preGlyph, cssglyph, action)
  //    }
  //  }
  //
  //  case class TwoStatesSpan(glyph: ModifierSeq,
  //                           glyph2: ModifierSeq,
  //                           action: () ⇒ Unit,
  //                           action2: () ⇒ Unit,
  //                           preString: String,
  //                           buttonStyle: ModifierSeq = emptyMod
  //                          ) extends TwoStates {
  //    val cssglyph = glyph +++ (paddingLeft := 3).toMS
  //
  //    lazy val cssbutton: ModifierSeq = Seq(
  //      paddingTop := 8,
  //      border := "none"
  //    )
  //
  //    lazy val div = button(preString, onClick := action)(buttonStyle +++ cssbutton +++ pointer)(
  //      span(cssglyph)
  //    )
  //
  //  }
  //
  //  object ExclusiveButton {
  //    def string(t: String, a: () ⇒ Unit) = new ExclusiveStringButton {
  //      def title = t
  //
  //      def action = a
  //    }
  //
  //    def glyph(g: Glyphicon, a: () ⇒ Unit) = new ExclusiveGlyphButton {
  //      def glyph = g
  //
  //      def action = a
  //    }
  //
  //    def twoGlyphButtonStates(
  //                              glyph1: ModifierSeq,
  //                              glyph2: ModifierSeq,
  //                              todo1: () ⇒ Unit,
  //                              todo2: () ⇒ Unit,
  //                              preGlyph: ModifierSeq
  //                            ) = twoStatesGlyphButton(glyph1, glyph2, todo1, todo2, preGlyph)
  //
  //    def twoGlyphSpan(
  //                      glyph1: ModifierSeq,
  //                      glyph2: ModifierSeq,
  //                      todo1: () ⇒ Unit,
  //                      todo2: () ⇒ Unit,
  //                      preString: String,
  //                      buttonStyle: ModifierSeq = emptyMod
  //                    ) = twoStatesSpan(glyph1, glyph2, todo1, todo2, preString, buttonStyle)
  //  }
  //
  //  class ExclusiveGroup(style: ModifierSeq,
  //                       defaultStyle: ModifierSeq,
  //                       selectionStyle: ModifierSeq,
  //                       buttons: Seq[ExclusiveButton]) {
  //
  //    implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  //
  //    val selected = Var(buttons.head)
  //    val selectedAgain = Var(false)
  //
  //    def buttonBackground(b: ExclusiveButton) = (if (b == selected.now) btn +++ selectionStyle else btn +++ defaultStyle)
  //
  //    def glyphButtonBackground(b: ExclusiveButton) = buttonBackground(b) +++ twoGlyphButton
  //
  //    def stringButtonBackground(b: ExclusiveButton) = buttonBackground(b) +++ stringButton
  //
  //    def glyphForTwoStates(ts: TwoStates, mod: ModifierSeq) = (ts == selected.now, mod, emptyMod)
  //
  //    val div: Modifier = Rx {
  //      selected()
  //      tags.div(style +++ btnGroup)(
  //        for (b ← buttons) yield {
  //          b match {
  //            case s: ExclusiveStringButton ⇒ button(s.title, onClick := action(b, s.action))(stringButtonBackground(s) +++ stringInGroup)
  //            case g: ExclusiveGlyphButton ⇒ buttonIcon("", glyphButtonBackground(g), g.glyph, action(b, g.action))
  //            case ts: TwoStatesGlyphButton ⇒
  //              if (selectedAgain()) twoStatesGlyphButton(glyphForTwoStates(ts, ts.glyph2), ts.glyph, action(ts, ts.action2), action(ts, ts.action), glyphButtonBackground(ts) +++ ts.preGlyph).div
  //              else twoStatesGlyphButton(glyphForTwoStates(ts, ts.glyph), ts.glyph2, action(ts, ts.action), action(ts, ts.action2), glyphButtonBackground(ts) +++ ts.preGlyph).div
  //            case ts: TwoStatesSpan ⇒
  //              if (selectedAgain()) twoStatesSpan(glyphForTwoStates(ts, ts.glyph2), ts.glyph, action(ts, ts.action2), action(ts, ts.action), ts.preString, glyphButtonBackground(ts)).div
  //              else twoStatesSpan(glyphForTwoStates(ts, ts.glyph), ts.glyph2, action(ts, ts.action), action(ts, ts.action2), ts.preString, glyphButtonBackground(ts)).div
  //          }
  //        }
  //      )
  //    }
  //
  //    private def action(b: ExclusiveButton, a: () ⇒ Unit) = () ⇒ {
  //      selectedAgain() = if (b == selected.now) !selectedAgain.now else false
  //      selected() = b
  //      a()
  //    }
  //
  //
  //    def reset = selected() = buttons.head
  //  }
  //
  //
  //  // FORMS
  //
  trait FormTag {
    def tag: HtmlElement
  }

  trait LabeledFormTag extends FormTag {
    def label: Label
  }

  implicit def htmlElementToFormTag(t: HtmlElement): FormTag = new FormTag {
    val tag: HtmlElement = t
  }

  implicit class LabelForModifiers(m: HtmlElement) {
    def withLabel(title: String, labelStyle: HESetters = emptySetters): LabeledFormTag = new LabeledFormTag {
      val label: Label = com.raquo.laminar.api.L.label(title, labelStyle, paddingRight := "5")

      val tag: HtmlElement = m
    }
  }

  private def insideForm(elements: Seq[FormTag]) =
    for {
      ft <- elements
    } yield {
      div(bsnsheet.formGroup, paddingRight := "5",
        ft match {
          case lft: LabeledFormTag => lft.label
          case _ => span()
        },
        ft.tag
      )
    }


  def vForm(elements: FormTag*): HtmlElement = vForm(emptySetters, elements: _*)


  def vForm(heSetters: HESetters, elements: FormTag*): HtmlElement =
    div(heSetters :+ bsnsheet.formVertical, insideForm(elements))


  def hForm(formTags: FormTag*): HtmlElement = hForm(emptySetters, formTags: _*)

  def hForm(heSetters: HESetters, formTags: FormTag*): HtmlElement = {
    form(bsn.formInline, heSetters, insideForm(formTags))
  }

  //TOAST
  type ToastPosition = HESetters
  type ToastID = String

  case class ToastHeader(text: String, comment: String = "", backgroundColor: String = "#fff")

  case class Toast(header: ToastHeader, bodyText: String, toastPosition: ToastPosition = bsn.bottomRightPosition, delay: Option[Int] = None, toastID: ToastID = uuID.short("t"))

  def toast(toastHeader: ToastHeader, bodyText: String, toastPosition: ToastPosition = bsn.bottomRightPosition, delay: Option[Int] = None) =
    Toast(toastHeader, bodyText, toastPosition, delay)

  case class ToastStack(toastPosition: ToastPosition, unstackOnClose: Boolean, initialToasts: Seq[Toast]) {

    val toasts = Var(initialToasts)
    val activeToasts = Var(Seq[Toast]())

    def toast(toastID: ToastID) = toasts.now.filter(_.toastID == toastID)

    def stack(toast: Toast) =
      if (!toasts.now.exists(_ == toast))
        toasts.update(ts => ts :+ toast)

    def unstack(toast: Toast) = toasts.update(ts => ts.filterNot(_ == toast))

    def show(toast: Toast) =
      if (!activeToasts.now.exists(_ == toast)) {
        activeToasts.update(ts => ts :+ toast)
        toast.delay.foreach { d =>
          scalajs.js.timers.setTimeout(d)(hide(toast))
        }
      }

    def stackAndShow(toast: Toast) = {
      stack(toast)
      show(toast)
    }

    def hide(toast: Toast) = activeToasts.update(at => at.filterNot(_ == toast))


    def toastRender(toastID: ToastID, initialToast: Toast, toastStream: Signal[Toast]): Div = {

      val isActive = activeToasts.signal.map { ts => ts.contains(initialToast) }

      div(
        bsnsheet.toastCls, role := "alert", aria.live := "assertive", aria.atomic := true, dataAttr("animation") := "true",
        cls <-- isActive.map { a =>
          if (a) "fade show" else "fade hide"
        },
        div(bsn.toastHeader, backgroundColor := initialToast.header.backgroundColor,
          strong(bsmargin.r.auto, initialToast.header.text),
          small(initialToast.header.comment),
          button(`type` := "button", bsmargin.l.two, bsmargin.b.one, cls := "close", dataAttr("dismiss") := "toast", aria.label := "Close",
            span(aria.hidden := true, "×"),
            onClick --> { e =>
              hide(initialToast)
              if (unstackOnClose)
                unstack(initialToast)
            }
          )
        ),
        div(bsn.toastBody, initialToast.bodyText)
      )
    }

    val render = {
      div(
        toastPosition,
        children <-- toasts.signal.split(_.toastID)(toastRender)
      )
    }
  }

  def toastStack(toastPosition: ToastPosition, unstackOnClose: Boolean, toasts: Toast*) = ToastStack(toastPosition: ToastPosition, unstackOnClose, toasts)
}




