(() => {
  var We = !1,
    Ge = !1,
    H = [],
    Je = -1;
  function Lt(e) {
    dn(e);
  }
  function dn(e) {
    H.includes(e) || H.push(e), pn();
  }
  function ye(e) {
    let t = H.indexOf(e);
    t !== -1 && t > Je && H.splice(t, 1);
  }
  function pn() {
    !Ge && !We && ((We = !0), queueMicrotask(mn));
  }
  function mn() {
    (We = !1), (Ge = !0);
    for (let e = 0; e < H.length; e++) H[e](), (Je = e);
    (H.length = 0), (Je = -1), (Ge = !1);
  }
  var T,
    P,
    L,
    Xe,
    Ye = !0;
  function $t(e) {
    (Ye = !1), e(), (Ye = !0);
  }
  function Ft(e) {
    (T = e.reactive),
      (L = e.release),
      (P = (t) =>
        e.effect(t, {
          scheduler: (r) => {
            Ye ? Lt(r) : r();
          },
        })),
      (Xe = e.raw);
  }
  function Ze(e) {
    P = e;
  }
  function jt(e) {
    let t = () => {};
    return [
      (n) => {
        let i = P(n);
        return (
          e._x_effects ||
            ((e._x_effects = new Set()),
            (e._x_runEffects = () => {
              e._x_effects.forEach((o) => o());
            })),
          e._x_effects.add(i),
          (t = () => {
            i !== void 0 && (e._x_effects.delete(i), L(i));
          }),
          i
        );
      },
      () => {
        t();
      },
    ];
  }
  var Bt = [],
    Kt = [],
    zt = [];
  function Ht(e) {
    zt.push(e);
  }
  function we(e, t) {
    typeof t == "function"
      ? (e._x_cleanups || (e._x_cleanups = []), e._x_cleanups.push(t))
      : ((t = e), Kt.push(t));
  }
  function Vt(e) {
    Bt.push(e);
  }
  function qt(e, t, r) {
    e._x_attributeCleanups || (e._x_attributeCleanups = {}),
      e._x_attributeCleanups[t] || (e._x_attributeCleanups[t] = []),
      e._x_attributeCleanups[t].push(r);
  }
  function et(e, t) {
    e._x_attributeCleanups &&
      Object.entries(e._x_attributeCleanups).forEach(([r, n]) => {
        (t === void 0 || t.includes(r)) &&
          (n.forEach((i) => i()), delete e._x_attributeCleanups[r]);
      });
  }
  var tt = new MutationObserver(ot),
    rt = !1;
  function ae() {
    tt.observe(document, {
      subtree: !0,
      childList: !0,
      attributes: !0,
      attributeOldValue: !0,
    }),
      (rt = !0);
  }
  function nt() {
    hn(), tt.disconnect(), (rt = !1);
  }
  var se = [],
    Qe = !1;
  function hn() {
    (se = se.concat(tt.takeRecords())),
      se.length &&
        !Qe &&
        ((Qe = !0),
        queueMicrotask(() => {
          _n(), (Qe = !1);
        }));
  }
  function _n() {
    ot(se), (se.length = 0);
  }
  function h(e) {
    if (!rt) return e();
    nt();
    let t = e();
    return ae(), t;
  }
  var it = !1,
    be = [];
  function Ut() {
    it = !0;
  }
  function Wt() {
    (it = !1), ot(be), (be = []);
  }
  function ot(e) {
    if (it) {
      be = be.concat(e);
      return;
    }
    let t = [],
      r = [],
      n = new Map(),
      i = new Map();
    for (let o = 0; o < e.length; o++)
      if (
        !e[o].target._x_ignoreMutationObserver &&
        (e[o].type === "childList" &&
          (e[o].addedNodes.forEach((s) => s.nodeType === 1 && t.push(s)),
          e[o].removedNodes.forEach((s) => s.nodeType === 1 && r.push(s))),
        e[o].type === "attributes")
      ) {
        let s = e[o].target,
          a = e[o].attributeName,
          c = e[o].oldValue,
          l = () => {
            n.has(s) || n.set(s, []),
              n.get(s).push({ name: a, value: s.getAttribute(a) });
          },
          u = () => {
            i.has(s) || i.set(s, []), i.get(s).push(a);
          };
        s.hasAttribute(a) && c === null
          ? l()
          : s.hasAttribute(a)
          ? (u(), l())
          : u();
      }
    i.forEach((o, s) => {
      et(s, o);
    }),
      n.forEach((o, s) => {
        Bt.forEach((a) => a(s, o));
      });
    for (let o of r)
      if (!t.includes(o) && (Kt.forEach((s) => s(o)), o._x_cleanups))
        for (; o._x_cleanups.length; ) o._x_cleanups.pop()();
    t.forEach((o) => {
      (o._x_ignoreSelf = !0), (o._x_ignore = !0);
    });
    for (let o of t)
      r.includes(o) ||
        (o.isConnected &&
          (delete o._x_ignoreSelf,
          delete o._x_ignore,
          zt.forEach((s) => s(o)),
          (o._x_ignore = !0),
          (o._x_ignoreSelf = !0)));
    t.forEach((o) => {
      delete o._x_ignoreSelf, delete o._x_ignore;
    }),
      (t = null),
      (r = null),
      (n = null),
      (i = null);
  }
  function Ee(e) {
    return F($(e));
  }
  function R(e, t, r) {
    return (
      (e._x_dataStack = [t, ...$(r || e)]),
      () => {
        e._x_dataStack = e._x_dataStack.filter((n) => n !== t);
      }
    );
  }
  function $(e) {
    return e._x_dataStack
      ? e._x_dataStack
      : typeof ShadowRoot == "function" && e instanceof ShadowRoot
      ? $(e.host)
      : e.parentNode
      ? $(e.parentNode)
      : [];
  }
  function F(e) {
    let t = new Proxy(
      {},
      {
        ownKeys: () => Array.from(new Set(e.flatMap((r) => Object.keys(r)))),
        has: (r, n) => e.some((i) => i.hasOwnProperty(n)),
        get: (r, n) =>
          (e.find((i) => {
            if (i.hasOwnProperty(n)) {
              let o = Object.getOwnPropertyDescriptor(i, n);
              if (
                (o.get && o.get._x_alreadyBound) ||
                (o.set && o.set._x_alreadyBound)
              )
                return !0;
              if ((o.get || o.set) && o.enumerable) {
                let s = o.get,
                  a = o.set,
                  c = o;
                (s = s && s.bind(t)),
                  (a = a && a.bind(t)),
                  s && (s._x_alreadyBound = !0),
                  a && (a._x_alreadyBound = !0),
                  Object.defineProperty(i, n, { ...c, get: s, set: a });
              }
              return !0;
            }
            return !1;
          }) || {})[n],
        set: (r, n, i) => {
          let o = e.find((s) => s.hasOwnProperty(n));
          return o ? (o[n] = i) : (e[e.length - 1][n] = i), !0;
        },
      }
    );
    return t;
  }
  function ve(e) {
    let t = (n) => typeof n == "object" && !Array.isArray(n) && n !== null,
      r = (n, i = "") => {
        Object.entries(Object.getOwnPropertyDescriptors(n)).forEach(
          ([o, { value: s, enumerable: a }]) => {
            if (a === !1 || s === void 0) return;
            let c = i === "" ? o : `${i}.${o}`;
            typeof s == "object" && s !== null && s._x_interceptor
              ? (n[o] = s.initialize(e, c, o))
              : t(s) && s !== n && !(s instanceof Element) && r(s, c);
          }
        );
      };
    return r(e);
  }
  function Se(e, t = () => {}) {
    let r = {
      initialValue: void 0,
      _x_interceptor: !0,
      initialize(n, i, o) {
        return e(
          this.initialValue,
          () => gn(n, i),
          (s) => st(n, i, s),
          i,
          o
        );
      },
    };
    return (
      t(r),
      (n) => {
        if (typeof n == "object" && n !== null && n._x_interceptor) {
          let i = r.initialize.bind(r);
          r.initialize = (o, s, a) => {
            let c = n.initialize(o, s, a);
            return (r.initialValue = c), i(o, s, a);
          };
        } else r.initialValue = n;
        return r;
      }
    );
  }
  function gn(e, t) {
    return t.split(".").reduce((r, n) => r[n], e);
  }
  function st(e, t, r) {
    if ((typeof t == "string" && (t = t.split(".")), t.length === 1))
      e[t[0]] = r;
    else {
      if (t.length === 0) throw error;
      return e[t[0]] || (e[t[0]] = {}), st(e[t[0]], t.slice(1), r);
    }
  }
  var Gt = {};
  function y(e, t) {
    Gt[e] = t;
  }
  function ce(e, t) {
    return (
      Object.entries(Gt).forEach(([r, n]) => {
        let i = null;
        function o() {
          if (i) return i;
          {
            let [s, a] = at(t);
            return (i = { interceptor: Se, ...s }), we(t, a), i;
          }
        }
        Object.defineProperty(e, `$${r}`, {
          get() {
            return n(t, o());
          },
          enumerable: !1,
        });
      }),
      e
    );
  }
  function Jt(e, t, r, ...n) {
    try {
      return r(...n);
    } catch (i) {
      X(i, e, t);
    }
  }
  function X(e, t, r = void 0) {
    Object.assign(e, { el: t, expression: r }),
      console.warn(
        `Alpine Expression Error: ${e.message}

${
  r
    ? 'Expression: "' +
      r +
      `"

`
    : ""
}`,
        t
      ),
      setTimeout(() => {
        throw e;
      }, 0);
  }
  var Ae = !0;
  function Yt(e) {
    let t = Ae;
    (Ae = !1), e(), (Ae = t);
  }
  function D(e, t, r = {}) {
    let n;
    return x(e, t)((i) => (n = i), r), n;
  }
  function x(...e) {
    return Xt(...e);
  }
  var Xt = lt;
  function Zt(e) {
    Xt = e;
  }
  function lt(e, t) {
    let r = {};
    ce(r, e);
    let n = [r, ...$(e)],
      i = typeof t == "function" ? xn(n, t) : bn(n, t, e);
    return Jt.bind(null, e, t, i);
  }
  function xn(e, t) {
    return (r = () => {}, { scope: n = {}, params: i = [] } = {}) => {
      let o = t.apply(F([n, ...e]), i);
      Oe(r, o);
    };
  }
  var ct = {};
  function yn(e, t) {
    if (ct[e]) return ct[e];
    let r = Object.getPrototypeOf(async function () {}).constructor,
      n =
        /^[\n\s]*if.*\(.*\)/.test(e) || /^(let|const)\s/.test(e)
          ? `(async()=>{ ${e} })()`
          : e,
      o = (() => {
        try {
          return new r(
            ["__self", "scope"],
            `with (scope) { __self.result = ${n} }; __self.finished = true; return __self.result;`
          );
        } catch (s) {
          return X(s, t, e), Promise.resolve();
        }
      })();
    return (ct[e] = o), o;
  }
  function bn(e, t, r) {
    let n = yn(t, r);
    return (i = () => {}, { scope: o = {}, params: s = [] } = {}) => {
      (n.result = void 0), (n.finished = !1);
      let a = F([o, ...e]);
      if (typeof n == "function") {
        let c = n(n, a).catch((l) => X(l, r, t));
        n.finished
          ? (Oe(i, n.result, a, s, r), (n.result = void 0))
          : c
              .then((l) => {
                Oe(i, l, a, s, r);
              })
              .catch((l) => X(l, r, t))
              .finally(() => (n.result = void 0));
      }
    };
  }
  function Oe(e, t, r, n, i) {
    if (Ae && typeof t == "function") {
      let o = t.apply(r, n);
      o instanceof Promise
        ? o.then((s) => Oe(e, s, r, n)).catch((s) => X(s, i, t))
        : e(o);
    } else
      typeof t == "object" && t instanceof Promise ? t.then((o) => e(o)) : e(t);
  }
  var pt = "x-";
  function O(e = "") {
    return pt + e;
  }
  function Qt(e) {
    pt = e;
  }
  var ut = {};
  function p(e, t) {
    return (
      (ut[e] = t),
      {
        before(r) {
          if (!ut[r]) {
            console.warn(
              "Cannot find directive `${directive}`. `${name}` will use the default order of execution"
            );
            return;
          }
          let n = V.indexOf(r);
          V.splice(n >= 0 ? n : V.indexOf("DEFAULT"), 0, e);
        },
      }
    );
  }
  function ue(e, t, r) {
    if (((t = Array.from(t)), e._x_virtualDirectives)) {
      let o = Object.entries(e._x_virtualDirectives).map(([a, c]) => ({
          name: a,
          value: c,
        })),
        s = mt(o);
      (o = o.map((a) =>
        s.find((c) => c.name === a.name)
          ? { name: `x-bind:${a.name}`, value: `"${a.value}"` }
          : a
      )),
        (t = t.concat(o));
    }
    let n = {};
    return t
      .map(rr((o, s) => (n[o] = s)))
      .filter(ir)
      .map(En(n, r))
      .sort(vn)
      .map((o) => wn(e, o));
  }
  function mt(e) {
    return Array.from(e)
      .map(rr())
      .filter((t) => !ir(t));
  }
  var ft = !1,
    le = new Map(),
    er = Symbol();
  function tr(e) {
    ft = !0;
    let t = Symbol();
    (er = t), le.set(t, []);
    let r = () => {
        for (; le.get(t).length; ) le.get(t).shift()();
        le.delete(t);
      },
      n = () => {
        (ft = !1), r();
      };
    e(r), n();
  }
  function at(e) {
    let t = [],
      r = (a) => t.push(a),
      [n, i] = jt(e);
    return (
      t.push(i),
      [
        {
          Alpine: j,
          effect: n,
          cleanup: r,
          evaluateLater: x.bind(x, e),
          evaluate: D.bind(D, e),
        },
        () => t.forEach((a) => a()),
      ]
    );
  }
  function wn(e, t) {
    let r = () => {},
      n = ut[t.type] || r,
      [i, o] = at(e);
    qt(e, t.original, o);
    let s = () => {
      e._x_ignore ||
        e._x_ignoreSelf ||
        (n.inline && n.inline(e, t, i),
        (n = n.bind(n, e, t, i)),
        ft ? le.get(er).push(n) : n());
    };
    return (s.runCleanups = o), s;
  }
  var Ce =
      (e, t) =>
      ({ name: r, value: n }) => (
        r.startsWith(e) && (r = r.replace(e, t)), { name: r, value: n }
      ),
    Te = (e) => e;
  function rr(e = () => {}) {
    return ({ name: t, value: r }) => {
      let { name: n, value: i } = nr.reduce((o, s) => s(o), {
        name: t,
        value: r,
      });
      return n !== t && e(n, t), { name: n, value: i };
    };
  }
  var nr = [];
  function Z(e) {
    nr.push(e);
  }
  function ir({ name: e }) {
    return or().test(e);
  }
  var or = () => new RegExp(`^${pt}([^:^.]+)\\b`);
  function En(e, t) {
    return ({ name: r, value: n }) => {
      let i = r.match(or()),
        o = r.match(/:([a-zA-Z0-9\-:]+)/),
        s = r.match(/\.[^.\]]+(?=[^\]]*$)/g) || [],
        a = t || e[r] || r;
      return {
        type: i ? i[1] : null,
        value: o ? o[1] : null,
        modifiers: s.map((c) => c.replace(".", "")),
        expression: n,
        original: a,
      };
    };
  }
  var dt = "DEFAULT",
    V = [
      "ignore",
      "ref",
      "data",
      "id",
      "bind",
      "init",
      "for",
      "model",
      "modelable",
      "transition",
      "show",
      "if",
      dt,
      "teleport",
    ];
  function vn(e, t) {
    let r = V.indexOf(e.type) === -1 ? dt : e.type,
      n = V.indexOf(t.type) === -1 ? dt : t.type;
    return V.indexOf(r) - V.indexOf(n);
  }
  function q(e, t, r = {}) {
    e.dispatchEvent(
      new CustomEvent(t, {
        detail: r,
        bubbles: !0,
        composed: !0,
        cancelable: !0,
      })
    );
  }
  function C(e, t) {
    if (typeof ShadowRoot == "function" && e instanceof ShadowRoot) {
      Array.from(e.children).forEach((i) => C(i, t));
      return;
    }
    let r = !1;
    if ((t(e, () => (r = !0)), r)) return;
    let n = e.firstElementChild;
    for (; n; ) C(n, t, !1), (n = n.nextElementSibling);
  }
  function S(e, ...t) {
    console.warn(`Alpine Warning: ${e}`, ...t);
  }
  var sr = !1;
  function ar() {
    sr &&
      S(
        "Alpine has already been initialized on this page. Calling Alpine.start() more than once can cause problems."
      ),
      (sr = !0),
      document.body ||
        S(
          "Unable to initialize. Trying to load Alpine before `<body>` is available. Did you forget to add `defer` in Alpine's `<script>` tag?"
        ),
      q(document, "alpine:init"),
      q(document, "alpine:initializing"),
      ae(),
      Ht((t) => v(t, C)),
      we((t) => _t(t)),
      Vt((t, r) => {
        ue(t, r).forEach((n) => n());
      });
    let e = (t) => !U(t.parentElement, !0);
    Array.from(document.querySelectorAll(ur()))
      .filter(e)
      .forEach((t) => {
        v(t);
      }),
      q(document, "alpine:initialized");
  }
  var ht = [],
    cr = [];
  function lr() {
    return ht.map((e) => e());
  }
  function ur() {
    return ht.concat(cr).map((e) => e());
  }
  function Me(e) {
    ht.push(e);
  }
  function Re(e) {
    cr.push(e);
  }
  function U(e, t = !1) {
    return Q(e, (r) => {
      if ((t ? ur() : lr()).some((i) => r.matches(i))) return !0;
    });
  }
  function Q(e, t) {
    if (e) {
      if (t(e)) return e;
      if ((e._x_teleportBack && (e = e._x_teleportBack), !!e.parentElement))
        return Q(e.parentElement, t);
    }
  }
  function fr(e) {
    return lr().some((t) => e.matches(t));
  }
  var dr = [];
  function pr(e) {
    dr.push(e);
  }
  function v(e, t = C, r = () => {}) {
    tr(() => {
      t(e, (n, i) => {
        r(n, i),
          dr.forEach((o) => o(n, i)),
          ue(n, n.attributes).forEach((o) => o()),
          n._x_ignore && i();
      });
    });
  }
  function _t(e) {
    C(e, (t) => et(t));
  }
  var gt = [],
    xt = !1;
  function ee(e = () => {}) {
    return (
      queueMicrotask(() => {
        xt ||
          setTimeout(() => {
            Ne();
          });
      }),
      new Promise((t) => {
        gt.push(() => {
          e(), t();
        });
      })
    );
  }
  function Ne() {
    for (xt = !1; gt.length; ) gt.shift()();
  }
  function mr() {
    xt = !0;
  }
  function fe(e, t) {
    return Array.isArray(t)
      ? hr(e, t.join(" "))
      : typeof t == "object" && t !== null
      ? Sn(e, t)
      : typeof t == "function"
      ? fe(e, t())
      : hr(e, t);
  }
  function hr(e, t) {
    let r = (o) => o.split(" ").filter(Boolean),
      n = (o) =>
        o
          .split(" ")
          .filter((s) => !e.classList.contains(s))
          .filter(Boolean),
      i = (o) => (
        e.classList.add(...o),
        () => {
          e.classList.remove(...o);
        }
      );
    return (t = t === !0 ? (t = "") : t || ""), i(n(t));
  }
  function Sn(e, t) {
    let r = (a) => a.split(" ").filter(Boolean),
      n = Object.entries(t)
        .flatMap(([a, c]) => (c ? r(a) : !1))
        .filter(Boolean),
      i = Object.entries(t)
        .flatMap(([a, c]) => (c ? !1 : r(a)))
        .filter(Boolean),
      o = [],
      s = [];
    return (
      i.forEach((a) => {
        e.classList.contains(a) && (e.classList.remove(a), s.push(a));
      }),
      n.forEach((a) => {
        e.classList.contains(a) || (e.classList.add(a), o.push(a));
      }),
      () => {
        s.forEach((a) => e.classList.add(a)),
          o.forEach((a) => e.classList.remove(a));
      }
    );
  }
  function W(e, t) {
    return typeof t == "object" && t !== null ? An(e, t) : On(e, t);
  }
  function An(e, t) {
    let r = {};
    return (
      Object.entries(t).forEach(([n, i]) => {
        (r[n] = e.style[n]),
          n.startsWith("--") || (n = Cn(n)),
          e.style.setProperty(n, i);
      }),
      setTimeout(() => {
        e.style.length === 0 && e.removeAttribute("style");
      }),
      () => {
        W(e, r);
      }
    );
  }
  function On(e, t) {
    let r = e.getAttribute("style", t);
    return (
      e.setAttribute("style", t),
      () => {
        e.setAttribute("style", r || "");
      }
    );
  }
  function Cn(e) {
    return e.replace(/([a-z])([A-Z])/g, "$1-$2").toLowerCase();
  }
  function de(e, t = () => {}) {
    let r = !1;
    return function () {
      r ? t.apply(this, arguments) : ((r = !0), e.apply(this, arguments));
    };
  }
  p(
    "transition",
    (e, { value: t, modifiers: r, expression: n }, { evaluate: i }) => {
      typeof n == "function" && (n = i(n)),
        n !== !1 && (!n || typeof n == "boolean" ? Mn(e, r, t) : Tn(e, n, t));
    }
  );
  function Tn(e, t, r) {
    _r(e, fe, ""),
      {
        enter: (i) => {
          e._x_transition.enter.during = i;
        },
        "enter-start": (i) => {
          e._x_transition.enter.start = i;
        },
        "enter-end": (i) => {
          e._x_transition.enter.end = i;
        },
        leave: (i) => {
          e._x_transition.leave.during = i;
        },
        "leave-start": (i) => {
          e._x_transition.leave.start = i;
        },
        "leave-end": (i) => {
          e._x_transition.leave.end = i;
        },
      }[r](t);
  }
  function Mn(e, t, r) {
    _r(e, W);
    let n = !t.includes("in") && !t.includes("out") && !r,
      i = n || t.includes("in") || ["enter"].includes(r),
      o = n || t.includes("out") || ["leave"].includes(r);
    t.includes("in") && !n && (t = t.filter((g, b) => b < t.indexOf("out"))),
      t.includes("out") && !n && (t = t.filter((g, b) => b > t.indexOf("out")));
    let s = !t.includes("opacity") && !t.includes("scale"),
      a = s || t.includes("opacity"),
      c = s || t.includes("scale"),
      l = a ? 0 : 1,
      u = c ? pe(t, "scale", 95) / 100 : 1,
      d = pe(t, "delay", 0) / 1e3,
      m = pe(t, "origin", "center"),
      w = "opacity, transform",
      k = pe(t, "duration", 150) / 1e3,
      xe = pe(t, "duration", 75) / 1e3,
      f = "cubic-bezier(0.4, 0.0, 0.2, 1)";
    i &&
      ((e._x_transition.enter.during = {
        transformOrigin: m,
        transitionDelay: `${d}s`,
        transitionProperty: w,
        transitionDuration: `${k}s`,
        transitionTimingFunction: f,
      }),
      (e._x_transition.enter.start = { opacity: l, transform: `scale(${u})` }),
      (e._x_transition.enter.end = { opacity: 1, transform: "scale(1)" })),
      o &&
        ((e._x_transition.leave.during = {
          transformOrigin: m,
          transitionDelay: `${d}s`,
          transitionProperty: w,
          transitionDuration: `${xe}s`,
          transitionTimingFunction: f,
        }),
        (e._x_transition.leave.start = { opacity: 1, transform: "scale(1)" }),
        (e._x_transition.leave.end = { opacity: l, transform: `scale(${u})` }));
  }
  function _r(e, t, r = {}) {
    e._x_transition ||
      (e._x_transition = {
        enter: { during: r, start: r, end: r },
        leave: { during: r, start: r, end: r },
        in(n = () => {}, i = () => {}) {
          Ie(
            e,
            t,
            {
              during: this.enter.during,
              start: this.enter.start,
              end: this.enter.end,
            },
            n,
            i
          );
        },
        out(n = () => {}, i = () => {}) {
          Ie(
            e,
            t,
            {
              during: this.leave.during,
              start: this.leave.start,
              end: this.leave.end,
            },
            n,
            i
          );
        },
      });
  }
  window.Element.prototype._x_toggleAndCascadeWithTransitions = function (
    e,
    t,
    r,
    n
  ) {
    let i =
        document.visibilityState === "visible"
          ? requestAnimationFrame
          : setTimeout,
      o = () => i(r);
    if (t) {
      e._x_transition && (e._x_transition.enter || e._x_transition.leave)
        ? e._x_transition.enter &&
          (Object.entries(e._x_transition.enter.during).length ||
            Object.entries(e._x_transition.enter.start).length ||
            Object.entries(e._x_transition.enter.end).length)
          ? e._x_transition.in(r)
          : o()
        : e._x_transition
        ? e._x_transition.in(r)
        : o();
      return;
    }
    (e._x_hidePromise = e._x_transition
      ? new Promise((s, a) => {
          e._x_transition.out(
            () => {},
            () => s(n)
          ),
            e._x_transitioning.beforeCancel(() =>
              a({ isFromCancelledTransition: !0 })
            );
        })
      : Promise.resolve(n)),
      queueMicrotask(() => {
        let s = gr(e);
        s
          ? (s._x_hideChildren || (s._x_hideChildren = []),
            s._x_hideChildren.push(e))
          : i(() => {
              let a = (c) => {
                let l = Promise.all([
                  c._x_hidePromise,
                  ...(c._x_hideChildren || []).map(a),
                ]).then(([u]) => u());
                return delete c._x_hidePromise, delete c._x_hideChildren, l;
              };
              a(e).catch((c) => {
                if (!c.isFromCancelledTransition) throw c;
              });
            });
      });
  };
  function gr(e) {
    let t = e.parentNode;
    if (t) return t._x_hidePromise ? t : gr(t);
  }
  function Ie(
    e,
    t,
    { during: r, start: n, end: i } = {},
    o = () => {},
    s = () => {}
  ) {
    if (
      (e._x_transitioning && e._x_transitioning.cancel(),
      Object.keys(r).length === 0 &&
        Object.keys(n).length === 0 &&
        Object.keys(i).length === 0)
    ) {
      o(), s();
      return;
    }
    let a, c, l;
    Rn(e, {
      start() {
        a = t(e, n);
      },
      during() {
        c = t(e, r);
      },
      before: o,
      end() {
        a(), (l = t(e, i));
      },
      after: s,
      cleanup() {
        c(), l();
      },
    });
  }
  function Rn(e, t) {
    let r,
      n,
      i,
      o = de(() => {
        h(() => {
          (r = !0),
            n || t.before(),
            i || (t.end(), Ne()),
            t.after(),
            e.isConnected && t.cleanup(),
            delete e._x_transitioning;
        });
      });
    (e._x_transitioning = {
      beforeCancels: [],
      beforeCancel(s) {
        this.beforeCancels.push(s);
      },
      cancel: de(function () {
        for (; this.beforeCancels.length; ) this.beforeCancels.shift()();
        o();
      }),
      finish: o,
    }),
      h(() => {
        t.start(), t.during();
      }),
      mr(),
      requestAnimationFrame(() => {
        if (r) return;
        let s =
            Number(
              getComputedStyle(e)
                .transitionDuration.replace(/,.*/, "")
                .replace("s", "")
            ) * 1e3,
          a =
            Number(
              getComputedStyle(e)
                .transitionDelay.replace(/,.*/, "")
                .replace("s", "")
            ) * 1e3;
        s === 0 &&
          (s =
            Number(getComputedStyle(e).animationDuration.replace("s", "")) *
            1e3),
          h(() => {
            t.before();
          }),
          (n = !0),
          requestAnimationFrame(() => {
            r ||
              (h(() => {
                t.end();
              }),
              Ne(),
              setTimeout(e._x_transitioning.finish, s + a),
              (i = !0));
          });
      });
  }
  function pe(e, t, r) {
    if (e.indexOf(t) === -1) return r;
    let n = e[e.indexOf(t) + 1];
    if (!n || (t === "scale" && isNaN(n))) return r;
    if (t === "duration" || t === "delay") {
      let i = n.match(/([0-9]+)ms/);
      if (i) return i[1];
    }
    return t === "origin" &&
      ["top", "right", "left", "center", "bottom"].includes(e[e.indexOf(t) + 2])
      ? [n, e[e.indexOf(t) + 2]].join(" ")
      : n;
  }
  var te = !1;
  function N(e, t = () => {}) {
    return (...r) => (te ? t(...r) : e(...r));
  }
  function xr(e) {
    return (...t) => te && e(...t);
  }
  function yr(e, t) {
    t._x_dataStack || (t._x_dataStack = e._x_dataStack),
      (te = !0),
      In(() => {
        Nn(t);
      }),
      (te = !1);
  }
  function Nn(e) {
    let t = !1;
    v(e, (n, i) => {
      C(n, (o, s) => {
        if (t && fr(o)) return s();
        (t = !0), i(o, s);
      });
    });
  }
  function In(e) {
    let t = P;
    Ze((r, n) => {
      let i = t(r);
      return L(i), () => {};
    }),
      e(),
      Ze(t);
  }
  function me(e, t, r, n = []) {
    switch (
      (e._x_bindings || (e._x_bindings = T({})),
      (e._x_bindings[t] = r),
      (t = n.includes("camel") ? Bn(t) : t),
      t)
    ) {
      case "value":
        Pn(e, r);
        break;
      case "style":
        kn(e, r);
        break;
      case "class":
        Dn(e, r);
        break;
      case "selected":
      case "checked":
        Ln(e, t, r);
        break;
      default:
        wr(e, t, r);
        break;
    }
  }
  function Pn(e, t) {
    if (e.type === "radio")
      e.attributes.value === void 0 && (e.value = t),
        window.fromModel && (e.checked = br(e.value, t));
    else if (e.type === "checkbox")
      Number.isInteger(t)
        ? (e.value = t)
        : !Number.isInteger(t) &&
          !Array.isArray(t) &&
          typeof t != "boolean" &&
          ![null, void 0].includes(t)
        ? (e.value = String(t))
        : Array.isArray(t)
        ? (e.checked = t.some((r) => br(r, e.value)))
        : (e.checked = !!t);
    else if (e.tagName === "SELECT") jn(e, t);
    else {
      if (e.value === t) return;
      e.value = t;
    }
  }
  function Dn(e, t) {
    e._x_undoAddedClasses && e._x_undoAddedClasses(),
      (e._x_undoAddedClasses = fe(e, t));
  }
  function kn(e, t) {
    e._x_undoAddedStyles && e._x_undoAddedStyles(),
      (e._x_undoAddedStyles = W(e, t));
  }
  function Ln(e, t, r) {
    wr(e, t, r), Fn(e, t, r);
  }
  function wr(e, t, r) {
    [null, void 0, !1].includes(r) && Kn(t)
      ? e.removeAttribute(t)
      : (Er(t) && (r = t), $n(e, t, r));
  }
  function $n(e, t, r) {
    e.getAttribute(t) != r && e.setAttribute(t, r);
  }
  function Fn(e, t, r) {
    e[t] !== r && (e[t] = r);
  }
  function jn(e, t) {
    let r = [].concat(t).map((n) => n + "");
    Array.from(e.options).forEach((n) => {
      n.selected = r.includes(n.value);
    });
  }
  function Bn(e) {
    return e.toLowerCase().replace(/-(\w)/g, (t, r) => r.toUpperCase());
  }
  function br(e, t) {
    return e == t;
  }
  function Er(e) {
    return [
      "disabled",
      "checked",
      "required",
      "readonly",
      "hidden",
      "open",
      "selected",
      "autofocus",
      "itemscope",
      "multiple",
      "novalidate",
      "allowfullscreen",
      "allowpaymentrequest",
      "formnovalidate",
      "autoplay",
      "controls",
      "loop",
      "muted",
      "playsinline",
      "default",
      "ismap",
      "reversed",
      "async",
      "defer",
      "nomodule",
    ].includes(e);
  }
  function Kn(e) {
    return ![
      "aria-pressed",
      "aria-checked",
      "aria-expanded",
      "aria-selected",
    ].includes(e);
  }
  function vr(e, t, r) {
    if (e._x_bindings && e._x_bindings[t] !== void 0) return e._x_bindings[t];
    let n = e.getAttribute(t);
    return n === null
      ? typeof r == "function"
        ? r()
        : r
      : n === ""
      ? !0
      : Er(t)
      ? !![t, "true"].includes(n)
      : n;
  }
  function Pe(e, t) {
    var r;
    return function () {
      var n = this,
        i = arguments,
        o = function () {
          (r = null), e.apply(n, i);
        };
      clearTimeout(r), (r = setTimeout(o, t));
    };
  }
  function De(e, t) {
    let r;
    return function () {
      let n = this,
        i = arguments;
      r || (e.apply(n, i), (r = !0), setTimeout(() => (r = !1), t));
    };
  }
  function Sr(e) {
    (Array.isArray(e) ? e : [e]).forEach((r) => r(j));
  }
  var G = {},
    Ar = !1;
  function Or(e, t) {
    if ((Ar || ((G = T(G)), (Ar = !0)), t === void 0)) return G[e];
    (G[e] = t),
      typeof t == "object" &&
        t !== null &&
        t.hasOwnProperty("init") &&
        typeof t.init == "function" &&
        G[e].init(),
      ve(G[e]);
  }
  function Cr() {
    return G;
  }
  var Tr = {};
  function Mr(e, t) {
    let r = typeof t != "function" ? () => t : t;
    e instanceof Element ? yt(e, r()) : (Tr[e] = r);
  }
  function Rr(e) {
    return (
      Object.entries(Tr).forEach(([t, r]) => {
        Object.defineProperty(e, t, {
          get() {
            return (...n) => r(...n);
          },
        });
      }),
      e
    );
  }
  function yt(e, t, r) {
    let n = [];
    for (; n.length; ) n.pop()();
    let i = Object.entries(t).map(([s, a]) => ({ name: s, value: a })),
      o = mt(i);
    (i = i.map((s) =>
      o.find((a) => a.name === s.name)
        ? { name: `x-bind:${s.name}`, value: `"${s.value}"` }
        : s
    )),
      ue(e, i, r).map((s) => {
        n.push(s.runCleanups), s();
      });
  }
  var Nr = {};
  function Ir(e, t) {
    Nr[e] = t;
  }
  function Pr(e, t) {
    return (
      Object.entries(Nr).forEach(([r, n]) => {
        Object.defineProperty(e, r, {
          get() {
            return (...i) => n.bind(t)(...i);
          },
          enumerable: !1,
        });
      }),
      e
    );
  }
  var zn = {
      get reactive() {
        return T;
      },
      get release() {
        return L;
      },
      get effect() {
        return P;
      },
      get raw() {
        return Xe;
      },
      version: "3.12.1",
      flushAndStopDeferringMutations: Wt,
      dontAutoEvaluateFunctions: Yt,
      disableEffectScheduling: $t,
      startObservingMutations: ae,
      stopObservingMutations: nt,
      setReactivityEngine: Ft,
      closestDataStack: $,
      skipDuringClone: N,
      onlyDuringClone: xr,
      addRootSelector: Me,
      addInitSelector: Re,
      addScopeToNode: R,
      deferMutations: Ut,
      mapAttributes: Z,
      evaluateLater: x,
      interceptInit: pr,
      setEvaluator: Zt,
      mergeProxies: F,
      findClosest: Q,
      closestRoot: U,
      destroyTree: _t,
      interceptor: Se,
      transition: Ie,
      setStyles: W,
      mutateDom: h,
      directive: p,
      throttle: De,
      debounce: Pe,
      evaluate: D,
      initTree: v,
      nextTick: ee,
      prefixed: O,
      prefix: Qt,
      plugin: Sr,
      magic: y,
      store: Or,
      start: ar,
      clone: yr,
      bound: vr,
      $data: Ee,
      walk: C,
      data: Ir,
      bind: Mr,
    },
    j = zn;
  function bt(e, t) {
    let r = Object.create(null),
      n = e.split(",");
    for (let i = 0; i < n.length; i++) r[n[i]] = !0;
    return t ? (i) => !!r[i.toLowerCase()] : (i) => !!r[i];
  }
  var Hn =
    "itemscope,allowfullscreen,formnovalidate,ismap,nomodule,novalidate,readonly";
  var fs = bt(
    Hn +
      ",async,autofocus,autoplay,controls,default,defer,disabled,hidden,loop,open,required,reversed,scoped,seamless,checked,muted,multiple,selected"
  );
  var Dr = Object.freeze({}),
    ds = Object.freeze([]);
  var Vn = Object.prototype.hasOwnProperty,
    he = (e, t) => Vn.call(e, t),
    B = Array.isArray,
    re = (e) => kr(e) === "[object Map]";
  var qn = (e) => typeof e == "string",
    ke = (e) => typeof e == "symbol",
    _e = (e) => e !== null && typeof e == "object";
  var Un = Object.prototype.toString,
    kr = (e) => Un.call(e),
    wt = (e) => kr(e).slice(8, -1);
  var Le = (e) =>
    qn(e) && e !== "NaN" && e[0] !== "-" && "" + parseInt(e, 10) === e;
  var $e = (e) => {
      let t = Object.create(null);
      return (r) => t[r] || (t[r] = e(r));
    },
    Wn = /-(\w)/g,
    ps = $e((e) => e.replace(Wn, (t, r) => (r ? r.toUpperCase() : ""))),
    Gn = /\B([A-Z])/g,
    ms = $e((e) => e.replace(Gn, "-$1").toLowerCase()),
    Et = $e((e) => e.charAt(0).toUpperCase() + e.slice(1)),
    hs = $e((e) => (e ? `on${Et(e)}` : "")),
    vt = (e, t) => e !== t && (e === e || t === t);
  var St = new WeakMap(),
    ge = [],
    I,
    J = Symbol("iterate"),
    At = Symbol("Map key iterate");
  function Jn(e) {
    return e && e._isEffect === !0;
  }
  function Kr(e, t = Dr) {
    Jn(e) && (e = e.raw);
    let r = Xn(e, t);
    return t.lazy || r(), r;
  }
  function zr(e) {
    e.active &&
      (Hr(e), e.options.onStop && e.options.onStop(), (e.active = !1));
  }
  var Yn = 0;
  function Xn(e, t) {
    let r = function () {
      if (!r.active) return e();
      if (!ge.includes(r)) {
        Hr(r);
        try {
          return Qn(), ge.push(r), (I = r), e();
        } finally {
          ge.pop(), Vr(), (I = ge[ge.length - 1]);
        }
      }
    };
    return (
      (r.id = Yn++),
      (r.allowRecurse = !!t.allowRecurse),
      (r._isEffect = !0),
      (r.active = !0),
      (r.raw = e),
      (r.deps = []),
      (r.options = t),
      r
    );
  }
  function Hr(e) {
    let { deps: t } = e;
    if (t.length) {
      for (let r = 0; r < t.length; r++) t[r].delete(e);
      t.length = 0;
    }
  }
  var ne = !0,
    Ct = [];
  function Zn() {
    Ct.push(ne), (ne = !1);
  }
  function Qn() {
    Ct.push(ne), (ne = !0);
  }
  function Vr() {
    let e = Ct.pop();
    ne = e === void 0 ? !0 : e;
  }
  function M(e, t, r) {
    if (!ne || I === void 0) return;
    let n = St.get(e);
    n || St.set(e, (n = new Map()));
    let i = n.get(r);
    i || n.set(r, (i = new Set())),
      i.has(I) ||
        (i.add(I),
        I.deps.push(i),
        I.options.onTrack &&
          I.options.onTrack({ effect: I, target: e, type: t, key: r }));
  }
  function z(e, t, r, n, i, o) {
    let s = St.get(e);
    if (!s) return;
    let a = new Set(),
      c = (u) => {
        u &&
          u.forEach((d) => {
            (d !== I || d.allowRecurse) && a.add(d);
          });
      };
    if (t === "clear") s.forEach(c);
    else if (r === "length" && B(e))
      s.forEach((u, d) => {
        (d === "length" || d >= n) && c(u);
      });
    else
      switch ((r !== void 0 && c(s.get(r)), t)) {
        case "add":
          B(e)
            ? Le(r) && c(s.get("length"))
            : (c(s.get(J)), re(e) && c(s.get(At)));
          break;
        case "delete":
          B(e) || (c(s.get(J)), re(e) && c(s.get(At)));
          break;
        case "set":
          re(e) && c(s.get(J));
          break;
      }
    let l = (u) => {
      u.options.onTrigger &&
        u.options.onTrigger({
          effect: u,
          target: e,
          key: r,
          type: t,
          newValue: n,
          oldValue: i,
          oldTarget: o,
        }),
        u.options.scheduler ? u.options.scheduler(u) : u();
    };
    a.forEach(l);
  }
  var ei = bt("__proto__,__v_isRef,__isVue"),
    qr = new Set(
      Object.getOwnPropertyNames(Symbol)
        .map((e) => Symbol[e])
        .filter(ke)
    ),
    ti = Ur();
  var ri = Ur(!0);
  var Lr = ni();
  function ni() {
    let e = {};
    return (
      ["includes", "indexOf", "lastIndexOf"].forEach((t) => {
        e[t] = function (...r) {
          let n = _(this);
          for (let o = 0, s = this.length; o < s; o++) M(n, "get", o + "");
          let i = n[t](...r);
          return i === -1 || i === !1 ? n[t](...r.map(_)) : i;
        };
      }),
      ["push", "pop", "shift", "unshift", "splice"].forEach((t) => {
        e[t] = function (...r) {
          Zn();
          let n = _(this)[t].apply(this, r);
          return Vr(), n;
        };
      }),
      e
    );
  }
  function Ur(e = !1, t = !1) {
    return function (n, i, o) {
      if (i === "__v_isReactive") return !e;
      if (i === "__v_isReadonly") return e;
      if (i === "__v_raw" && o === (e ? (t ? yi : Yr) : t ? xi : Jr).get(n))
        return n;
      let s = B(n);
      if (!e && s && he(Lr, i)) return Reflect.get(Lr, i, o);
      let a = Reflect.get(n, i, o);
      return (ke(i) ? qr.has(i) : ei(i)) || (e || M(n, "get", i), t)
        ? a
        : Ot(a)
        ? !s || !Le(i)
          ? a.value
          : a
        : _e(a)
        ? e
          ? Xr(a)
          : Ve(a)
        : a;
    };
  }
  var ii = oi();
  function oi(e = !1) {
    return function (r, n, i, o) {
      let s = r[n];
      if (!e && ((i = _(i)), (s = _(s)), !B(r) && Ot(s) && !Ot(i)))
        return (s.value = i), !0;
      let a = B(r) && Le(n) ? Number(n) < r.length : he(r, n),
        c = Reflect.set(r, n, i, o);
      return (
        r === _(o) &&
          (a ? vt(i, s) && z(r, "set", n, i, s) : z(r, "add", n, i)),
        c
      );
    };
  }
  function si(e, t) {
    let r = he(e, t),
      n = e[t],
      i = Reflect.deleteProperty(e, t);
    return i && r && z(e, "delete", t, void 0, n), i;
  }
  function ai(e, t) {
    let r = Reflect.has(e, t);
    return (!ke(t) || !qr.has(t)) && M(e, "has", t), r;
  }
  function ci(e) {
    return M(e, "iterate", B(e) ? "length" : J), Reflect.ownKeys(e);
  }
  var li = { get: ti, set: ii, deleteProperty: si, has: ai, ownKeys: ci },
    ui = {
      get: ri,
      set(e, t) {
        return (
          console.warn(
            `Set operation on key "${String(t)}" failed: target is readonly.`,
            e
          ),
          !0
        );
      },
      deleteProperty(e, t) {
        return (
          console.warn(
            `Delete operation on key "${String(
              t
            )}" failed: target is readonly.`,
            e
          ),
          !0
        );
      },
    };
  var Tt = (e) => (_e(e) ? Ve(e) : e),
    Mt = (e) => (_e(e) ? Xr(e) : e),
    Rt = (e) => e,
    He = (e) => Reflect.getPrototypeOf(e);
  function Fe(e, t, r = !1, n = !1) {
    e = e.__v_raw;
    let i = _(e),
      o = _(t);
    t !== o && !r && M(i, "get", t), !r && M(i, "get", o);
    let { has: s } = He(i),
      a = n ? Rt : r ? Mt : Tt;
    if (s.call(i, t)) return a(e.get(t));
    if (s.call(i, o)) return a(e.get(o));
    e !== i && e.get(t);
  }
  function je(e, t = !1) {
    let r = this.__v_raw,
      n = _(r),
      i = _(e);
    return (
      e !== i && !t && M(n, "has", e),
      !t && M(n, "has", i),
      e === i ? r.has(e) : r.has(e) || r.has(i)
    );
  }
  function Be(e, t = !1) {
    return (
      (e = e.__v_raw), !t && M(_(e), "iterate", J), Reflect.get(e, "size", e)
    );
  }
  function $r(e) {
    e = _(e);
    let t = _(this);
    return He(t).has.call(t, e) || (t.add(e), z(t, "add", e, e)), this;
  }
  function Fr(e, t) {
    t = _(t);
    let r = _(this),
      { has: n, get: i } = He(r),
      o = n.call(r, e);
    o ? Gr(r, n, e) : ((e = _(e)), (o = n.call(r, e)));
    let s = i.call(r, e);
    return (
      r.set(e, t),
      o ? vt(t, s) && z(r, "set", e, t, s) : z(r, "add", e, t),
      this
    );
  }
  function jr(e) {
    let t = _(this),
      { has: r, get: n } = He(t),
      i = r.call(t, e);
    i ? Gr(t, r, e) : ((e = _(e)), (i = r.call(t, e)));
    let o = n ? n.call(t, e) : void 0,
      s = t.delete(e);
    return i && z(t, "delete", e, void 0, o), s;
  }
  function Br() {
    let e = _(this),
      t = e.size !== 0,
      r = re(e) ? new Map(e) : new Set(e),
      n = e.clear();
    return t && z(e, "clear", void 0, void 0, r), n;
  }
  function Ke(e, t) {
    return function (n, i) {
      let o = this,
        s = o.__v_raw,
        a = _(s),
        c = t ? Rt : e ? Mt : Tt;
      return (
        !e && M(a, "iterate", J), s.forEach((l, u) => n.call(i, c(l), c(u), o))
      );
    };
  }
  function ze(e, t, r) {
    return function (...n) {
      let i = this.__v_raw,
        o = _(i),
        s = re(o),
        a = e === "entries" || (e === Symbol.iterator && s),
        c = e === "keys" && s,
        l = i[e](...n),
        u = r ? Rt : t ? Mt : Tt;
      return (
        !t && M(o, "iterate", c ? At : J),
        {
          next() {
            let { value: d, done: m } = l.next();
            return m
              ? { value: d, done: m }
              : { value: a ? [u(d[0]), u(d[1])] : u(d), done: m };
          },
          [Symbol.iterator]() {
            return this;
          },
        }
      );
    };
  }
  function K(e) {
    return function (...t) {
      {
        let r = t[0] ? `on key "${t[0]}" ` : "";
        console.warn(
          `${Et(e)} operation ${r}failed: target is readonly.`,
          _(this)
        );
      }
      return e === "delete" ? !1 : this;
    };
  }
  function fi() {
    let e = {
        get(o) {
          return Fe(this, o);
        },
        get size() {
          return Be(this);
        },
        has: je,
        add: $r,
        set: Fr,
        delete: jr,
        clear: Br,
        forEach: Ke(!1, !1),
      },
      t = {
        get(o) {
          return Fe(this, o, !1, !0);
        },
        get size() {
          return Be(this);
        },
        has: je,
        add: $r,
        set: Fr,
        delete: jr,
        clear: Br,
        forEach: Ke(!1, !0),
      },
      r = {
        get(o) {
          return Fe(this, o, !0);
        },
        get size() {
          return Be(this, !0);
        },
        has(o) {
          return je.call(this, o, !0);
        },
        add: K("add"),
        set: K("set"),
        delete: K("delete"),
        clear: K("clear"),
        forEach: Ke(!0, !1),
      },
      n = {
        get(o) {
          return Fe(this, o, !0, !0);
        },
        get size() {
          return Be(this, !0);
        },
        has(o) {
          return je.call(this, o, !0);
        },
        add: K("add"),
        set: K("set"),
        delete: K("delete"),
        clear: K("clear"),
        forEach: Ke(!0, !0),
      };
    return (
      ["keys", "values", "entries", Symbol.iterator].forEach((o) => {
        (e[o] = ze(o, !1, !1)),
          (r[o] = ze(o, !0, !1)),
          (t[o] = ze(o, !1, !0)),
          (n[o] = ze(o, !0, !0));
      }),
      [e, r, t, n]
    );
  }
  var [di, pi, mi, hi] = fi();
  function Wr(e, t) {
    let r = t ? (e ? hi : mi) : e ? pi : di;
    return (n, i, o) =>
      i === "__v_isReactive"
        ? !e
        : i === "__v_isReadonly"
        ? e
        : i === "__v_raw"
        ? n
        : Reflect.get(he(r, i) && i in n ? r : n, i, o);
  }
  var _i = { get: Wr(!1, !1) };
  var gi = { get: Wr(!0, !1) };
  function Gr(e, t, r) {
    let n = _(r);
    if (n !== r && t.call(e, n)) {
      let i = wt(e);
      console.warn(
        `Reactive ${i} contains both the raw and reactive versions of the same object${
          i === "Map" ? " as keys" : ""
        }, which can lead to inconsistencies. Avoid differentiating between the raw and reactive versions of an object and only use the reactive version if possible.`
      );
    }
  }
  var Jr = new WeakMap(),
    xi = new WeakMap(),
    Yr = new WeakMap(),
    yi = new WeakMap();
  function bi(e) {
    switch (e) {
      case "Object":
      case "Array":
        return 1;
      case "Map":
      case "Set":
      case "WeakMap":
      case "WeakSet":
        return 2;
      default:
        return 0;
    }
  }
  function wi(e) {
    return e.__v_skip || !Object.isExtensible(e) ? 0 : bi(wt(e));
  }
  function Ve(e) {
    return e && e.__v_isReadonly ? e : Zr(e, !1, li, _i, Jr);
  }
  function Xr(e) {
    return Zr(e, !0, ui, gi, Yr);
  }
  function Zr(e, t, r, n, i) {
    if (!_e(e))
      return console.warn(`value cannot be made reactive: ${String(e)}`), e;
    if (e.__v_raw && !(t && e.__v_isReactive)) return e;
    let o = i.get(e);
    if (o) return o;
    let s = wi(e);
    if (s === 0) return e;
    let a = new Proxy(e, s === 2 ? n : r);
    return i.set(e, a), a;
  }
  function _(e) {
    return (e && _(e.__v_raw)) || e;
  }
  function Ot(e) {
    return Boolean(e && e.__v_isRef === !0);
  }
  y("nextTick", () => ee);
  y("dispatch", (e) => q.bind(q, e));
  y("watch", (e, { evaluateLater: t, effect: r }) => (n, i) => {
    let o = t(n),
      s = !0,
      a,
      c = r(() =>
        o((l) => {
          JSON.stringify(l),
            s
              ? (a = l)
              : queueMicrotask(() => {
                  i(l, a), (a = l);
                }),
            (s = !1);
        })
      );
    e._x_effects.delete(c);
  });
  y("store", Cr);
  y("data", (e) => Ee(e));
  y("root", (e) => U(e));
  y(
    "refs",
    (e) => (e._x_refs_proxy || (e._x_refs_proxy = F(Ei(e))), e._x_refs_proxy)
  );
  function Ei(e) {
    let t = [],
      r = e;
    for (; r; ) r._x_refs && t.push(r._x_refs), (r = r.parentNode);
    return t;
  }
  var Nt = {};
  function It(e) {
    return Nt[e] || (Nt[e] = 0), ++Nt[e];
  }
  function Qr(e, t) {
    return Q(e, (r) => {
      if (r._x_ids && r._x_ids[t]) return !0;
    });
  }
  function en(e, t) {
    e._x_ids || (e._x_ids = {}), e._x_ids[t] || (e._x_ids[t] = It(t));
  }
  y("id", (e) => (t, r = null) => {
    let n = Qr(e, t),
      i = n ? n._x_ids[t] : It(t);
    return r ? `${t}-${i}-${r}` : `${t}-${i}`;
  });
  y("el", (e) => e);
  tn("Focus", "focus", "focus");
  tn("Persist", "persist", "persist");
  function tn(e, t, r) {
    y(t, (n) =>
      S(
        `You can't use [$${directiveName}] without first installing the "${e}" plugin here: https://alpinejs.dev/plugins/${r}`,
        n
      )
    );
  }
  function rn({ get: e, set: t }, { get: r, set: n }) {
    let i = !0,
      o,
      s,
      a,
      c,
      l = P(() => {
        let u, d;
        i
          ? ((u = e()), n(u), (d = r()), (i = !1))
          : ((u = e()),
            (d = r()),
            (a = JSON.stringify(u)),
            (c = JSON.stringify(d)),
            a !== o ? ((d = r()), n(u), (d = u)) : (t(d), (u = d))),
          (o = JSON.stringify(u)),
          (s = JSON.stringify(d));
      });
    return () => {
      L(l);
    };
  }
  p(
    "modelable",
    (e, { expression: t }, { effect: r, evaluateLater: n, cleanup: i }) => {
      let o = n(t),
        s = () => {
          let u;
          return o((d) => (u = d)), u;
        },
        a = n(`${t} = __placeholder`),
        c = (u) => a(() => {}, { scope: { __placeholder: u } }),
        l = s();
      c(l),
        queueMicrotask(() => {
          if (!e._x_model) return;
          e._x_removeModelListeners.default();
          let u = e._x_model.get,
            d = e._x_model.set,
            m = rn(
              {
                get() {
                  return u();
                },
                set(w) {
                  d(w);
                },
              },
              {
                get() {
                  return s();
                },
                set(w) {
                  c(w);
                },
              }
            );
          i(m);
        });
    }
  );
  var vi = document.createElement("div");
  p("teleport", (e, { modifiers: t, expression: r }, { cleanup: n }) => {
    e.tagName.toLowerCase() !== "template" &&
      S("x-teleport can only be used on a <template> tag", e);
    let i = N(
      () => document.querySelector(r),
      () => vi
    )();
    i || S(`Cannot find x-teleport element for selector: "${r}"`);
    let o = e.content.cloneNode(!0).firstElementChild;
    (e._x_teleport = o),
      (o._x_teleportBack = e),
      e._x_forwardEvents &&
        e._x_forwardEvents.forEach((s) => {
          o.addEventListener(s, (a) => {
            a.stopPropagation(), e.dispatchEvent(new a.constructor(a.type, a));
          });
        }),
      R(o, {}, e),
      h(() => {
        t.includes("prepend")
          ? i.parentNode.insertBefore(o, i)
          : t.includes("append")
          ? i.parentNode.insertBefore(o, i.nextSibling)
          : i.appendChild(o),
          v(o),
          (o._x_ignore = !0);
      }),
      n(() => o.remove());
  });
  var nn = () => {};
  nn.inline = (e, { modifiers: t }, { cleanup: r }) => {
    t.includes("self") ? (e._x_ignoreSelf = !0) : (e._x_ignore = !0),
      r(() => {
        t.includes("self") ? delete e._x_ignoreSelf : delete e._x_ignore;
      });
  };
  p("ignore", nn);
  p("effect", (e, { expression: t }, { effect: r }) => r(x(e, t)));
  function ie(e, t, r, n) {
    let i = e,
      o = (c) => n(c),
      s = {},
      a = (c, l) => (u) => l(c, u);
    if (
      (r.includes("dot") && (t = Si(t)),
      r.includes("camel") && (t = Ai(t)),
      r.includes("passive") && (s.passive = !0),
      r.includes("capture") && (s.capture = !0),
      r.includes("window") && (i = window),
      r.includes("document") && (i = document),
      r.includes("debounce"))
    ) {
      let c = r[r.indexOf("debounce") + 1] || "invalid-wait",
        l = qe(c.split("ms")[0]) ? Number(c.split("ms")[0]) : 250;
      o = Pe(o, l);
    }
    if (r.includes("throttle")) {
      let c = r[r.indexOf("throttle") + 1] || "invalid-wait",
        l = qe(c.split("ms")[0]) ? Number(c.split("ms")[0]) : 250;
      o = De(o, l);
    }
    return (
      r.includes("prevent") &&
        (o = a(o, (c, l) => {
          l.preventDefault(), c(l);
        })),
      r.includes("stop") &&
        (o = a(o, (c, l) => {
          l.stopPropagation(), c(l);
        })),
      r.includes("self") &&
        (o = a(o, (c, l) => {
          l.target === e && c(l);
        })),
      (r.includes("away") || r.includes("outside")) &&
        ((i = document),
        (o = a(o, (c, l) => {
          e.contains(l.target) ||
            (l.target.isConnected !== !1 &&
              ((e.offsetWidth < 1 && e.offsetHeight < 1) ||
                (e._x_isShown !== !1 && c(l))));
        }))),
      r.includes("once") &&
        (o = a(o, (c, l) => {
          c(l), i.removeEventListener(t, o, s);
        })),
      (o = a(o, (c, l) => {
        (Ci(t) && Ti(l, r)) || c(l);
      })),
      i.addEventListener(t, o, s),
      () => {
        i.removeEventListener(t, o, s);
      }
    );
  }
  function Si(e) {
    return e.replace(/-/g, ".");
  }
  function Ai(e) {
    return e.toLowerCase().replace(/-(\w)/g, (t, r) => r.toUpperCase());
  }
  function qe(e) {
    return !Array.isArray(e) && !isNaN(e);
  }
  function Oi(e) {
    return [" ", "_"].includes(e)
      ? e
      : e
          .replace(/([a-z])([A-Z])/g, "$1-$2")
          .replace(/[_\s]/, "-")
          .toLowerCase();
  }
  function Ci(e) {
    return ["keydown", "keyup"].includes(e);
  }
  function Ti(e, t) {
    let r = t.filter(
      (o) =>
        !["window", "document", "prevent", "stop", "once", "capture"].includes(
          o
        )
    );
    if (r.includes("debounce")) {
      let o = r.indexOf("debounce");
      r.splice(o, qe((r[o + 1] || "invalid-wait").split("ms")[0]) ? 2 : 1);
    }
    if (r.includes("throttle")) {
      let o = r.indexOf("throttle");
      r.splice(o, qe((r[o + 1] || "invalid-wait").split("ms")[0]) ? 2 : 1);
    }
    if (r.length === 0 || (r.length === 1 && on(e.key).includes(r[0])))
      return !1;
    let i = ["ctrl", "shift", "alt", "meta", "cmd", "super"].filter((o) =>
      r.includes(o)
    );
    return (
      (r = r.filter((o) => !i.includes(o))),
      !(
        i.length > 0 &&
        i.filter(
          (s) => ((s === "cmd" || s === "super") && (s = "meta"), e[`${s}Key`])
        ).length === i.length &&
        on(e.key).includes(r[0])
      )
    );
  }
  function on(e) {
    if (!e) return [];
    e = Oi(e);
    let t = {
      ctrl: "control",
      slash: "/",
      space: " ",
      spacebar: " ",
      cmd: "meta",
      esc: "escape",
      up: "arrow-up",
      down: "arrow-down",
      left: "arrow-left",
      right: "arrow-right",
      period: ".",
      equal: "=",
      minus: "-",
      underscore: "_",
    };
    return (
      (t[e] = e),
      Object.keys(t)
        .map((r) => {
          if (t[r] === e) return r;
        })
        .filter((r) => r)
    );
  }
  p(
    "model",
    (e, { modifiers: t, expression: r }, { effect: n, cleanup: i }) => {
      let o = e;
      t.includes("parent") && (o = e.parentNode);
      let s = x(o, r),
        a;
      typeof r == "string"
        ? (a = x(o, `${r} = __placeholder`))
        : typeof r == "function" && typeof r() == "string"
        ? (a = x(o, `${r()} = __placeholder`))
        : (a = () => {});
      let c = () => {
          let m;
          return s((w) => (m = w)), sn(m) ? m.get() : m;
        },
        l = (m) => {
          let w;
          s((k) => (w = k)),
            sn(w) ? w.set(m) : a(() => {}, { scope: { __placeholder: m } });
        };
      typeof r == "string" &&
        e.type === "radio" &&
        h(() => {
          e.hasAttribute("name") || e.setAttribute("name", r);
        });
      var u =
        e.tagName.toLowerCase() === "select" ||
        ["checkbox", "radio"].includes(e.type) ||
        t.includes("lazy")
          ? "change"
          : "input";
      let d = te
        ? () => {}
        : ie(e, u, t, (m) => {
            l(Mi(e, t, m, c()));
          });
      if (
        (t.includes("fill") &&
          [null, ""].includes(c()) &&
          e.dispatchEvent(new Event(u, {})),
        e._x_removeModelListeners || (e._x_removeModelListeners = {}),
        (e._x_removeModelListeners.default = d),
        i(() => e._x_removeModelListeners.default()),
        e.form)
      ) {
        let m = ie(e.form, "reset", [], (w) => {
          ee(() => e._x_model && e._x_model.set(e.value));
        });
        i(() => m());
      }
      (e._x_model = {
        get() {
          return c();
        },
        set(m) {
          l(m);
        },
      }),
        (e._x_forceModelUpdate = (m) => {
          (m = m === void 0 ? c() : m),
            m === void 0 && typeof r == "string" && r.match(/\./) && (m = ""),
            (window.fromModel = !0),
            h(() => me(e, "value", m)),
            delete window.fromModel;
        }),
        n(() => {
          let m = c();
          (t.includes("unintrusive") && document.activeElement.isSameNode(e)) ||
            e._x_forceModelUpdate(m);
        });
    }
  );
  function Mi(e, t, r, n) {
    return h(() => {
      if (r instanceof CustomEvent && r.detail !== void 0)
        return r.detail ?? r.target.value;
      if (e.type === "checkbox")
        if (Array.isArray(n)) {
          let i = t.includes("number") ? Pt(r.target.value) : r.target.value;
          return r.target.checked ? n.concat([i]) : n.filter((o) => !Ri(o, i));
        } else return r.target.checked;
      else {
        if (e.tagName.toLowerCase() === "select" && e.multiple)
          return t.includes("number")
            ? Array.from(r.target.selectedOptions).map((i) => {
                let o = i.value || i.text;
                return Pt(o);
              })
            : Array.from(r.target.selectedOptions).map(
                (i) => i.value || i.text
              );
        {
          let i = r.target.value;
          return t.includes("number")
            ? Pt(i)
            : t.includes("trim")
            ? i.trim()
            : i;
        }
      }
    });
  }
  function Pt(e) {
    let t = e ? parseFloat(e) : null;
    return Ni(t) ? t : e;
  }
  function Ri(e, t) {
    return e == t;
  }
  function Ni(e) {
    return !Array.isArray(e) && !isNaN(e);
  }
  function sn(e) {
    return (
      e !== null &&
      typeof e == "object" &&
      typeof e.get == "function" &&
      typeof e.set == "function"
    );
  }
  p("cloak", (e) =>
    queueMicrotask(() => h(() => e.removeAttribute(O("cloak"))))
  );
  Re(() => `[${O("init")}]`);
  p(
    "init",
    N((e, { expression: t }, { evaluate: r }) =>
      typeof t == "string" ? !!t.trim() && r(t, {}, !1) : r(t, {}, !1)
    )
  );
  p("text", (e, { expression: t }, { effect: r, evaluateLater: n }) => {
    let i = n(t);
    r(() => {
      i((o) => {
        h(() => {
          e.textContent = o;
        });
      });
    });
  });
  p("html", (e, { expression: t }, { effect: r, evaluateLater: n }) => {
    let i = n(t);
    r(() => {
      i((o) => {
        h(() => {
          (e.innerHTML = o),
            (e._x_ignoreSelf = !0),
            v(e),
            delete e._x_ignoreSelf;
        });
      });
    });
  });
  Z(Ce(":", Te(O("bind:"))));
  p(
    "bind",
    (
      e,
      { value: t, modifiers: r, expression: n, original: i },
      { effect: o }
    ) => {
      if (!t) {
        let a = {};
        Rr(a),
          x(e, n)(
            (l) => {
              yt(e, l, i);
            },
            { scope: a }
          );
        return;
      }
      if (t === "key") return Ii(e, n);
      let s = x(e, n);
      o(() =>
        s((a) => {
          a === void 0 && typeof n == "string" && n.match(/\./) && (a = ""),
            h(() => me(e, t, a, r));
        })
      );
    }
  );
  function Ii(e, t) {
    e._x_keyExpression = t;
  }
  Me(() => `[${O("data")}]`);
  p(
    "data",
    N((e, { expression: t }, { cleanup: r }) => {
      t = t === "" ? "{}" : t;
      let n = {};
      ce(n, e);
      let i = {};
      Pr(i, n);
      let o = D(e, t, { scope: i });
      (o === void 0 || o === !0) && (o = {}), ce(o, e);
      let s = T(o);
      ve(s);
      let a = R(e, s);
      s.init && D(e, s.init),
        r(() => {
          s.destroy && D(e, s.destroy), a();
        });
    })
  );
  p("show", (e, { modifiers: t, expression: r }, { effect: n }) => {
    let i = x(e, r);
    e._x_doHide ||
      (e._x_doHide = () => {
        h(() => {
          e.style.setProperty(
            "display",
            "none",
            t.includes("important") ? "important" : void 0
          );
        });
      }),
      e._x_doShow ||
        (e._x_doShow = () => {
          h(() => {
            e.style.length === 1 && e.style.display === "none"
              ? e.removeAttribute("style")
              : e.style.removeProperty("display");
          });
        });
    let o = () => {
        e._x_doHide(), (e._x_isShown = !1);
      },
      s = () => {
        e._x_doShow(), (e._x_isShown = !0);
      },
      a = () => setTimeout(s),
      c = de(
        (d) => (d ? s() : o()),
        (d) => {
          typeof e._x_toggleAndCascadeWithTransitions == "function"
            ? e._x_toggleAndCascadeWithTransitions(e, d, s, o)
            : d
            ? a()
            : o();
        }
      ),
      l,
      u = !0;
    n(() =>
      i((d) => {
        (!u && d === l) ||
          (t.includes("immediate") && (d ? a() : o()), c(d), (l = d), (u = !1));
      })
    );
  });
  p("for", (e, { expression: t }, { effect: r, cleanup: n }) => {
    let i = Di(t),
      o = x(e, i.items),
      s = x(e, e._x_keyExpression || "index");
    (e._x_prevKeys = []),
      (e._x_lookup = {}),
      r(() => Pi(e, i, o, s)),
      n(() => {
        Object.values(e._x_lookup).forEach((a) => a.remove()),
          delete e._x_prevKeys,
          delete e._x_lookup;
      });
  });
  function Pi(e, t, r, n) {
    let i = (s) => typeof s == "object" && !Array.isArray(s),
      o = e;
    r((s) => {
      ki(s) && s >= 0 && (s = Array.from(Array(s).keys(), (f) => f + 1)),
        s === void 0 && (s = []);
      let a = e._x_lookup,
        c = e._x_prevKeys,
        l = [],
        u = [];
      if (i(s))
        s = Object.entries(s).map(([f, g]) => {
          let b = an(t, g, f, s);
          n((E) => u.push(E), { scope: { index: f, ...b } }), l.push(b);
        });
      else
        for (let f = 0; f < s.length; f++) {
          let g = an(t, s[f], f, s);
          n((b) => u.push(b), { scope: { index: f, ...g } }), l.push(g);
        }
      let d = [],
        m = [],
        w = [],
        k = [];
      for (let f = 0; f < c.length; f++) {
        let g = c[f];
        u.indexOf(g) === -1 && w.push(g);
      }
      c = c.filter((f) => !w.includes(f));
      let xe = "template";
      for (let f = 0; f < u.length; f++) {
        let g = u[f],
          b = c.indexOf(g);
        if (b === -1) c.splice(f, 0, g), d.push([xe, f]);
        else if (b !== f) {
          let E = c.splice(f, 1)[0],
            A = c.splice(b - 1, 1)[0];
          c.splice(f, 0, A), c.splice(b, 0, E), m.push([E, A]);
        } else k.push(g);
        xe = g;
      }
      for (let f = 0; f < w.length; f++) {
        let g = w[f];
        a[g]._x_effects && a[g]._x_effects.forEach(ye),
          a[g].remove(),
          (a[g] = null),
          delete a[g];
      }
      for (let f = 0; f < m.length; f++) {
        let [g, b] = m[f],
          E = a[g],
          A = a[b],
          Y = document.createElement("div");
        h(() => {
          A || S('x-for ":key" is undefined or invalid', o),
            A.after(Y),
            E.after(A),
            A._x_currentIfEl && A.after(A._x_currentIfEl),
            Y.before(E),
            E._x_currentIfEl && E.after(E._x_currentIfEl),
            Y.remove();
        }),
          A._x_refreshXForScope(l[u.indexOf(b)]);
      }
      for (let f = 0; f < d.length; f++) {
        let [g, b] = d[f],
          E = g === "template" ? o : a[g];
        E._x_currentIfEl && (E = E._x_currentIfEl);
        let A = l[b],
          Y = u[b],
          oe = document.importNode(o.content, !0).firstElementChild,
          kt = T(A);
        R(oe, kt, o),
          (oe._x_refreshXForScope = (ln) => {
            Object.entries(ln).forEach(([un, fn]) => {
              kt[un] = fn;
            });
          }),
          h(() => {
            E.after(oe), v(oe);
          }),
          typeof Y == "object" &&
            S(
              "x-for key cannot be an object, it must be a string or an integer",
              o
            ),
          (a[Y] = oe);
      }
      for (let f = 0; f < k.length; f++)
        a[k[f]]._x_refreshXForScope(l[u.indexOf(k[f])]);
      o._x_prevKeys = u;
    });
  }
  function Di(e) {
    let t = /,([^,\}\]]*)(?:,([^,\}\]]*))?$/,
      r = /^\s*\(|\)\s*$/g,
      n = /([\s\S]*?)\s+(?:in|of)\s+([\s\S]*)/,
      i = e.match(n);
    if (!i) return;
    let o = {};
    o.items = i[2].trim();
    let s = i[1].replace(r, "").trim(),
      a = s.match(t);
    return (
      a
        ? ((o.item = s.replace(t, "").trim()),
          (o.index = a[1].trim()),
          a[2] && (o.collection = a[2].trim()))
        : (o.item = s),
      o
    );
  }
  function an(e, t, r, n) {
    let i = {};
    return (
      /^\[.*\]$/.test(e.item) && Array.isArray(t)
        ? e.item
            .replace("[", "")
            .replace("]", "")
            .split(",")
            .map((s) => s.trim())
            .forEach((s, a) => {
              i[s] = t[a];
            })
        : /^\{.*\}$/.test(e.item) && !Array.isArray(t) && typeof t == "object"
        ? e.item
            .replace("{", "")
            .replace("}", "")
            .split(",")
            .map((s) => s.trim())
            .forEach((s) => {
              i[s] = t[s];
            })
        : (i[e.item] = t),
      e.index && (i[e.index] = r),
      e.collection && (i[e.collection] = n),
      i
    );
  }
  function ki(e) {
    return !Array.isArray(e) && !isNaN(e);
  }
  function cn() {}
  cn.inline = (e, { expression: t }, { cleanup: r }) => {
    let n = U(e);
    n._x_refs || (n._x_refs = {}),
      (n._x_refs[t] = e),
      r(() => delete n._x_refs[t]);
  };
  p("ref", cn);
  p("if", (e, { expression: t }, { effect: r, cleanup: n }) => {
    let i = x(e, t),
      o = () => {
        if (e._x_currentIfEl) return e._x_currentIfEl;
        let a = e.content.cloneNode(!0).firstElementChild;
        return (
          R(a, {}, e),
          h(() => {
            e.after(a), v(a);
          }),
          (e._x_currentIfEl = a),
          (e._x_undoIf = () => {
            C(a, (c) => {
              c._x_effects && c._x_effects.forEach(ye);
            }),
              a.remove(),
              delete e._x_currentIfEl;
          }),
          a
        );
      },
      s = () => {
        e._x_undoIf && (e._x_undoIf(), delete e._x_undoIf);
      };
    r(() =>
      i((a) => {
        a ? o() : s();
      })
    ),
      n(() => e._x_undoIf && e._x_undoIf());
  });
  p("id", (e, { expression: t }, { evaluate: r }) => {
    r(t).forEach((i) => en(e, i));
  });
  Z(Ce("@", Te(O("on:"))));
  p(
    "on",
    N((e, { value: t, modifiers: r, expression: n }, { cleanup: i }) => {
      let o = n ? x(e, n) : () => {};
      e.tagName.toLowerCase() === "template" &&
        (e._x_forwardEvents || (e._x_forwardEvents = []),
        e._x_forwardEvents.includes(t) || e._x_forwardEvents.push(t));
      let s = ie(e, t, r, (a) => {
        o(() => {}, { scope: { $event: a }, params: [a] });
      });
      i(() => s());
    })
  );
  Ue("Collapse", "collapse", "collapse");
  Ue("Intersect", "intersect", "intersect");
  Ue("Focus", "trap", "focus");
  Ue("Mask", "mask", "mask");
  function Ue(e, t, r) {
    p(t, (n) =>
      S(
        `You can't use [x-${t}] without first installing the "${e}" plugin here: https://alpinejs.dev/plugins/${r}`,
        n
      )
    );
  }
  j.setEvaluator(lt);
  j.setReactivityEngine({ reactive: Ve, effect: Kr, release: zr, raw: _ });
  var Dt = j;
  window.Alpine = Dt;
  queueMicrotask(() => {
    Dt.start();
  });
})();
