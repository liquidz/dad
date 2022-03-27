# render

Render a template string with a data

Examples
```clojure
(render "hello" {}) ;; => "hello"

(render "hello {{msg}}" {:msg "world"}) ;; => "hello world"

(render "hello {{not-found}}" {:msg "world"}) ;; => "hello {{not-found}}"
```