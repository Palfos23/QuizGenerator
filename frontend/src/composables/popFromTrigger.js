// Popovers should grow out of the button that opened them and shrink back into it. Wire as
// <Transition name="pop" @enter="popFromTrigger" @leave="popFromTrigger">: it points transform-origin at the middle
// of the trigger (the popup's previous sibling) before the scale animation starts.
export function popFromTrigger(el) {
  const trigger = el.previousElementSibling
  if (!trigger) return
  el.style.transformOrigin = '50% 50%' // measure from a known origin
  const s = parseFloat(getComputedStyle(el).scale) || 1 // the 0.95 start state shrinks the box a little
  const p = el.getBoundingClientRect()
  const t = trigger.getBoundingClientRect()
  const left = p.left - (p.width / s - p.width) / 2
  const top = p.top - (p.height / s - p.height) / 2
  el.style.transformOrigin = `${t.left + t.width / 2 - left}px ${t.top + t.height / 2 - top}px`
}
